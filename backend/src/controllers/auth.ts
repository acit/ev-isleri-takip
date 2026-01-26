import { Response } from 'express';
import { getDatabase } from '../config/database';
import { sendEmail } from '../config/email';
import jwt from 'jsonwebtoken';
import bcrypt from 'bcryptjs';
import { v4 as uuidv4 } from 'uuid';
import { AuthRequest } from '../middleware/auth';
import { getEmailStatus as checkEmailStatus } from '../utils/emailHelper';

export const loginWithPassword = async (req: AuthRequest, res: Response) => {
  try {
    const { email, password } = req.body;
    const db = await getDatabase();

    // Get user with password hash
    const user = await db.get(
      'SELECT id, email, password_hash, family_id, role, full_name, status FROM users WHERE email = ?',
      [email]
    );

    if (!user) {
      return res.status(401).json({ error: 'Geçersiz email veya şifre' });
    }

    // Check if user has a password set
    if (!user.password_hash) {
      return res.status(400).json({ 
        error: 'Bu hesap için şifre belirlenmemiş. Lütfen email ile giriş yapın.',
        requiresEmailLogin: true
      });
    }

    // Verify password
    const isValidPassword = await bcrypt.compare(password, user.password_hash);
    if (!isValidPassword) {
      return res.status(401).json({ error: 'Geçersiz email veya şifre' });
    }

    // Check if user is active
    if (user.status !== 'active') {
      return res.status(403).json({ error: 'Hesabınız aktif değil' });
    }

    // Generate JWT
    const token = jwt.sign(
      {
        userId: user.id,
        familyId: user.family_id,
        role: user.role,
      },
      process.env.JWT_SECRET || 'fallback_secret',
      { expiresIn: '7d' }
    );

    res.json({ 
      token, 
      userId: user.id, 
      familyId: user.family_id,
      message: 'Giriş başarılı!'
    });
  } catch (error) {
    console.error('Error logging in with password:', error);
    res.status(500).json({ error: 'Giriş yapılamadı' });
  }
};

export const setPassword = async (req: AuthRequest, res: Response) => {
  try {
    const { currentPassword, newPassword } = req.body;
    const userId = req.userId;
    const db = await getDatabase();

    // Get current user
    const user = await db.get('SELECT password_hash FROM users WHERE id = ?', [userId]);
    if (!user) {
      return res.status(404).json({ error: 'Kullanıcı bulunamadı' });
    }

    // If user has existing password, verify it
    if (user.password_hash && currentPassword) {
      const isValidPassword = await bcrypt.compare(currentPassword, user.password_hash);
      if (!isValidPassword) {
        return res.status(401).json({ error: 'Mevcut şifre yanlış' });
      }
    }

    // Hash new password
    const saltRounds = 12;
    const hashedPassword = await bcrypt.hash(newPassword, saltRounds);

    // Update password
    await db.run(
      'UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
      [hashedPassword, userId]
    );

    res.json({ message: 'Şifre başarıyla güncellendi' });
  } catch (error) {
    console.error('Error setting password:', error);
    res.status(500).json({ error: 'Şifre güncellenemedi' });
  }
};

