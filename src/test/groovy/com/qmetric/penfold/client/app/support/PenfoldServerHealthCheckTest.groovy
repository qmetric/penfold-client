package com.qmetric.penfold.client.app.support

import org.apache.http.HttpResponse
import org.apache.http.ProtocolVersion
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.message.BasicStatusLine
import spock.lang.Specification

class PenfoldServerHealthCheckTest extends Specification
{
    final client = Mock(HttpClient)

    final response = Mock(HttpResponse)

    final healthCheck = new PenfoldServerHealthCheck("http://host:123/", client)

    def setup()
    {
        client.execute({ request -> request.getURI().toString() == "http://host:123/ping"} as HttpUriRequest) >> response
    }

    def "should know when penfold server is healthy"()
    {
        given:
        mock200Response()

        when:
        final result = healthCheck.check()

        then:
        result.isHealthy()
    }

    def "should know when penfold server is unhealthy"()
    {
        given:
        mock500Response()

        when:
        final result = healthCheck.check()

        then:
        !result.isHealthy()
    }

    def mock200Response()
    {
        response.getStatusLine() >> new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK")
    }

    def mock500Response()
    {
        response.getStatusLine() >> new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 500, "Error")
    }
}
