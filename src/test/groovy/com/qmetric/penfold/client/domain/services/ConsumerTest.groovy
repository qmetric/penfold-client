package com.qmetric.penfold.client.domain.services

import com.github.rholder.retry.RetryerBuilder
import com.qmetric.penfold.client.app.support.LocalDateTimeSource
import com.qmetric.penfold.client.domain.exceptions.ConflictException
import com.qmetric.penfold.client.domain.model.*
import com.qmetric.penfold.client.domain.services.events.*
import spock.lang.Specification

import java.time.Duration
import java.time.LocalDateTime

import static com.github.rholder.retry.StopStrategies.stopAfterAttempt
import static com.qmetric.penfold.client.domain.model.TaskStatus.*
import static java.util.Optional.empty
import static org.apache.commons.collections.CollectionUtils.isNotEmpty

class ConsumerTest extends Specification {

    static final queueId = new QueueId("q1")

    static final retryDelay = Duration.ofHours(2)

    static final retryDelayInSeconds = 7200

    static final failureReason = Optional.of("err")

    static final readyTask1 = new Task(new TaskId("t1"), 1, queueId, READY, LocalDateTime.now(), LocalDateTime.now(), 0, new Payload([:]))

    static final readyTask2 = new Task(new TaskId("t2"), 1, queueId, READY, LocalDateTime.now(), LocalDateTime.now(), 0, new Payload([:]))

    static final startedTask1 = readyTask1.builder().withStatus(STARTED).build()

    static final startedTask2 = readyTask2.builder().withStatus(STARTED).build()

    final consumerFunction = Stub(ConsumerFunction)

    final taskQueryService = Stub(TaskQueryService)

    final dateTimeSource = Stub(LocalDateTimeSource)

    final taskStoreService = Mock(TaskStoreService)

    final listener = new EventListenerStub()

    final notifier = new Notifier(listener)

    final now = LocalDateTime.now()

    final consumer = new Consumer(queueId, consumerFunction, Optional.of(retryDelay), taskQueryService, taskStoreService, dateTimeSource, notifier)

    def setup()
    {
        dateTimeSource.now() >> now
        taskStoreService.start(readyTask1) >> startedTask1
        taskStoreService.start(readyTask2) >> startedTask2
        listener.reset()
        assert notifier.getListeners().size() == 1
    }

    def "should consume tasks successfully"()
    {
        given:
        taskQueryService.find(queueId, READY, []) >> [readyTask1, readyTask2].iterator()
        consumerFunction.execute(startedTask1) >> Reply.success()
        consumerFunction.execute(startedTask2) >> Reply.success()
        taskQueryService.find(startedTask1.id) >> Optional.of(startedTask1)
        taskQueryService.find(startedTask2.id) >> Optional.of(startedTask2)

        when:
        consumer.consume()

        then:
        1 * taskStoreService.close(startedTask1, Optional.of(CloseResultType.success), empty())
        1 * taskStoreService.close(startedTask2, Optional.of(CloseResultType.success), empty())
        listener.receivedEvents == [new TaskConsumedEvent(readyTask1.id), new TaskConsumedEvent(readyTask2.id), new QueueConsumedEvent(queueId)]
    }

    def "should close task on consume failure"()
    {
        given:
        taskQueryService.find(queueId, READY, []) >> [readyTask1, readyTask2].iterator()
        consumerFunction.execute(startedTask1) >> Reply.fail(failureReason)
        consumerFunction.execute(startedTask2) >> Reply.success()
        taskQueryService.find(startedTask1.id) >> Optional.of(startedTask1)
        taskQueryService.find(startedTask2.id) >> Optional.of(startedTask2)

        when:
        consumer.consume()

        then:
        1 * taskStoreService.close(startedTask1, Optional.of(CloseResultType.failure), failureReason)
        1 * taskStoreService.close(startedTask2, Optional.of(CloseResultType.success), empty())
        listener.receivedEvents == [new TaskConsumedEvent(readyTask1.id), new TaskConsumedEvent(readyTask2.id), new QueueConsumedEvent(queueId)]
    }

    def "should reschedule task on consume failure when delayed retry applicable"()
    {
        given:
        taskQueryService.find(queueId, READY, []) >> [readyTask1, readyTask2].iterator()
        consumerFunction.execute(startedTask1) >> Reply.retry(failureReason)
        consumerFunction.execute(startedTask2) >> Reply.success()
        taskQueryService.find(startedTask1.id) >> Optional.of(startedTask1)
        taskQueryService.find(startedTask2.id) >> Optional.of(startedTask2)

        when:
        consumer.consume()

        then:
        1 * taskStoreService.reschedule(startedTask1, now.plusSeconds(retryDelayInSeconds), failureReason)
        1 * taskStoreService.close(startedTask2, Optional.of(CloseResultType.success), empty())
        listener.receivedEvents == [new TaskConsumedEvent(readyTask1.id), new TaskConsumedEvent(readyTask2.id), new QueueConsumedEvent(queueId)]
    }

