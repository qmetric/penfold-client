package com.qmetric.penfold.client.app.commands;

import java.time.LocalDateTime;
import java.util.Optional;

public class RescheduleTaskCommand
{
    public final LocalDateTime triggerDate;

    public final Optional<String> reason;

    public RescheduleTaskCommand(final LocalDateTime triggerDate, final Optional<String> reason)
    {
        this.triggerDate = triggerDate;
        this.reason = reason;
    }
}
