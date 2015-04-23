package com.qmetric.penfold.client.app

import com.qmetric.penfold.client.domain.model.Reply
import com.qmetric.penfold.client.domain.model.Task
import com.qmetric.penfold.client.domain.services.ConsumerFunction
import spock.lang.Specification

import static java.util.concurrent.TimeUnit.MINUTES

class TaskConsumerBuilderTest extends Specification {

    def "should build task consumer"()
    {
        expect:
        new TaskConsumerBuilder()
                .fromServer("http://localhost")
                .withCredentials("user", "pass")
                .fromQueue("testqueue")
                .withPollingFrequency(1, MINUTES)
                .delayBetweenEachRetryOf(15, MINUTES)
                .consumeWith(new ConsumerFunction() {
            @Override
            public Reply execute(final Task task) {
                // your implementation here
            }})
                .build();
    }
}
