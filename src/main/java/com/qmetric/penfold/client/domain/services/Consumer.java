package com.qmetric.penfold.client.domain.services;

import com.qmetric.penfold.client.domain.model.Task;

public interface Consumer
{
    void consume();

    void consumeTask(Task task);
}
