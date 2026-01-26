"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getExpenses = exports.recordExpense = exports.setBudgetLimit = exports.getBudget = void 0;
const database_1 = require("../config/database");
const getBudget = async (req, res) => {
    try {
        const { monthYear } = req.query;
        const month = monthYear || new Date().toISOString().slice(0, 7);
        const db = await (0, database_1.getDatabase)();
        const budgets = await db.all('SELECT * FROM budgets WHERE family_id = ? AND month_year = ?', [req.familyId, month]);
        res.json(budgets);
    }
    catch (error) {
        console.error('Error fetching budget:', error);
        res.status(500).json({ error: 'Failed to fetch budget' });
    }
};
exports.getBudget = getBudget;
const setBudgetLimit = async (req, res) => {
    try {
        const { category, monthlyLimit, monthYear } = req.body;
        const month = monthYear || new Date().toISOString().slice(0, 7);
        const db = await (0, database_1.getDatabase)();
        // Check if budget exists
        const existingBudget = await db.get('SELECT id FROM budgets WHERE family_id = ? AND category = ? AND month_year = ?', [req.familyId, category, month]);
        if (existingBudget) {
            // Update existing budget
            await db.run('UPDATE budgets SET monthly_limit = ? WHERE id = ?', [monthlyLimit, existingBudget.id]);
            const updatedBudget = await db.get('SELECT * FROM budgets WHERE id = ?', [existingBudget.id]);
            res.json(updatedBudget);
        }
        else {
            // Create new budget
            const result = await db.run('INSERT INTO budgets (family_id, category, monthly_limit, month_year) VALUES (?, ?, ?, ?)', [req.familyId, category, monthlyLimit, month]);
            const newBudget = await db.get('SELECT * FROM budgets WHERE id = ?', [result.lastID]);
            res.json(newBudget);
        }
    }
    catch (error) {
        console.error('Error setting budget:', error);
        res.status(500).json({ error: 'Failed to set budget' });
    }
};
exports.setBudgetLimit = setBudgetLimit;
const recordExpense = async (req, res) => {
    try {
        const { amount, category, description, taskId, expenseDate } = req.body;
        const db = await (0, database_1.getDatabase)();
        const result = await db.run('INSERT INTO expenses (family_id, recorded_by, amount, category, description, task_id, expense_date) VALUES (?, ?, ?, ?, ?, ?, ?)', [req.familyId, req.userId, amount, category, description, taskId, expenseDate]);
        // Update budget spent amount
        const monthYear = new Date(expenseDate).toISOString().slice(0, 7);
        await db.run('UPDATE budgets SET spent_amount = spent_amount + ? WHERE family_id = ? AND category = ? AND month_year = ?', [amount, req.familyId, category, monthYear]);
        const newExpense = await db.get('SELECT * FROM expenses WHERE id = ?', [result.lastID]);
        res.json(newExpense);
    }
    catch (error) {
        console.error('Error recording expense:', error);
        res.status(500).json({ error: 'Failed to record expense' });
    }
};
exports.recordExpense = recordExpense;
const getExpenses = async (req, res) => {
    try {
        const { category, monthYear } = req.query;
        const db = await (0, database_1.getDatabase)();
        let query = 'SELECT * FROM expenses WHERE family_id = ?';
        const params = [req.familyId];
        if (category) {
            query += ' AND category = ?';
            params.push(category);
        }
        if (monthYear) {
            query += ' AND strftime("%Y-%m", expense_date) = ?';
            params.push(monthYear);
        }
        query += ' ORDER BY expense_date DESC';
        const expenses = await db.all(query, params);
        res.json(expenses);
    }
    catch (error) {
        console.error('Error fetching expenses:', error);
        res.status(500).json({ error: 'Failed to fetch expenses' });
    }
};
exports.getExpenses = getExpenses;
//# sourceMappingURL=budget.js.map