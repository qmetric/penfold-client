package com.qmetric.penfold.client.app

import com.qmetric.penfold.client.app.support.Credentials
import com.qmetric.penfold.client.app.support.ObjectMapperFactory
import com.qmetric.penfold.client.domain.model.*
import com.theoryinpractise.halbuilder.api.RepresentationFactory
import groovy.json.JsonSlurper
import spock.lang.Specification

import javax.ws.rs.client.Client
import javax.ws.rs.client.Entity
import javax.ws.rs.client.Invocation
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.Response
import java.time.LocalDateTime

import static java.util.Optional.empty

class TaskStoreServiceImplTest extends Specification {

    static final taskId = new TaskId("1")

    static final queueId = new QueueId("q1")

    static final created = LocalDateTime.of(2014, 3, 15, 10, 35, 5)

    static final attempts = 0

    static final payload = new Payload([type: "type1"])

    static final triggerDate = LocalDateTime.of(2015, 4, 15, 10, 35, 5)

    final client = Mock(Client)

    final builder = Mock(Invocation.Builder)

    final webTarget = Mock(WebTarget)

    final store = new TaskStoreServiceImpl("http://localhost", credentials(), client, ObjectMapperFactory.create())

    def "should create task"()
    {
        given:
        final response = response(201, "/fixtures/api/create_task_response.json")
        final builder = setupRequestBuilder("http://localhost/tasks")

        when:
        store.create(new NewTask(queueId, payload, empty()))

        then:
        1 * builder.post(_ as Entity) >> { entity ->
            assertExpectedJson((entity.get(0) as Entity).entity as String, "/fixtures/api/command/create_task_command.json")
            (entity.get(0) as Entity).mediaType.type == contentType(CommandType.CreateTask)
            response
        }
    }

    def "should create future task"()
    {
        given:
        final response = response(201, "/fixtures/api/create_future_task_response.json")
        final builder = setupRequestBuilder("http://localhost/tasks")

        when:
        store.create(new NewTask(queueId, payload, Optional.of(triggerDate)))

        then:
        1 * builder.post(_ as Entity) >> { entity ->
            assertExpectedJson((entity.get(0) as Entity).entity as String, "/fixtures/api/command/create_future_task_command.json")
            (entity.get(0) as Entity).mediaType.type == contentType(CommandType.CreateFutureTask)
            response
        }
    }

    def "should start task"()
    {
        given:
        setupTaskRetrievalResponse("/fixtures/api/task.json")
        setupTaskCommand("http://localhost/tasks/1/2")
        final postResponse = response(200, "/fixtures/api/start_task_response.json")

        when:
        store.start(createTask())

        then:
        1 * builder.post(_ as Entity) >> { entity ->
            assertExpectedJson((entity.get(0) as Entity).entity as String, "/fixtures/api/command/start_task_command.json")
            (entity.get(0) as Entity).mediaType.type == contentType(CommandType.StartTask)
            postResponse
        }
    }

    def "should requeue task"()
    {
        given:
        setupTaskRetrievalResponse("/fixtures/api/task.json")
        setupTaskCommand("http://localhost/tasks/1/2")
        final postResponse = response(200, "/fixtures/api/requeue_task_response.json")

        when:
        store.requeue(createTask(), Optional.of("reason1"))

        then:
        1 * builder.post(_ as Entity) >> { entity ->
            assertExpectedJson((entity.get(0) as Entity).entity as String, "/fixtures/api/command/requeue_task_command.json")
            (entity.get(0) as Entity).mediaType.type == contentType(CommandType.RequeueTask)
            postResponse
        }
    }

    def "should reschedule task"()
    {
        given:
        setupTaskRetrievalResponse("/fixtures/api/task.json")
        setupTaskCommand("http://localhost/tasks/1/2")
        final postResponse = response(200, "/fixtures/api/reschedule_task_response.json")

        when:
        store.reschedule(createTask(), triggerDate, Optional.of("reason1"))

        then:
        1 * builder.post(_ as Entity) >> { entity ->
            assertExpectedJson((entity.get(0) as Entity).getEntity() as String, "/fixtures/api/command/reschedule_task_command.json")
            (entity.get(0) as Entity).mediaType.type == contentType(CommandType.RescheduleTask)
            postResponse
        }
    }

    def "should close task"()
    {
        given:
        setupTaskRetrievalResponse("/fixtures/api/task.json")
        setupTaskCommand("http://localhost/tasks/1/2")
        final postResponse = response(200, "/fixtures/api/close_task_response.json")

        when:
        store.close(createTask(), Optional.of(CloseResultType.success), Optional.of("reason1"))

        then:
        1 * builder.post(_ as Entity) >> { entity ->
            assertExpectedJson((entity.get(0) as Entity).entity as String, "/fixtures/api/command/close_task_command.json")
            (entity.get(0) as Entity).mediaType.type == contentType(CommandType.CloseTask)
            postResponse
        }
    }

    def "should cancel task"()
    {
        given:
        setupTaskRetrievalResponse("/fixtures/api/task.json")
        setupTaskCommand("http://localhost/tasks/1/2")
        final postResponse = response(200, "/fixtures/api/cancel_task_response.json")

        when:
        store.cancel(createTask(), Optional.of("reason1"))

        then:
        1 * builder.post(_ as Entity) >> { entity ->
            assertExpectedJson((entity.get(0) as Entity).entity as String, "/fixtures/api/command/cancel_task_command.json")
            (entity.get(0) as Entity).mediaType.type == contentType(CommandType.CancelTask)
            postResponse
        }
    }

    private def setupTaskRetrievalResponse(final String json, final int status = 200)
    {
        final Response response = response(status)
        response.readEntity(String.class) >> this.getClass().getResource(json).text
        final builder = Mock(Invocation.Builder)
        final webTarget = Mock(WebTarget)
        client.target("http://localhost/tasks/${taskId}") >> webTarget
        webTarget.request(RepresentationFactory.HAL_JSON) >> builder
        builder.get() >> response
        response
    }

    private void assertExpectedJson(final String json, final String expectedJson)
    {
        assert new JsonSlurper().parseText(json as String) == new JsonSlurper().parseText(this.getClass().getResource(expectedJson).text)
    }

    private def setupRequestBuilder(final String url)
    {
        final builder = Mock(Invocation.Builder)
        final webTarget = Mock(WebTarget)
        client.target(url) >> webTarget
        webTarget.request(RepresentationFactory.HAL_JSON) >> builder
        builder
    }

    private def response(final statusCode)
    {
        final response = Mock(Response)
        response.getStatus() >> statusCode
        response
    }

    private def response(final statusCode, final String jsonPath)
    {
        final response = response(statusCode)
        response.readEntity(String.class) >> this.getClass().getResource(jsonPath).text
        response
    }

    private def setupTaskCommand(final String url)
    {
        client.target(url) >> webTarget
        webTarget.request(RepresentationFactory.HAL_JSON) >> builder
    }

    private static def contentType(final CommandType commandType)
    {
        return "application/json;domain-command=" + commandType.name()
    }

    private static Credentials credentials()
    {
        return new Credentials("user", "pwd")
    }

    private static Task createTask()
    {
        return new Task(taskId, 2, queueId, TaskStatus.READY, created, triggerDate, attempts, payload)
    }
}
