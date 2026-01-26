import { Response } from 'express';
import { getDatabase } from '../config/database';
import { AuthRequest } from '../middleware/auth';

export const createShoppingList = async (req: AuthRequest, res: Response) => {
  try {
    const { title, items } = req.body;
    const db = await getDatabase();

    // Create shopping list
    const listResult = await db.run(
      'INSERT INTO shopping_lists (family_id, created_by, title, status) VALUES (?, ?, ?, ?)',
      [req.familyId, req.userId, title || 'Alışveriş Listesi', 'draft']
    );

    // Add items if provided
    if (items && items.length > 0) {
      for (const item of items) {
        await db.run(
          'INSERT INTO shopping_list_items (shopping_list_id, item_name, quantity, unit, estimated_cost) VALUES (?, ?, ?, ?, ?)',
          [listResult.lastID, item.item_name, item.quantity, item.unit, item.estimated_cost]
        );
      }
    }

    const newList = await db.get('SELECT * FROM shopping_lists WHERE id = ?', [listResult.lastID]);
    res.json(newList);
  } catch (error) {
    console.error('Error creating shopping list:', error);
    res.status(500).json({ error: 'Failed to create shopping list' });
  }
};

export const getShoppingLists = async (req: AuthRequest, res: Response) => {
  try {
    const db = await getDatabase();
    const lists = await db.all(
      'SELECT * FROM shopping_lists WHERE family_id = ? ORDER BY created_at DESC',
      [req.familyId]
    );
    res.json(lists);
  } catch (error) {
    console.error('Error fetching shopping lists:', error);
    res.status(500).json({ error: 'Failed to fetch shopping lists' });
  }
};

export const generateShoppingListFromLowStock = async (req: AuthRequest, res: Response) => {
  try {
    const db = await getDatabase();

    // Get low stock items
    const lowStockItems = await db.all(
      'SELECT * FROM inventory WHERE family_id = ? AND quantity <= min_threshold',
      [req.familyId]
    );

    if (lowStockItems.length === 0) {
      return res.json({ message: 'No low stock items found' });
    }

    // Create shopping list
    const listResult = await db.run(
      'INSERT INTO shopping_lists (family_id, created_by, title, auto_generated, status) VALUES (?, ?, ?, ?, ?)',
      [req.familyId, req.userId, 'Otomatik Alışveriş Listesi', true, 'draft']
    );

    // Add low stock items to shopping list
    for (const item of lowStockItems) {
      const neededQuantity = Math.max(item.min_threshold * 2 - item.quantity, 1);
      await db.run(
        'INSERT INTO shopping_list_items (shopping_list_id, item_name, quantity, unit) VALUES (?, ?, ?, ?)',
        [listResult.lastID, item.item_name, neededQuantity, item.unit]
      );
    }

    const newList = await db.get('SELECT * FROM shopping_lists WHERE id = ?', [listResult.lastID]);
    res.json(newList);
  } catch (error) {
    console.error('Error generating shopping list:', error);
    res.status(500).json({ error: 'Failed to generate shopping list' });
  }
};

export const getShoppingListItems = async (req: AuthRequest, res: Response) => {
  try {
    const { listId } = req.params;
    const db = await getDatabase();

    // Get shopping list to verify ownership
    const list = await db.get(
      'SELECT * FROM shopping_lists WHERE id = ? AND family_id = ?',
      [listId, req.familyId]
    );

    if (!list) {
      return res.status(404).json({ error: 'Shopping list not found' });
    }

    // Get items
    const items = await db.all(
      'SELECT * FROM shopping_list_items WHERE shopping_list_id = ? ORDER BY id',
      [listId]
    );

    res.json({ list, items });
  } catch (error) {
    console.error('Error fetching shopping list items:', error);
    res.status(500).json({ error: 'Failed to fetch shopping list items' });
  }
};

