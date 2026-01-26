"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getUnreadCount = exports.getFamilyMembers = exports.markAsRead = exports.getMessages = exports.sendMessage = void 0;
const database_1 = require("../config/database");
const sendMessage = async (req, res) => {
    try {
        const { recipientId, message, messageType = 'text' } = req.body;
        const senderId = req.userId;
        const familyId = req.familyId;
        const db = await (0, database_1.getDatabase)();
        // Validate recipient is in same family (if specified)
        if (recipientId) {
            const recipient = await db.get('SELECT id FROM users WHERE id = ? AND family_id = ?', [recipientId, familyId]);
            if (!recipient) {
                return res.status(404).json({ error: 'Alıcı bulunamadı veya aynı ailede değil' });
            }
        }
        // Insert message
        const result = await db.run('INSERT INTO messages (family_id, sender_id, recipient_id, message, message_type) VALUES (?, ?, ?, ?, ?)', [familyId, senderId, recipientId || null, message, messageType]);
        // Get the created message with sender info
        const newMessage = await db.get(`
      SELECT m.*, u.full_name as sender_name, u.email as sender_email,
             r.full_name as recipient_name, r.email as recipient_email
      FROM messages m
      JOIN users u ON m.sender_id = u.id
      LEFT JOIN users r ON m.recipient_id = r.id
      WHERE m.id = ?
    `, [result.lastID]);
        res.json(newMessage);
    }
    catch (error) {
        console.error('Error sending message:', error);
        res.status(500).json({ error: 'Mesaj gönderilemedi' });
    }
};
exports.sendMessage = sendMessage;
const getMessages = async (req, res) => {
    try {
        const { recipientId, limit = 50 } = req.query;
        const userId = req.userId;
        const familyId = req.familyId;
        const db = await (0, database_1.getDatabase)();
        let query = `
      SELECT m.*, u.full_name as sender_name, u.email as sender_email,
             r.full_name as recipient_name, r.email as recipient_email
      FROM messages m
      JOIN users u ON m.sender_id = u.id
      LEFT JOIN users r ON m.recipient_id = r.id
      WHERE m.family_id = ?
    `;
        const params = [familyId];
        if (recipientId) {
            // Private conversation between two users
            query += ` AND ((m.sender_id = ? AND m.recipient_id = ?) OR (m.sender_id = ? AND m.recipient_id = ?))`;
            params.push(userId, recipientId, recipientId, userId);
        }
        else {
            // Family-wide messages or messages to/from current user
            query += ` AND (m.recipient_id IS NULL OR m.sender_id = ? OR m.recipient_id = ?)`;
            params.push(userId, userId);
        }
        query += ` ORDER BY m.created_at DESC LIMIT ?`;
        params.push(Number(limit));
        const messages = await db.all(query, params);
        res.json(messages.reverse()); // Return in chronological order
    }
    catch (error) {
        console.error('Error fetching messages:', error);
        res.status(500).json({ error: 'Mesajlar getirilemedi' });
    }
};
exports.getMessages = getMessages;
const markAsRead = async (req, res) => {
    try {
        const { messageId } = req.params;
        const userId = req.userId;
        const familyId = req.familyId;
        const db = await (0, database_1.getDatabase)();
        // Mark message as read (only if user is recipient or it's a family message)
        const result = await db.run(`
      UPDATE messages 
      SET is_read = 1, updated_at = CURRENT_TIMESTAMP 
      WHERE id = ? AND family_id = ? AND (recipient_id = ? OR recipient_id IS NULL)
    `, [messageId, familyId, userId]);
        if (result.changes === 0) {
            return res.status(404).json({ error: 'Mesaj bulunamadı' });
        }
        res.json({ message: 'Mesaj okundu olarak işaretlendi' });
    }
    catch (error) {
        console.error('Error marking message as read:', error);
        res.status(500).json({ error: 'Mesaj güncellenemedi' });
    }
};
exports.markAsRead = markAsRead;
const getFamilyMembers = async (req, res) => {
    try {
        const familyId = req.familyId;
        const userId = req.userId;
        const db = await (0, database_1.getDatabase)();
        const members = await db.all(`
      SELECT id, full_name, email, role, status, created_at
      FROM users 
      WHERE family_id = ? AND id != ? AND status = 'active'
      ORDER BY full_name
    `, [familyId, userId]);
        res.json(members);
    }
    catch (error) {
        console.error('Error fetching family members:', error);
        res.status(500).json({ error: 'Aile üyeleri getirilemedi' });
    }
};
exports.getFamilyMembers = getFamilyMembers;
const getUnreadCount = async (req, res) => {
    try {
        const userId = req.userId;
        const familyId = req.familyId;
        const db = await (0, database_1.getDatabase)();
        const result = await db.get(`
      SELECT COUNT(*) as unread_count
      FROM messages 
      WHERE family_id = ? AND (recipient_id = ? OR recipient_id IS NULL) 
      AND sender_id != ? AND is_read = 0
    `, [familyId, userId, userId]);
        res.json({ unreadCount: result.unread_count });
    }
    catch (error) {
        console.error('Error getting unread count:', error);
        res.status(500).json({ error: 'Okunmamış mesaj sayısı alınamadı' });
    }
};
exports.getUnreadCount = getUnreadCount;
//# sourceMappingURL=messages.js.map