export const resetPassword = async (req: AuthRequest, res: Response) => {
  try {
    const { email } = req.body;
    const db = await getDatabase();

    // Check if user exists
    const user = await db.get('SELECT id, full_name FROM users WHERE email = ?', [email]);
    if (!user) {
      // Don't reveal if email exists or not for security
      return res.json({ message: 'Eğer bu email kayıtlıysa, şifre sıfırlama kodu gönderildi' });
    }

    // Generate reset code
    const resetCode = Math.floor(100000 + Math.random() * 900000).toString();
    const expiresAt = new Date(Date.now() + 30 * 60 * 1000); // 30 minutes

    // Store reset code
    await db.run(
      'INSERT OR REPLACE INTO login_codes (email, code, expires_at) VALUES (?, ?, ?)',
      [email, resetCode, expiresAt.toISOString()]
    );

    // Send reset email
    await sendEmail(
      email,
      'Aile Takip Sistemi - Şifre Sıfırlama',
      `
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
          <div style="background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%); padding: 30px; border-radius: 10px; text-align: center; margin-bottom: 30px;">
            <h1 style="color: white; margin: 0; font-size: 28px;">🔐 Şifre Sıfırlama</h1>
          </div>
          
          <div style="background: #f8f9fa; padding: 30px; border-radius: 10px; text-align: center; margin-bottom: 20px;">
            <h2 style="color: #2c3e50; margin-bottom: 20px;">Şifre Sıfırlama Kodunuz</h2>
            <div style="background: white; padding: 20px; border-radius: 8px; border: 2px dashed #ff6b6b; margin: 20px 0;">
              <span style="font-size: 36px; font-weight: bold; color: #ff6b6b; letter-spacing: 8px;">${resetCode}</span>
            </div>
            <p style="color: #6c757d; margin: 15px 0;">Bu kod <strong>30 dakika</strong> geçerlidir.</p>
          </div>
          
          <div style="background: #fff3cd; padding: 20px; border-radius: 8px; margin-bottom: 20px;">
            <h3 style="color: #856404; margin-top: 0;">⚠️ Güvenlik Uyarısı</h3>
            <ul style="color: #856404; text-align: left;">
              <li>Bu kodu kimseyle paylaşmayın</li>
              <li>Şifrenizi güçlü tutun (en az 8 karakter)</li>
              <li>Bu işlemi siz yapmadıysanız hesabınızı kontrol edin</li>
            </ul>
          </div>
          
          <div style="text-align: center; color: #6c757d; font-size: 14px;">
            <p>Bu e-posta otomatik olarak gönderilmiştir.</p>
            <p>Güvenliğiniz için bu kodu 30 dakika içinde kullanın.</p>
          </div>
        </div>
      `
    );

    res.json({ message: 'Eğer bu email kayıtlıysa, şifre sıfırlama kodu gönderildi' });
  } catch (error) {
    console.error('Error sending reset code:', error);
    res.status(500).json({ error: 'Şifre sıfırlama kodu gönderilemedi' });
  }
};

export const confirmPasswordReset = async (req: AuthRequest, res: Response) => {
  try {
    const { email, code, newPassword } = req.body;
    const db = await getDatabase();

    // Verify reset code
    const resetCode = await db.get(
      'SELECT * FROM login_codes WHERE email = ? AND code = ? AND expires_at > datetime("now")',
      [email, code]
    );

    if (!resetCode) {
      return res.status(400).json({ error: 'Geçersiz veya süresi dolmuş kod' });
    }

    // Hash new password
    const saltRounds = 12;
    const hashedPassword = await bcrypt.hash(newPassword, saltRounds);

    // Update password
    const result = await db.run(
      'UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE email = ?',
      [hashedPassword, email]
    );

    if (result.changes === 0) {
      return res.status(404).json({ error: 'Kullanıcı bulunamadı' });
    }

    // Delete used code
    await db.run('DELETE FROM login_codes WHERE email = ?', [email]);

    res.json({ message: 'Şifre başarıyla sıfırlandı' });
  } catch (error) {
    console.error('Error confirming password reset:', error);
    res.status(500).json({ error: 'Şifre sıfırlanamadı' });
  }
};

