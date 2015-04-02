package com.qmetric.penfold.client.app

import com.codahale.metrics.health.HealthCheckRegistry
import com.qmetric.penfold.client.app.support.PenfoldServerHealthCheck
import spock.lang.Specification

class HealthCheckConfigurerTest extends Specification {

    final healthCheckRegistry = Mock(HealthCheckRegistry)

    final configurer = new HealthCheckConfigurer("http://localhost", healthCheckRegistry)

    def "should append health checks to existing registry"()
    {
        when:
        configurer.configure()

        then:
        1 * healthCheckRegistry.register(_ as String, _ as PenfoldServerHealthCheck)
    }
}
