package com.qmetric.penfold.client.domain.services;

import com.qmetric.penfold.client.domain.model.PageReference;
import com.qmetric.penfold.client.app.commands.filter.Filter;
import com.qmetric.penfold.client.domain.model.QueueId;
import com.qmetric.penfold.client.domain.model.TaskStatus;
import com.qmetric.penfold.client.domain.model.TasksPage;

import java.util.List;
import java.util.Optional;

public class QueueIterator extends AbstractTaskIterator
{
    private final QueueId queue;

    private final TaskStatus status;

    private final List<Filter> filters;

    public QueueIterator(final QueueId queue, final TaskStatus status, final List<Filter> filters, final PageAwareTaskQueryService taskQueryService)
    {
        super(taskQueryService);
        this.queue = queue;
        this.status = status;
        this.filters = filters;
    }

    protected TasksPage loadPageOfTasks(final Optional<PageReference> pageReference)
    {
        return taskQueryService.retrieve(queue, status, filters, pageReference);
    }
}
