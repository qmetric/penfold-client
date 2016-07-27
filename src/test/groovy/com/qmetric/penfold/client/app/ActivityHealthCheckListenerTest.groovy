package com.qmetric.penfold.client.app

import com.qmetric.penfold.client.app.support.LocalDateTimeSource
import com.qmetric.penfold.client.domain.model.QueueId
import com.qmetric.penfold.client.domain.model.TaskId
import com.qmetric.penfold.client.domain.services.events.QueueConsumedEvent
import com.qmetric.penfold.client.domain.services.events.TaskConsumedEvent
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.time.LocalDateTime

class ActivityHealthCheckListenerTest extends Specification {

    static final QueueId queueId = new QueueId("q1")

    final dateTimeSource = Mock(LocalDateTimeSource)

    def "should be unhealthy if never any activity"()
    {
        given:
        final healthCheck = new ActivityHealthCheckListener(dateTimeSource, Duration.ofMinutes(1))

        when:
        final result = healthCheck.check()

        then:
        !result.isHealthy()
    }

    @Unroll def "should evaluate health of consumer using date of last activity"()
    {
        given:
        final healthCheck = new ActivityHealthCheckListener(dateTimeSource, interval)
        dateTimeSource.now() >>> [lastConsumedDate, currentDate]
        assert !healthCheck.check().isHealthy()

        when:
        healthCheck.notify(new QueueConsumedEvent(queueId))

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
        final now = LocalDateTime.of(2013, 7, 19, 0, 0, 0, 0)
        dateTimeSource.now() >> now
        final healthCheck = new ActivityHealthCheckListener(dateTimeSource, Duration.ofMinutes(1))
        assert !healthCheck.check().isHealthy()

        when:
        healthCheck.notify(new TaskConsumedEvent(new TaskId("t1")))

        then:
        healthCheck.check().isHealthy()
    }

    def "should update date of last activity after consuming queue without any current tasks to consume"()
    {
        given:
        final healthCheck = new ActivityHealthCheckListener(dateTimeSource, Duration.ofMinutes(1))
        dateTimeSource.now() >> LocalDateTime.of(2013, 7, 19, 0, 0, 0, 0)
        assert !healthCheck.check().isHealthy()

        when:
        healthCheck.notify(new QueueConsumedEvent(queueId))

        then:
        healthCheck.check().isHealthy()
    }
}
