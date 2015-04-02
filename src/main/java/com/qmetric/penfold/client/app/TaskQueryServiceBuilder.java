package com.qmetric.penfold.client.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qmetric.penfold.client.domain.services.TaskQueryService;
import com.sun.jersey.api.client.Client;
import com.qmetric.penfold.client.app.support.Credentials;
import com.qmetric.penfold.client.app.support.ObjectMapperFactory;

import static com.google.common.base.Preconditions.checkArgument;

public class TaskQueryServiceBuilder
{
    private String url;

    private Credentials credentials;

    public TaskQueryServiceBuilder forServer(final String url)
    {
        this.url = url;
        return this;
    }

    public TaskQueryServiceBuilder withCredentials(final String username, final String password)
    {
        this.credentials = new Credentials(username, password);
        return this;
    }

    public TaskQueryService build()
    {
        checkValid();

        final Client client = Client.create();

        final ObjectMapper objectMapper = ObjectMapperFactory.create();

        return new TaskQueryServiceImpl(url, credentials, client, objectMapper);
    }

    private void checkValid()
    {
        checkArgument(url != null, "missing url");
        checkArgument(credentials != null, "missing credentials");
    }
}
