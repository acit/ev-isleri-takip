package com.aile.takip.data.dao

import com.aile.takip.data.model.Task
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class TaskDaoTest {

    private lateinit var taskDao: TaskDao

    @Before
    fun setup() {
        taskDao = mock()
    }

    @Test
    fun `getAll returns tasks ordered by createdAt DESC`() = runTest {
        val task1 = Task(id = "1", title = "Task 1", createdAt = 1000L)
        val task2 = Task(id = "2", title = "Task 2", createdAt = 2000L)
        val task3 = Task(id = "3", title = "Task 3", createdAt = 3000L)

        whenever(taskDao.getAll()).thenReturn(flowOf(listOf(task3, task2, task1)))

        val result = taskDao.getAll().first()

        assertEquals(3, result.size)
        assertEquals("Task 3", result[0].title)
        assertEquals("Task 1", result[2].title)
    }

    @Test
    fun `upsert inserts new task`() = runTest {
        val task = Task(title = "New Task", description = "Description")

        taskDao.upsert(task)

        verify(taskDao).upsert(task)
    }

    @Test
    fun `upsert updates existing task`() = runTest {
        val existingTask = Task(id = "1", title = "Old Title", status = "bekleyen")
        val updatedTask = existingTask.copy(title = "New Title", status = "tamamlanan")

        taskDao.upsert(updatedTask)

        verify(taskDao).upsert(updatedTask)
    }

    @Test
    fun `upsertAll inserts multiple tasks`() = runTest {
        val tasks = listOf(
            Task(title = "Task 1"),
            Task(title = "Task 2"),
            Task(title = "Task 3")
        )

        taskDao.upsertAll(tasks)

        verify(taskDao).upsertAll(tasks)
    }

    @Test
    fun `delete removes task`() = runTest {
        val task = Task(id = "1", title = "Task to delete")

        taskDao.delete(task)

        verify(taskDao).delete(task)
    }

    @Test
    fun `deleteAll removes all tasks`() = runTest {
        taskDao.deleteAll()

        verify(taskDao).deleteAll()
    }

    @Test
    fun `getAllOnce returns all tasks without Flow`() = runTest {
        val tasks = listOf(
            Task(id = "1", title = "Task 1"),
            Task(id = "2", title = "Task 2")
        )

        whenever(taskDao.getAllOnce()).thenReturn(tasks)

        val result = taskDao.getAllOnce()

        assertEquals(2, result.size)
        assertEquals("Task 1", result[0].title)
        assertEquals("Task 2", result[1].title)
    }

    @Test
    fun `task has correct default values`() {
        val task = Task(title = "Test Task")

        assertEquals("Test Task", task.title)
        assertEquals("", task.description)
        assertEquals("Genel", task.category)
        assertEquals("orta", task.priority)
        assertEquals("", task.assignee)
        assertEquals("bekleyen", task.status)
        assertEquals("", task.dueDate)
        assertNotNull(task.id)
        assertTrue(task.createdAt > 0)
        assertNull(task.completedAt)
    }

    @Test
    fun `task with all fields`() {
        val task = Task(
            id = "custom-id",
            title = "Custom Task",
            description = "Custom description",
            category = "İş",
            priority = "yüksek",
            assignee = "Ali",
            status = "tamamlanan",
            dueDate = "2024-03-15",
            createdAt = 1000L,
            completedAt = 2000L,
            syncVersion = 3000L
        )

        assertEquals("custom-id", task.id)
        assertEquals("Custom Task", task.title)
        assertEquals("Custom description", task.description)
        assertEquals("İş", task.category)
        assertEquals("yüksek", task.priority)
        assertEquals("Ali", task.assignee)
        assertEquals("tamamlanan", task.status)
        assertEquals("2024-03-15", task.dueDate)
        assertEquals(1000L, task.createdAt)
        assertEquals(2000L, task.completedAt)
        assertEquals(3000L, task.syncVersion)
    }
}
