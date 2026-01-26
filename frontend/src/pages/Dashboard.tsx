import React, { useState, useEffect } from 'react';
import { taskAPI, budgetAPI, inventoryAPI, shoppingAPI, authAPI } from '../utils/api';
import { useAuthStore } from '../store/authStore';

interface Task {
  id: number;
  title: string;
  description: string;
  priority: string;
  status: string;
  due_date: string;
  assigned_to: number;
}

interface Budget {
  category: string;
  monthly_limit: number;
  spent_amount: number;
}

interface InventoryItem {
  id: number;
  item_name: string;
  quantity: number;
  min_threshold: number;
}

interface ShoppingList {
  id: number;
  title: string;
  status: string;
  created_at: string;
}

const Dashboard: React.FC = () => {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [budgets, setBudgets] = useState<Budget[]>([]);
  const [lowStockItems, setLowStockItems] = useState<InventoryItem[]>([]);
  const [recentLists, setRecentLists] = useState<ShoppingList[]>([]);
  const [emailStatus, setEmailStatus] = useState({ status: 'checking', message: 'Kontrol ediliyor...', mode: 'development' });
  const [loading, setLoading] = useState(true);
  const { userId } = useAuthStore();

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      const [tasksRes, budgetRes, inventoryRes, shoppingRes, emailRes] = await Promise.all([
        taskAPI.getAll({ limit: 5 }),
        budgetAPI.get(),
        inventoryAPI.getLowStock(),
        shoppingAPI.getAll(),
        authAPI.getEmailStatus()
      ]);
      
      setTasks(tasksRes.data);
      setBudgets(budgetRes.data);
      setLowStockItems(inventoryRes.data);
      setRecentLists(shoppingRes.data.slice(0, 3));
      setEmailStatus(emailRes.data);
    } catch (error) {
      console.error('Error loading dashboard:', error);
    } finally {
      setLoading(false);
    }
  };

  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'high': return '#ff4757';
      case 'medium': return '#ffa502';
      case 'low': return '#2ed573';
      default: return '#747d8c';
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'completed': return '#2ed573';
      case 'in_progress': return '#3742fa';
      case 'pending': return '#ffa502';
      default: return '#747d8c';
    }
  };

  if (loading) {
    return (
      <div style={styles.loadingContainer}>
        <div style={styles.spinner}></div>
        <p>Dashboard yükleniyor...</p>
      </div>
    );
  }

  return (
    <div style={styles.container}>
      <style>
        {`
          @keyframes fadeIn {
            from { opacity: 0; transform: translateY(20px); }
            to { opacity: 1; transform: translateY(0); }
          }
          @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
          }
          .dashboard-widget {
            animation: fadeIn 0.6s ease-out;
          }
          .dashboard-widget:hover {
            transform: translateY(-4px);
            box-shadow: 0 8px 25px rgba(0,0,0,0.15);
          }
        `}
      </style>

      {/* Header */}
      <header style={styles.header}>
        <div>
          <h1 style={styles.title}>🏠 Aile Dashboard</h1>
          <p style={styles.subtitle}>Günlük aktivitelerinizi takip edin</p>
        </div>
      </header>

      {/* Email Status Widget */}
      <div style={styles.emailStatusWidget} className="dashboard-widget">
        <div style={styles.emailStatusHeader}>
          <span style={styles.emailStatusIcon}>
            {emailStatus.status === 'configured' ? '✅' : '⚠️'}
          </span>
          <div>
            <h3 style={styles.emailStatusTitle}>E-posta Sistemi</h3>
            <p style={styles.emailStatusMessage}>{emailStatus.message}</p>
          </div>
        </div>
        <div style={styles.emailStatusMode}>
          <span style={{
            ...styles.modeBadge,
            backgroundColor: emailStatus.mode === 'production' ? '#28a745' : '#ffc107',
            color: emailStatus.mode === 'production' ? 'white' : '#212529'
          }}>
            {emailStatus.mode === 'production' ? 'Üretim Modu' : 'Geliştirme Modu'}
          </span>
        </div>
      </div>

      {/* Stats Grid */}
      <div style={styles.statsGrid}>
        {/* Tasks Widget */}
        <div style={styles.widget} className="dashboard-widget">
          <div style={styles.widgetHeader}>
            <h3 style={styles.widgetTitle}>📋 Görevler</h3>
            <span style={styles.widgetCount}>{tasks.length}</span>
          </div>
          <div style={styles.widgetContent}>
            {tasks.length === 0 ? (
              <p style={styles.emptyText}>Henüz görev yok</p>
            ) : (
              tasks.slice(0, 3).map(task => (
                <div key={task.id} style={styles.taskItem}>
                  <div style={styles.taskInfo}>
                    <span style={styles.taskTitle}>{task.title}</span>
                    <span style={styles.taskDate}>
                      {task.due_date ? new Date(task.due_date).toLocaleDateString('tr-TR') : 'Tarih yok'}
                    </span>
                  </div>
                  <div style={styles.taskBadges}>
                    <span style={{
                      ...styles.priorityBadge,
                      backgroundColor: getPriorityColor(task.priority)
                    }}>
                      {task.priority}
                    </span>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Budget Widget */}
        <div style={styles.widget} className="dashboard-widget">
          <div style={styles.widgetHeader}>
            <h3 style={styles.widgetTitle}>💰 Bütçe</h3>
            <span style={styles.widgetCount}>{budgets.length}</span>
          </div>
          <div style={styles.widgetContent}>
            {budgets.length === 0 ? (
              <p style={styles.emptyText}>Bütçe tanımlanmamış</p>
            ) : (
              budgets.slice(0, 3).map((budget, index) => (
                <div key={index} style={styles.budgetItem}>
                  <div style={styles.budgetInfo}>
                    <span style={styles.budgetCategory}>{budget.category}</span>
                    <span style={styles.budgetAmount}>
                      ₺{budget.spent_amount.toLocaleString('tr-TR')} / ₺{budget.monthly_limit.toLocaleString('tr-TR')}
                    </span>
                  </div>
                  <div style={styles.budgetProgress}>
                    <div style={{
                      ...styles.budgetProgressBar,
                      width: `${Math.min((budget.spent_amount / budget.monthly_limit) * 100, 100)}%`,
                      backgroundColor: budget.spent_amount > budget.monthly_limit ? '#ff4757' : '#2ed573'
                    }}></div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Low Stock Widget */}
        <div style={styles.widget} className="dashboard-widget">
          <div style={styles.widgetHeader}>
            <h3 style={styles.widgetTitle}>📦 Düşük Stok</h3>
            <span style={styles.widgetCount}>{lowStockItems.length}</span>
          </div>
          <div style={styles.widgetContent}>
            {lowStockItems.length === 0 ? (
              <p style={styles.emptyText}>Tüm stoklar yeterli</p>
            ) : (
              lowStockItems.slice(0, 3).map(item => (
                <div key={item.id} style={styles.stockItem}>
                  <span style={styles.stockName}>{item.item_name}</span>
                  <span style={styles.stockQuantity}>
                    {item.quantity} / {item.min_threshold}
                  </span>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Shopping Lists Widget */}
        <div style={styles.widget} className="dashboard-widget">
          <div style={styles.widgetHeader}>
            <h3 style={styles.widgetTitle}>🛒 Alışveriş</h3>
            <span style={styles.widgetCount}>{recentLists.length}</span>
          </div>
          <div style={styles.widgetContent}>
            {recentLists.length === 0 ? (
              <p style={styles.emptyText}>Alışveriş listesi yok</p>
            ) : (
              recentLists.map(list => (
                <div key={list.id} style={styles.shoppingItem}>
                  <span style={styles.shoppingTitle}>{list.title}</span>
                  <span style={{
                    ...styles.shoppingStatus,
                    backgroundColor: getStatusColor(list.status)
                  }}>
                    {list.status}
                  </span>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

const styles = {
  container: {
    padding: '24px',
    maxWidth: '1400px',
    margin: '0 auto',
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
  },
  loadingContainer: {
    display: 'flex',
    flexDirection: 'column' as const,
    alignItems: 'center',
    justifyContent: 'center',
    height: '400px',
    color: '#6c757d',
  },
  spinner: {
    width: '40px',
    height: '40px',
    border: '4px solid #f3f3f3',
    borderTop: '4px solid #007bff',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite',
    marginBottom: '16px',
  },
  header: {
    marginBottom: '32px',
    textAlign: 'center' as const,
  },
  title: {
    fontSize: '36px',
    fontWeight: '800',
    color: '#2c3e50',
    margin: '0 0 8px 0',
    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    WebkitBackgroundClip: 'text',
    WebkitTextFillColor: 'transparent',
  },
  subtitle: {
    fontSize: '18px',
    color: '#6c757d',
    margin: 0,
    fontWeight: '500',
  },
  emailStatusWidget: {
    backgroundColor: '#fff',
    borderRadius: '16px',
    padding: '24px',
    marginBottom: '32px',
    boxShadow: '0 4px 20px rgba(0,0,0,0.08)',
    border: '1px solid #e9ecef',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    transition: 'all 0.3s ease',
  },
  emailStatusHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: '16px',
  },
  emailStatusIcon: {
    fontSize: '24px',
  },
  emailStatusTitle: {
    fontSize: '18px',
    fontWeight: '600',
    color: '#2c3e50',
    margin: '0 0 4px 0',
  },
  emailStatusMessage: {
    fontSize: '14px',
    color: '#6c757d',
    margin: 0,
  },
  emailStatusMode: {},
  modeBadge: {
    padding: '6px 12px',
    borderRadius: '20px',
    fontSize: '12px',
    fontWeight: '600',
  },
  statsGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
    gap: '24px',
  },
  widget: {
    backgroundColor: '#fff',
    borderRadius: '16px',
    padding: '24px',
    boxShadow: '0 4px 20px rgba(0,0,0,0.08)',
    border: '1px solid #e9ecef',
    transition: 'all 0.3s ease',
  },
  widgetHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '20px',
  },
  widgetTitle: {
    fontSize: '18px',
    fontWeight: '600',
    color: '#2c3e50',
    margin: 0,
  },
  widgetCount: {
    backgroundColor: '#007bff',
    color: '#fff',
    padding: '4px 12px',
    borderRadius: '20px',
    fontSize: '14px',
    fontWeight: '600',
  },
  widgetContent: {
    display: 'flex',
    flexDirection: 'column' as const,
    gap: '12px',
  },
  emptyText: {
    color: '#95a5a6',
    fontStyle: 'italic',
    textAlign: 'center' as const,
    padding: '20px',
  },
  taskItem: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '12px',
    backgroundColor: '#f8f9fa',
    borderRadius: '8px',
  },
  taskInfo: {
    display: 'flex',
    flexDirection: 'column' as const,
    gap: '4px',
  },
  taskTitle: {
    fontSize: '14px',
    fontWeight: '600',
    color: '#2c3e50',
  },
  taskDate: {
    fontSize: '12px',
    color: '#6c757d',
  },
  taskBadges: {},
  priorityBadge: {
    color: '#fff',
    padding: '4px 8px',
    borderRadius: '12px',
    fontSize: '10px',
    fontWeight: '600',
    textTransform: 'uppercase' as const,
  },
  budgetItem: {
    padding: '12px',
    backgroundColor: '#f8f9fa',
    borderRadius: '8px',
  },
  budgetInfo: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '8px',
  },
  budgetCategory: {
    fontSize: '14px',
    fontWeight: '600',
    color: '#2c3e50',
  },
  budgetAmount: {
    fontSize: '12px',
    color: '#6c757d',
  },
  budgetProgress: {
    width: '100%',
    height: '6px',
    backgroundColor: '#e9ecef',
    borderRadius: '3px',
    overflow: 'hidden',
  },
  budgetProgressBar: {
    height: '100%',
    transition: 'width 0.3s ease',
  },
  stockItem: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '12px',
    backgroundColor: '#fff3cd',
    borderRadius: '8px',
    border: '1px solid #ffeaa7',
  },
  stockName: {
    fontSize: '14px',
    fontWeight: '600',
    color: '#856404',
  },
  stockQuantity: {
    fontSize: '12px',
    color: '#856404',
    fontWeight: '600',
  },
  shoppingItem: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '12px',
    backgroundColor: '#f8f9fa',
    borderRadius: '8px',
  },
  shoppingTitle: {
    fontSize: '14px',
    fontWeight: '600',
    color: '#2c3e50',
  },
  shoppingStatus: {
    color: '#fff',
    padding: '4px 8px',
    borderRadius: '12px',
    fontSize: '10px',
    fontWeight: '600',
    textTransform: 'uppercase' as const,
  },
};

export default Dashboard;