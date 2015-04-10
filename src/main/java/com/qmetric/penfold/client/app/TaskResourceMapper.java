package com.qmetric.penfold.client.app;

import com.google.common.reflect.TypeToken;
import com.qmetric.hal.reader.HalResource;
import com.qmetric.penfold.client.app.support.TaskDateTimeFormatter;
import com.qmetric.penfold.client.domain.model.Task;
import com.qmetric.penfold.client.domain.model.TaskId;
import com.qmetric.penfold.client.domain.model.Payload;
import com.qmetric.penfold.client.domain.model.QueueId;
import com.qmetric.penfold.client.domain.model.TaskStatus;

import java.time.LocalDateTime;
import java.util.Map;

class TaskResourceMapper
{
    public Task getTaskFromResource(final HalResource input)
    {
        final TaskId id = new TaskId(input.getValueAsString("id").orNull());
        final Integer version = Integer.valueOf(input.getValueAsString("version").orNull());
        final Integer attempts = Integer.valueOf(input.getValueAsString("attempts").orNull());
        final QueueId queueId = new QueueId(input.getValueAsString("queue").orNull());
        final TaskStatus status = new TaskStatus(input.getValueAsString("status").orNull());
        final LocalDateTime created = TaskDateTimeFormatter.parse(input.getValueAsString("created").get());
        final Payload payload = new Payload(input.getValueAsObject("payload", new TypeToken<Map<String, Object>>() {}).get());

        return new Task(id, version, queueId, status, created, attempts, payload);
    }
}
