package com.qmetric.penfold.client.app.support;

import com.codahale.metrics.health.HealthCheck;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final HttpClient httpClient;

    public PenfoldServerHealthCheck(final String feedUrl, final HttpClient httpClient)
    {
        this.serverUrl = new UrlUtils().pingUrlFrom(feedUrl);
        this.httpClient = httpClient;
    }

    @Override protected Result check() throws Exception
    {
        final HttpGet httpGet = new HttpGet(serverUrl);
        httpGet.addHeader(HttpHeaders.ACCEPT, HAL_JSON);

        final HttpResponse clientResponse = httpClient.execute(httpGet);

        final int statusCode = clientResponse.getStatusLine().getStatusCode();
        if (statusCode == HTTP_OK)
        {
            return healthy("Penfold server ok at %s", serverUrl);
        }
        else
        {
            return unhealthy("Penfold server not ok at %s with status %s", serverUrl, statusCode);
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
