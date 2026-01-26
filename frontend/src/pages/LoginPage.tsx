import React, { useState, useEffect } from 'react';
import { useAuthStore } from '../store/authStore';
import { authAPI } from '../utils/api';

type AuthMode = 'login' | 'register';
type AuthStep = 'email' | 'code' | 'password';

interface FormData {
  email: string;
  fullName: string;
  password: string;
  confirmPassword: string;
}

const LoginPage: React.FC = () => {
  const [mode, setMode] = useState<AuthMode>('login');
  const [step, setStep] = useState<AuthStep>('email');
  const [formData, setFormData] = useState<FormData>({
    email: '',
    fullName: '',
    password: '',
    confirmPassword: ''
  });
  const [code, setCode] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showConsoleHelp, setShowConsoleHelp] = useState(false);
  const [isNewUser, setIsNewUser] = useState(false);
  const setAuth = useAuthStore((state) => state.setAuth);

  // Auto-fill email for development
  useEffect(() => {
    if (process.env.NODE_ENV === 'development') {
      setFormData(prev => ({ ...prev, email: 'erhan.koksal@gmail.com' }));
    }
  }, []);

  const handleInputChange = (field: keyof FormData, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    setError('');
  };

  const validateEmail = (email: string) => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  };

  const validatePassword = (password: string) => {
    return password.length >= 6;
  };

  const handleSendCode = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.email) {
      setError('Email adresi gerekli');
      return;
    }

    if (!validateEmail(formData.email)) {
      setError('Geçerli bir email adresi girin');
      return;
    }

    if (mode === 'register') {
      if (!formData.fullName.trim()) {
        setError('Ad soyad gerekli');
        return;
      }
      if (formData.fullName.trim().length < 2) {
        setError('Ad soyad en az 2 karakter olmalı');
        return;
      }
    }

    setLoading(true);
    setError('');
    setShowConsoleHelp(false);
    
    try {
      const response = await authAPI.sendLoginCode(formData.email);
      setStep('code');
      setShowConsoleHelp(true);
      setIsNewUser(response.data.isNewUser);
      
      console.log('✅ Kod gönderildi:', response.data.message);
      
    } catch (err: any) {
      console.error('❌ Kod gönderme hatası:', err);
      setError(err.response?.data?.error || 'Kod gönderilemedi');
      setShowConsoleHelp(true);
    }
    setLoading(false);
  };

  const handleVerifyCode = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!code || code.length !== 6) {
      setError('6 haneli kodu girin');
      return;
    }

    setLoading(true);
    setError('');
    
    try {
      const response = await authAPI.verifyCode(formData.email, code);
      console.log('✅ Giriş başarılı:', response.data);
      
      setAuth(response.data.token, response.data.userId, response.data.familyId);
      window.location.href = '/';
      
    } catch (err: any) {
      console.error('❌ Kod doğrulama hatası:', err);
      setError(err.response?.data?.error || 'Kod doğrulanamadı');
      setCode('');
    }
    setLoading(false);
  };

  const handlePasswordLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.email || !formData.password) {
      setError('Email ve şifre gerekli');
      return;
    }

    if (!validatePassword(formData.password)) {
      setError('Şifre en az 6 karakter olmalı');
      return;
    }

    setLoading(true);
    setError('');
    
    try {
      const response = await authAPI.loginWithPassword(formData.email, formData.password);
      console.log('✅ Şifre ile giriş başarılı:', response.data);
      
      setAuth(response.data.token, response.data.userId, response.data.familyId);
      window.location.href = '/';
      
    } catch (err: any) {
      console.error('❌ Şifre giriş hatası:', err);
      setError(err.response?.data?.error || 'Giriş başarısız');
    }
    setLoading(false);
  };

  const handleCodeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value.replace(/\D/g, '').slice(0, 6);
    setCode(value);
  };

  const resetForm = () => {
    setStep('email');
    setCode('');
    setError('');
    setShowConsoleHelp(false);
    setIsNewUser(false);
  };

  const switchMode = (newMode: AuthMode) => {
    setMode(newMode);
    resetForm();
  };

  const openConsole = () => {
    alert('Geliştirici konsolunu açmak için:\n\n' +
          '• Chrome/Edge: F12 veya Ctrl+Shift+I\n' +
          '• Firefox: F12 veya Ctrl+Shift+K\n' +
          '• Safari: Cmd+Option+I\n\n' +
          'Console sekmesinde giriş kodunu bulabilirsiniz.');
  };

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        {/* Header */}
        <div style={styles.header}>
          <h1 style={styles.title}>🏠 Aile Takip Sistemi</h1>
          <p style={styles.subtitle}>
            {step === 'email' 
              ? (mode === 'login' ? 'Hesabınıza giriş yapın' : 'Yeni hesap oluşturun')
              : step === 'code' 
              ? 'Doğrulama kodunu girin'
              : 'Şifrenizi girin'
            }
          </p>
        </div>

        {/* Mode Tabs */}
        {step === 'email' && (
          <div style={styles.tabs}>
            <button
              style={{
                ...styles.tab,
                ...(mode === 'login' ? styles.tabActive : {})
              }}
              onClick={() => switchMode('login')}
            >
              🔑 Giriş Yap
            </button>
            <button
              style={{
                ...styles.tab,
                ...(mode === 'register' ? styles.tabActive : {})
              }}
              onClick={() => switchMode('register')}
            >
              ✨ Kayıt Ol
            </button>
          </div>
        )}

        {/* Error Display */}
        {error && (
          <div style={styles.error}>
            <span>⚠️ {error}</span>
          </div>
        )}

        {/* Console Help */}
        {showConsoleHelp && (
          <div style={styles.consoleHelp}>
            <div style={styles.consoleHeader}>
              <span>🖥️ Geliştirici Modu</span>
              <button 
                style={styles.consoleButton}
                onClick={openConsole}
                type="button"
              >
                Konsol Nasıl Açılır?
              </button>
            </div>
            <p style={styles.consoleText}>
              Email gönderilemiyor, ancak giriş kodu <strong>backend konsolunda</strong> görünüyor.
              Kodu oradan kopyalayıp buraya yapıştırabilirsiniz.
            </p>
          </div>
        )}

        {/* Email Step */}
        {step === 'email' && (
          <form onSubmit={handleSendCode} style={styles.form}>
            {/* Email Input */}
            <div style={styles.inputGroup}>
              <label style={styles.label}>📧 Email Adresiniz</label>
              <input
                type="email"
                value={formData.email}
                onChange={(e) => handleInputChange('email', e.target.value)}
                style={styles.input}
                placeholder="ornek@email.com"
                required
                autoFocus
              />
            </div>

            {/* Full Name Input (Register only) */}
            {mode === 'register' && (
              <div style={styles.inputGroup}>
                <label style={styles.label}>👤 Ad Soyad</label>
                <input
                  type="text"
                  value={formData.fullName}
                  onChange={(e) => handleInputChange('fullName', e.target.value)}
                  style={styles.input}
                  placeholder="Adınız Soyadınız"
                  required
                />
              </div>
            )}

            {/* Submit Button */}
            <button type="submit" style={styles.button} disabled={loading}>
              {loading ? (
                <span>
                  <span style={styles.spinner}>⏳</span> Kod Gönderiliyor...
                </span>
              ) : (
                mode === 'login' ? '📧 Giriş Kodu Gönder' : '📧 Kayıt Kodu Gönder'
              )}
            </button>

            {/* Alternative Login Methods */}
            {mode === 'login' && (
              <div style={styles.divider}>
                <span style={styles.dividerText}>veya</span>
              </div>
            )}

            {mode === 'login' && (
              <button 
                type="button" 
                style={styles.secondaryButton}
                onClick={() => setStep('password')}
              >
                🔐 Şifre ile Giriş
              </button>
            )}
            
            {/* Info */}
            <div style={styles.info}>
              {mode === 'login' ? (
                <>
                  <p>✨ Hesabınız yoksa otomatik kayıt olacaksınız</p>
                  <p>📱 Kod email adresinize gönderilecek</p>
                  <p>🖥️ Email çalışmıyorsa konsol loglarını kontrol edin</p>
                </>
              ) : (
                <>
                  <p>🎉 Hoş geldiniz! Yeni hesabınızı oluşturalım</p>
                  <p>📧 Email adresinize doğrulama kodu gönderilecek</p>
                  <p>👨‍👩‍👧‍👦 Otomatik olarak aile grubunuz oluşturulacak</p>
                </>
              )}
            </div>
          </form>
        )}

        {/* Code Verification Step */}
        {step === 'code' && (
          <form onSubmit={handleVerifyCode} style={styles.form}>
            <div style={styles.codeInfo}>
              <p><strong>{formData.email}</strong> adresine kod gönderildi</p>
              <p style={styles.codeInfoSub}>
                {isNewUser ? '🎉 Yeni hesabınız oluşturuluyor' : '👋 Tekrar hoş geldiniz'}
              </p>
            </div>
            
            <div style={styles.inputGroup}>
              <label style={styles.label}>🔑 Doğrulama Kodu</label>
              <input
                type="text"
                value={code}
                onChange={handleCodeChange}
                style={styles.codeInput}
                placeholder="123456"
                maxLength={6}
                required
                autoFocus
              />
              <div style={styles.codeHint}>
                6 haneli sayısal kod girin
              </div>
            </div>
            
            <button type="submit" style={styles.button} disabled={loading || code.length !== 6}>
              {loading ? (
                <span>
                  <span style={styles.spinner}>⏳</span> Doğrulanıyor...
                </span>
              ) : (
                isNewUser ? '✅ Hesap Oluştur' : '✅ Giriş Yap'
              )}
            </button>
            
            <div style={styles.actionButtons}>
              <button 
                type="button" 
                style={styles.backButton}
                onClick={resetForm}
              >
                ← Email Değiştir
              </button>
              
              <button 
                type="button" 
                style={styles.resendButton}
                onClick={() => handleSendCode({ preventDefault: () => {} } as any)}
                disabled={loading}
              >
                🔄 Kodu Yeniden Gönder
              </button>
            </div>
          </form>
        )}

        {/* Password Login Step */}
        {step === 'password' && (
          <form onSubmit={handlePasswordLogin} style={styles.form}>
            <div style={styles.inputGroup}>
              <label style={styles.label}>📧 Email Adresiniz</label>
              <input
                type="email"
                value={formData.email}
                onChange={(e) => handleInputChange('email', e.target.value)}
                style={styles.input}
                placeholder="ornek@email.com"
                required
              />
            </div>

            <div style={styles.inputGroup}>
              <label style={styles.label}>🔐 Şifreniz</label>
              <input
                type="password"
                value={formData.password}
                onChange={(e) => handleInputChange('password', e.target.value)}
                style={styles.input}
                placeholder="En az 6 karakter"
                required
                autoFocus
              />
            </div>
            
            <button type="submit" style={styles.button} disabled={loading}>
              {loading ? (
                <span>
                  <span style={styles.spinner}>⏳</span> Giriş yapılıyor...
                </span>
              ) : (
                '🔐 Şifre ile Giriş'
              )}
            </button>

            <div style={styles.actionButtons}>
              <button 
                type="button" 
                style={styles.backButton}
                onClick={resetForm}
              >
                ← Email Kodu ile Giriş
              </button>
              
              <button 
                type="button" 
                style={styles.linkButton}
                onClick={() => alert('Şifre sıfırlama özelliği yakında eklenecek!')}
              >
                🔄 Şifremi Unuttum
              </button>
            </div>
          </form>
        )}

        {/* Footer */}
        <div style={styles.footer}>
          <p>🔒 Güvenli giriş sistemi</p>
          <p>💡 Sorun yaşıyorsanız konsol loglarını kontrol edin</p>
          {mode === 'register' && (
            <p>📋 Kayıt olarak <a href="#" style={styles.link}>Kullanım Şartları</a>'nı kabul etmiş olursunuz</p>
          )}
        </div>
      </div>
    </div>
  );
};

