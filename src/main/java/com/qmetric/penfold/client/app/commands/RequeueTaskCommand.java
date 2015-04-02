package com.qmetric.penfold.client.app.commands;

import java.util.Optional;

public class RequeueTaskCommand
{
    public final Optional<String> reason;

    public RequeueTaskCommand(final Optional<String> reason)
    {
        this.reason = reason;
    }
}
