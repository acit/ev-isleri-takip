import React, { useState, useEffect, useCallback } from 'react';
import { taskAPI } from '../utils/api';

interface Task {
  id: number;
  title: string;
  description: string;
  priority: string;
  status: string;
  due_date: string;
  assigned_to: number;
  frequency: string;
  category: string;
}

const TasksPage: React.FC = () => {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [filter, setFilter] = useState('all');
  const [newTask, setNewTask] = useState({
    title: '',
    description: '',
    priority: 'medium',
    frequency: 'once',
    category: 'household',
    due_date: '',
  });

  const loadTasks = useCallback(async () => {
    try {
      const params = filter !== 'all' ? { status: filter } : {};
      const response = await taskAPI.getAll(params);
      setTasks(response.data);
    } catch (error) {
      console.error('Error loading tasks:', error);
    } finally {
      setLoading(false);
    }
  }, [filter]);

  useEffect(() => {
    loadTasks();
  }, [filter, loadTasks]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await taskAPI.create(newTask);
      setNewTask({
        title: '',
        description: '',
        priority: 'medium',
        frequency: 'once',
        category: 'household',
        due_date: '',
      });
      setShowForm(false);
      loadTasks();
    } catch (error) {
      console.error('Error creating task:', error);
    }
  };

  const completeTask = async (taskId: number) => {
    try {
      await taskAPI.complete(taskId);
      loadTasks();
    } catch (error) {
      console.error('Error completing task:', error);
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

  const filteredTasks = tasks.filter(task => {
    if (filter === 'all') return true;
    return task.status === filter;
  });

  return (
    <div style={styles.container}>
      <header style={styles.header}>
        <h1 style={styles.title}>Görevler</h1>
        <button 
          style={styles.addButton}
          onClick={() => setShowForm(!showForm)}
        >
          {showForm ? 'İptal' : '+ Yeni Görev'}
        </button>
      </header>

      {/* Filters */}
      <div style={styles.filters}>
        {['all', 'pending', 'in_progress', 'completed'].map(status => (
          <button
            key={status}
            style={{
              ...styles.filterButton,
              ...(filter === status ? styles.filterButtonActive : {})
            }}
            onClick={() => setFilter(status)}
          >
            {status === 'all' ? 'Tümü' : 
             status === 'pending' ? 'Bekleyen' :
             status === 'in_progress' ? 'Devam Eden' : 'Tamamlanan'}
          </button>
        ))}
      </div>

      {/* New Task Form */}
      {showForm && (
        <div style={styles.formCard}>
          <h2 style={styles.formTitle}>Yeni Görev Ekle</h2>
          <form onSubmit={handleSubmit} style={styles.form}>
            <div style={styles.formRow}>
              <div style={styles.formGroup}>
                <label style={styles.label}>Başlık</label>
                <input
                  type="text"
                  style={styles.input}
                  value={newTask.title}
                  onChange={(e) => setNewTask({...newTask, title: e.target.value})}
                  required
                />
              </div>
              <div style={styles.formGroup}>
                <label style={styles.label}>Kategori</label>
                <select
                  style={styles.select}
                  value={newTask.category}
                  onChange={(e) => setNewTask({...newTask, category: e.target.value})}
                >
                  <option value="household">Ev İşleri</option>
                  <option value="external">Dış İşler</option>
                  <option value="shopping">Alışveriş</option>
                  <option value="maintenance">Bakım</option>
                </select>
              </div>
            </div>

            <div style={styles.formGroup}>
              <label style={styles.label}>Açıklama</label>
              <textarea
                style={styles.textarea}
                value={newTask.description}
                onChange={(e) => setNewTask({...newTask, description: e.target.value})}
                rows={3}
              />
            </div>

            <div style={styles.formRow}>
              <div style={styles.formGroup}>
                <label style={styles.label}>Öncelik</label>
                <select
                  style={styles.select}
                  value={newTask.priority}
                  onChange={(e) => setNewTask({...newTask, priority: e.target.value})}
                >
                  <option value="low">Düşük</option>
                  <option value="medium">Orta</option>
                  <option value="high">Yüksek</option>
                </select>
              </div>
              <div style={styles.formGroup}>
                <label style={styles.label}>Sıklık</label>
                <select
                  style={styles.select}
                  value={newTask.frequency}
                  onChange={(e) => setNewTask({...newTask, frequency: e.target.value})}
                >
                  <option value="once">Bir kez</option>
                  <option value="daily">Günlük</option>
                  <option value="weekly">Haftalık</option>
                  <option value="monthly">Aylık</option>
                </select>
              </div>
              <div style={styles.formGroup}>
                <label style={styles.label}>Bitiş Tarihi</label>
                <input
                  type="date"
                  style={styles.input}
                  value={newTask.due_date}
                  onChange={(e) => setNewTask({...newTask, due_date: e.target.value})}
                />
              </div>
            </div>

            <button type="submit" style={styles.submitButton}>
              Görev Ekle
            </button>
          </form>
        </div>
      )}

      {/* Tasks List */}
      <div style={styles.tasksList}>
        {loading ? (
          <div style={styles.loading}>Yükleniyor...</div>
        ) : filteredTasks.length === 0 ? (
          <div style={styles.emptyState}>
            <p>Henüz görev yok</p>
          </div>
        ) : (
          filteredTasks.map(task => (
            <div key={task.id} style={styles.taskCard}>
              <div style={styles.taskHeader}>
                <div>
                  <h3 style={styles.taskTitle}>{task.title}</h3>
                  <span style={styles.taskCategory}>{task.category}</span>
                </div>
                <div style={styles.taskActions}>
                  <span 
                    style={{
                      ...styles.priorityBadge,
                      backgroundColor: getPriorityColor(task.priority)
                    }}
                  >
                    {task.priority}
                  </span>
                  {task.status !== 'completed' && (
                    <button
                      style={styles.completeButton}
                      onClick={() => completeTask(task.id)}
                    >
                      ✓ Tamamla
                    </button>
                  )}
                </div>
              </div>
              
              {task.description && (
                <p style={styles.taskDescription}>{task.description}</p>
              )}
              
              <div style={styles.taskMeta}>
                <span style={styles.taskStatus}>
                  Durum: {task.status === 'pending' ? 'Bekliyor' : 
                          task.status === 'in_progress' ? 'Devam Ediyor' : 'Tamamlandı'}
                </span>
                <span style={styles.taskFrequency}>Sıklık: {task.frequency}</span>
                {task.due_date && (
                  <span style={styles.taskDate}>
                    Bitiş: {new Date(task.due_date).toLocaleDateString('tr-TR')}
                  </span>
                )}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

const styles = {
  container: {
    padding: '24px',
    maxWidth: '1000px',
    margin: '0 auto',
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '24px',
  },
  title: {
    fontSize: '28px',
    fontWeight: '700',
    color: '#2c3e50',
    margin: 0,
  },
  addButton: {
    backgroundColor: '#007bff',
    color: '#fff',
    border: 'none',
    borderRadius: '8px',
    padding: '12px 20px',
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'background-color 0.2s',
  },
  filters: {
    display: 'flex',
    gap: '8px',
    marginBottom: '24px',
  },
  filterButton: {
    padding: '8px 16px',
    border: '1px solid #dee2e6',
    borderRadius: '6px',
    backgroundColor: '#fff',
    color: '#6c757d',
    cursor: 'pointer',
    fontSize: '14px',
    transition: 'all 0.2s',
  },
  filterButtonActive: {
    backgroundColor: '#007bff',
    color: '#fff',
    borderColor: '#007bff',
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
    transition: 'border-color 0.2s',
  },
  select: {
    padding: '10px 12px',
    border: '1px solid #ced4da',
    borderRadius: '6px',
    fontSize: '14px',
    backgroundColor: '#fff',
  },
  textarea: {
    padding: '10px 12px',
    border: '1px solid #ced4da',
    borderRadius: '6px',
    fontSize: '14px',
    resize: 'vertical' as const,
    fontFamily: 'inherit',
  },
  submitButton: {
    backgroundColor: '#28a745',
    color: '#fff',
    border: 'none',
    borderRadius: '8px',
    padding: '12px 24px',
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
    alignSelf: 'flex-start',
  },
  tasksList: {
    display: 'flex',
    flexDirection: 'column' as const,
    gap: '16px',
  },
  loading: {
    textAlign: 'center' as const,
    padding: '40px',
    color: '#6c757d',
  },
  emptyState: {
    textAlign: 'center' as const,
    padding: '60px 20px',
    color: '#95a5a6',
  },
  taskCard: {
    backgroundColor: '#fff',
    borderRadius: '12px',
    padding: '20px',
    boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
    border: '1px solid #e9ecef',
  },
  taskHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: '12px',
  },
  taskTitle: {
    fontSize: '18px',
    fontWeight: '600',
    color: '#2c3e50',
    margin: '0 0 4px 0',
  },
  taskCategory: {
    fontSize: '12px',
    color: '#6c757d',
    textTransform: 'uppercase' as const,
    letterSpacing: '0.5px',
  },
  taskActions: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
  },
  priorityBadge: {
    padding: '4px 8px',
    borderRadius: '12px',
    fontSize: '12px',
    fontWeight: '500',
    color: '#fff',
    textTransform: 'uppercase' as const,
  },
  completeButton: {
    backgroundColor: '#28a745',
    color: '#fff',
    border: 'none',
    borderRadius: '6px',
    padding: '6px 12px',
    fontSize: '12px',
    cursor: 'pointer',
  },
  taskDescription: {
    fontSize: '14px',
    color: '#6c757d',
    lineHeight: '1.5',
    margin: '0 0 16px 0',
  },
  taskMeta: {
    display: 'flex',
    gap: '16px',
    fontSize: '12px',
    color: '#95a5a6',
  },
  taskStatus: {},
  taskFrequency: {},
  taskDate: {},
};

export default TasksPage;