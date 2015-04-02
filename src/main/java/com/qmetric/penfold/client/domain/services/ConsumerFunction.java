package com.qmetric.penfold.client.domain.services;

import com.qmetric.penfold.client.domain.model.Result;
import com.qmetric.penfold.client.domain.model.Task;

public interface ConsumerFunction
{
    Result execute(Task task);
}
