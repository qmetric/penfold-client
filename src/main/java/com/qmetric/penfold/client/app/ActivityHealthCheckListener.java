package com.qmetric.penfold.client.app;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.collect.ImmutableList;
import com.qmetric.penfold.client.app.support.LocalDateTimeSource;
import com.qmetric.penfold.client.domain.services.events.Event;
import com.qmetric.penfold.client.domain.services.events.EventListener;
import com.qmetric.penfold.client.domain.services.events.QueueConsumedEvent;
import com.qmetric.penfold.client.domain.services.events.TaskConsumedEvent;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

import static com.codahale.metrics.health.HealthCheck.Result.healthy;
import static com.codahale.metrics.health.HealthCheck.Result.unhealthy;

public class ActivityHealthCheckListener extends HealthCheck implements EventListener
{
    private static final Collection<String> EVENTS_OF_INTEREST = ImmutableList.of(QueueConsumedEvent.TYPE, TaskConsumedEvent.TYPE);

    private final LocalDateTimeSource dateTimeSource;

    private final Duration tolerableDelay;

    private Optional<LocalDateTime> lastConsumed;

    public ActivityHealthCheckListener(final LocalDateTimeSource dateTimeSource, final Duration tolerableDelay)
    {
        this.dateTimeSource = dateTimeSource;
        this.tolerableDelay = tolerableDelay;
        this.lastConsumed = Optional.empty();
    }

    @Override public void notify(final Event event)
    {
        if (EVENTS_OF_INTEREST.contains(event.getType()))
        {
            lastConsumed = Optional.of(dateTimeSource.now());
        }
    }

    @Override public HealthCheck.Result check() throws Exception
    {
        return !lastConsumed.isPresent() || durationSinceLastConsumedIsLongerThanTolerableDelay() ? unhealthyResult() : healthyResult();
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
