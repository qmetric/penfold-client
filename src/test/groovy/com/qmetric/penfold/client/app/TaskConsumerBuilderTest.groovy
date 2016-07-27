package com.qmetric.penfold.client.app

import com.codahale.metrics.health.HealthCheckRegistry
import com.qmetric.penfold.client.domain.model.Reply
import com.qmetric.penfold.client.domain.model.Task
import com.qmetric.penfold.client.domain.services.ConsumerFunction
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration

class TaskConsumerBuilderTest extends Specification {

    static final consumerFunction = new ConsumerFunction() {
        @Override
        public Reply execute(final Task task)
        {
            return Reply.success()
        }
    }

    def "should build minimally configured consumer"()
    {
        expect:
        new TaskConsumerBuilder()
                .fromServer("http://localhost")
                .withCredentials("user", "pass")
                .fromQueue("testqueue")
                .consumeWith(consumerFunction)
                .build()
    }

    def "should build fully configured consumer"()
    {
        given:
        final healthCheckRegistry = new HealthCheckRegistry()
        final consumer = new TaskConsumerBuilder()
                .fromServer("http://localhost")
                .withCredentials("user", "pass")
                .fromQueue("testqueue")
                .withPollingFrequency(Duration.ofMinutes(1))
                .delayBetweenEachRetryOf(Duration.ofMinutes(15))
                .withActivityHealthCheck(Duration.ofMinutes(30), healthCheckRegistry)
                .consumeWith(consumerFunction)
                .build()

        expect:
        consumer != null
        healthCheckRegistry.names.size() == 1
        healthCheckRegistry.names.first() == "testqueue scheduling consumer"
    }

    @Unroll def "should enforce mandatory configuration options"()
    {
        when:
        builder.build()

        then:
        thrown(IllegalArgumentException)

        where:
        builder << [
                new TaskConsumerBuilder().withCredentials("user", "pass").fromQueue("testqueue").consumeWith(consumerFunction),
                new TaskConsumerBuilder().fromServer("http://localhost").fromQueue("testqueue").consumeWith(consumerFunction),
                new TaskConsumerBuilder().fromServer("http://localhost").withCredentials("user", "pass").consumeWith(consumerFunction),
                new TaskConsumerBuilder().fromServer("http://localhost").withCredentials("user", "pass").fromQueue("testqueue")
        ]
    }
}