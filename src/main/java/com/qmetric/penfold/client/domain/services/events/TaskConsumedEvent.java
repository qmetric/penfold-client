package com.qmetric.penfold.client.domain.services.events;

import com.qmetric.penfold.client.domain.model.TaskId;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class TaskConsumedEvent implements Event
{
    public static final String TYPE = "TaskConsumed";

    private final TaskId taskId;

    public TaskConsumedEvent(final TaskId taskId)
    {
        this.taskId = taskId;
    }

    public TaskId getTaskId()
    {
        return taskId;
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
