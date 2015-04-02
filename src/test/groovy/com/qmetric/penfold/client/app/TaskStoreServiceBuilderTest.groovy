package com.qmetric.penfold.client.app

import com.qmetric.penfold.client.app.TaskStoreServiceBuilder
import spock.lang.Specification

class TaskStoreServiceBuilderTest extends Specification {

    def "should build task store service"()
    {
        expect:
        new TaskStoreServiceBuilder()
                .forServer("http://localhost")
                .withCredentials("user", "pass")
                .build();
    }
}
