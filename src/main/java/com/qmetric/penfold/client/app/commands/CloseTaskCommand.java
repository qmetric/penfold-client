package com.qmetric.penfold.client.app.commands;

import com.qmetric.penfold.client.domain.model.CloseResultType;

import java.util.Optional;

public class CloseTaskCommand
{
    public final Optional<CloseResultType> resultType;

    public final Optional<String> reason;

    public CloseTaskCommand(final Optional<CloseResultType> resultType, final Optional<String> reason)
    {
        this.resultType = resultType;
        this.reason = reason;
    }
}
