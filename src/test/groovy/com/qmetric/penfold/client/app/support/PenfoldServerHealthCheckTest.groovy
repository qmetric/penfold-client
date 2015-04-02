package com.qmetric.penfold.client.app.support

import com.sun.jersey.api.client.Client
import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.api.client.WebResource
import com.theoryinpractise.halbuilder.api.RepresentationFactory
import spock.lang.Specification

class PenfoldServerHealthCheckTest extends Specification {
    final client = Mock(Client)

    final webResource = Mock(WebResource)

    final webResourceBuilder = Mock(WebResource.Builder)

    final response = Mock(ClientResponse)

    final healthCheck = new PenfoldServerHealthCheck("http://host:123/", client)

    def setup()
    {
        client.resource("http://host:123/ping") >> webResource
        webResource.accept(RepresentationFactory.HAL_JSON) >> webResourceBuilder
        webResourceBuilder.get(ClientResponse.class) >> response
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
