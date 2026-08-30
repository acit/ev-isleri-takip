package com.aile.takip.data.repository

import com.aile.takip.data.dao.*
import com.aile.takip.data.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class FamilyRepositoryTest {

    private lateinit var taskDao: TaskDao
    private lateinit var noteDao: NoteDao
    private lateinit var memberDao: MemberDao
    private lateinit var shoppingDao: ShoppingDao
    private lateinit var invoiceDao: InvoiceDao
    private lateinit var messageDao: MessageDao
    private lateinit var inventoryDao: InventoryDao
    private lateinit var budgetDao: BudgetDao
    private lateinit var expenseDao: ExpenseDao
    private lateinit var mealPlanDao: MealPlanDao
    private lateinit var sportsClubDao: SportsClubDao
    private lateinit var workoutLogDao: WorkoutLogDao
    private lateinit var calorieLogDao: CalorieLogDao
    private lateinit var menstrualCycleDao: MenstrualCycleDao
    private lateinit var authDao: AuthDao
    private lateinit var syncEventDao: SyncEventDao
    private lateinit var reminderDao: ReminderDao
    private lateinit var waterLogDao: WaterLogDao
    private lateinit var sleepLogDao: SleepLogDao
    private lateinit var repository: FamilyRepository

    @Before
    fun setup() {
        taskDao = mock()
        noteDao = mock()
        memberDao = mock()
        shoppingDao = mock()
        invoiceDao = mock()
        messageDao = mock()
        inventoryDao = mock()
        budgetDao = mock()
        expenseDao = mock()
        mealPlanDao = mock()
        sportsClubDao = mock()
        workoutLogDao = mock()
        calorieLogDao = mock()
        menstrualCycleDao = mock()
        authDao = mock()
        syncEventDao = mock()
        reminderDao = mock()
        waterLogDao = mock()
        sleepLogDao = mock()

        // Set up all DAO return values BEFORE creating repository
        whenever(taskDao.getAll()).thenReturn(flowOf(emptyList()))
        whenever(noteDao.getAll()).thenReturn(flowOf(emptyList()))
        whenever(memberDao.getAll()).thenReturn(flowOf(emptyList()))
        whenever(shoppingDao.getAll()).thenReturn(flowOf(emptyList()))
        whenever(invoiceDao.getAll()).thenReturn(flowOf(emptyList()))
        whenever(messageDao.getAll()).thenReturn(flowOf(emptyList()))
        whenever(inventoryDao.getAll()).thenReturn(flowOf(emptyList()))
        whenever(budgetDao.getAll()).thenReturn(flowOf(emptyList()))
        whenever(expenseDao.getAll()).thenReturn(flowOf(emptyList()))
        whenever(mealPlanDao.getAll()).thenReturn(flowOf(emptyList()))
        whenever(sportsClubDao.getAll()).thenReturn(flowOf(emptyList()))
        whenever(workoutLogDao.getAll()).thenReturn(flowOf(emptyList()))
        whenever(calorieLogDao.getAll()).thenReturn(flowOf(emptyList()))
        whenever(menstrualCycleDao.getAll()).thenReturn(flowOf(emptyList()))
        whenever(authDao.get()).thenReturn(flowOf(null))
        whenever(syncEventDao.getUnsynced()).thenReturn(flowOf(emptyList()))
        whenever(reminderDao.getActive()).thenReturn(flowOf(emptyList()))
        whenever(reminderDao.getAll()).thenReturn(flowOf(emptyList()))
        whenever(waterLogDao.getAll()).thenReturn(flowOf(emptyList()))
        whenever(sleepLogDao.getAll()).thenReturn(flowOf(emptyList()))

        repository = FamilyRepository(
            taskDao = taskDao,
            inventoryDao = inventoryDao,
            budgetDao = budgetDao,
            expenseDao = expenseDao,
            invoiceDao = invoiceDao,
            messageDao = messageDao,
            shoppingDao = shoppingDao,
            memberDao = memberDao,
            mealPlanDao = mealPlanDao,
            sportsClubDao = sportsClubDao,
            workoutLogDao = workoutLogDao,
            calorieLogDao = calorieLogDao,
            menstrualCycleDao = menstrualCycleDao,
            authDao = authDao,
            syncEventDao = syncEventDao,
            noteDao = noteDao,
            reminderDao = reminderDao,
            waterLogDao = waterLogDao,
            sleepLogDao = sleepLogDao
        )
    }

    // ===== TASKS =====

    @Test
    fun `tasks flow delegates to taskDao`() = runTest {
        val tasks = listOf(Task(id = "1", title = "Task 1"))
        whenever(taskDao.getAll()).thenReturn(flowOf(tasks))

        val repo = FamilyRepository(taskDao, inventoryDao, budgetDao, expenseDao,
            invoiceDao, messageDao, shoppingDao, memberDao, mealPlanDao,
            sportsClubDao, workoutLogDao, calorieLogDao, menstrualCycleDao,
            authDao, syncEventDao, noteDao, reminderDao, waterLogDao, sleepLogDao)

        val result = repo.tasks.first()

        assertEquals(1, result.size)
        assertEquals("Task 1", result[0].title)
    }

    @Test
    fun `upsertTask delegates to taskDao upsert`() = runTest {
        val task = Task(title = "New Task")
        repository.upsertTask(task)
        verify(taskDao).upsert(task)
    }

    @Test
    fun `deleteTask delegates to taskDao delete`() = runTest {
        val task = Task(id = "1", title = "Task to delete")
        repository.deleteTask(task)
        verify(taskDao).delete(task)
    }

    // ===== NOTES =====

    @Test
    fun `notes flow delegates to noteDao`() = runTest {
        val notes = listOf(Note(id = "1", title = "Note 1"))
        whenever(noteDao.getAll()).thenReturn(flowOf(notes))

        val repo = FamilyRepository(taskDao, inventoryDao, budgetDao, expenseDao,
            invoiceDao, messageDao, shoppingDao, memberDao, mealPlanDao,
            sportsClubDao, workoutLogDao, calorieLogDao, menstrualCycleDao,
            authDao, syncEventDao, noteDao, reminderDao, waterLogDao, sleepLogDao)

        val result = repo.notes.first()

        assertEquals(1, result.size)
        assertEquals("Note 1", result[0].title)
    }

    @Test
    fun `searchNotes delegates to noteDao search`() = runTest {
        val notes = listOf(Note(id = "1", title = "Shopping"))
        whenever(noteDao.search("shopping")).thenReturn(flowOf(notes))

        val result = repository.searchNotes("shopping").first()

        assertEquals(1, result.size)
        verify(noteDao).search("shopping")
    }

    @Test
    fun `getArchivedNotes delegates to noteDao getArchived`() = runTest {
        val archived = listOf(Note(id = "1", title = "Archived", isArchived = true))
        whenever(noteDao.getArchived()).thenReturn(flowOf(archived))

        val result = repository.getArchivedNotes().first()

        assertEquals(1, result.size)
        assertTrue(result[0].isArchived)
        verify(noteDao).getArchived()
    }

    @Test
    fun `getNotesByCategory delegates to noteDao getByCategory`() = runTest {
        val notes = listOf(Note(id = "1", title = "Recipe", category = "Tarif"))
        whenever(noteDao.getByCategory("Tarif")).thenReturn(flowOf(notes))

        val result = repository.getNotesByCategory("Tarif").first()

        assertEquals(1, result.size)
        assertEquals("Tarif", result[0].category)
        verify(noteDao).getByCategory("Tarif")
    }

    @Test
    fun `upsertNote delegates to noteDao upsert`() = runTest {
        val note = Note(title = "New Note")
        repository.upsertNote(note)
        verify(noteDao).upsert(note)
    }

    @Test
    fun `deleteNote delegates to noteDao delete`() = runTest {
        val note = Note(id = "1", title = "Note to delete")
        repository.deleteNote(note)
        verify(noteDao).delete(note)
    }

    @Test
    fun `getAllNotesOnce delegates to noteDao getAllOnce`() = runTest {
        val notes = listOf(Note(id = "1", title = "Note 1"))
        whenever(noteDao.getAllOnce()).thenReturn(notes)

        val result = repository.getAllNotesOnce()

        assertEquals(1, result.size)
        verify(noteDao).getAllOnce()
    }

    // ===== REMINDERS =====

    @Test
    fun `activeReminders flow delegates to reminderDao`() = runTest {
        val reminders = listOf(Reminder(id = "1", title = "Reminder 1", reminderTime = System.currentTimeMillis()))
        whenever(reminderDao.getActive()).thenReturn(flowOf(reminders))

        val repo = FamilyRepository(taskDao, inventoryDao, budgetDao, expenseDao,
            invoiceDao, messageDao, shoppingDao, memberDao, mealPlanDao,
            sportsClubDao, workoutLogDao, calorieLogDao, menstrualCycleDao,
            authDao, syncEventDao, noteDao, reminderDao, waterLogDao, sleepLogDao)

        val result = repo.activeReminders.first()

        assertEquals(1, result.size)
        assertEquals("Reminder 1", result[0].title)
    }

    @Test
    fun `upsertReminder delegates to reminderDao upsert`() = runTest {
        val reminder = Reminder(title = "New Reminder", reminderTime = System.currentTimeMillis())
        repository.upsertReminder(reminder)
        verify(reminderDao).upsert(reminder)
    }

    @Test
    fun `deleteReminder delegates to reminderDao delete`() = runTest {
        val reminder = Reminder(id = "1", title = "Reminder to delete", reminderTime = System.currentTimeMillis())
        repository.deleteReminder(reminder)
        verify(reminderDao).delete(reminder)
    }

    // ===== MEMBERS =====

    @Test
    fun `members flow delegates to memberDao`() = runTest {
        val members = listOf(
            FamilyMember(id = "1", name = "Ali"),
            FamilyMember(id = "2", name = "Ayşe")
        )
        whenever(memberDao.getAll()).thenReturn(flowOf(members))

        val repo = FamilyRepository(taskDao, inventoryDao, budgetDao, expenseDao,
            invoiceDao, messageDao, shoppingDao, memberDao, mealPlanDao,
            sportsClubDao, workoutLogDao, calorieLogDao, menstrualCycleDao,
            authDao, syncEventDao, noteDao, reminderDao, waterLogDao, sleepLogDao)

        val result = repo.members.first()

        assertEquals(2, result.size)
    }

    @Test
    fun `upsertMember delegates to memberDao upsert`() = runTest {
        val member = FamilyMember(name = "New Member")
        repository.upsertMember(member)
        verify(memberDao).upsert(member)
    }

    // ===== SHOPPING =====

    @Test
    fun `shoppingItems flow delegates to shoppingDao`() = runTest {
        val items = listOf(ShoppingItem(id = "1", name = "Milk"))
        whenever(shoppingDao.getAll()).thenReturn(flowOf(items))

        val repo = FamilyRepository(taskDao, inventoryDao, budgetDao, expenseDao,
            invoiceDao, messageDao, shoppingDao, memberDao, mealPlanDao,
            sportsClubDao, workoutLogDao, calorieLogDao, menstrualCycleDao,
            authDao, syncEventDao, noteDao, reminderDao, waterLogDao, sleepLogDao)

        val result = repo.shoppingItems.first()

        assertEquals(1, result.size)
    }

    @Test
    fun `upsertShopping delegates to shoppingDao upsert`() = runTest {
        val item = ShoppingItem(name = "New Item")
        repository.upsertShopping(item)
        verify(shoppingDao).upsert(item)
    }

    // ===== INVOICES =====

    @Test
    fun `invoices flow delegates to invoiceDao`() = runTest {
        val invoices = listOf(Invoice(id = "1", title = "Electricity", amount = 150.0))
        whenever(invoiceDao.getAll()).thenReturn(flowOf(invoices))

        val repo = FamilyRepository(taskDao, inventoryDao, budgetDao, expenseDao,
            invoiceDao, messageDao, shoppingDao, memberDao, mealPlanDao,
            sportsClubDao, workoutLogDao, calorieLogDao, menstrualCycleDao,
            authDao, syncEventDao, noteDao, reminderDao, waterLogDao, sleepLogDao)

        val result = repo.invoices.first()

        assertEquals(1, result.size)
    }

    @Test
    fun `upsertInvoice delegates to invoiceDao upsert`() = runTest {
        val invoice = Invoice(title = "New Invoice", amount = 100.0)
        repository.upsertInvoice(invoice)
        verify(invoiceDao).upsert(invoice)
    }

    // ===== MESSAGES =====

    @Test
    fun `messages flow delegates to messageDao`() = runTest {
        val messages = listOf(
            Message(id = "1", senderName = "Ali", senderId = "1", content = "Hello")
        )
        whenever(messageDao.getAll()).thenReturn(flowOf(messages))

        val repo = FamilyRepository(taskDao, inventoryDao, budgetDao, expenseDao,
            invoiceDao, messageDao, shoppingDao, memberDao, mealPlanDao,
            sportsClubDao, workoutLogDao, calorieLogDao, menstrualCycleDao,
            authDao, syncEventDao, noteDao, reminderDao, waterLogDao, sleepLogDao)

        val result = repo.messages.first()

        assertEquals(1, result.size)
    }

    @Test
    fun `upsertMessage delegates to messageDao upsert`() = runTest {
        val message = Message(senderName = "Ali", senderId = "1", content = "Hello")
        repository.upsertMessage(message)
        verify(messageDao).upsert(message)
    }

    // ===== MODEL TESTS =====

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
    fun `task has correct default values`() {
        val task = Task(title = "Test Task")

        assertEquals("Test Task", task.title)
        assertEquals("", task.description)
        assertEquals("Genel", task.category)
        assertEquals("orta", task.priority)
        assertEquals("", task.assignee)
        assertEquals("bekleyen", task.status)
        assertNotNull(task.id)
        assertTrue(task.createdAt > 0)
    }

    @Test
    fun `reminder has correct default values`() {
        val reminder = Reminder(title = "Test Reminder", reminderTime = 1000L)

        assertEquals("Test Reminder", reminder.title)
        assertEquals("", reminder.description)
        assertEquals(1000L, reminder.reminderTime)
        assertEquals("once", reminder.repeatType)
        assertEquals("Genel", reminder.category)
        assertEquals("orta", reminder.priority)
        assertFalse(reminder.isCompleted)
        assertFalse(reminder.isSnoozed)
        assertEquals(0L, reminder.snoozeUntil)
        assertEquals("", reminder.linkedId)
        assertEquals("", reminder.linkedType)
        assertEquals("", reminder.createdBy)
        assertNotNull(reminder.id)
        assertTrue(reminder.createdAt > 0)
    }

    @Test
    fun `note supports pinning and unpinning`() {
        val note = Note(title = "Pinned", isPinned = true)
        val unpinned = note.copy(isPinned = false)

        assertTrue(note.isPinned)
        assertFalse(unpinned.isPinned)
    }

    @Test
    fun `note supports archiving and unarchiving`() {
        val note = Note(title = "Archived", isArchived = true)
        val active = note.copy(isArchived = false)

        assertTrue(note.isArchived)
        assertFalse(active.isArchived)
    }

    @Test
    fun `task supports status toggle`() {
        val pending = Task(title = "Task", status = "bekleyen")
        val completed = pending.copy(status = "tamamlanan", completedAt = 1000L)

        assertEquals("bekleyen", pending.status)
        assertEquals("tamamlanan", completed.status)
        assertEquals(1000L, completed.completedAt)
    }

    @Test
    fun `reminder supports completion`() {
        val reminder = Reminder(title = "Task", reminderTime = 1000L)
        val completed = reminder.copy(isCompleted = true)

        assertFalse(reminder.isCompleted)
        assertTrue(completed.isCompleted)
    }

    @Test
    fun `reminder supports snooze`() {
        val reminder = Reminder(title = "Task", reminderTime = 1000L)
        val snoozed = reminder.copy(isSnoozed = true, snoozeUntil = 2000L)

        assertFalse(reminder.isSnoozed)
        assertTrue(snoozed.isSnoozed)
        assertEquals(2000L, snoozed.snoozeUntil)
    }
}
