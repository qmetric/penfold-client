package com.qmetric.penfold.client.domain.services.events;

import com.qmetric.penfold.client.domain.model.QueueId;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class QueueConsumedEvent implements Event
{
    public static final String TYPE = "QueueConsumed";

    private final QueueId queueId;

    public QueueConsumedEvent(final QueueId queueId)
    {
        this.queueId = queueId;
    }

    public QueueId getQueueId()
    {
        return queueId;
    }

    @Override public String getType()
    {
        return TYPE;
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
