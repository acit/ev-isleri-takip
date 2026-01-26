import { Response } from 'express';
import { getDatabase } from '../config/database';
import { AuthRequest } from '../middleware/auth';

export const createTask = async (req: AuthRequest, res: Response) => {
  try {
    const {
      title,
      description,
      assignedTo,
      frequency,
      dueDate,
      priority,
      category,
      estimatedCost,
    } = req.body;

    const db = await getDatabase();
    const result = await db.run(
      `INSERT INTO tasks (family_id, title, description, assigned_to, created_by, frequency, due_date, priority, category, estimated_cost)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [req.familyId, title, description, assignedTo, req.userId, frequency, dueDate, priority, category, estimatedCost]
    );

    const newTask = await db.get('SELECT * FROM tasks WHERE id = ?', [result.lastID]);
    res.json(newTask);
  } catch (error) {
    console.error('Error creating task:', error);
    res.status(500).json({ error: 'Failed to create task' });
  }
};

export const getTasks = async (req: AuthRequest, res: Response) => {
  try {
    const { frequency, status, assignedTo } = req.query;
    const db = await getDatabase();
    
    let query = 'SELECT * FROM tasks WHERE family_id = ?';
    const params: any[] = [req.familyId];

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
  } catch (error) {
    console.error('Error fetching tasks:', error);
    res.status(500).json({ error: 'Failed to fetch tasks' });
  }
};

export const completeTask = async (req: AuthRequest, res: Response) => {
  try {
    const { taskId } = req.params;
    const db = await getDatabase();

    const result = await db.run(
      `UPDATE tasks 
       SET completed = 1, status = 'completed', completed_by = ?, completed_at = CURRENT_TIMESTAMP
       WHERE id = ? AND family_id = ?`,
      [req.userId, taskId, req.familyId]
    );

    if (result.changes === 0) {
      return res.status(404).json({ error: 'Task not found' });
    }

    const updatedTask = await db.get('SELECT * FROM tasks WHERE id = ?', [taskId]);
    res.json(updatedTask);
  } catch (error) {
    console.error('Error completing task:', error);
    res.status(500).json({ error: 'Failed to complete task' });
  }
};

export const updateTask = async (req: AuthRequest, res: Response) => {
  try {
    const { taskId } = req.params;
    const updates = req.body;
    const db = await getDatabase();

    // Build dynamic update query
    const fields = Object.keys(updates);
    const setClause = fields.map(field => `${field} = ?`).join(', ');
    const values = fields.map(field => updates[field]);

    const result = await db.run(
      `UPDATE tasks SET ${setClause}, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND family_id = ?`,
      [...values, taskId, req.familyId]
    );

    if (result.changes === 0) {
      return res.status(404).json({ error: 'Task not found' });
    }

    const updatedTask = await db.get('SELECT * FROM tasks WHERE id = ?', [taskId]);
    res.json(updatedTask);
  } catch (error) {
    console.error('Error updating task:', error);
    res.status(500).json({ error: 'Failed to update task' });
  }
};

export const getTasksByFrequency = async (req: AuthRequest, res: Response) => {
  try {
    const frequencies = ['daily', 'weekly', 'monthly', 'quarterly', 'biannual', 'yearly'];
    const tasksByFrequency: any = {};
    const db = await getDatabase();

    for (const freq of frequencies) {
      const tasks = await db.all(
        'SELECT * FROM tasks WHERE family_id = ? AND frequency = ? ORDER BY due_date ASC',
        [req.familyId, freq]
      );
      tasksByFrequency[freq] = tasks;
    }

    res.json(tasksByFrequency);
  } catch (error) {
    console.error('Error fetching tasks by frequency:', error);
    res.status(500).json({ error: 'Failed to fetch tasks' });
  }
};