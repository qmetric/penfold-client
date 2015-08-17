package com.qmetric.penfold.client.domain.services

import com.google.common.collect.ImmutableList
import com.qmetric.penfold.client.app.TaskQueryServiceImpl
import com.qmetric.penfold.client.domain.model.*
import spock.lang.Specification

import java.time.LocalDateTime

import static com.qmetric.penfold.client.domain.model.TaskStatus.READY
import static java.util.Optional.empty

class QueueIteratorTest extends Specification {

    public static final QueueId queue = new QueueId("q1")

    public static final TaskStatus status = READY

    final taskQueryService = Mock(TaskQueryServiceImpl)

    def "should paginate through all pages of stored tasks"()
    {
        given:
        final task1 = createTask("1")
        final task2 = createTask("2")
        final task3 = createTask("3")
        final task4 = createTask("4")
        final task5 = createTask("5")
        final task6 = createTask("6")
        setupTasksPage(empty(), [task1, task2, task3], Optional.of(new PageReference("2")))
        setupTasksPage(Optional.of(new PageReference("2")), [task4, task5], Optional.of(new PageReference("3")))
        setupTasksPage(Optional.of(new PageReference("3")), [task6], empty())

        when:
        final readyTaskIterator = new QueueIterator(queue, status, [], taskQueryService)

        then:
        ImmutableList.copyOf(readyTaskIterator) == [task1, task2, task3, task4, task5, task6]
    }

    private static def Task createTask(final String id)
    {
        return new Task(new TaskId(id), 1, queue, status, LocalDateTime.of(2014, 2, 25, 12, 0, 0), LocalDateTime.of(2014, 2, 25, 12, 0, 0), 1, new Payload([type: "type1"]))
    }

    private def setupTasksPage(final Optional<PageReference> currentPage, final List<Task> currentPageTasks, final Optional<PageReference> nextPage)
    {
        taskQueryService.retrieve(queue, status, [], currentPage) >> new TasksPage(currentPageTasks, empty(), nextPage)
    }
}
