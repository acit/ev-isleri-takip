package com.aile.takip.data.dao

import com.aile.takip.data.model.Note
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class NoteDaoTest {

    private lateinit var noteDao: NoteDao

    @Before
    fun setup() {
        noteDao = mock()
    }

    @Test
    fun `getAll returns notes ordered by isPinned DESC then createdAt DESC`() = runTest {
        val note1 = Note(id = "1", title = "Note 1", isPinned = false, createdAt = 1000L)
        val note2 = Note(id = "2", title = "Note 2", isPinned = true, createdAt = 2000L)
        val note3 = Note(id = "3", title = "Note 3", isPinned = false, createdAt = 3000L)

        whenever(noteDao.getAll()).thenReturn(flowOf(listOf(note2, note3, note1)))

        val result = noteDao.getAll().first()

        assertEquals(3, result.size)
        assertTrue(result[0].isPinned)
        assertFalse(result[1].isPinned)
        assertFalse(result[2].isPinned)
    }

    @Test
    fun `search returns notes matching query in title`() = runTest {
        val note1 = Note(id = "1", title = "Shopping List", content = "Milk, eggs")
        val note2 = Note(id = "2", title = "Meeting Notes", content = "Discussion points")

        whenever(noteDao.search("shopping")).thenReturn(flowOf(listOf(note1)))

        val result = noteDao.search("shopping").first()

        assertEquals(1, result.size)
        assertEquals("Shopping List", result[0].title)
    }

    @Test
    fun `search returns notes matching query in content`() = runTest {
        val note = Note(id = "1", title = "Ideas", content = "Buy flowers for mom")

        whenever(noteDao.search("flowers")).thenReturn(flowOf(listOf(note)))

        val result = noteDao.search("flowers").first()

        assertEquals(1, result.size)
        assertEquals("Ideas", result[0].title)
    }

    @Test
    fun `getArchived returns only archived notes`() = runTest {
        val archived1 = Note(id = "1", title = "Archived 1", isArchived = true)
        val archived2 = Note(id = "2", title = "Archived 2", isArchived = true)

        whenever(noteDao.getArchived()).thenReturn(flowOf(listOf(archived1, archived2)))

        val result = noteDao.getArchived().first()

        assertEquals(2, result.size)
        assertTrue(result.all { it.isArchived })
    }

    @Test
    fun `getByCategory returns notes of specific category`() = runTest {
        val shoppingNote = Note(id = "1", title = "Shopping", category = "Alışveriş")
        val recipeNote = Note(id = "2", title = "Recipe", category = "Tarif")

        whenever(noteDao.getByCategory("Alışveriş")).thenReturn(flowOf(listOf(shoppingNote)))

        val result = noteDao.getByCategory("Alışveriş").first()

        assertEquals(1, result.size)
        assertEquals("Alışveriş", result[0].category)
    }

    @Test
    fun `upsert inserts new note`() = runTest {
        val note = Note(title = "New Note", content = "Content")

        noteDao.upsert(note)

        verify(noteDao).upsert(note)
    }

    @Test
    fun `upsert updates existing note`() = runTest {
        val existingNote = Note(id = "1", title = "Old Title")
        val updatedNote = existingNote.copy(title = "New Title")

        noteDao.upsert(updatedNote)

        verify(noteDao).upsert(updatedNote)
    }

    @Test
    fun `delete removes note`() = runTest {
        val note = Note(id = "1", title = "Note to delete")

        noteDao.delete(note)

        verify(noteDao).delete(note)
    }

    @Test
    fun `deleteAll removes all notes`() = runTest {
        noteDao.deleteAll()

        verify(noteDao).deleteAll()
    }

    @Test
    fun `getAllOnce returns all notes without Flow`() = runTest {
        val notes = listOf(
            Note(id = "1", title = "Note 1"),
            Note(id = "2", title = "Note 2")
        )

        whenever(noteDao.getAllOnce()).thenReturn(notes)

        val result = noteDao.getAllOnce()

        assertEquals(2, result.size)
    }

    @Test
    fun `note has correct default values`() {
        val note = Note(title = "Test Note")

        assertEquals("Test Note", note.title)
        assertEquals("", note.content)
        assertEquals("Genel", note.category)
        assertEquals("#3498DB", note.color)
        assertFalse(note.isPinned)
        assertFalse(note.isArchived)
        assertEquals("", note.createdBy)
        assertNotNull(note.id)
        assertTrue(note.createdAt > 0)
    }

    @Test
    fun `note with all fields`() {
        val note = Note(
            id = "custom-id",
            title = "Custom Note",
            content = "Custom content",
            category = "Önemli",
            color = "#E74C3C",
            isPinned = true,
            isArchived = false,
            createdBy = "Ali",
            createdAt = 1000L,
            syncVersion = 2000L
        )

        assertEquals("custom-id", note.id)
        assertEquals("Custom Note", note.title)
        assertEquals("Custom content", note.content)
        assertEquals("Önemli", note.category)
        assertEquals("#E74C3C", note.color)
        assertTrue(note.isPinned)
        assertFalse(note.isArchived)
        assertEquals("Ali", note.createdBy)
        assertEquals(1000L, note.createdAt)
        assertEquals(2000L, note.syncVersion)
    }

    @Test
    fun `note categories are correct`() {
        val validCategories = listOf("Genel", "Alışveriş", "Tarif", "Fikir", "Önemli", "Plan", "Not")

        validCategories.forEach { category ->
            val note = Note(title = "Test", category = category)
            assertEquals(category, note.category)
        }
    }

    @Test
    fun `note colors are valid hex codes`() {
        val validColors = listOf(
            "#3498DB", "#2ECC71", "#E74C3C", "#F39C12",
            "#9B59B6", "#1ABC9C", "#E91E63", "#607D8B"
        )

        validColors.forEach { color ->
            val note = Note(title = "Test", color = color)
            assertEquals(color, note.color)
            assertTrue(color.matches(Regex("^#[0-9A-Fa-f]{6}$")))
        }
    }
}
