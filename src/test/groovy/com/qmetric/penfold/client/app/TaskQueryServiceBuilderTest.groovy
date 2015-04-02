package com.qmetric.penfold.client.app

import com.qmetric.penfold.client.app.TaskQueryServiceBuilder
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
