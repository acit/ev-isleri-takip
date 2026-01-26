import { Response } from 'express';
import { getDatabase } from '../config/database';
import { AuthRequest } from '../middleware/auth';

export const addInventoryItem = async (req: AuthRequest, res: Response) => {
  try {
    const { item_name, quantity, unit, min_threshold, category, location, notes } = req.body;
    const db = await getDatabase();

    const result = await db.run(
      `INSERT INTO inventory (family_id, item_name, quantity, unit, min_threshold, category, location, notes, last_updated)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [req.familyId, item_name, quantity, unit, min_threshold, category, location, notes, req.userId]
    );

    const newItem = await db.get('SELECT * FROM inventory WHERE id = ?', [result.lastID]);
    res.json(newItem);
  } catch (error) {
    console.error('Error adding inventory item:', error);
    res.status(500).json({ error: 'Failed to add inventory item' });
  }
};

export const getInventory = async (req: AuthRequest, res: Response) => {
  try {
    const db = await getDatabase();
    const items = await db.all('SELECT * FROM inventory WHERE family_id = ? ORDER BY item_name', [req.familyId]);
    res.json(items);
  } catch (error) {
    console.error('Error fetching inventory:', error);
    res.status(500).json({ error: 'Failed to fetch inventory' });
  }
};

export const getLowStockItems = async (req: AuthRequest, res: Response) => {
  try {
    const db = await getDatabase();
    const items = await db.all(
      'SELECT * FROM inventory WHERE family_id = ? AND quantity <= min_threshold ORDER BY item_name',
      [req.familyId]
    );
    res.json(items);
  } catch (error) {
    console.error('Error fetching low stock items:', error);
    res.status(500).json({ error: 'Failed to fetch low stock items' });
  }
};

export const updateInventoryItem = async (req: AuthRequest, res: Response) => {
  try {
    const { itemId } = req.params;
    const updates = req.body;
    const db = await getDatabase();

    // Build dynamic update query
    const fields = Object.keys(updates);
    const setClause = fields.map(field => `${field} = ?`).join(', ');
    const values = fields.map(field => updates[field]);
    
    await db.run(
      `UPDATE inventory SET ${setClause}, last_updated = ?, updated_at = CURRENT_TIMESTAMP 
       WHERE id = ? AND family_id = ?`,
      [...values, req.userId, itemId, req.familyId]
    );

    const updatedItem = await db.get('SELECT * FROM inventory WHERE id = ?', [itemId]);
    res.json(updatedItem);
  } catch (error) {
    console.error('Error updating inventory item:', error);
    res.status(500).json({ error: 'Failed to update inventory item' });
  }
};

export const deleteInventoryItem = async (req: AuthRequest, res: Response) => {
  try {
    const { itemId } = req.params;
    const db = await getDatabase();

    const result = await db.run(
      'DELETE FROM inventory WHERE id = ? AND family_id = ?',
      [itemId, req.familyId]
    );

    if (result.changes === 0) {
      return res.status(404).json({ error: 'Item not found' });
    }

    res.json({ message: 'Item deleted successfully' });
  } catch (error) {
    console.error('Error deleting inventory item:', error);
    res.status(500).json({ error: 'Failed to delete inventory item' });
  }
};