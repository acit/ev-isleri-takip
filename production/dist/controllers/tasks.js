"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getTasksByFrequency = exports.updateTask = exports.completeTask = exports.getTasks = exports.createTask = void 0;
const database_1 = require("../config/database");
const createTask = async (req, res) => {
    try {
        const { title, description, assignedTo, frequency, dueDate, priority, category, estimatedCost, } = req.body;
        const db = await (0, database_1.getDatabase)();
        const result = await db.run(`INSERT INTO tasks (family_id, title, description, assigned_to, created_by, frequency, due_date, priority, category, estimated_cost)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`, [req.familyId, title, description, assignedTo, req.userId, frequency, dueDate, priority, category, estimatedCost]);
        const newTask = await db.get('SELECT * FROM tasks WHERE id = ?', [result.lastID]);
        res.json(newTask);
    }
    catch (error) {
        console.error('Error creating task:', error);
        res.status(500).json({ error: 'Failed to create task' });
    }
};
exports.createTask = createTask;
const getTasks = async (req, res) => {
    try {
        const { frequency, status, assignedTo } = req.query;
        const db = await (0, database_1.getDatabase)();
        let query = 'SELECT * FROM tasks WHERE family_id = ?';
        const params = [req.familyId];
        if (frequency) {
            query += ` AND frequency = ?`;
            params.push(frequency);
        }
        if (status) {
            query += ` AND status = ?`;
            params.push(status);
        }
        if (assignedTo) {
            query += ` AND assigned_to = ?`;
            params.push(assignedTo);
        }
        query += ' ORDER BY due_date ASC';
        const tasks = await db.all(query, params);
        res.json(tasks);
    }
    catch (error) {
        console.error('Error fetching tasks:', error);
        res.status(500).json({ error: 'Failed to fetch tasks' });
    }
};
exports.getTasks = getTasks;
const completeTask = async (req, res) => {
    try {
        const { taskId } = req.params;
        const db = await (0, database_1.getDatabase)();
        const result = await db.run(`UPDATE tasks 
       SET completed = 1, status = 'completed', completed_by = ?, completed_at = CURRENT_TIMESTAMP
       WHERE id = ? AND family_id = ?`, [req.userId, taskId, req.familyId]);
        if (result.changes === 0) {
            return res.status(404).json({ error: 'Task not found' });
        }
        const updatedTask = await db.get('SELECT * FROM tasks WHERE id = ?', [taskId]);
        res.json(updatedTask);
    }
    catch (error) {
        console.error('Error completing task:', error);
        res.status(500).json({ error: 'Failed to complete task' });
    }
};
exports.completeTask = completeTask;
const updateTask = async (req, res) => {
    try {
        const { taskId } = req.params;
        const updates = req.body;
        const db = await (0, database_1.getDatabase)();
        // Build dynamic update query
        const fields = Object.keys(updates);
        const setClause = fields.map(field => `${field} = ?`).join(', ');
        const values = fields.map(field => updates[field]);
        const result = await db.run(`UPDATE tasks SET ${setClause}, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND family_id = ?`, [...values, taskId, req.familyId]);
        if (result.changes === 0) {
            return res.status(404).json({ error: 'Task not found' });
        }
        const updatedTask = await db.get('SELECT * FROM tasks WHERE id = ?', [taskId]);
        res.json(updatedTask);
    }
    catch (error) {
        console.error('Error updating task:', error);
        res.status(500).json({ error: 'Failed to update task' });
    }
};
exports.updateTask = updateTask;
const getTasksByFrequency = async (req, res) => {
    try {
        const frequencies = ['daily', 'weekly', 'monthly', 'quarterly', 'biannual', 'yearly'];
        const tasksByFrequency = {};
        const db = await (0, database_1.getDatabase)();
        for (const freq of frequencies) {
            const tasks = await db.all('SELECT * FROM tasks WHERE family_id = ? AND frequency = ? ORDER BY due_date ASC', [req.familyId, freq]);
            tasksByFrequency[freq] = tasks;
        }
        res.json(tasksByFrequency);
    }
    catch (error) {
        console.error('Error fetching tasks by frequency:', error);
        res.status(500).json({ error: 'Failed to fetch tasks' });
    }
};
exports.getTasksByFrequency = getTasksByFrequency;
//# sourceMappingURL=tasks.js.map