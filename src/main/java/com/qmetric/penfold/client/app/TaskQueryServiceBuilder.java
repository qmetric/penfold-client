package com.qmetric.penfold.client.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qmetric.penfold.client.app.support.ClientFactory;
import com.qmetric.penfold.client.app.support.Credentials;
import com.qmetric.penfold.client.app.support.ObjectMapperFactory;
import com.qmetric.penfold.client.domain.services.TaskQueryService;
import org.apache.http.client.HttpClient;

import static com.google.common.base.Preconditions.checkArgument;

public class TaskQueryServiceBuilder
{
    private String url;

    private Credentials credentials;

    /**
     * Penfold server url
     *
     * @param url Url
     * @return Updated builder
     */
    public TaskQueryServiceBuilder forServer(final String url)
    {
        this.url = url;
        return this;
    }

    /**
     * Penfold server credentials
     *
     * @param username Username
     * @param password Password
     * @return Updated builder
     */
    public TaskQueryServiceBuilder withCredentials(final String username, final String password)
    {
        this.credentials = new Credentials(username, password);
        return this;
    }

    public TaskQueryService build()
    {
        checkValid();

        final HttpClient httpClient = ClientFactory.createHttpClient(credentials);

        final ObjectMapper objectMapper = ObjectMapperFactory.create();

        return new TaskQueryServiceImpl(url, httpClient, objectMapper);
    }

    private void checkValid()
    {
        checkArgument(url != null, "missing url");
    }
}
