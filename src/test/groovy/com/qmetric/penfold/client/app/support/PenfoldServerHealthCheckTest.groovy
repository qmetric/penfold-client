package com.qmetric.penfold.client.app.support

import com.theoryinpractise.halbuilder.api.RepresentationFactory
import spock.lang.Specification

import javax.ws.rs.client.Client
import javax.ws.rs.client.Invocation
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.Response

class PenfoldServerHealthCheckTest extends Specification {
    final client = Mock(Client)

    final response = Mock(Response)

    final healthCheck = new PenfoldServerHealthCheck("http://host:123/", client)

    def setup()
    {
        final webTarget = Mock(WebTarget)
        final builder = Mock(Invocation.Builder)
        client.target("http://host:123/ping") >> webTarget
        webTarget.request(RepresentationFactory.HAL_JSON) >> builder
        builder.get() >> response
    }

    def "should know when penfold server is healthy"()
    {
        given:
        response.getStatus() >> 200

        when:
        final result = healthCheck.check()

        then:
        result.isHealthy()
    }

    def "should know when penfold server is unhealthy"()
    {
        given:
        response.getStatus() >> 500

        when:
        final result = healthCheck.check()

        then:
        !result.isHealthy()
    }
}
