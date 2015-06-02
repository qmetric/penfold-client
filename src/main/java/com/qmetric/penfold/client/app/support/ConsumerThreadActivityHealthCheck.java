package com.qmetric.penfold.client.app.support;

import com.codahale.metrics.health.HealthCheck;
import com.qmetric.penfold.client.domain.model.Task;
import com.qmetric.penfold.client.domain.services.Consumer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.codahale.metrics.health.HealthCheck.Result.healthy;
import static com.codahale.metrics.health.HealthCheck.Result.unhealthy;

public class ConsumerThreadActivityHealthCheck extends HealthCheck implements Consumer
{
    private final Consumer delegate;

    private final LocalDateTimeSource dateTimeSource;

    private final Duration tolerableDelay;

    private Optional<LocalDateTime> lastConsumed;

    public ConsumerThreadActivityHealthCheck(final Consumer delegate, final LocalDateTimeSource dateTimeSource, final Duration tolerableDelay)
    {
        this.delegate = delegate;
        this.dateTimeSource = dateTimeSource;
        this.tolerableDelay = tolerableDelay;
        lastConsumed = Optional.empty();
    }

    @Override protected synchronized HealthCheck.Result check() throws Exception
    {
        return !lastConsumed.isPresent() || durationSinceLastConsumedIsLongerThanTolerableDelay() ? unhealthyResult() : healthyResult();
    }

    @Override public void consume()
    {
        delegate.consume();

        lastConsumed = Optional.of(dateTimeSource.now());
    }

    @Override public void consumeTask(final Task task)
    {
        delegate.consumeTask(task);

        lastConsumed = Optional.of(dateTimeSource.now());
    }

    private boolean durationSinceLastConsumedIsLongerThanTolerableDelay()
    {
        return Duration.between(lastConsumed.get(), dateTimeSource.now()).getSeconds() > tolerableDelay.getSeconds();
    }

    private HealthCheck.Result unhealthyResult()
    {
        if (lastConsumed.isPresent())
        {
            return unhealthy(String.format("No activity since %s", lastConsumed.get()));
        }
        else
        {
            return unhealthy("No activity");
        }
    }

    private HealthCheck.Result healthyResult()
    {
        return healthy(String.format("Active at %s", lastConsumed.get()));
    }
}
