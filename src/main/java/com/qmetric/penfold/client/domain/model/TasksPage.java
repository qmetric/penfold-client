package com.qmetric.penfold.client.domain.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;
import java.util.Optional;

public class TasksPage
{
    public final List<Task> tasks;

    public final Optional<PageReference> previousPage;

    public final Optional<PageReference> nextPage;

    public TasksPage(final List<Task> tasks, final Optional<PageReference> previousPage, final Optional<PageReference> nextPage)
    {
        this.tasks = tasks;
        this.previousPage = previousPage;
        this.nextPage = nextPage;
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
