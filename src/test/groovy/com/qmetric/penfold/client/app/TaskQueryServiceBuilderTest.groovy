package com.qmetric.penfold.client.app

import spock.lang.Specification

class TaskQueryServiceBuilderTest extends Specification {

    def "should build task query service"()
    {
        expect:
        new TaskQueryServiceBuilder()
                .forServer("http://localhost")
                .withCredentials("user", "pass")
                .build();
    }
}
