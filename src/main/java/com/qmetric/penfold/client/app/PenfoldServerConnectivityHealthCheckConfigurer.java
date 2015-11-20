package com.qmetric.penfold.client.app;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.qmetric.penfold.client.app.support.ClientFactory;
import com.qmetric.penfold.client.app.support.PenfoldServerHealthCheck;
import org.apache.http.client.HttpClient;

public class PenfoldServerConnectivityHealthCheckConfigurer
{
    private final String url;

    private final HealthCheckRegistry healthCheckRegistry;

    /**
     * Construct new instance
     *
     * @param serverUrl Penfold server url
     * @param healthCheckRegistry existing health check registry
     */
    public PenfoldServerConnectivityHealthCheckConfigurer(final String serverUrl, final HealthCheckRegistry healthCheckRegistry)
    {
        this.url = serverUrl;
        this.healthCheckRegistry = healthCheckRegistry;
    }

    /**
     * Append penfold connectivity health check to health check registry.
     *
     * @return Updated health check registry.
     */
    public HealthCheckRegistry configure()
    {
        final HttpClient httpClient = ClientFactory.createHttpClient(null);

        healthCheckRegistry.register("penfold server", new PenfoldServerHealthCheck(url, httpClient));

        return healthCheckRegistry;
    }
}
