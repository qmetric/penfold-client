package com.qmetric.penfold.client.app

import com.codahale.metrics.health.HealthCheckRegistry
import com.qmetric.penfold.client.domain.model.Reply
import com.qmetric.penfold.client.domain.model.Task
import com.qmetric.penfold.client.domain.services.ConsumerFunction
import spock.lang.Specification

import java.time.Duration

class TaskConsumerBuilderTest extends Specification {

    def "should build task consumer"()
    {
        expect:
        new TaskConsumerBuilder()
                .fromServer("http://localhost")
                .withCredentials("user", "pass")
                .fromQueue("testqueue")
                .withPollingFrequency(Duration.ofMinutes(1))
                .delayBetweenEachRetryOf(Duration.ofMinutes(15))
                .withActivityHealthCheck(Duration.ofMinutes(30), new HealthCheckRegistry())
                .consumeWith(new ConsumerFunction() {
            @Override
            public Reply execute(final Task task) {
                // your implementation here
                return Reply.success()
            }})
           .build();
    }
}
