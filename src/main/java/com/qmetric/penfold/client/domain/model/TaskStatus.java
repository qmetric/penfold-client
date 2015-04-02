package com.qmetric.penfold.client.domain.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class TaskStatus
{
    public static final TaskStatus WAITING = new TaskStatus("waiting");

    public static final TaskStatus READY = new TaskStatus("ready");

    public static final TaskStatus STARTED = new TaskStatus("started");

    public static final TaskStatus CLOSED = new TaskStatus("closed");

    public static final TaskStatus CANCELLED = new TaskStatus("cancelled");

    public final String value;

    public TaskStatus(final String value)
    {
        checkArgument(isNotBlank(value), "missing status value");
        this.value = value;
    }

    public boolean isWaiting()
    {
        return this.equals(WAITING);
    }

    public boolean isReady()
    {
        return this.equals(READY);
    }

    public boolean isStarted()
    {
        return this.equals(STARTED);
    }

    public boolean isClosed()
    {
        return this.equals(CLOSED);
    }

    public boolean isCancelled()
    {
        return this.equals(CANCELLED);
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
        return value;
    }
}
