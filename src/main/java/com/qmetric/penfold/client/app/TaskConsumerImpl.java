package com.qmetric.penfold.client.app;

import com.qmetric.penfold.client.app.support.Interval;
import com.qmetric.penfold.client.app.support.ShutdownProcedure;
import com.qmetric.penfold.client.domain.services.Consumer;
import com.qmetric.penfold.client.domain.services.TaskConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

public class TaskConsumerImpl implements TaskConsumer
{
    private static final Logger LOG = LoggerFactory.getLogger(TaskConsumerImpl.class);

    private final Consumer consumer;

    private final Interval interval;

    private final ScheduledExecutorService scheduledExecutorService;

    private final ShutdownProcedure shutdownProcedure;

    public TaskConsumerImpl(final Consumer consumer, final Interval interval)
    {
        this.consumer = consumer;
        this.interval = interval;
        this.scheduledExecutorService = newSingleThreadScheduledExecutor();
        this.shutdownProcedure = new ShutdownProcedure(scheduledExecutorService);
    }

    @Override public void start()
    {
        scheduledExecutorService.scheduleAtFixedRate(this::consume, 0, interval.duration, interval.unit);
        shutdownProcedure.registerShutdownHook();
    }

    public void stop()
    {
        shutdownProcedure.runAndRemoveHook();
    }

    private void consume()
    {
        try
        {
            consumer.consume();
        }
        catch (final Exception | Error e)
        {
            LOG.error("failed to consume", e);
        }
    }
}
