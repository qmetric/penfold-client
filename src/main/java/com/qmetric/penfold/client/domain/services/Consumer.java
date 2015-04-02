package com.qmetric.penfold.client.domain.services;

import com.github.rholder.retry.RetryerBuilder;
import com.google.common.collect.ImmutableList;
import com.qmetric.penfold.client.app.support.Interval;
import com.qmetric.penfold.client.app.support.LocalDateTimeSource;
import com.qmetric.penfold.client.domain.model.QueueId;
import com.qmetric.penfold.client.domain.model.Result;
import com.qmetric.penfold.client.domain.model.Task;
import com.qmetric.penfold.client.domain.model.TaskId;
import com.qmetric.penfold.client.domain.model.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.Callable;

import static com.github.rholder.retry.StopStrategies.stopAfterAttempt;
import static com.github.rholder.retry.WaitStrategies.fixedWait;
import static java.util.concurrent.TimeUnit.SECONDS;
import static com.qmetric.penfold.client.domain.model.CloseResultType.failure;
import static com.qmetric.penfold.client.domain.model.CloseResultType.success;
import static com.qmetric.penfold.client.domain.model.ResultType.FAIL;
import static com.qmetric.penfold.client.domain.model.ResultType.SUCCESS;

public class Consumer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Consumer.class);

    private static final Void VOID = null;

    private static final RetryerBuilder<Void> DEFAULT_RETRY_BUILDER = RetryerBuilder.<Void>newBuilder() //
            .retryIfException() //
            .withWaitStrategy(fixedWait(10, SECONDS)) //
            .withStopStrategy(stopAfterAttempt(2));

    private final RetryerBuilder<Void> retryBuilder;

    private final QueueId queue;

    private final ConsumerFunction function;

    private final Optional<Interval> retryDelay;

    private final TaskQueryService taskQueryService;

    private final TaskStoreService taskStoreService;

    private final LocalDateTimeSource dateTimeSource;

    public Consumer(final QueueId queue, final ConsumerFunction function, final Optional<Interval> retryDelay, final TaskQueryService taskQueryService,
                    final TaskStoreService taskStoreService, final LocalDateTimeSource dateTimeSource)
    {
        this(queue, function, retryDelay, taskQueryService, taskStoreService, dateTimeSource, DEFAULT_RETRY_BUILDER);
    }

    Consumer(final QueueId queue, final ConsumerFunction function, final Optional<Interval> retryDelay, final TaskQueryService taskQueryService,
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

    public void consume()
    {
        final Iterator<Task> tasks = taskQueryService.find(queue, TaskStatus.READY, ImmutableList.of());

        while (tasks.hasNext())
        {
            final Task task = taskStoreService.start(tasks.next());

            final Result result = executeFunction(task);

            applyResultWithRetries(task.id, result);
        }
    }

    private void applyResultWithRetries(final TaskId taskId, final Result result)
    {
        retryCodeBlock(taskId, () -> applyResult(taskId, result));
    }

    private Void applyResult(final TaskId taskId, final Result result)
    {
        final Optional<Task> updatedVersionOfTask = taskQueryService.find(taskId);

        final boolean isTaskStillStarted = updatedVersionOfTask.isPresent() && updatedVersionOfTask.get().status.isStarted();

        if (isTaskStillStarted)
        {
            if (result.type == SUCCESS)
            {
                success(updatedVersionOfTask);
            }
            else if (result.type == FAIL)
            {
                fail(updatedVersionOfTask, result.reason);
            }
            else
            {
                retry(updatedVersionOfTask, result.reason);
            }
        }
        else
        {
            LOGGER.info("task {} already closed or rescheduled - doing nothing", taskId);
        }

        return VOID;
    }

    private void success(final Optional<Task> updatedVersionOfTask)
    {
        taskStoreService.close(updatedVersionOfTask.get(), Optional.of(success), Optional.empty());
    }

    private void fail(final Optional<Task> updatedVersionOfTask, final Optional<String> reason)
    {
        taskStoreService.close(updatedVersionOfTask.get(), Optional.of(failure), reason);
    }

    private void retry(final Optional<Task> updatedVersionOfTask, final Optional<String> reason)
    {
        if (retryDelay.isPresent())
        {
            taskStoreService.reschedule(updatedVersionOfTask.get(), dateTimeSource.now().plusSeconds(retryDelay.get().seconds()), reason);
        }
        else
        {
            taskStoreService.requeue(updatedVersionOfTask.get(), reason);
        }
    }

    private void retryCodeBlock(final TaskId taskId, final Callable<Void> callable)
    {
        try
        {
            retryBuilder.build().call(callable);
        }
        catch (final Exception e)
        {
            LOGGER.error(String.format("task %s processed ok, but could not be closed/rescheduled", taskId), e);
            throw new RuntimeException(e);
        }
    }

    private Result executeFunction(final Task task)
    {
        try
        {
            return function.execute(task);
        }
        catch (Exception e)
        {
            return Result.retry(Optional.empty());
        }
    }
}
