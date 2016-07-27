package com.qmetric.penfold.client.domain.services.events;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class Notifier
{
    public static final Notifier EMPTY = new Notifier();

    private final List<EventListener> listeners;

    public Notifier(final EventListener... listeners)
    {
        this.listeners = ImmutableList.copyOf(listeners);
    }

    public void notify(final Event event)
    {
        listeners.forEach(listener -> listener.notify(event));
    }

    public List<EventListener> getListeners()
    {
        return listeners;
    }
}