const styles = {
  container: {
    minHeight: '100vh',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    padding: '20px',
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
  },
  card: {
    backgroundColor: '#fff',
    borderRadius: '20px',
    padding: '40px',
    width: '100%',
    maxWidth: '500px',
    boxShadow: '0 20px 40px rgba(0,0,0,0.15)',
    textAlign: 'center' as const,
  },
  header: {
    marginBottom: '32px',
  },
  title: {
    fontSize: '32px',
    fontWeight: '800',
    color: '#2c3e50',
    margin: '0 0 8px 0',
    textShadow: '0 2px 4px rgba(0,0,0,0.1)',
  },
  subtitle: {
    fontSize: '16px',
    color: '#6c757d',
    margin: 0,
    fontWeight: '500',
  },
  tabs: {
    display: 'flex',
    backgroundColor: '#f8f9fa',
    borderRadius: '12px',
    padding: '4px',
    marginBottom: '24px',
    gap: '4px',
  },
  tab: {
    flex: 1,
    padding: '12px 16px',
    border: 'none',
    borderRadius: '8px',
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'all 0.3s ease',
    backgroundColor: 'transparent',
    color: '#6c757d',
  },
  tabActive: {
    backgroundColor: '#fff',
    color: '#3b82f6',
    boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
  },
  error: {
    backgroundColor: '#fee',
    color: '#c53030',
    padding: '16px',
    borderRadius: '12px',
    marginBottom: '24px',
    fontSize: '14px',
    fontWeight: '600',
    border: '2px solid #feb2b2',
  },
  consoleHelp: {
    backgroundColor: '#f0f9ff',
    border: '2px solid #0ea5e9',
    borderRadius: '12px',
    padding: '20px',
    marginBottom: '24px',
    textAlign: 'left' as const,
  },
  consoleHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '12px',
    fontSize: '16px',
    fontWeight: '700',
    color: '#0369a1',
  },
  consoleButton: {
    backgroundColor: '#0ea5e9',
    color: '#fff',
    border: 'none',
    borderRadius: '6px',
    padding: '6px 12px',
    fontSize: '12px',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'background-color 0.2s',
  },
  consoleText: {
    fontSize: '14px',
    color: '#0369a1',
    margin: 0,
    lineHeight: '1.5',
  },
  form: {
    display: 'flex',
    flexDirection: 'column' as const,
    gap: '24px',
  },
  inputGroup: {
    textAlign: 'left' as const,
  },
  label: {
    display: 'block',
    fontSize: '15px',
    fontWeight: '700',
    color: '#374151',
    marginBottom: '8px',
  },
  input: {
    width: '100%',
    padding: '16px 20px',
    border: '2px solid #e5e7eb',
    borderRadius: '12px',
    fontSize: '16px',
    outline: 'none',
    transition: 'all 0.3s ease',
    boxSizing: 'border-box' as const,
    fontWeight: '500',
  },
  codeInput: {
    width: '100%',
    padding: '20px',
    border: '3px solid #e5e7eb',
    borderRadius: '16px',
    fontSize: '28px',
    textAlign: 'center' as const,
    letterSpacing: '8px',
    fontWeight: '800',
    outline: 'none',
    boxSizing: 'border-box' as const,
    color: '#1f2937',
    transition: 'all 0.3s ease',
  },
  codeHint: {
    fontSize: '12px',
    color: '#6b7280',
    marginTop: '8px',
    textAlign: 'center' as const,
  },
  button: {
    backgroundColor: '#3b82f6',
    color: '#fff',
    border: 'none',
    borderRadius: '12px',
    padding: '18px 24px',
    fontSize: '16px',
    fontWeight: '700',
    cursor: 'pointer',
    transition: 'all 0.3s ease',
    boxShadow: '0 4px 12px rgba(59, 130, 246, 0.3)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '8px',
  },
  secondaryButton: {
    backgroundColor: '#10b981',
    color: '#fff',
    border: 'none',
    borderRadius: '12px',
    padding: '16px 24px',
    fontSize: '15px',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'all 0.3s ease',
    boxShadow: '0 4px 12px rgba(16, 185, 129, 0.3)',
  },
  divider: {
    position: 'relative' as const,
    textAlign: 'center' as const,
    margin: '20px 0',
  },
  dividerText: {
    backgroundColor: '#fff',
    color: '#6b7280',
    padding: '0 16px',
    fontSize: '14px',
    fontWeight: '500',
  },
  spinner: {
    animation: 'spin 1s linear infinite',
  },
  actionButtons: {
    display: 'flex',
    gap: '12px',
    justifyContent: 'space-between',
  },
  backButton: {
    backgroundColor: 'transparent',
    color: '#6b7280',
    border: '2px solid #e5e7eb',
    borderRadius: '8px',
    padding: '12px 16px',
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'all 0.2s',
    flex: 1,
  },
  resendButton: {
    backgroundColor: '#10b981',
    color: '#fff',
    border: 'none',
    borderRadius: '8px',
    padding: '12px 16px',
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'all 0.2s',
    flex: 1,
  },
  linkButton: {
    backgroundColor: 'transparent',
    color: '#3b82f6',
    border: 'none',
    padding: '12px 16px',
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
    textDecoration: 'underline',
    flex: 1,
  },
  codeInfo: {
    backgroundColor: '#eff6ff',
    padding: '20px',
    borderRadius: '12px',
    fontSize: '14px',
    color: '#1e40af',
    border: '2px solid #dbeafe',
  },
  codeInfoSub: {
    fontSize: '12px',
    color: '#6b7280',
    marginTop: '8px',
    margin: '8px 0 0 0',
  },
  info: {
    backgroundColor: '#f0fdf4',
    padding: '20px',
    borderRadius: '12px',
    fontSize: '14px',
    color: '#166534',
    lineHeight: '1.6',
  },
  footer: {
    marginTop: '32px',
    padding: '20px 0 0 0',
    borderTop: '1px solid #e5e7eb',
    fontSize: '13px',
    color: '#6b7280',
    lineHeight: '1.5',
  },
  link: {
    color: '#3b82f6',
    textDecoration: 'none',
    fontWeight: '600',
  },
};

export default LoginPage;