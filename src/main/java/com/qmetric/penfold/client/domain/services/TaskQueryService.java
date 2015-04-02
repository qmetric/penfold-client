package com.qmetric.penfold.client.domain.services;

import com.qmetric.penfold.client.app.commands.filter.Filter;
import com.qmetric.penfold.client.domain.model.QueueId;
import com.qmetric.penfold.client.domain.model.Task;
import com.qmetric.penfold.client.domain.model.TaskId;
import com.qmetric.penfold.client.domain.model.TaskStatus;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public interface TaskQueryService
{
    Optional<Task> find(TaskId id);

    Iterator<Task> find(QueueId queue, TaskStatus status, List<Filter> filters);

    Iterator<Task> find(List<Filter> filters);
}
