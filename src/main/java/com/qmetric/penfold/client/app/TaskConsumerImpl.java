package com.qmetric.penfold.client.app;

import com.qmetric.penfold.client.app.support.ShutdownProcedure;
import com.qmetric.penfold.client.domain.services.Consumer;
import com.qmetric.penfold.client.domain.services.TaskConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

public class TaskConsumerImpl implements TaskConsumer
{
    private static final Logger LOG = LoggerFactory.getLogger(TaskConsumerImpl.class);

    private final Consumer consumer;

    private final Duration interval;

    private final ScheduledExecutorService scheduledExecutorService;

    private final ShutdownProcedure shutdownProcedure;

    public TaskConsumerImpl(final Consumer consumer, final Duration interval)
    {
        this.consumer = consumer;
        this.interval = interval;
        this.scheduledExecutorService = newSingleThreadScheduledExecutor();
        this.shutdownProcedure = new ShutdownProcedure(scheduledExecutorService);
    }

    @Override public void start()
    {
        scheduledExecutorService.scheduleAtFixedRate(this::consume, 0, interval.getSeconds(), TimeUnit.SECONDS);
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
