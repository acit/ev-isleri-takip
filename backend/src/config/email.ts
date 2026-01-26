import nodemailer from 'nodemailer';
import dotenv from 'dotenv';

dotenv.config();

const createTransporter = () => {
  const config = {
    service: 'gmail',
    host: 'smtp.gmail.com',
    port: 587,
    secure: false,
    auth: {
      user: process.env.EMAIL_USER,
      pass: process.env.EMAIL_PASSWORD,
    },
    tls: {
      rejectUnauthorized: false
    }
  };

  console.log('📧 Email yapılandırması:');
  console.log('- Service: gmail');
  console.log('- User:', config.auth.user);
  console.log('- Password:', config.auth.pass ? '***' + config.auth.pass.slice(-4) : 'YOK');
  console.log('- Host:', config.host);
  console.log('- Port:', config.port);

  return nodemailer.createTransport(config);
};

export const sendEmail = async (to: string, subject: string, html: string) => {
  try {
    // Check if email credentials are properly configured
    const hasValidConfig = process.env.EMAIL_USER && 
                          process.env.EMAIL_PASSWORD && 
                          process.env.EMAIL_USER !== 'your_email@gmail.com' &&
                          process.env.EMAIL_PASSWORD !== 'your_gmail_app_password_here' &&
                          process.env.EMAIL_PASSWORD !== 'abcd efgh ijkl mnop';

    if (!hasValidConfig) {
      console.log('\n⚠️ EMAIL YAPILANDIRMASI EKSİK:');
      console.log('- EMAIL_USER:', process.env.EMAIL_USER || 'YOK');
      console.log('- EMAIL_PASSWORD:', process.env.EMAIL_PASSWORD ? 'MEVCUT (ama placeholder olabilir)' : 'YOK');
      
      // Extract and show code
      const codeMatch = html.match(/class="code">(\d{6})</);
      if (codeMatch) {
        console.log('\n🔑 E-POSTA BAŞARISIZ - GİRİŞ KODU:', codeMatch[1]);
        console.log('👆 Bu kodu kullanarak giriş yapabilirsiniz');
        console.log('💡 Gmail App Password oluşturun: https://myaccount.google.com/apppasswords\n');
      }
      return false;
    }

    // Try to send email
    console.log(`📧 Email gönderiliyor: ${to}`);
    const transporter = createTransporter();
    
    // Test connection first
    console.log('🔍 SMTP bağlantısı test ediliyor...');
    await transporter.verify();
    console.log('✅ SMTP bağlantısı başarılı');
    
    const mailOptions = {
      from: process.env.EMAIL_FROM || process.env.EMAIL_USER,
      to,
      subject,
      html,
    };
    
    const result = await transporter.sendMail(mailOptions);
    console.log('✅ Email başarıyla gönderildi:', result.messageId);
    return true;
    
  } catch (error: any) {
    console.error('❌ Email gönderme hatası:', error.message);
    console.error('📋 Hata kodu:', error.code);
    console.error('📋 Hata komutu:', error.command);
    
    // Gmail specific errors
    if (error.code === 'EAUTH') {
      console.error('\n🔐 GMAIL AUTHENTICATION HATASI:');
      console.error('1. Gmail hesabınızda 2FA aktif mi?');
      console.error('2. App Password oluşturdunuz mu?');
      console.error('3. .env dosyasındaki EMAIL_PASSWORD doğru mu?');
      console.error('4. Link: https://myaccount.google.com/apppasswords');
    }
    
    // Show fallback code
    const codeMatch = html.match(/class="code">(\d{6})</);
    if (codeMatch) {
      console.log('\n🔑 E-POSTA BAŞARISIZ - GİRİŞ KODU:', codeMatch[1]);
      console.log('👆 Bu kodu kullanarak giriş yapabilirsiniz');
      console.log('💡 E-posta ayarlarını düzeltin\n');
    }
    
    return false;
  }
};

export const sendLoginCode = async (email: string, code: string): Promise<boolean> => {
  const subject = '🔐 Aile Takip Sistemi - Giriş Kodunuz';
  
  const html = `
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <title>Giriş Kodu</title>
      <style>
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; padding: 0; background: #f8f9fa; }
        .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 40px 20px; text-align: center; }
        .header h1 { color: white; margin: 0; font-size: 28px; font-weight: 700; }
        .header p { color: #e3f2fd; margin: 10px 0 0 0; font-size: 16px; }
        .content { padding: 40px 20px; text-align: center; }
        .code-box { background: linear-gradient(135deg, #e3f2fd 0%, #f3e5f5 100%); border: 3px solid #2196f3; border-radius: 16px; padding: 30px; margin: 30px 0; box-shadow: 0 4px 8px rgba(33,150,243,0.2); }
        .code { font-size: 42px; font-weight: 800; color: #1976d2; letter-spacing: 12px; margin: 0; text-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .code-label { margin: 15px 0 0 0; color: #1976d2; font-weight: 600; font-size: 16px; }
        .info { background: linear-gradient(135deg, #e8f5e8 0%, #f1f8e9 100%); padding: 25px; border-radius: 12px; margin: 25px 0; border-left: 4px solid #4caf50; }
        .info p { margin: 0; color: #2e7d32; font-weight: 500; }
        .info p:first-child { font-size: 18px; font-weight: 700; }
        .warning { color: #6c757d; margin-top: 30px; font-size: 14px; line-height: 1.5; }
        .footer { background: #f8f9fa; padding: 25px; text-align: center; color: #6c757d; font-size: 14px; }
        .footer p { margin: 5px 0; }
      </style>
    </head>
    <body>
      <div class="container">
        <div class="header">
          <h1>🏠 Aile Takip Sistemi</h1>
          <p>Giriş kodunuz hazır</p>
        </div>
        
        <div class="content">
          <h2 style="color: #2c3e50; margin-bottom: 20px; font-size: 24px;">Giriş Kodunuz</h2>
          
          <div class="code-box">
            <div class="code">${code}</div>
            <p class="code-label">Bu kodu giriş sayfasına yazın</p>
          </div>
          
          <div class="info">
            <p>📱 Kod 10 dakika geçerlidir</p>
            <p style="margin-top: 10px;">🔒 Güvenliğiniz için kodu kimseyle paylaşmayın</p>
          </div>
          
          <p class="warning">
            Eğer bu isteği siz yapmadıysanız, bu emaili görmezden gelebilirsiniz.<br>
            Hesabınızın güvenliği için şifrenizi değiştirmeyi düşünün.
          </p>
        </div>
        
        <div class="footer">
          <p><strong>© 2026 Aile Takip Sistemi</strong></p>
          <p>Güvenli Aile Yönetimi Platformu</p>
          <p style="margin-top: 15px; font-size: 12px;">Bu otomatik bir emaildir, lütfen yanıtlamayın.</p>
        </div>
      </div>
    </body>
    </html>
  `;

  console.log('📧 Login kodu gönderiliyor:', { email, code });
  return await sendEmail(email, subject, html);
};

export default createTransporter();