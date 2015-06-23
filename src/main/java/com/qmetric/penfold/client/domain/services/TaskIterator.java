package com.qmetric.penfold.client.domain.services;

import com.qmetric.penfold.client.app.commands.filter.Filter;
import com.qmetric.penfold.client.domain.model.PageReference;
import com.qmetric.penfold.client.domain.model.TasksPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class TaskIterator extends AbstractTaskIterator
{
    private static final Logger LOG = LoggerFactory.getLogger(TaskIterator.class);

    public final List<Filter> filters;

    public TaskIterator(final List<Filter> filters, final PageAwareTaskQueryService taskQueryService)
    {
        super(taskQueryService);
        this.filters = filters;
    }

    protected TasksPage loadPageOfTasks(final Optional<PageReference> pageReference)
    {
        LOG.info("loading next page of tasks with page reference {}", pageReference);

        final TasksPage tasksPage = taskQueryService.retrieve(filters, pageReference);

        LOG.info("loaded next page of tasks with page reference {}", pageReference);

        return tasksPage;
    }
}
