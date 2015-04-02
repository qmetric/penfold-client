package com.qmetric.penfold.client.app;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.qmetric.penfold.client.app.support.PenfoldServerHealthCheck;
import com.sun.jersey.api.client.Client;

public class HealthCheckConfigurer
{
    private final String url;

    private final HealthCheckRegistry healthCheckRegistry;

    /**
     * Construct new instance
     *
     * @param serverUrl Penfold server url
     * @param healthCheckRegistry existing health check registry
     */
    public HealthCheckConfigurer(final String serverUrl, final HealthCheckRegistry healthCheckRegistry)
    {
        this.url = serverUrl;
        this.healthCheckRegistry = healthCheckRegistry;
    }

    /**
     * Append penfold health checks to existing health check registry.
     *
     * @return Updated health check registry.
     */
    public HealthCheckRegistry configure()
    {
        final Client client = Client.create();

        healthCheckRegistry.register("penfold server", new PenfoldServerHealthCheck(url, client));

        return healthCheckRegistry;
    }
}
