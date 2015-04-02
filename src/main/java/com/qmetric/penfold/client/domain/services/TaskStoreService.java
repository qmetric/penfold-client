package com.qmetric.penfold.client.domain.services;

import com.qmetric.penfold.client.domain.model.CloseResultType;
import com.qmetric.penfold.client.domain.model.NewTask;
import com.qmetric.penfold.client.domain.model.Task;
import com.qmetric.penfold.client.domain.exceptions.ConflictException;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TaskStoreService
{
    Task create(NewTask task);

    Task start(Task task) throws ConflictException;

    Task requeue(Task task, Optional<String> reason) throws ConflictException;

    Task reschedule(Task task, LocalDateTime triggerDate, Optional<String> reason) throws ConflictException;

    Task cancel(Task task, Optional<String> reason) throws ConflictException;

    Task close(Task task, Optional<CloseResultType> resultType, Optional<String> reason) throws ConflictException;
}
