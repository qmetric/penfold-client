package com.qmetric.penfold.client.domain.services.events;

public interface EventListener
{
    void notify(final Event event);
}
