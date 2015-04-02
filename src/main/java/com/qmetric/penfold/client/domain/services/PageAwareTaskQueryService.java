package com.qmetric.penfold.client.domain.services;

import com.qmetric.penfold.client.app.commands.filter.Filter;
import com.qmetric.penfold.client.domain.model.PageReference;
import com.qmetric.penfold.client.domain.model.QueueId;
import com.qmetric.penfold.client.domain.model.TaskStatus;
import com.qmetric.penfold.client.domain.model.TasksPage;

import java.util.List;
import java.util.Optional;

public interface PageAwareTaskQueryService
{
    TasksPage retrieve(QueueId queue, TaskStatus status, List<Filter> filters, Optional<PageReference> pageRequest);

    TasksPage retrieve(List<Filter> filters, Optional<PageReference> pageRequest);
}