export const sendLoginCode = async (req: AuthRequest, res: Response) => {
  try {
    const { email } = req.body;
    const db = await getDatabase();
    
    // Check if user exists
    let user = await db.get('SELECT id, family_id, status FROM users WHERE email = ?', [email]);
    let isNewUser = false;
    
    if (!user) {
      // Create new user with pending status
      const result = await db.run(
        'INSERT INTO users (email, full_name, status) VALUES (?, ?, ?)',
        [email, email.split('@')[0], 'pending']
      );
      
      user = { id: result.lastID, family_id: null, status: 'pending' };
      isNewUser = true;
      console.log(`✅ New user registered: ${email}`);
    }

    // Generate 6-digit code
    const code = Math.floor(100000 + Math.random() * 900000).toString();
    const expiresAt = new Date(Date.now() + 15 * 60 * 1000); // 15 minutes

    // Clean up old codes for this email
    await db.run('DELETE FROM login_codes WHERE email = ?', [email]);
    
    await db.run(
      'INSERT INTO login_codes (email, code, expires_at) VALUES (?, ?, ?)',
      [email, code, expiresAt.toISOString()]
    );

    // Send email with login code
    const emailSubject = isNewUser ? 
      'Aile Takip Sistemi - Hoş Geldiniz! Doğrulama Kodunuz' : 
      'Aile Takip Sistemi - Giriş Kodunuz';
      
    const emailContent = isNewUser ? `
      <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
        <div style="background: linear-gradient(135deg, #28a745 0%, #20c997 100%); padding: 30px; border-radius: 10px; text-align: center; margin-bottom: 30px;">
          <h1 style="color: white; margin: 0; font-size: 28px;">🎉 Hoş Geldiniz!</h1>
          <p style="color: rgba(255,255,255,0.9); margin: 10px 0 0 0; font-size: 16px;">Aile Takip Sistemi</p>
        </div>
        
        <div style="background: #f8f9fa; padding: 30px; border-radius: 10px; text-align: center; margin-bottom: 20px;">
          <h2 style="color: #2c3e50; margin-bottom: 20px;">Hesabınızı Doğrulayın</h2>
          <div style="background: white; padding: 20px; border-radius: 8px; border: 2px dashed #28a745; margin: 20px 0;">
            <span style="font-size: 36px; font-weight: bold; color: #28a745; letter-spacing: 8px;">${code}</span>
          </div>
          <p style="color: #6c757d; margin: 15px 0;">Bu kod <strong>15 dakika</strong> geçerlidir.</p>
        </div>
        
        <div style="background: #e8f5e8; padding: 20px; border-radius: 8px; margin-bottom: 20px;">
          <h3 style="color: #2e7d32; margin-top: 0;">🏠 Aile Takip Sistemi ile:</h3>
          <ul style="color: #2e7d32; text-align: left;">
            <li>✅ Görevleri organize edin</li>
            <li>📦 Ev envanterini takip edin</li>
            <li>🛒 Alışveriş listesi oluşturun</li>
            <li>💰 Bütçenizi yönetin</li>
            <li>👨‍👩‍👧‍👦 Aile üyelerini davet edin</li>
          </ul>
        </div>
        
        <div style="text-align: center; color: #6c757d; font-size: 14px;">
          <p>Bu e-posta otomatik olarak gönderilmiştir.</p>
          <p>Hesabınız doğrulandıktan sonra sistemi kullanmaya başlayabilirsiniz.</p>
        </div>
      </div>
    ` : `
      <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
        <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; border-radius: 10px; text-align: center; margin-bottom: 30px;">
          <h1 style="color: white; margin: 0; font-size: 28px;">🏠 Aile Takip Sistemi</h1>
        </div>
        
        <div style="background: #f8f9fa; padding: 30px; border-radius: 10px; text-align: center; margin-bottom: 20px;">
          <h2 style="color: #2c3e50; margin-bottom: 20px;">Giriş Kodunuz</h2>
          <div style="background: white; padding: 20px; border-radius: 8px; border: 2px dashed #007bff; margin: 20px 0;">
            <span style="font-size: 36px; font-weight: bold; color: #007bff; letter-spacing: 8px;">${code}</span>
          </div>
          <p style="color: #6c757d; margin: 15px 0;">Bu kod <strong>15 dakika</strong> geçerlidir.</p>
        </div>
        
        <div style="text-align: center; color: #6c757d; font-size: 14px;">
          <p>Bu e-posta otomatik olarak gönderilmiştir.</p>
          <p>Güvenliğiniz için bu kodu kimseyle paylaşmayın.</p>
        </div>
      </div>
    `;

    await sendEmail(email, emailSubject, emailContent);

    res.json({ 
      message: isNewUser ? 
        'Hoş geldiniz! Doğrulama kodu email adresinize gönderildi' : 
        'Giriş kodu email adresinize gönderildi',
      email: email,
      isNewUser: isNewUser
    });
  } catch (error) {
    console.error('Error sending login code:', error);
    res.status(500).json({ error: 'Giriş kodu gönderilemedi' });
  }
};;

