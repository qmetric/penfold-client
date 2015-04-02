package com.qmetric.penfold.client.domain.model;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Map;

public class Payload
{
    public static final Payload empty = new Payload(ImmutableMap.of());

    private final Map<String, Object> map;

    public Payload(final Map<String, Object> map)
    {
        this.map = map;
    }

    public Map<String, Object> getAsMap()
    {
        return map;
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
