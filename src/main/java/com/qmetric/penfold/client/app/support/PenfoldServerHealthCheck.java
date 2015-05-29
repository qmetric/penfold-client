package com.qmetric.penfold.client.app.support;

import com.codahale.metrics.health.HealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import java.net.URI;
import java.net.URISyntaxException;

import static com.codahale.metrics.health.HealthCheck.Result.healthy;
import static com.codahale.metrics.health.HealthCheck.Result.unhealthy;
import static com.theoryinpractise.halbuilder.api.RepresentationFactory.HAL_JSON;
import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_OK;

public class PenfoldServerHealthCheck extends HealthCheck
{
    private static final Logger LOG = LoggerFactory.getLogger(PenfoldServerHealthCheck.class);

    private final String serverUrl;

    private final Client client;

    public PenfoldServerHealthCheck(final String feedUrl, final Client client)
    {
        this.serverUrl = new UrlUtils().pingUrlFrom(feedUrl);
        this.client = client;
    }

    @Override protected Result check() throws Exception
    {
        final Response clientResponse = client.target(serverUrl).request(HAL_JSON).get();

        if (clientResponse.getStatus() == HTTP_OK)
        {
            return healthy("Penfold server ok at %s", serverUrl);
        }
        else
        {
            return unhealthy("Penfold server not ok at %s with status %s", serverUrl, clientResponse.getStatus());
        }
    }

    static class UrlUtils
    {
        String pingUrlFrom(final String url)
        {
            try
            {
                final URI uri = new URI(url);

                return uri.getPort() > 0 ? format("%s://%s:%s/ping", uri.getScheme(), uri.getHost(), uri.getPort()) : format("%s://%s/ping", uri.getScheme(), uri.getHost());
            }
            catch (URISyntaxException e)
            {
                LOG.error("ping url invalid", e);
                throw new RuntimeException(e);
            }
        }
    }
}
