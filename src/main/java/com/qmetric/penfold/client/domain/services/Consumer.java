package com.qmetric.penfold.client.domain.services;

import com.qmetric.penfold.client.domain.model.QueueId;
import com.qmetric.penfold.client.domain.model.Task;

public interface Consumer
{
    QueueId getQueue();

    void consume();

    void consumeTask(Task task);
}
