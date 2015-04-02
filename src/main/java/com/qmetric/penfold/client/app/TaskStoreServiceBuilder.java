package com.qmetric.penfold.client.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.qmetric.penfold.client.app.support.Credentials;
import com.qmetric.penfold.client.app.support.ObjectMapperFactory;
import com.qmetric.penfold.client.domain.services.TaskStoreService;

import static com.google.common.base.Preconditions.checkArgument;

public class TaskStoreServiceBuilder
{
    private String url;

    private Credentials credentials;

    public TaskStoreServiceBuilder forServer(final String url)
    {
        this.url = url;
        return this;
    }

    public TaskStoreServiceBuilder withCredentials(final String username, final String password)
    {
        this.credentials = new Credentials(username, password);
        return this;
    }

    public TaskStoreService build()
    {
        checkValid();

        final Client client = Client.create();

        final ObjectMapper objectMapper = ObjectMapperFactory.create();

        return new TaskStoreServiceImpl(url, credentials, client, objectMapper);
    }

    private void checkValid()
    {
        checkArgument(url != null, "missing url");
        checkArgument(credentials != null, "missing credentials");
    }
}
