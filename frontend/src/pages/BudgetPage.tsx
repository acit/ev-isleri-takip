import React, { useState, useEffect, useCallback } from 'react';
import { budgetAPI } from '../utils/api';

interface Budget {
  id: number;
  category: string;
  monthly_limit: number;
  spent_amount: number;
  month_year: string;
}

interface Expense {
  id: number;
  amount: number;
  category: string;
  description: string;
  expense_date: string;
  recorded_by: number;
}

const BudgetPage: React.FC = () => {
  const [budgets, setBudgets] = useState<Budget[]>([]);
  const [expenses, setExpenses] = useState<Expense[]>([]);
  const [loading, setLoading] = useState(true);
  const [currentMonth, setCurrentMonth] = useState(new Date().toISOString().slice(0, 7));
  const [showBudgetForm, setShowBudgetForm] = useState(false);
  const [showExpenseForm, setShowExpenseForm] = useState(false);
  const [newBudget, setNewBudget] = useState({
    category: 'food',
    monthlyLimit: 0,
    monthYear: currentMonth,
  });
  const [newExpense, setNewExpense] = useState({
    amount: 0,
    category: 'food',
    description: '',
    expenseDate: new Date().toISOString().slice(0, 10),
  });

  const categories = [
    { id: 'food', name: 'Gıda', icon: '🍎' },
    { id: 'transport', name: 'Ulaşım', icon: '🚗' },
    { id: 'utilities', name: 'Faturalar', icon: '💡' },
    { id: 'entertainment', name: 'Eğlence', icon: '🎬' },
    { id: 'healthcare', name: 'Sağlık', icon: '🏥' },
    { id: 'shopping', name: 'Alışveriş', icon: '🛒' },
    { id: 'education', name: 'Eğitim', icon: '📚' },
    { id: 'other', name: 'Diğer', icon: '📦' },
  ];

  const loadBudgetData = useCallback(async () => {
    try {
      const [budgetRes, expenseRes] = await Promise.all([
        budgetAPI.get({ monthYear: currentMonth }),
        budgetAPI.getExpenses({ monthYear: currentMonth })
      ]);
      setBudgets(budgetRes.data);
      setExpenses(expenseRes.data);
    } catch (error) {
      console.error('Error loading budget data:', error);
    } finally {
      setLoading(false);
    }
  }, [currentMonth]);

  useEffect(() => {
    loadBudgetData();
  }, [loadBudgetData]);

  const handleBudgetSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await budgetAPI.setLimit(newBudget);
      setNewBudget({
        category: 'food',
        monthlyLimit: 0,
        monthYear: currentMonth,
      });
      setShowBudgetForm(false);
      loadBudgetData();
    } catch (error) {
      console.error('Error setting budget:', error);
    }
  };

  const handleExpenseSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await budgetAPI.recordExpense(newExpense);
      setNewExpense({
        amount: 0,
        category: 'food',
        description: '',
        expenseDate: new Date().toISOString().slice(0, 10),
      });
      setShowExpenseForm(false);
      loadBudgetData();
    } catch (error) {
      console.error('Error recording expense:', error);
    }
  };

  const getCategoryInfo = (categoryId: string) => {
    return categories.find(cat => cat.id === categoryId) || { name: categoryId, icon: '📦' };
  };

  const getProgressColor = (percentage: number) => {
    if (percentage >= 100) return '#dc3545';
    if (percentage >= 80) return '#ffc107';
    if (percentage >= 60) return '#fd7e14';
    return '#28a745';
  };

  const getTotalBudget = () => {
    return budgets.reduce((sum, budget) => sum + budget.monthly_limit, 0);
  };

  const getTotalSpent = () => {
    return budgets.reduce((sum, budget) => sum + budget.spent_amount, 0);
  };

  return (
    <div style={styles.container}>
      <header style={styles.header}>
        <div>
          <h1 style={styles.title}>Bütçe Yönetimi</h1>
          <div style={styles.monthSelector}>
            <label style={styles.monthLabel}>Ay:</label>
            <input
              type="month"
              style={styles.monthInput}
              value={currentMonth}
              onChange={(e) => setCurrentMonth(e.target.value)}
            />
          </div>
        </div>
        <div style={styles.headerActions}>
          <button 
            style={styles.budgetButton}
            onClick={() => setShowBudgetForm(!showBudgetForm)}
          >
            {showBudgetForm ? 'İptal' : '💰 Bütçe Belirle'}
          </button>
          <button 
            style={styles.expenseButton}
            onClick={() => setShowExpenseForm(!showExpenseForm)}
          >
            {showExpenseForm ? 'İptal' : '💸 Harcama Ekle'}
          </button>
        </div>
      </header>

      {/* Summary Cards */}
      <div style={styles.summaryGrid}>
        <div style={styles.summaryCard}>
          <div style={styles.summaryIcon}>💰</div>
          <div style={styles.summaryContent}>
            <div style={styles.summaryLabel}>Toplam Bütçe</div>
            <div style={styles.summaryValue}>₺{getTotalBudget().toLocaleString('tr-TR')}</div>
          </div>
        </div>
        <div style={styles.summaryCard}>
          <div style={styles.summaryIcon}>💸</div>
          <div style={styles.summaryContent}>
            <div style={styles.summaryLabel}>Toplam Harcama</div>
            <div style={styles.summaryValue}>₺{getTotalSpent().toLocaleString('tr-TR')}</div>
          </div>
        </div>
        <div style={styles.summaryCard}>
          <div style={styles.summaryIcon}>💳</div>
          <div style={styles.summaryContent}>
            <div style={styles.summaryLabel}>Kalan Bütçe</div>
            <div style={{
              ...styles.summaryValue,
              color: getTotalBudget() - getTotalSpent() >= 0 ? '#28a745' : '#dc3545'
            }}>
              ₺{(getTotalBudget() - getTotalSpent()).toLocaleString('tr-TR')}
            </div>
          </div>
        </div>
      </div>

      {/* Budget Form */}
      {showBudgetForm && (
        <div style={styles.formCard}>
          <h2 style={styles.formTitle}>Bütçe Limiti Belirle</h2>
          <form onSubmit={handleBudgetSubmit} style={styles.form}>
            <div style={styles.formRow}>
              <div style={styles.formGroup}>
                <label style={styles.label}>Kategori</label>
                <select
                  style={styles.select}
                  value={newBudget.category}
                  onChange={(e) => setNewBudget({...newBudget, category: e.target.value})}
                >
                  {categories.map(cat => (
                    <option key={cat.id} value={cat.id}>
                      {cat.icon} {cat.name}
                    </option>
                  ))}
                </select>
              </div>
              <div style={styles.formGroup}>
                <label style={styles.label}>Aylık Limit (₺)</label>
                <input
                  type="number"
                  style={styles.input}
                  value={newBudget.monthlyLimit}
                  onChange={(e) => setNewBudget({...newBudget, monthlyLimit: Number(e.target.value)})}
                  min="0"
                  step="0.01"
                  required
                />
              </div>
            </div>
            <button type="submit" style={styles.submitButton}>
              Bütçe Belirle
            </button>
          </form>
        </div>
      )}

      {/* Expense Form */}
      {showExpenseForm && (
        <div style={styles.formCard}>
          <h2 style={styles.formTitle}>Harcama Kaydet</h2>
          <form onSubmit={handleExpenseSubmit} style={styles.form}>
            <div style={styles.formRow}>
              <div style={styles.formGroup}>
                <label style={styles.label}>Kategori</label>
                <select
                  style={styles.select}
                  value={newExpense.category}
                  onChange={(e) => setNewExpense({...newExpense, category: e.target.value})}
                >
                  {categories.map(cat => (
                    <option key={cat.id} value={cat.id}>
                      {cat.icon} {cat.name}
                    </option>
                  ))}
                </select>
              </div>
              <div style={styles.formGroup}>
                <label style={styles.label}>Tutar (₺)</label>
                <input
                  type="number"
                  style={styles.input}
                  value={newExpense.amount}
                  onChange={(e) => setNewExpense({...newExpense, amount: Number(e.target.value)})}
                  min="0"
                  step="0.01"
                  required
                />
              </div>
              <div style={styles.formGroup}>
                <label style={styles.label}>Tarih</label>
                <input
                  type="date"
                  style={styles.input}
                  value={newExpense.expenseDate}
                  onChange={(e) => setNewExpense({...newExpense, expenseDate: e.target.value})}
                  required
                />
              </div>
            </div>
            <div style={styles.formGroup}>
              <label style={styles.label}>Açıklama</label>
              <input
                type="text"
                style={styles.input}
                value={newExpense.description}
                onChange={(e) => setNewExpense({...newExpense, description: e.target.value})}
                placeholder="Harcama açıklaması..."
              />
            </div>
            <button type="submit" style={styles.submitButton}>
              Harcama Kaydet
            </button>
          </form>
        </div>
      )}

      {/* Budget Categories */}
      <div style={styles.categoriesGrid}>
        {loading ? (
          <div style={styles.loading}>Yükleniyor...</div>
        ) : budgets.length === 0 ? (
          <div style={styles.emptyState}>
            <p>Henüz bütçe belirlenmemiş</p>
            <p>Kategoriler için aylık limitler belirleyin</p>
          </div>
        ) : (
          budgets.map(budget => {
            const percentage = (budget.spent_amount / budget.monthly_limit) * 100;
            const categoryInfo = getCategoryInfo(budget.category);
            
            return (
              <div key={budget.id} style={styles.categoryCard}>
                <div style={styles.categoryHeader}>
                  <div style={styles.categoryInfo}>
                    <span style={styles.categoryIcon}>{categoryInfo.icon}</span>
                    <span style={styles.categoryName}>{categoryInfo.name}</span>
                  </div>
                  <div style={styles.categoryAmount}>
                    ₺{budget.spent_amount.toLocaleString('tr-TR')} / ₺{budget.monthly_limit.toLocaleString('tr-TR')}
                  </div>
                </div>
                
                <div style={styles.progressContainer}>
                  <div style={styles.progressBar}>
                    <div 
                      style={{
                        ...styles.progressFill,
                        width: `${Math.min(percentage, 100)}%`,
                        backgroundColor: getProgressColor(percentage)
                      }}
                    />
                  </div>
                  <span style={styles.progressText}>{percentage.toFixed(1)}%</span>
                </div>

                <div style={styles.categoryFooter}>
                  <span style={styles.remainingAmount}>
                    Kalan: ₺{(budget.monthly_limit - budget.spent_amount).toLocaleString('tr-TR')}
                  </span>
                  {percentage >= 80 && (
                    <span style={styles.warningText}>
                      {percentage >= 100 ? '⚠️ Limit aşıldı!' : '⚠️ Limite yaklaşıldı'}
                    </span>
                  )}
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Recent Expenses */}
      <div style={styles.expensesSection}>
        <h2 style={styles.sectionTitle}>Son Harcamalar</h2>
        {expenses.length === 0 ? (
          <div style={styles.emptyExpenses}>
            <p>Bu ay henüz harcama kaydı yok</p>
          </div>
        ) : (
          <div style={styles.expensesList}>
            {expenses.slice(0, 10).map(expense => {
              const categoryInfo = getCategoryInfo(expense.category);
              return (
                <div key={expense.id} style={styles.expenseItem}>
                  <div style={styles.expenseIcon}>{categoryInfo.icon}</div>
                  <div style={styles.expenseDetails}>
                    <div style={styles.expenseDescription}>
                      {expense.description || categoryInfo.name}
                    </div>
                    <div style={styles.expenseCategory}>{categoryInfo.name}</div>
                  </div>
                  <div style={styles.expenseRight}>
                    <div style={styles.expenseAmount}>₺{expense.amount.toLocaleString('tr-TR')}</div>
                    <div style={styles.expenseDate}>
                      {new Date(expense.expense_date).toLocaleDateString('tr-TR')}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
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
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: '24px',
  },
  title: {
    fontSize: '28px',
    fontWeight: '700',
    color: '#2c3e50',
    margin: '0 0 8px 0',
  },
  monthSelector: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
  },
  monthLabel: {
    fontSize: '14px',
    color: '#6c757d',
    fontWeight: '500',
  },
  monthInput: {
    padding: '6px 8px',
    border: '1px solid #ced4da',
    borderRadius: '4px',
    fontSize: '14px',
  },
  headerActions: {
    display: 'flex',
    gap: '12px',
  },
  budgetButton: {
    backgroundColor: '#28a745',
    color: '#fff',
    border: 'none',
    borderRadius: '8px',
    padding: '12px 16px',
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
  },
  expenseButton: {
    backgroundColor: '#dc3545',
    color: '#fff',
    border: 'none',
    borderRadius: '8px',
    padding: '12px 16px',
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
  },
  summaryGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
    gap: '16px',
    marginBottom: '24px',
  },
  summaryCard: {
    backgroundColor: '#fff',
    borderRadius: '12px',
    padding: '20px',
    boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
    border: '1px solid #e9ecef',
    display: 'flex',
    alignItems: 'center',
    gap: '16px',
  },
  summaryIcon: {
    fontSize: '32px',
    width: '60px',
    height: '60px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#f8f9fa',
    borderRadius: '12px',
  },
  summaryContent: {
    flex: 1,
  },
  summaryLabel: {
    fontSize: '14px',
    color: '#6c757d',
    marginBottom: '4px',
  },
  summaryValue: {
    fontSize: '24px',
    fontWeight: '700',
    color: '#2c3e50',
  },
  formCard: {
    backgroundColor: '#fff',
    borderRadius: '12px',
    padding: '24px',
    marginBottom: '24px',
    boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
    border: '1px solid #e9ecef',
  },
  formTitle: {
    fontSize: '20px',
    fontWeight: '600',
    color: '#2c3e50',
    marginBottom: '20px',
  },
  form: {
    display: 'flex',
    flexDirection: 'column' as const,
    gap: '16px',
  },
  formRow: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
    gap: '16px',
  },
  formGroup: {
    display: 'flex',
    flexDirection: 'column' as const,
    gap: '4px',
  },
  label: {
    fontSize: '14px',
    fontWeight: '600',
    color: '#495057',
  },
  input: {
    padding: '10px 12px',
    border: '1px solid #ced4da',
    borderRadius: '6px',
    fontSize: '14px',
  },
  select: {
    padding: '10px 12px',
    border: '1px solid #ced4da',
    borderRadius: '6px',
    fontSize: '14px',
    backgroundColor: '#fff',
  },
  submitButton: {
    backgroundColor: '#007bff',
    color: '#fff',
    border: 'none',
    borderRadius: '8px',
    padding: '12px 24px',
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
    alignSelf: 'flex-start',
  },
  categoriesGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
    gap: '20px',
    marginBottom: '32px',
  },
  loading: {
    gridColumn: '1 / -1',
    textAlign: 'center' as const,
    padding: '40px',
    color: '#6c757d',
  },
  emptyState: {
    gridColumn: '1 / -1',
    textAlign: 'center' as const,
    padding: '60px 20px',
    color: '#95a5a6',
  },
  categoryCard: {
    backgroundColor: '#fff',
    borderRadius: '12px',
    padding: '20px',
    boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
    border: '1px solid #e9ecef',
  },
  categoryHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '16px',
  },
  categoryInfo: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
  },
  categoryIcon: {
    fontSize: '20px',
  },
  categoryName: {
    fontSize: '16px',
    fontWeight: '600',
    color: '#2c3e50',
  },
  categoryAmount: {
    fontSize: '14px',
    color: '#6c757d',
    fontWeight: '500',
  },
  progressContainer: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    marginBottom: '12px',
  },
  progressBar: {
    flex: 1,
    height: '8px',
    backgroundColor: '#e9ecef',
    borderRadius: '4px',
    overflow: 'hidden',
  },
  progressFill: {
    height: '100%',
    transition: 'width 0.3s ease',
  },
  progressText: {
    fontSize: '12px',
    color: '#6c757d',
    fontWeight: '600',
    minWidth: '40px',
  },
  categoryFooter: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  remainingAmount: {
    fontSize: '12px',
    color: '#28a745',
    fontWeight: '500',
  },
  warningText: {
    fontSize: '12px',
    color: '#dc3545',
    fontWeight: '600',
  },
  expensesSection: {
    backgroundColor: '#fff',
    borderRadius: '12px',
    padding: '24px',
    boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
    border: '1px solid #e9ecef',
  },
  sectionTitle: {
    fontSize: '20px',
    fontWeight: '600',
    color: '#2c3e50',
    marginBottom: '20px',
  },
  emptyExpenses: {
    textAlign: 'center' as const,
    padding: '40px 20px',
    color: '#95a5a6',
  },
  expensesList: {
    display: 'flex',
    flexDirection: 'column' as const,
    gap: '12px',
  },
  expenseItem: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    padding: '12px',
    backgroundColor: '#f8f9fa',
    borderRadius: '8px',
  },
  expenseIcon: {
    fontSize: '20px',
    width: '40px',
    height: '40px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#fff',
    borderRadius: '8px',
  },
  expenseDetails: {
    flex: 1,
  },
  expenseDescription: {
    fontSize: '14px',
    fontWeight: '500',
    color: '#2c3e50',
    marginBottom: '2px',
  },
  expenseCategory: {
    fontSize: '12px',
    color: '#6c757d',
  },
  expenseRight: {
    textAlign: 'right' as const,
  },
  expenseAmount: {
    fontSize: '16px',
    fontWeight: '600',
    color: '#dc3545',
    marginBottom: '2px',
  },
  expenseDate: {
    fontSize: '12px',
    color: '#6c757d',
  },
};

export default BudgetPage;