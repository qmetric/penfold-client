package com.qmetric.penfold.client.app.support

import com.qmetric.penfold.client.domain.services.Consumer
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.time.LocalDateTime

class ConsumerThreadActivityHealthCheckTest extends Specification {
    final dateTimeSource = Mock(LocalDateTimeSource)

    final consumerDelegate = Mock(Consumer)

    def "should be unhealthy if never any activity"()
    {
        given:
        final healthCheck = new ConsumerThreadActivityHealthCheck(consumerDelegate, dateTimeSource, Duration.ofMinutes(1))

        when:
        final result = healthCheck.check()

        then:
        !result.isHealthy()
    }

    @Unroll def "should evaluate health of consumer using date of last activity"()
    {
        given:
        final healthCheck = new ConsumerThreadActivityHealthCheck(consumerDelegate, dateTimeSource, interval)
        dateTimeSource.now() >>> [lastConsumedDate, currentDate]

        when:
        healthCheck.consume()

        then:
        healthCheck.check().isHealthy() == expectedHealthyResult

        where:
        interval              | lastConsumedDate                          | currentDate                                | expectedHealthyResult
        Duration.ofMinutes(1) | LocalDateTime.of(2013, 7, 19, 0, 0, 0, 0) | LocalDateTime.of(2013, 7, 19, 0, 1, 0, 0)  | true
        Duration.ofMinutes(1) | LocalDateTime.of(2013, 7, 19, 0, 0, 0, 0) | LocalDateTime.of(2013, 7, 19, 0, 0, 59, 0) | true
        Duration.ofMinutes(1) | LocalDateTime.of(2013, 7, 19, 0, 0, 0, 0) | LocalDateTime.of(2013, 7, 19, 0, 1, 1, 0)  | false
        Duration.ofSeconds(1) | LocalDateTime.of(2013, 7, 19, 0, 0, 0, 0) | LocalDateTime.of(2013, 7, 19, 0, 0, 1, 0)  | true
        Duration.ofSeconds(1) | LocalDateTime.of(2013, 7, 19, 0, 0, 0, 0) | LocalDateTime.of(2013, 7, 19, 0, 0, 2, 0)  | false
    }

    def "should update date of last activity after consuming each task"()
    {
        given:
        final healthCheck = new ConsumerThreadActivityHealthCheck(consumerDelegate, dateTimeSource, Duration.ofMinutes(1))
        dateTimeSource.now() >> LocalDateTime.of(2013, 7, 19, 0, 0, 0, 0)

        when:
        healthCheck.consumeTask(null)

        then:
        healthCheck.check().isHealthy()
    }

    def "should update date of last activity after consuming all tasks"()
    {
        given:
        final healthCheck = new ConsumerThreadActivityHealthCheck(consumerDelegate, dateTimeSource, Duration.ofMinutes(1))
        dateTimeSource.now() >> LocalDateTime.of(2013, 7, 19, 0, 0, 0, 0)

        when:
        healthCheck.consume()

        then:
        healthCheck.check().isHealthy()
    }
}
