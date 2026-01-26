import React, { useState, useEffect } from 'react';
import { useAuthStore } from './store/authStore';
import LoginPage from './pages/LoginPage';
import Dashboard from './pages/Dashboard';
import TasksPage from './pages/TasksPage';
import InventoryPage from './pages/InventoryPage';
import ShoppingPage from './pages/ShoppingPage';
import BudgetPage from './pages/BudgetPage';
import MessagesPage from './pages/MessagesPage';
import ProfilePage from './pages/ProfilePage';
import Layout from './components/Layout';

const App: React.FC = () => {
  const { isAuthenticated, checkAuth } = useAuthStore();
  const [currentPage, setCurrentPage] = useState('dashboard');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    // Check authentication on app load
    checkAuth();
    setIsLoading(false);
    
    // Handle URL-based navigation
    const path = window.location.pathname;
    if (path === '/tasks') setCurrentPage('tasks');
    else if (path === '/inventory') setCurrentPage('inventory');
    else if (path === '/shopping') setCurrentPage('shopping');
    else if (path === '/budget') setCurrentPage('budget');
    else if (path === '/messages') setCurrentPage('messages');
    else if (path === '/profile') setCurrentPage('profile');
    else setCurrentPage('dashboard');
  }, [checkAuth]);

  const handleNavigate = (page: string) => {
    setCurrentPage(page);
    // Update URL without page reload
    window.history.pushState({}, '', `/${page === 'dashboard' ? '' : page}`);
  };

  if (isLoading) {
    return (
      <div style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        height: '100vh',
        fontSize: '18px',
        color: '#666'
      }}>
        🔄 Yükleniyor...
      </div>
    );
  }

  if (!isAuthenticated) {
    return <LoginPage />;
  }

  const renderPage = () => {
    switch (currentPage) {
      case 'dashboard':
        return <Dashboard />;
      case 'tasks':
        return <TasksPage />;
      case 'inventory':
        return <InventoryPage />;
      case 'shopping':
        return <ShoppingPage />;
      case 'budget':
        return <BudgetPage />;
      case 'messages':
        return <MessagesPage />;
      case 'profile':
        return <ProfilePage />;
      default:
        return <Dashboard />;
    }
  };

  return (
    <Layout currentPage={currentPage} onNavigate={handleNavigate}>
      {renderPage()}
    </Layout>
  );
};

export default App;