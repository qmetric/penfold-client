package com.qmetric.penfold.client.app;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qmetric.penfold.client.app.support.ClientFactory;
import com.qmetric.penfold.client.app.support.ConsumerThreadActivityHealthCheck;
import com.qmetric.penfold.client.app.support.Credentials;
import com.qmetric.penfold.client.app.support.LocalDateTimeSource;
import com.qmetric.penfold.client.app.support.ObjectMapperFactory;
import com.qmetric.penfold.client.domain.model.QueueId;
import com.qmetric.penfold.client.domain.services.Consumer;
import com.qmetric.penfold.client.domain.services.ConsumerFunction;
import com.qmetric.penfold.client.domain.services.ConsumerImpl;
import com.qmetric.penfold.client.domain.services.TaskConsumer;
import com.qmetric.penfold.client.domain.services.TaskQueryService;
import com.qmetric.penfold.client.domain.services.TaskStoreService;

import java.time.Duration;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

public class TaskConsumerBuilder
{
    private String url;

    private QueueId queue;

    private ConsumerFunction function;

    private Credentials credentials;

    private Duration pollingFrequency = Duration.ofMinutes(1);

    private Optional<Duration> retryDelay = Optional.empty();

    private Optional<Duration> minimumTimeBetweenConsumingForHealthCheck = Optional.empty();

    private Optional<HealthCheckRegistry> healthCheckRegistry = Optional.empty();

    /**
     * Penfold server url
     *
     * @param url Url
     * @return Updated builder
     */
    public TaskConsumerBuilder fromServer(final String url)
    {
        this.url = url;
        return this;
    }

    /**
     * Penfold server credentials
     *
     * @param username Username
     * @param password Password
     * @return Updated builder
     */
    public TaskConsumerBuilder withCredentials(final String username, final String password)
    {
        this.credentials = new Credentials(username, password);
        return this;
    }

    /**
     * Penfold queue to consume from.
     *
     * @param queue Queue id
     * @return Updated builder
     */
    public TaskConsumerBuilder fromQueue(final String queue)
    {
        this.queue = new QueueId(queue);
        return this;
    }

    /**
     * Custom function defining how to consume each received task.
     *
     * @param function Custom function
     * @return Updated builder
     */
    public TaskConsumerBuilder consumeWith(final ConsumerFunction function)
    {
        this.function = function;
        return this;
    }

    /**
     * How often to check queue for new entries to consume (default 1 minute).
     *
     * @param interval Polling interval.
     * @return Updated builder
     */
    public TaskConsumerBuilder withPollingFrequency(final Duration interval)
    {
        this.pollingFrequency = interval;
        return this;
    }

    /**
     * How long to wait before retying after a task fails to be consumed (default no delay).
     *
     * @param interval Polling interval.
     * @return Updated builder
     */
    public TaskConsumerBuilder delayBetweenEachRetryOf(final Duration interval)
    {
        this.retryDelay = Optional.of(interval);
        return this;
    }

    /**
     * How long between consuming before consumer marked as unhealthy.
     *
     * @param minimumTimeBetweenConsumingForHealthCheck Time between consuming.
     * @return Updated builder
     */
    public TaskConsumerBuilder withActivityHealthCheck(final Duration minimumTimeBetweenConsumingForHealthCheck, final HealthCheckRegistry healthCheckRegistry)
    {
        this.minimumTimeBetweenConsumingForHealthCheck = Optional.of(minimumTimeBetweenConsumingForHealthCheck);
        this.healthCheckRegistry = Optional.of(healthCheckRegistry);

        return this;
    }

    public TaskConsumer build()
    {
        checkValid();

        final ObjectMapper objectMapper = ObjectMapperFactory.create();

        final TaskQueryService taskQueryService = new TaskQueryServiceImpl(url, ClientFactory.createHttpClient(credentials), objectMapper);
        final TaskStoreService taskStoreService = new TaskStoreServiceImpl(url, ClientFactory.createHttpClient(credentials), objectMapper);

        final LocalDateTimeSource dateTimeSource = new LocalDateTimeSource();

        final Consumer consumer;
        if (minimumTimeBetweenConsumingForHealthCheck.isPresent())
        {
            consumer = new ConsumerThreadActivityHealthCheck(new ConsumerImpl(queue, function, retryDelay, taskQueryService, taskStoreService, dateTimeSource), dateTimeSource, minimumTimeBetweenConsumingForHealthCheck.get());

            healthCheckRegistry.get().register(String.format("%s scheduling consumer", queue.value), (ConsumerThreadActivityHealthCheck) consumer);
        }
        else
        {
            consumer = new ConsumerImpl(queue, function, retryDelay, taskQueryService, taskStoreService, dateTimeSource);
        }

        return new TaskConsumerImpl(consumer, pollingFrequency);
    }

    private void checkValid()
    {
        checkArgument(url != null, "missing url");
        checkArgument(queue != null, "missing queue");
        checkArgument(function != null, "missing function");
        checkArgument(credentials != null, "missing credentials");
        checkArgument(pollingFrequency != null, "missing polling frequency");
        checkArgument(retryDelay != null, "missing retry delay");
        checkArgument(minimumTimeBetweenConsumingForHealthCheck != null, "missing minimumTimeBetweenConsumingForHealthCheck");
        checkArgument(healthCheckRegistry != null, "missing healthCheckRegistry");
    }
}
