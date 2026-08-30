package com.aile.takip.ui.viewmodel

import com.aile.takip.data.model.*
import com.aile.takip.data.repository.FamilyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FamilyRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `repository tasks flow returns correct data`() = runTest {
        val tasks = listOf(
            Task(id = "1", title = "Task 1", status = "bekleyen"),
            Task(id = "2", title = "Task 2", status = "tamamlanan"),
            Task(id = "3", title = "Task 3", status = "bekleyen")
        )

        whenever(repository.tasks).thenReturn(flowOf(tasks))

        val result = repository.tasks.first()

        assertEquals(3, result.size)
        assertEquals(2, result.count { it.status == "bekleyen" })
        assertEquals(1, result.count { it.status == "tamamlanan" })
    }

    @Test
    fun `repository notes flow returns correct data`() = runTest {
        val notes = listOf(
            Note(id = "1", title = "Note 1", category = "Genel", isPinned = true),
            Note(id = "2", title = "Note 2", category = "Alışveriş", isPinned = false)
        )

        whenever(repository.notes).thenReturn(flowOf(notes))

        val result = repository.notes.first()

        assertEquals(2, result.size)
        assertTrue(result[0].isPinned)
        assertFalse(result[1].isPinned)
    }

    @Test
    fun `repository search notes returns filtered results`() = runTest {
        val notes = listOf(
            Note(id = "1", title = "Shopping List", content = "Milk"),
            Note(id = "2", title = "Meeting Notes", content = "Discussion")
        )

        whenever(repository.searchNotes("shopping")).thenReturn(flowOf(listOf(notes[0])))

        val result = repository.searchNotes("shopping").first()

        assertEquals(1, result.size)
        assertEquals("Shopping List", result[0].title)
    }

    @Test
    fun `repository archived notes returns only archived`() = runTest {
        val archivedNotes = listOf(
            Note(id = "1", title = "Archived 1", isArchived = true),
            Note(id = "2", title = "Archived 2", isArchived = true)
        )

        whenever(repository.getArchivedNotes()).thenReturn(flowOf(archivedNotes))

        val result = repository.getArchivedNotes().first()

        assertEquals(2, result.size)
        assertTrue(result.all { it.isArchived })
    }

    @Test
    fun `repository notes by category returns filtered results`() = runTest {
        val shoppingNotes = listOf(
            Note(id = "1", title = "Shopping 1", category = "Alışveriş"),
            Note(id = "2", title = "Shopping 2", category = "Alışveriş")
        )

        whenever(repository.getNotesByCategory("Alışveriş")).thenReturn(flowOf(shoppingNotes))

        val result = repository.getNotesByCategory("Alışveriş").first()

        assertEquals(2, result.size)
        assertTrue(result.all { it.category == "Alışveriş" })
    }

    @Test
    fun `task model has correct status values`() {
        val pendingTask = Task(title = "Pending", status = "bekleyen")
        val completedTask = Task(title = "Completed", status = "tamamlanan")

        assertEquals("bekleyen", pendingTask.status)
        assertEquals("tamamlanan", completedTask.status)
    }

    @Test
    fun `task model has correct priority values`() {
        val lowPriority = Task(title = "Low", priority = "düşük")
        val mediumPriority = Task(title = "Medium", priority = "orta")
        val highPriority = Task(title = "High", priority = "yüksek")

        assertEquals("düşük", lowPriority.priority)
        assertEquals("orta", mediumPriority.priority)
        assertEquals("yüksek", highPriority.priority)
    }

    @Test
    fun `note model has correct color values`() {
        val blueNote = Note(title = "Blue", color = "#3498DB")
        val greenNote = Note(title = "Green", color = "#2ECC71")
        val redNote = Note(title = "Red", color = "#E74C3C")

        assertEquals("#3498DB", blueNote.color)
        assertEquals("#2ECC71", greenNote.color)
        assertEquals("#E74C3C", redNote.color)
    }

    @Test
    fun `note model has correct category values`() {
        val categories = listOf("Genel", "Alışveriş", "Tarif", "Fikir", "Önemli", "Plan", "Not")

        categories.forEach { category ->
            val note = Note(title = "Test", category = category)
            assertEquals(category, note.category)
        }
    }

    @Test
    fun `repository members flow returns correct data`() = runTest {
        val members = listOf(
            FamilyMember(id = "1", name = "Ali", role = "Baba", points = 100),
            FamilyMember(id = "2", name = "Ayşe", role = "Anne", points = 150),
            FamilyMember(id = "3", name = "Can", role = "Çocuk", points = 50)
        )

        whenever(repository.members).thenReturn(flowOf(members))

        val result = repository.members.first()

        assertEquals(3, result.size)
        assertEquals("Ali", result[0].name)
        assertEquals("Baba", result[0].role)
        assertEquals(100, result[0].points)
    }

    @Test
    fun `repository shopping items flow returns correct data`() = runTest {
        val items = listOf(
            ShoppingItem(id = "1", name = "Milk", quantity = 2, checked = false),
            ShoppingItem(id = "2", name = "Bread", quantity = 1, checked = true)
        )

        whenever(repository.shoppingItems).thenReturn(flowOf(items))

        val result = repository.shoppingItems.first()

        assertEquals(2, result.size)
        assertEquals(1, result.count { !it.checked })
        assertEquals(1, result.count { it.checked })
    }

    @Test
    fun `repository invoices flow returns correct data`() = runTest {
        val invoices = listOf(
            Invoice(id = "1", title = "Electricity", amount = 150.0, status = "pending"),
            Invoice(id = "2", title = "Water", amount = 80.0, status = "paid")
        )

        whenever(repository.invoices).thenReturn(flowOf(invoices))

        val result = repository.invoices.first()

        assertEquals(2, result.size)
        assertEquals(1, result.count { it.status == "pending" })
        assertEquals(1, result.count { it.status == "paid" })
    }

    @Test
    fun `repository messages flow returns correct data`() = runTest {
        val messages = listOf(
            Message(id = "1", senderName = "Ali", senderId = "1", content = "Hello", channel = "genel"),
            Message(id = "2", senderName = "Ayşe", senderId = "2", content = "Hi", channel = "genel")
        )

        whenever(repository.messages).thenReturn(flowOf(messages))

        val result = repository.messages.first()

        assertEquals(2, result.size)
        assertEquals("Ali", result[0].senderName)
        assertEquals("Hello", result[0].content)
    }

    @Test
    fun `repository calorie logs flow returns correct data`() = runTest {
        val logs = listOf(
            CalorieLog(id = "1", memberId = "1", mealType = "Kahvaltı", calories = 350),
            CalorieLog(id = "2", memberId = "1", mealType = "Öğle", calories = 500)
        )

        whenever(repository.calorieLogs).thenReturn(flowOf(logs))

        val result = repository.calorieLogs.first()

        assertEquals(2, result.size)
        assertEquals(850, result.sumOf { it.calories })
    }

    @Test
    fun `note model supports pinning`() {
        val note = Note(title = "Pinned Note", isPinned = true)
        val unpinnedNote = note.copy(isPinned = false)

        assertTrue(note.isPinned)
        assertFalse(unpinnedNote.isPinned)
    }

    @Test
    fun `note model supports archiving`() {
        val note = Note(title = "Archived Note", isArchived = true)
        val activeNote = note.copy(isArchived = false)

        assertTrue(note.isArchived)
        assertFalse(activeNote.isArchived)
    }

    @Test
    fun `task model supports status toggle`() {
        val pendingTask = Task(title = "Task", status = "bekleyen")
        val completedTask = pendingTask.copy(
            status = "tamamlanan",
            completedAt = System.currentTimeMillis()
        )

        assertEquals("bekleyen", pendingTask.status)
        assertEquals("tamamlanan", completedTask.status)
        assertNotNull(completedTask.completedAt)
    }
}
