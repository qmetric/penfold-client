package com.qmetric.penfold.client.app.support;

import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

public class Interval
{
    public final long duration;

    public final TimeUnit unit;

    public Interval(final long duration, final TimeUnit unit)
    {
        checkArgument(duration > 0 && unit != null, "Invalid interval");

        this.duration = duration;
        this.unit = unit;
    }

    public long seconds()
    {
        return unit.toSeconds(duration);
    }

    @Override
    public boolean equals(final Object obj)
    {
        return reflectionEquals(this, obj);
    }

    @Override public String toString()
    {
        return reflectionToString(this);
    }
}
