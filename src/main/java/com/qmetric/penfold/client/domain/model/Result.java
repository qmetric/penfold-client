package com.qmetric.penfold.client.domain.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Optional;

public class Result
{
    public final ResultType type;

    public final Optional<String> reason;

    private Result(final ResultType type, final Optional<String> reason)
    {
        this.type = type;
        this.reason = reason;
    }

    public static Result success()
    {
        return new Result(ResultType.SUCCESS, Optional.empty());
    }

    public static Result fail(final Optional<String> reason)
    {
        return new Result(ResultType.FAIL, reason);
    }

    public static Result retry(final Optional<String> reason)
    {
        return new Result(ResultType.RETRY, reason);
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
