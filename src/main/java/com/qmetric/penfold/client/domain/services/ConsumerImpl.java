package com.qmetric.penfold.client.domain.services;

import com.github.rholder.retry.RetryerBuilder;
import com.google.common.collect.ImmutableList;
import com.qmetric.penfold.client.app.support.LocalDateTimeSource;
import com.qmetric.penfold.client.domain.model.QueueId;
import com.qmetric.penfold.client.domain.model.Reply;
import com.qmetric.penfold.client.domain.model.Task;
import com.qmetric.penfold.client.domain.model.TaskId;
import com.qmetric.penfold.client.domain.model.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.Callable;

import static com.github.rholder.retry.StopStrategies.stopAfterAttempt;
import static com.github.rholder.retry.WaitStrategies.fixedWait;
import static com.qmetric.penfold.client.domain.model.CloseResultType.failure;
import static com.qmetric.penfold.client.domain.model.CloseResultType.success;
import static com.qmetric.penfold.client.domain.model.ReplyType.FAIL;
import static com.qmetric.penfold.client.domain.model.ReplyType.SUCCESS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class ConsumerImpl implements Consumer
{
    private static final Logger LOG = LoggerFactory.getLogger(ConsumerImpl.class);

    private static final Void VOID = null;

    private static final RetryerBuilder<Void> DEFAULT_RETRY_BUILDER = RetryerBuilder.<Void>newBuilder() //
            .retryIfException() //
            .withWaitStrategy(fixedWait(10, SECONDS)) //
            .withStopStrategy(stopAfterAttempt(2));

    private final RetryerBuilder<Void> retryBuilder;

    private final QueueId queue;

    private final ConsumerFunction function;

    private final Optional<Duration> retryDelay;

    private final TaskQueryService taskQueryService;

    private final TaskStoreService taskStoreService;

    private final LocalDateTimeSource dateTimeSource;

    public ConsumerImpl(final QueueId queue, final ConsumerFunction function, final Optional<Duration> retryDelay, final TaskQueryService taskQueryService,
                        final TaskStoreService taskStoreService, final LocalDateTimeSource dateTimeSource)
    {
        this(queue, function, retryDelay, taskQueryService, taskStoreService, dateTimeSource, DEFAULT_RETRY_BUILDER);
    }

    ConsumerImpl(final QueueId queue, final ConsumerFunction function, final Optional<Duration> retryDelay, final TaskQueryService taskQueryService,
                 final TaskStoreService taskStoreService, final LocalDateTimeSource dateTimeSource, final RetryerBuilder<Void> retryBuilder)
    {
        this.queue = queue;
        this.function = function;
        this.retryDelay = retryDelay;
        this.taskQueryService = taskQueryService;
        this.taskStoreService = taskStoreService;
        this.dateTimeSource = dateTimeSource;
        this.retryBuilder = retryBuilder;
    }

    @Override public void consume()
    {
        LOG.info(String.format("consuming from %s queue", queue));

        final Iterator<Task> tasks = taskQueryService.find(queue, TaskStatus.READY, ImmutableList.of());

        while (tasks.hasNext())
        {
            final Task task = taskStoreService.start(tasks.next());

            consumeTask(task);
        }
    }

    @Override public void consumeTask(final Task task)
    {
        final Reply reply = executeFunction(task);

        applyReplyWithRetries(task.id, reply);

        LOG.info(String.format("task %s consumed from %s queue with reply %s", task, queue, reply));
    }

    private void applyReplyWithRetries(final TaskId taskId, final Reply reply)
    {
        retryCodeBlock(taskId, () -> applyReply(taskId, reply));
    }

    private Void applyReply(final TaskId taskId, final Reply reply)
    {
        LOG.debug(String.format("applying consumer reply for task %s %s", taskId, reply));

        final Optional<Task> updatedVersionOfTask = taskQueryService.find(taskId);

        final boolean isTaskStillStarted = updatedVersionOfTask.isPresent() && updatedVersionOfTask.get().status.isStarted();

        if (isTaskStillStarted)
        {
            if (reply.type == SUCCESS)
            {
                success(updatedVersionOfTask.get());
            }
            else if (reply.type == FAIL)
            {
                fail(updatedVersionOfTask.get(), reply.reason);
            }
            else
            {
                retry(updatedVersionOfTask.get(), reply.reason);
            }
        }
        else
        {
            LOG.info("task {} already closed or rescheduled - doing nothing", taskId);
        }

        return VOID;
    }

    private void success(final Task updatedVersionOfTask)
    {
        LOG.debug("closing task {} on success", updatedVersionOfTask.id);

        taskStoreService.close(updatedVersionOfTask, Optional.of(success), Optional.empty());

        LOG.debug("closed task {} on success", updatedVersionOfTask.id);
    }

    private void fail(final Task updatedVersionOfTask, final Optional<String> reason)
    {
        LOG.debug("closing task {} on failure", updatedVersionOfTask.id);

        taskStoreService.close(updatedVersionOfTask, Optional.of(failure), reason);

        LOG.debug("closed task {} on failure", updatedVersionOfTask.id);
    }

    private void retry(final Task updatedVersionOfTask, final Optional<String> reason)
    {
        LOG.debug("rescheduling/requeuing task {} on failure", updatedVersionOfTask.id);

        if (retryDelay.isPresent())
        {
            taskStoreService.reschedule(updatedVersionOfTask, dateTimeSource.now().plusSeconds(retryDelay.get().getSeconds()), reason);
        }
        else
        {
            taskStoreService.requeue(updatedVersionOfTask, reason);
        }

        LOG.debug("rescheduled/requeued task {} on failure", updatedVersionOfTask.id);
    }

    private void retryCodeBlock(final TaskId taskId, final Callable<Void> callable)
    {
        try
        {
            retryBuilder.build().call(callable);
        }
        catch (final Exception e)
        {
            LOG.error(String.format("task %s processed ok, but could not be closed/rescheduled", taskId), e);
            throw new RuntimeException(e);
        }
    }

    private Reply executeFunction(final Task task)
    {
        try
        {
            return function.execute(task);
        }
        catch (Exception e)
        {
            LOG.error(String.format("failed to consume task %s", task), e);
            return Reply.retry(Optional.empty());
        }
    }
}
