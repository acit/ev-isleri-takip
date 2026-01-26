import React from 'react';
import { useAuthStore } from '../store/authStore';

interface LayoutProps {
  children: React.ReactNode;
  currentPage: string;
  onNavigate: (page: string) => void;
}

const Layout: React.FC<LayoutProps> = ({ children, currentPage, onNavigate }) => {
  const { clearAuth } = useAuthStore();

  const handleLogout = () => {
    clearAuth();
    window.location.replace('/');
  };

  const menuItems = [
    { id: 'dashboard', label: 'Ana Panel', icon: '🏠' },
    { id: 'tasks', label: 'Görevler', icon: '📋' },
    { id: 'inventory', label: 'Envanter', icon: '📦' },
    { id: 'shopping', label: 'Alışveriş', icon: '🛒' },
    { id: 'budget', label: 'Bütçe', icon: '💰' },
    { id: 'messages', label: 'Mesajlar', icon: '💬' },
    { id: 'profile', label: 'Profil', icon: '⚙️' },
  ];

  return (
    <div style={styles.container}>
      {/* Sidebar */}
      <aside style={styles.sidebar}>
        <div style={styles.logo}>
          <h2 style={styles.logoText}>🏠 Aile Takip</h2>
          <p style={styles.logoSubtext}>Yönetim Sistemi</p>
        </div>
        
        <nav style={styles.nav}>
          {menuItems.map(item => (
            <button
              key={item.id}
              style={{
                ...styles.navItem,
                ...(currentPage === item.id ? styles.navItemActive : {})
              }}
              onClick={() => onNavigate(item.id)}
            >
              <span style={styles.navIcon}>{item.icon}</span>
              <span style={styles.navLabel}>{item.label}</span>
              {currentPage === item.id && <span style={styles.activeIndicator}>●</span>}
            </button>
          ))}
        </nav>

        <div style={styles.sidebarFooter}>
          <div style={styles.userInfo}>
            <div style={styles.userAvatar}>👤</div>
            <div style={styles.userDetails}>
              <div style={styles.userName}>Test Kullanıcı</div>
              <div style={styles.userRole}>Aile Yöneticisi</div>
            </div>
          </div>
          <button style={styles.logoutButton} onClick={handleLogout}>
            <span style={styles.navIcon}>🚪</span>
            <span style={styles.navLabel}>Güvenli Çıkış</span>
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main style={styles.main}>
        <div style={styles.topBar}>
          <div style={styles.breadcrumb}>
            <span style={styles.breadcrumbHome}>🏠</span>
            <span style={styles.breadcrumbSeparator}>›</span>
            <span style={styles.breadcrumbCurrent}>
              {menuItems.find(item => item.id === currentPage)?.label || 'Ana Panel'}
            </span>
          </div>
          <div style={styles.topBarActions}>
            <span style={styles.currentTime}>
              {new Date().toLocaleDateString('tr-TR', { 
                weekday: 'long', 
                year: 'numeric', 
                month: 'long', 
                day: 'numeric' 
              })}
            </span>
          </div>
        </div>
        <div style={styles.content}>
          {children}
        </div>
      </main>
    </div>
  );
};

const styles = {
  container: {
    display: 'flex',
    height: '100vh',
    backgroundColor: '#f8f9fa',
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
  },
  sidebar: {
    width: '280px',
    backgroundColor: '#2c3e50',
    color: '#fff',
    display: 'flex',
    flexDirection: 'column' as const,
    boxShadow: '2px 0 12px rgba(0,0,0,0.15)',
  },
  logo: {
    padding: '24px 20px',
    borderBottom: '1px solid #34495e',
    textAlign: 'center' as const,
  },
  logoText: {
    fontSize: '20px',
    fontWeight: '700',
    margin: '0 0 4px 0',
    color: '#fff',
  },
  logoSubtext: {
    fontSize: '12px',
    color: '#bdc3c7',
    margin: 0,
    fontWeight: '400',
  },
  nav: {
    flex: 1,
    padding: '20px 0',
  },
  navItem: {
    width: '100%',
    display: 'flex',
    alignItems: 'center',
    padding: '14px 20px',
    border: 'none',
    backgroundColor: 'transparent',
    color: '#bdc3c7',
    cursor: 'pointer',
    transition: 'all 0.3s ease',
    fontSize: '15px',
    textAlign: 'left' as const,
    position: 'relative' as const,
  },
  navItemActive: {
    backgroundColor: '#3498db',
    color: '#fff',
    boxShadow: 'inset 4px 0 0 #2980b9',
  },
  navIcon: {
    fontSize: '20px',
    marginRight: '12px',
    width: '24px',
    textAlign: 'center' as const,
  },
  navLabel: {
    fontWeight: '500',
    flex: 1,
  },
  activeIndicator: {
    color: '#fff',
    fontSize: '8px',
    marginLeft: 'auto',
  },
  sidebarFooter: {
    padding: '20px',
    borderTop: '1px solid #34495e',
  },
  userInfo: {
    display: 'flex',
    alignItems: 'center',
    marginBottom: '16px',
    padding: '12px',
    backgroundColor: '#34495e',
    borderRadius: '8px',
  },
  userAvatar: {
    width: '40px',
    height: '40px',
    borderRadius: '50%',
    backgroundColor: '#3498db',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '18px',
    marginRight: '12px',
  },
  userDetails: {
    flex: 1,
  },
  userName: {
    fontSize: '14px',
    fontWeight: '600',
    color: '#fff',
    marginBottom: '2px',
  },
  userRole: {
    fontSize: '12px',
    color: '#bdc3c7',
  },
  logoutButton: {
    width: '100%',
    display: 'flex',
    alignItems: 'center',
    padding: '12px',
    border: 'none',
    backgroundColor: '#e74c3c',
    color: '#fff',
    cursor: 'pointer',
    fontSize: '14px',
    fontWeight: '500',
    borderRadius: '6px',
    transition: 'background-color 0.3s ease',
  },
  main: {
    flex: 1,
    display: 'flex',
    flexDirection: 'column' as const,
    overflow: 'hidden',
  },
  topBar: {
    height: '60px',
    backgroundColor: '#fff',
    borderBottom: '1px solid #e9ecef',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '0 24px',
    boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
  },
  breadcrumb: {
    display: 'flex',
    alignItems: 'center',
    fontSize: '14px',
    color: '#6c757d',
  },
  breadcrumbHome: {
    fontSize: '16px',
  },
  breadcrumbSeparator: {
    margin: '0 8px',
    color: '#dee2e6',
  },
  breadcrumbCurrent: {
    color: '#2c3e50',
    fontWeight: '600',
  },
  topBarActions: {
    display: 'flex',
    alignItems: 'center',
  },
  currentTime: {
    fontSize: '13px',
    color: '#6c757d',
    fontWeight: '500',
  },
  content: {
    flex: 1,
    overflow: 'auto',
    backgroundColor: '#f8f9fa',
  },
};

export default Layout;