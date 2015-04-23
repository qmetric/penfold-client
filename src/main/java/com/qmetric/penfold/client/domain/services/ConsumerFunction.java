package com.qmetric.penfold.client.domain.services;

import com.qmetric.penfold.client.domain.model.Reply;
import com.qmetric.penfold.client.domain.model.Task;

public interface ConsumerFunction
{
    Reply execute(Task task);
}
