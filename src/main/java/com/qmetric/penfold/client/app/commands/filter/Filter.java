package com.qmetric.penfold.client.app.commands.filter;

public abstract class Filter
{
    public enum Operation {
        EQ,
    }

    public final Operation op;

    public final String key;

    public Filter(final Operation op, final String key)
    {
        this.op = op;
        this.key = key;
    }
}