import React, { useState, useEffect } from 'react';
import { authAPI } from '../utils/api';
import { useAuthStore } from '../store/authStore';

const ProfilePage: React.FC = () => {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [emailStatus, setEmailStatus] = useState({ status: 'checking', message: 'Kontrol ediliyor...', mode: 'development' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const { userId } = useAuthStore();

  useEffect(() => {
    loadEmailStatus();
  }, []);

  const loadEmailStatus = async () => {
    try {
      const response = await authAPI.getEmailStatus();
      setEmailStatus(response.data);
    } catch (error) {
      console.error('Error loading email status:', error);
    }
  };

  const handlePasswordChange = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (newPassword !== confirmPassword) {
      setError('Yeni şifreler eşleşmiyor');
      return;
    }

    if (newPassword.length < 8) {
      setError('Şifre en az 8 karakter olmalıdır');
      return;
    }

    setLoading(true);
    try {
      await authAPI.setPassword(currentPassword, newPassword);
      setSuccess('Şifre başarıyla güncellendi');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err: any) {
      setError(err.response?.data?.error || 'Şifre güncellenemedi');
    }
    setLoading(false);
  };

  const testEmailSystem = async () => {
    setLoading(true);
    setError('');
    setSuccess('');
    
    try {
      // Test email config
      const configResponse = await fetch('/api/auth/test-email-config', {
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
      });
      const configData = await configResponse.json();
      console.log('📧 Email Config Test:', configData);
      
      // Send test email
      const testResponse = await fetch('/api/auth/send-test-email', {
        method: 'POST',
        headers: { 
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ email: configData.env.user })
      });
      
      if (testResponse.ok) {
        setSuccess('✅ Test emaili gönderildi! Email kutunuzu kontrol edin.');
      } else {
        const errorData = await testResponse.json();
        setError(`❌ Test email hatası: ${errorData.details || errorData.error}`);
        console.error('Email test error:', errorData);
      }
    } catch (err: any) {
      setError(`❌ Test hatası: ${err.message}`);
      console.error('Email test error:', err);
    }
    setLoading(false);
  };

  return (
    <div style={styles.container}>
      <style>
        {`
          @keyframes slideIn {
            from { opacity: 0; transform: translateY(20px); }
            to { opacity: 1; transform: translateY(0); }
          }
          @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
          }
          .profile-card {
            animation: slideIn 0.6s ease-out;
          }
        `}
      </style>

      {/* Header */}
      <header style={styles.header}>
        <h1 style={styles.title}>⚙️ Profil Ayarları</h1>
        <p style={styles.subtitle}>Hesap bilgilerinizi yönetin</p>
      </header>

      <div style={styles.cardsContainer}>
        {/* Email Status Card */}
        <div style={styles.card} className="profile-card">
          <div style={styles.cardHeader}>
            <h2 style={styles.cardTitle}>📧 Email Sistemi Durumu</h2>
          </div>
          <div style={styles.cardContent}>
            <div style={styles.statusRow}>
              <span style={styles.statusIcon}>
                {emailStatus.status === 'configured' ? '✅' : '⚠️'}
              </span>
              <div style={styles.statusInfo}>
                <p style={styles.statusMessage}>{emailStatus.message}</p>
                <span style={{
                  ...styles.modeBadge,
                  backgroundColor: emailStatus.mode === 'production' ? '#28a745' : '#ffc107',
                  color: emailStatus.mode === 'production' ? 'white' : '#212529'
                }}>
                  {emailStatus.mode === 'production' ? 'Üretim Modu' : 'Geliştirme Modu'}
                </span>
              </div>
            </div>
            
            {emailStatus.mode === 'development' && (
              <div style={styles.infoBox}>
                <h4 style={styles.infoTitle}>💡 Email Konfigürasyonu</h4>
                <p style={styles.infoText}>
                  Gerçek email göndermek için <code>.env</code> dosyasında Gmail App Password'ünüzü ayarlayın.
                </p>
                <p style={styles.infoText}>
                  Şu anda kodlar console'da gösterilmektedir.
                </p>
              </div>
            )}
            
            <div style={styles.testSection}>
              <button 
                style={styles.testButton}
                onClick={testEmailSystem}
                disabled={loading}
              >
                {loading ? (
                  <>
                    <div style={styles.spinner}></div>
                    Test Ediliyor...
                  </>
                ) : (
                  '🧪 Email Sistemini Test Et'
                )}
              </button>
            </div>
          </div>
        </div>

        {/* Password Change Card */}
        <div style={styles.card} className="profile-card">
          <div style={styles.cardHeader}>
            <h2 style={styles.cardTitle}>🔐 Şifre Değiştir</h2>
          </div>
          <div style={styles.cardContent}>
            {error && (
              <div style={styles.errorAlert}>
                <span style={styles.alertIcon}>⚠️</span>
                <span>{error}</span>
              </div>
            )}
            
            {success && (
              <div style={styles.successAlert}>
                <span style={styles.alertIcon}>✅</span>
                <span>{success}</span>
              </div>
            )}

            <form onSubmit={handlePasswordChange} style={styles.form}>
              <div style={styles.inputGroup}>
                <label style={styles.label}>Mevcut Şifre</label>
                <input
                  type="password"
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                  style={styles.input}
                  placeholder="Mevcut şifrenizi girin"
                />
                <small style={styles.helpText}>
                  Şifreniz yoksa boş bırakabilirsiniz
                </small>
              </div>

              <div style={styles.inputGroup}>
                <label style={styles.label}>Yeni Şifre</label>
                <input
                  type="password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  style={styles.input}
                  placeholder="En az 8 karakter"
                  minLength={8}
                  required
                />
              </div>

              <div style={styles.inputGroup}>
                <label style={styles.label}>Yeni Şifre (Tekrar)</label>
                <input
                  type="password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  style={styles.input}
                  placeholder="Yeni şifrenizi tekrar girin"
                  minLength={8}
                  required
                />
              </div>

              <button type="submit" style={styles.primaryButton} disabled={loading}>
                {loading ? (
                  <>
                    <div style={styles.spinner}></div>
                    Güncelleniyor...
                  </>
                ) : (
                  '🔐 Şifreyi Güncelle'
                )}
              </button>
            </form>
          </div>
        </div>

        {/* Security Tips Card */}
        <div style={styles.card} className="profile-card">
          <div style={styles.cardHeader}>
            <h2 style={styles.cardTitle}>🛡️ Güvenlik İpuçları</h2>
          </div>
          <div style={styles.cardContent}>
            <div style={styles.tipsList}>
              <div style={styles.tip}>
                <span style={styles.tipIcon}>🔒</span>
                <div>
                  <h4 style={styles.tipTitle}>Güçlü Şifre Kullanın</h4>
                  <p style={styles.tipText}>En az 8 karakter, büyük-küçük harf, rakam ve özel karakter içersin</p>
                </div>
              </div>
              
              <div style={styles.tip}>
                <span style={styles.tipIcon}>📧</span>
                <div>
                  <h4 style={styles.tipTitle}>Email Güvenliği</h4>
                  <p style={styles.tipText}>Giriş kodlarını kimseyle paylaşmayın, 15 dakika içinde kullanın</p>
                </div>
              </div>
              
              <div style={styles.tip}>
                <span style={styles.tipIcon}>🔄</span>
                <div>
                  <h4 style={styles.tipTitle}>Düzenli Güncelleme</h4>
                  <p style={styles.tipText}>Şifrenizi düzenli olarak değiştirin ve farklı servislerde aynı şifreyi kullanmayın</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

const styles = {
  container: {
    padding: '24px',
    maxWidth: '1200px',
    margin: '0 auto',
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
  },
  header: {
    textAlign: 'center' as const,
    marginBottom: '32px',
  },
  title: {
    fontSize: '32px',
    fontWeight: '800',
    color: '#2c3e50',
    margin: '0 0 8px 0',
    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    WebkitBackgroundClip: 'text',
    WebkitTextFillColor: 'transparent',
  },
  subtitle: {
    fontSize: '16px',
    color: '#6c757d',
    margin: 0,
    fontWeight: '500',
  },
  cardsContainer: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))',
    gap: '24px',
  },
  card: {
    backgroundColor: '#fff',
    borderRadius: '16px',
    boxShadow: '0 4px 20px rgba(0,0,0,0.08)',
    border: '1px solid #e9ecef',
    overflow: 'hidden',
    transition: 'all 0.3s ease',
  },
  cardHeader: {
    padding: '24px 24px 0 24px',
  },
  cardTitle: {
    fontSize: '20px',
    fontWeight: '600',
    color: '#2c3e50',
    margin: 0,
  },
  cardContent: {
    padding: '24px',
  },
  statusRow: {
    display: 'flex',
    alignItems: 'center',
    gap: '16px',
    marginBottom: '16px',
  },
  statusIcon: {
    fontSize: '24px',
  },
  statusInfo: {
    flex: 1,
  },
  statusMessage: {
    fontSize: '16px',
    color: '#2c3e50',
    margin: '0 0 8px 0',
    fontWeight: '500',
  },
  modeBadge: {
    padding: '4px 12px',
    borderRadius: '20px',
    fontSize: '12px',
    fontWeight: '600',
  },
  infoBox: {
    backgroundColor: '#e3f2fd',
    padding: '16px',
    borderRadius: '8px',
    border: '1px solid #bbdefb',
  },
  infoTitle: {
    fontSize: '14px',
    fontWeight: '600',
    color: '#1976d2',
    margin: '0 0 8px 0',
  },
  infoText: {
    fontSize: '14px',
    color: '#1976d2',
    margin: '0 0 8px 0',
    lineHeight: '1.4',
  },
  errorAlert: {
    backgroundColor: '#f8d7da',
    color: '#721c24',
    padding: '12px 16px',
    borderRadius: '8px',
    marginBottom: '16px',
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    border: '1px solid #f5c6cb',
  },
  successAlert: {
    backgroundColor: '#d4edda',
    color: '#155724',
    padding: '12px 16px',
    borderRadius: '8px',
    marginBottom: '16px',
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    border: '1px solid #c3e6cb',
  },
  alertIcon: {
    fontSize: '16px',
  },
  form: {
    display: 'flex',
    flexDirection: 'column' as const,
    gap: '20px',
  },
  inputGroup: {
    display: 'flex',
    flexDirection: 'column' as const,
    gap: '6px',
  },
  label: {
    fontSize: '14px',
    fontWeight: '600',
    color: '#495057',
  },
  input: {
    padding: '12px 16px',
    border: '2px solid #e9ecef',
    borderRadius: '8px',
    fontSize: '16px',
    transition: 'border-color 0.3s ease',
    outline: 'none',
  },
  helpText: {
    fontSize: '12px',
    color: '#6c757d',
    fontStyle: 'italic',
  },
  primaryButton: {
    backgroundColor: '#007bff',
    color: '#fff',
    border: 'none',
    borderRadius: '8px',
    padding: '12px 20px',
    fontSize: '16px',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'all 0.3s ease',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '8px',
  },
  spinner: {
    width: '16px',
    height: '16px',
    border: '2px solid transparent',
    borderTop: '2px solid #fff',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite',
  },
  testSection: {
    marginTop: '16px',
    paddingTop: '16px',
    borderTop: '1px solid #e9ecef',
  },
  testButton: {
    backgroundColor: '#17a2b8',
    color: '#fff',
    border: 'none',
    borderRadius: '8px',
    padding: '10px 16px',
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'all 0.3s ease',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '8px',
    width: '100%',
  },
  tipsList: {
    display: 'flex',
    flexDirection: 'column' as const,
    gap: '16px',
  },
  tip: {
    display: 'flex',
    alignItems: 'flex-start',
    gap: '12px',
  },
  tipIcon: {
    fontSize: '20px',
    marginTop: '2px',
  },
  tipTitle: {
    fontSize: '16px',
    fontWeight: '600',
    color: '#2c3e50',
    margin: '0 0 4px 0',
  },
  tipText: {
    fontSize: '14px',
    color: '#6c757d',
    margin: 0,
    lineHeight: '1.4',
  },
};

export default ProfilePage;