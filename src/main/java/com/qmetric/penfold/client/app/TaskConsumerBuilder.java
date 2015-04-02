package com.qmetric.penfold.client.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qmetric.penfold.client.app.support.Credentials;
import com.qmetric.penfold.client.app.support.Interval;
import com.qmetric.penfold.client.app.support.LocalDateTimeSource;
import com.qmetric.penfold.client.app.support.ObjectMapperFactory;
import com.qmetric.penfold.client.domain.services.Consumer;
import com.qmetric.penfold.client.domain.services.ConsumerFunction;
import com.qmetric.penfold.client.domain.services.TaskConsumer;
import com.qmetric.penfold.client.domain.services.TaskQueryService;
import com.qmetric.penfold.client.domain.services.TaskStoreService;
import com.sun.jersey.api.client.Client;
import com.qmetric.penfold.client.domain.model.QueueId;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.TimeUnit.MINUTES;

public class TaskConsumerBuilder
{
    private String url;

    private QueueId queue;

    private ConsumerFunction function;

    private Credentials credentials;

    private Interval pollingFrequency = new Interval(1, MINUTES);

    private Optional<Interval> retryDelay = Optional.empty();

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
     * @param timeUnit Time unit
     * @return Updated builder
     */
    public TaskConsumerBuilder withPollingFrequency(final long interval, final TimeUnit timeUnit)
    {
        this.pollingFrequency = new Interval(interval, timeUnit);
        return this;
    }

    /**
     * How long to wait before retying after a task fails to be consumed (default no delay).
     *
     * @param interval Polling interval.
     * @param timeUnit Time unit
     * @return Updated builder
     */
    public TaskConsumerBuilder delayBetweenEachRetryOf(final long interval, final TimeUnit timeUnit)
    {
        this.retryDelay = Optional.of(new Interval(interval, timeUnit));
        return this;
    }

    public TaskConsumer build()
    {
        checkValid();

        final Client client = Client.create();
        final ObjectMapper objectMapper = ObjectMapperFactory.create();

        final TaskQueryService taskQueryService = new TaskQueryServiceImpl(url, credentials, client, objectMapper);
        final TaskStoreService taskStoreService = new TaskStoreServiceImpl(url, credentials, client, objectMapper);

        final LocalDateTimeSource dateTimeSource = new LocalDateTimeSource();

        final Consumer consumer = new Consumer(queue, function, retryDelay, taskQueryService, taskStoreService, dateTimeSource);

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
    }
}
