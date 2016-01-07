package com.qmetric.penfold.client.app

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
