package com.qmetric.penfold.client.domain.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Optional;

public class Reply
{
    public final ReplyType type;

    public final Optional<String> reason;

    private Reply(final ReplyType type, final Optional<String> reason)
    {
        this.type = type;
        this.reason = reason;
    }

    public static Reply success()
    {
        return new Reply(ReplyType.SUCCESS, Optional.empty());
    }

    public static Reply fail(final Optional<String> reason)
    {
        return new Reply(ReplyType.FAIL, reason);
    }

    public static Reply retry(final Optional<String> reason)
    {
        return new Reply(ReplyType.RETRY, reason);
    }

    public boolean isSucessful()
    {
        return type == ReplyType.SUCCESS;
    }

    public boolean isFailure()
    {
        return type == ReplyType.FAIL;
    }

    public boolean isRetry()
    {
        return type == ReplyType.RETRY;
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
