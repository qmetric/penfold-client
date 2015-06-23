package com.qmetric.penfold.client.domain.services;

import com.qmetric.penfold.client.app.commands.filter.Filter;
import com.qmetric.penfold.client.domain.model.PageReference;
import com.qmetric.penfold.client.domain.model.QueueId;
import com.qmetric.penfold.client.domain.model.TaskStatus;
import com.qmetric.penfold.client.domain.model.TasksPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class QueueIterator extends AbstractTaskIterator
{
    private static final Logger LOG = LoggerFactory.getLogger(QueueIterator.class);

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
        LOG.info("loading next page of tasks with page reference {} from {}", pageReference, queue);

        final TasksPage tasksPage = taskQueryService.retrieve(queue, status, filters, pageReference);

        LOG.info("loaded next page of tasks with page reference {} from {}", pageReference, queue);

        return tasksPage;
    }
}