export const updateShoppingListItem = async (req: AuthRequest, res: Response) => {
  try {
    const { itemId } = req.params;
    const { checked, quantity, estimated_cost } = req.body;
    const db = await getDatabase();

    // Verify item belongs to user's family
    const item = await db.get(`
      SELECT sli.*, sl.family_id 
      FROM shopping_list_items sli
      JOIN shopping_lists sl ON sli.shopping_list_id = sl.id
      WHERE sli.id = ? AND sl.family_id = ?
    `, [itemId, req.familyId]);

    if (!item) {
      return res.status(404).json({ error: 'Shopping list item not found' });
    }

    // Build update query dynamically
    const updates = [];
    const values = [];

    if (checked !== undefined) {
      updates.push('checked = ?');
      values.push(checked ? 1 : 0);
    }
    if (quantity !== undefined) {
      updates.push('quantity = ?');
      values.push(quantity);
    }
    if (estimated_cost !== undefined) {
      updates.push('estimated_cost = ?');
      values.push(estimated_cost);
    }

    if (updates.length === 0) {
      return res.status(400).json({ error: 'No valid fields to update' });
    }

    values.push(itemId);
    
    await db.run(
      `UPDATE shopping_list_items SET ${updates.join(', ')} WHERE id = ?`,
      values
    );

    // Get updated item
    const updatedItem = await db.get('SELECT * FROM shopping_list_items WHERE id = ?', [itemId]);
    res.json(updatedItem);
  } catch (error) {
    console.error('Error updating shopping list item:', error);
    res.status(500).json({ error: 'Failed to update shopping list item' });
  }
};
export const sendShoppingListByEmail = async (req: AuthRequest, res: Response) => {
  try {
    const { listId } = req.params;
    const { recipientEmails } = req.body;
    const db = await getDatabase();

    // Get shopping list and items
    const list = await db.get('SELECT * FROM shopping_lists WHERE id = ? AND family_id = ?', [listId, req.familyId]);
    if (!list) {
      return res.status(404).json({ error: 'Shopping list not found' });
    }

    const items = await db.all('SELECT * FROM shopping_list_items WHERE shopping_list_id = ?', [listId]);
    
    // Get sender info
    const sender = await db.get('SELECT full_name, email FROM users WHERE id = ?', [req.userId]);

    // Create HTML email content
    const itemsHtml = items.map((item: any) => `
      <tr style="border-bottom: 1px solid #e9ecef;">
        <td style="padding: 12px; text-align: left;">${item.item_name}</td>
        <td style="padding: 12px; text-align: center;">${item.quantity} ${item.unit}</td>
        <td style="padding: 12px; text-align: right;">${item.estimated_cost > 0 ? `₺${item.estimated_cost.toLocaleString('tr-TR')}` : '-'}</td>
        <td style="padding: 12px; text-align: center;">
          <input type="checkbox" style="transform: scale(1.2);">
        </td>
      </tr>
    `).join('');

    const totalCost = items.reduce((sum: number, item: any) => sum + (item.estimated_cost || 0), 0);

    const emailHtml = `
      <div style="font-family: Arial, sans-serif; max-width: 800px; margin: 0 auto; padding: 20px;">
        <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; border-radius: 10px; text-align: center; margin-bottom: 30px;">
          <h1 style="color: white; margin: 0; font-size: 28px;">🛒 Alışveriş Listesi</h1>
          <p style="color: rgba(255,255,255,0.9); margin: 10px 0 0 0; font-size: 16px;">${list.title}</p>
        </div>
        
        <div style="background: #f8f9fa; padding: 20px; border-radius: 10px; margin-bottom: 20px;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px;">
            <h2 style="color: #2c3e50; margin: 0;">📋 Liste Detayları</h2>
            <span style="background: #007bff; color: white; padding: 6px 12px; border-radius: 20px; font-size: 12px; font-weight: 600;">
              ${items.length} Ürün
            </span>
          </div>
          
          <div style="background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
            <table style="width: 100%; border-collapse: collapse;">
              <thead>
                <tr style="background: #007bff; color: white;">
                  <th style="padding: 15px; text-align: left; font-weight: 600;">Ürün</th>
                  <th style="padding: 15px; text-align: center; font-weight: 600;">Miktar</th>
                  <th style="padding: 15px; text-align: right; font-weight: 600;">Fiyat</th>
                  <th style="padding: 15px; text-align: center; font-weight: 600;">✓</th>
                </tr>
              </thead>
              <tbody>
                ${itemsHtml}
              </tbody>
            </table>
          </div>
          
          ${totalCost > 0 ? `
            <div style="background: #e8f5e8; padding: 15px; border-radius: 8px; margin-top: 15px; text-align: right;">
              <span style="font-size: 18px; font-weight: 700; color: #28a745;">
                Tahmini Toplam: ₺${totalCost.toLocaleString('tr-TR')}
              </span>
            </div>
          ` : ''}
        </div>
        
        <div style="background: #fff3cd; border: 1px solid #ffeaa7; padding: 20px; border-radius: 8px; margin-bottom: 20px;">
          <h3 style="color: #856404; margin-top: 0;">💡 Alışveriş İpuçları</h3>
          <ul style="color: #856404; margin: 0; padding-left: 20px;">
            <li>Bu listeyi yazdırabilir veya telefonunuzda saklayabilirsiniz</li>
            <li>Alışveriş yaparken ürünleri işaretlemeyi unutmayın</li>
            <li>Fiyatlar tahminidir, gerçek fiyatlar değişebilir</li>
          </ul>
        </div>
        
        <div style="background: #e3f2fd; padding: 20px; border-radius: 8px; margin-bottom: 20px;">
          <h3 style="color: #1976d2; margin-top: 0;">👨‍👩‍👧‍👦 Gönderen Bilgileri</h3>
          <p style="color: #424242; margin: 5px 0;"><strong>Gönderen:</strong> ${sender?.full_name || sender?.email || 'Aile Üyesi'}</p>
          <p style="color: #424242; margin: 5px 0;"><strong>Tarih:</strong> ${new Date(list.created_at).toLocaleDateString('tr-TR', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
          })}</p>
          ${list.auto_generated ? '<p style="color: #6f42c1; margin: 5px 0;"><strong>🤖 Otomatik oluşturuldu</strong> (düşük stok ürünlerinden)</p>' : ''}
        </div>
        
        <div style="text-align: center; color: #6c757d; font-size: 14px; border-top: 1px solid #e9ecef; padding-top: 20px;">
          <p>Bu e-posta <strong>Aile Takip Sistemi</strong> tarafından otomatik olarak gönderilmiştir.</p>
          <p>🏠 Aile organizasyonunu kolaylaştıran akıllı sistem</p>
        </div>
      </div>
    `;

    // Import sendEmail function
    const { sendEmail } = require('../config/email');

    // Send email to each recipient
    for (const email of recipientEmails) {
      await sendEmail(
        email.trim(),
        `🛒 Alışveriş Listesi: ${list.title}`,
        emailHtml
      );
    }

    // Update list as sent
    await db.run(
      'UPDATE shopping_lists SET sent_via_email = 1, sent_at = CURRENT_TIMESTAMP, status = ? WHERE id = ?',
      ['sent', listId]
    );

    console.log(`✅ Shopping list "${list.title}" sent to: ${recipientEmails.join(', ')}`);
    res.json({ 
      message: 'Alışveriş listesi başarıyla gönderildi',
      recipients: recipientEmails,
      itemCount: items.length
    });
  } catch (error) {
    console.error('Error sending shopping list:', error);
    res.status(500).json({ error: 'Alışveriş listesi gönderilemedi' });
  }
};