export const verifyLoginCode = async (req: AuthRequest, res: Response) => {
  try {
    const { email, code } = req.body;
    const db = await getDatabase();

    // Verify code
    const loginCode = await db.get(
      'SELECT * FROM login_codes WHERE email = ? AND code = ? AND expires_at > datetime("now")',
      [email, code]
    );

    if (!loginCode) {
      return res.status(400).json({ error: 'Geçersiz veya süresi dolmuş kod' });
    }

    // Get user
    const user = await db.get(
      'SELECT id, family_id, role, full_name FROM users WHERE email = ?',
      [email]
    );

    // If user doesn't have a family, create one
    let familyId = user.family_id;
    if (!familyId) {
      const familyName = `${user.full_name || email.split('@')[0]} Ailesi`;
      const familyResult = await db.run(
        'INSERT INTO families (name, created_by) VALUES (?, ?)',
        [familyName, user.id]
      );
      
      familyId = familyResult.lastID;
      
      // Update user with family_id and set as admin
      await db.run(
        'UPDATE users SET family_id = ?, role = ?, status = ? WHERE id = ?',
        [familyId, 'admin', 'active', user.id]
      );
      
      console.log(`✅ New family created: ${familyName} (ID: ${familyId})`);
    }

    // Generate JWT
    const token = jwt.sign(
      {
        userId: user.id,
        familyId: familyId,
        role: user.role || 'admin',
      },
      process.env.JWT_SECRET || 'fallback_secret',
      { expiresIn: '7d' }
    );

    // Delete used code
    await db.run('DELETE FROM login_codes WHERE email = ?', [email]);

    res.json({ 
      token, 
      userId: user.id, 
      familyId: familyId,
      message: !user.family_id ? 'Hoş geldiniz! Yeni aileniz oluşturuldu.' : 'Giriş başarılı!'
    });
  } catch (error) {
    console.error('Error verifying login code:', error);
    res.status(500).json({ error: 'Kod doğrulanamadı' });
  }
};

export const inviteFamilyMember = async (req: AuthRequest, res: Response) => {
  try {
    const { email } = req.body;
    const familyId = req.familyId;
    const db = await getDatabase();

    // Check if user already exists
    const existingUser = await db.get('SELECT id FROM users WHERE email = ?', [email]);
    
    if (existingUser) {
      return res.status(400).json({ error: 'User already exists' });
    }

    // Create new user with pending status
    await db.run(
      'INSERT INTO users (email, family_id, status) VALUES (?, ?, ?)',
      [email, familyId, 'pending']
    );

    console.log(`User ${email} invited to family ${familyId}`);
    res.json({ message: 'Invite sent successfully' });
  } catch (error) {
    console.error('Error inviting member:', error);
    res.status(500).json({ error: 'Failed to send invite' });
  }
};

export const acceptInvite = async (req: AuthRequest, res: Response) => {
  try {
    const { email } = req.body;
    const db = await getDatabase();

    // Update user status to active
    const result = await db.run(
      'UPDATE users SET status = ? WHERE email = ?',
      ['active', email]
    );

    if (result.changes === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    res.json({ message: 'Invite accepted successfully' });
  } catch (error) {
    console.error('Error accepting invite:', error);
    res.status(500).json({ error: 'Failed to accept invite' });
  }
};
export const getEmailStatus = async (req: AuthRequest, res: Response) => {
  try {
    const status = checkEmailStatus();
    res.json(status);
  } catch (error) {
    console.error('Error checking email status:', error);
    res.status(500).json({ error: 'Email status check failed' });
  }
};