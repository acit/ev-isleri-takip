"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.deleteInventoryItem = exports.updateInventoryItem = exports.getLowStockItems = exports.getInventory = exports.addInventoryItem = void 0;
const database_1 = require("../config/database");
const addInventoryItem = async (req, res) => {
    try {
        const { item_name, quantity, unit, min_threshold, category, location, notes } = req.body;
        const db = await (0, database_1.getDatabase)();
        const result = await db.run(`INSERT INTO inventory (family_id, item_name, quantity, unit, min_threshold, category, location, notes, last_updated)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`, [req.familyId, item_name, quantity, unit, min_threshold, category, location, notes, req.userId]);
        const newItem = await db.get('SELECT * FROM inventory WHERE id = ?', [result.lastID]);
        res.json(newItem);
    }
    catch (error) {
        console.error('Error adding inventory item:', error);
        res.status(500).json({ error: 'Failed to add inventory item' });
    }
};
exports.addInventoryItem = addInventoryItem;
const getInventory = async (req, res) => {
    try {
        const db = await (0, database_1.getDatabase)();
        const items = await db.all('SELECT * FROM inventory WHERE family_id = ? ORDER BY item_name', [req.familyId]);
        res.json(items);
    }
    catch (error) {
        console.error('Error fetching inventory:', error);
        res.status(500).json({ error: 'Failed to fetch inventory' });
    }
};
exports.getInventory = getInventory;
const getLowStockItems = async (req, res) => {
    try {
        const db = await (0, database_1.getDatabase)();
        const items = await db.all('SELECT * FROM inventory WHERE family_id = ? AND quantity <= min_threshold ORDER BY item_name', [req.familyId]);
        res.json(items);
    }
    catch (error) {
        console.error('Error fetching low stock items:', error);
        res.status(500).json({ error: 'Failed to fetch low stock items' });
    }
};
exports.getLowStockItems = getLowStockItems;
const updateInventoryItem = async (req, res) => {
    try {
        const { itemId } = req.params;
        const updates = req.body;
        const db = await (0, database_1.getDatabase)();
        // Build dynamic update query
        const fields = Object.keys(updates);
        const setClause = fields.map(field => `${field} = ?`).join(', ');
        const values = fields.map(field => updates[field]);
        await db.run(`UPDATE inventory SET ${setClause}, last_updated = ?, updated_at = CURRENT_TIMESTAMP 
       WHERE id = ? AND family_id = ?`, [...values, req.userId, itemId, req.familyId]);
        const updatedItem = await db.get('SELECT * FROM inventory WHERE id = ?', [itemId]);
        res.json(updatedItem);
    }
    catch (error) {
        console.error('Error updating inventory item:', error);
        res.status(500).json({ error: 'Failed to update inventory item' });
    }
};
exports.updateInventoryItem = updateInventoryItem;
const deleteInventoryItem = async (req, res) => {
    try {
        const { itemId } = req.params;
        const db = await (0, database_1.getDatabase)();
        const result = await db.run('DELETE FROM inventory WHERE id = ? AND family_id = ?', [itemId, req.familyId]);
        if (result.changes === 0) {
            return res.status(404).json({ error: 'Item not found' });
        }
        res.json({ message: 'Item deleted successfully' });
    }
    catch (error) {
        console.error('Error deleting inventory item:', error);
        res.status(500).json({ error: 'Failed to delete inventory item' });
    }
};
exports.deleteInventoryItem = deleteInventoryItem;
//# sourceMappingURL=inventory.js.map