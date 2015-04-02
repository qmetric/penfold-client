package com.qmetric.penfold.client.app.commands;

import java.util.Optional;

public class CancelTaskCommand
{
    public final Optional<String> reason;

    public CancelTaskCommand(final Optional<String> reason)
    {
        this.reason = reason;
    }
}