    def "should ignore tasks where consumer function updates task status"()
    {
        given:
        taskQueryService.find(queueId, READY, []) >> [readyTask1].iterator()
        consumerFunction.execute(startedTask1) >> Reply.success()
        taskQueryService.find(startedTask1.id) >> Optional.of(startedTask1.builder().withStatus(CLOSED).build())

        when:
        consumer.consume()

        then:
        0 * taskStoreService.close(_ as Task, _ as Optional<CloseResultType>, _ as Optional<String>)
        0 * taskStoreService.reschedule(_ as Task, _ as LocalDateTime, _ as Optional<String>)
        0 * taskStoreService.requeue(_ as Task, _ as Optional<String>)
        listener.receivedEvents == [new TaskConsumedEvent(readyTask1.id), new QueueConsumedEvent(queueId)]
    }

    def "should requeue task on consume failure when delayed retry applicable"()
    {
        given:
        final consumer = new Consumer(queueId, consumerFunction, empty(), taskQueryService, taskStoreService, dateTimeSource, notifier)
        taskQueryService.find(queueId, READY, []) >> [readyTask1].iterator()
        consumerFunction.execute(startedTask1) >> Reply.retry(failureReason)
        taskQueryService.find(startedTask1.id) >> Optional.of(startedTask1)

        when:
        consumer.consume()

        then:
        1 * taskStoreService.requeue(startedTask1, failureReason)
        listener.receivedEvents == [new TaskConsumedEvent(readyTask1.id), new QueueConsumedEvent(queueId)]
    }

    def "should ignore task conflicts"()
    {
        given:
        final readyTaskAlreadyStarted = readyTask1.builder().withQueue(new QueueId("q2")).build()
        taskStoreService.start(readyTaskAlreadyStarted) >> {throw new ConflictException("")}
        taskQueryService.find(queueId, READY, []) >> [readyTask1, readyTaskAlreadyStarted, readyTask2].iterator()
        consumerFunction.execute(startedTask1) >> Reply.success()
        consumerFunction.execute(startedTask2) >> Reply.success()
        taskQueryService.find(startedTask1.id) >> Optional.of(startedTask1)
        taskQueryService.find(startedTask2.id) >> Optional.of(startedTask2)

        when:
        consumer.consume()

        then:
        1 * taskStoreService.close(startedTask1, Optional.of(CloseResultType.success), empty())
        1 * taskStoreService.close(startedTask2, Optional.of(CloseResultType.success), empty())
        listener.receivedEvents == [new TaskConsumedEvent(readyTask1.id), new TaskConsumedEvent(readyTaskAlreadyStarted.id), new TaskConsumedEvent(readyTask2.id), new QueueConsumedEvent(queueId)]
    }

    def "should retry when attempt to close a started task fails - after max retries we quit attempt to consume from queue"()
    {
        given:
        final retryBuilder = RetryerBuilder.<Void> newBuilder().retryIfException().withStopStrategy(stopAfterAttempt(2))
        final consumer = new Consumer(queueId, consumerFunction, Optional.of(retryDelay), taskQueryService, taskStoreService, dateTimeSource, notifier, retryBuilder)
        taskQueryService.find(queueId, READY, []) >> [readyTask1].iterator()
        consumerFunction.execute(startedTask1) >> Reply.fail(failureReason)
        taskQueryService.find(startedTask1.id) >> Optional.of(startedTask1)

        when:
        consumer.consume()

        then:
        2 * taskStoreService.close(startedTask1, Optional.of(CloseResultType.failure), failureReason) >> { throw new RuntimeException() }
        listener.receivedEvents == []
        thrown(RuntimeException)
    }

    def "should not notify anything when no listeners configured"()
    {
        given:
        final consumerNoListeners = new Consumer(queueId, consumerFunction, Optional.of(retryDelay), taskQueryService, taskStoreService, dateTimeSource, new Notifier())
        taskQueryService.find(queueId, READY, []) >> [readyTask1].iterator()
        consumerFunction.execute(startedTask1) >> Reply.success()
        taskQueryService.find(startedTask1.id) >> Optional.of(startedTask1)

        when:
        consumerNoListeners.consume()

        then:
        isNotEmpty(notifier.getListeners())
    }

    def "should notify multiple listeners"()
    {
        given:
        final listener1 = new EventListenerStub()
        final listener2 = new EventListenerStub()
        final notifierWithTwoListeners = new Notifier(listener1, listener2)
        final consumerNoListeners = new Consumer(queueId, consumerFunction, Optional.of(retryDelay), taskQueryService, taskStoreService, dateTimeSource, notifierWithTwoListeners)
        taskQueryService.find(queueId, READY, []) >> [readyTask1].iterator()
        consumerFunction.execute(startedTask1) >> Reply.success()
        taskQueryService.find(startedTask1.id) >> Optional.of(startedTask1)

        when:
        consumerNoListeners.consume()

        then:
        notifierWithTwoListeners.getListeners().size() == 2
        listener1.receivedEvents == [new TaskConsumedEvent(readyTask1.id), new QueueConsumedEvent(queueId)]
        listener2.receivedEvents == [new TaskConsumedEvent(readyTask1.id), new QueueConsumedEvent(queueId)]
    }

    private class EventListenerStub implements EventListener
    {
        final List<Event> receivedEvents = new ArrayList<>();

        void reset()
        {
            receivedEvents.clear()
        }

        @Override
        void notify(final Event event)
        {
            receivedEvents.add(event)
        }
    }
}
