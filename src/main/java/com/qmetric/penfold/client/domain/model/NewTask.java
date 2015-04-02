package com.qmetric.penfold.client.domain.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

public class NewTask
{
    public final QueueId queue;

    public final Payload payload;

    public final Optional<LocalDateTime> triggerDate;

    public NewTask(final QueueId queue, final Payload payload, final Optional<LocalDateTime> triggerDate)
    {
        checkArgument(queue != null, "missing queue");
        checkArgument(payload != null, "missing payload");
        checkArgument(triggerDate != null, "null trigger date");
        this.queue = queue;
        this.payload = payload;
        this.triggerDate = triggerDate;
    }

    @Override public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override public boolean equals(final Object obj)
    {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
    }
}
