import React, { useState, useEffect } from 'react';
import { inventoryAPI } from '../utils/api';

interface InventoryItem {
  id: number;
  item_name: string;
  quantity: number;
  unit: string;
  min_threshold: number;
  category: string;
  location: string;
  notes: string;
}

const InventoryPage: React.FC = () => {
  const [items, setItems] = useState<InventoryItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [newItem, setNewItem] = useState({
    item_name: '',
    quantity: 0,
    unit: 'adet',
    min_threshold: 0,
    category: 'food',
    location: '',
    notes: '',
  });

  useEffect(() => {
    loadInventory();
  }, []);

  const loadInventory = async () => {
    try {
      const response = await inventoryAPI.getAll();
      setItems(response.data);
    } catch (error) {
      console.error('Error loading inventory:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await inventoryAPI.add(newItem);
      setNewItem({
        item_name: '',
        quantity: 0,
        unit: 'adet',
        min_threshold: 0,
        category: 'food',
        location: '',
        notes: '',
      });
      setShowForm(false);
      loadInventory();
    } catch (error) {
      console.error('Error adding item:', error);
    }
  };

  const deleteItem = async (itemId: number) => {
    if (window.confirm('Bu ürünü silmek istediğinizden emin misiniz?')) {
      try {
        await inventoryAPI.delete(itemId);
        loadInventory();
      } catch (error) {
        console.error('Error deleting item:', error);
      }
    }
  };

  const isLowStock = (item: InventoryItem) => {
    return item.quantity <= item.min_threshold;
  };

  const getCategoryIcon = (category: string) => {
    switch (category) {
      case 'food': return '🍎';
      case 'cleaning': return '🧽';
      case 'personal': return '🧴';
      case 'household': return '🏠';
      default: return '📦';
    }
  };

  return (
    <div style={styles.container}>
      <header style={styles.header}>
        <h1 style={styles.title}>Envanter</h1>
        <button 
          style={styles.addButton}
          onClick={() => setShowForm(!showForm)}
        >
          {showForm ? 'İptal' : '+ Yeni Ürün'}
        </button>
      </header>

      {/* New Item Form */}
      {showForm && (
        <div style={styles.formCard}>
          <h2 style={styles.formTitle}>Yeni Ürün Ekle</h2>
          <form onSubmit={handleSubmit} style={styles.form}>
            <div style={styles.formRow}>
              <div style={styles.formGroup}>
                <label style={styles.label}>Ürün Adı</label>
                <input
                  type="text"
                  style={styles.input}
                  value={newItem.item_name}
                  onChange={(e) => setNewItem({...newItem, item_name: e.target.value})}
                  required
                />
              </div>
              <div style={styles.formGroup}>
                <label style={styles.label}>Kategori</label>
                <select
                  style={styles.select}
                  value={newItem.category}
                  onChange={(e) => setNewItem({...newItem, category: e.target.value})}
                >
                  <option value="food">Gıda</option>
                  <option value="cleaning">Temizlik</option>
                  <option value="personal">Kişisel Bakım</option>
                  <option value="household">Ev Eşyası</option>
                </select>
              </div>
            </div>

            <div style={styles.formRow}>
              <div style={styles.formGroup}>
                <label style={styles.label}>Miktar</label>
                <input
                  type="number"
                  style={styles.input}
                  value={newItem.quantity}
                  onChange={(e) => setNewItem({...newItem, quantity: Number(e.target.value)})}
                  min="0"
                  step="0.1"
                  required
                />
              </div>
              <div style={styles.formGroup}>
                <label style={styles.label}>Birim</label>
                <select
                  style={styles.select}
                  value={newItem.unit}
                  onChange={(e) => setNewItem({...newItem, unit: e.target.value})}
                >
                  <option value="adet">Adet</option>
                  <option value="kg">Kilogram</option>
                  <option value="lt">Litre</option>
                  <option value="paket">Paket</option>
                  <option value="kutu">Kutu</option>
                </select>
              </div>
              <div style={styles.formGroup}>
                <label style={styles.label}>Minimum Eşik</label>
                <input
                  type="number"
                  style={styles.input}
                  value={newItem.min_threshold}
                  onChange={(e) => setNewItem({...newItem, min_threshold: Number(e.target.value)})}
                  min="0"
                  step="0.1"
                />
              </div>
            </div>

            <div style={styles.formRow}>
              <div style={styles.formGroup}>
                <label style={styles.label}>Konum</label>
                <input
                  type="text"
                  style={styles.input}
                  value={newItem.location}
                  onChange={(e) => setNewItem({...newItem, location: e.target.value})}
                  placeholder="Örn: Mutfak dolabı, Banyo"
                />
              </div>
            </div>

            <div style={styles.formGroup}>
              <label style={styles.label}>Notlar</label>
              <textarea
                style={styles.textarea}
                value={newItem.notes}
                onChange={(e) => setNewItem({...newItem, notes: e.target.value})}
                rows={2}
                placeholder="Ek bilgiler..."
              />
            </div>

            <button type="submit" style={styles.submitButton}>
              Ürün Ekle
            </button>
          </form>
        </div>
      )}

      {/* Inventory Grid */}
      <div style={styles.inventoryGrid}>
        {loading ? (
          <div style={styles.loading}>Yükleniyor...</div>
        ) : items.length === 0 ? (
          <div style={styles.emptyState}>
            <p>Henüz ürün yok</p>
          </div>
        ) : (
          items.map(item => (
            <div 
              key={item.id} 
              style={{
                ...styles.itemCard,
                ...(isLowStock(item) ? styles.lowStockCard : {})
              }}
            >
              <div style={styles.itemHeader}>
                <div style={styles.itemIcon}>
                  {getCategoryIcon(item.category)}
                </div>
                <button
                  style={styles.deleteButton}
                  onClick={() => deleteItem(item.id)}
                >
                  ×
                </button>
              </div>

              <h3 style={styles.itemName}>{item.item_name}</h3>
              
              <div style={styles.quantitySection}>
                <div style={styles.quantity}>
                  <span style={styles.quantityNumber}>{item.quantity}</span>
                  <span style={styles.quantityUnit}>{item.unit}</span>
                </div>
                {isLowStock(item) && (
                  <div style={styles.lowStockBadge}>
                    ⚠️ Az Stok
                  </div>
                )}
              </div>

              <div style={styles.itemDetails}>
                <div style={styles.detailRow}>
                  <span style={styles.detailLabel}>Kategori:</span>
                  <span style={styles.detailValue}>
                    {item.category === 'food' ? 'Gıda' :
                     item.category === 'cleaning' ? 'Temizlik' :
                     item.category === 'personal' ? 'Kişisel Bakım' : 'Ev Eşyası'}
                  </span>
                </div>
                
                {item.location && (
                  <div style={styles.detailRow}>
                    <span style={styles.detailLabel}>Konum:</span>
                    <span style={styles.detailValue}>{item.location}</span>
                  </div>
                )}
                
                <div style={styles.detailRow}>
                  <span style={styles.detailLabel}>Min. Eşik:</span>
                  <span style={styles.detailValue}>{item.min_threshold} {item.unit}</span>
                </div>
              </div>

              {item.notes && (
                <div style={styles.itemNotes}>
                  <p style={styles.notesText}>{item.notes}</p>
                </div>
              )}
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
    maxWidth: '1200px',
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
  inventoryGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
    gap: '20px',
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
  itemCard: {
    backgroundColor: '#fff',
    borderRadius: '12px',
    padding: '20px',
    boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
    border: '1px solid #e9ecef',
    transition: 'transform 0.2s',
  },
  lowStockCard: {
    borderColor: '#ffc107',
    backgroundColor: '#fff8e1',
  },
  itemHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '12px',
  },
  itemIcon: {
    fontSize: '24px',
    width: '40px',
    height: '40px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#f8f9fa',
    borderRadius: '8px',
  },
  deleteButton: {
    backgroundColor: 'transparent',
    border: 'none',
    color: '#dc3545',
    fontSize: '20px',
    cursor: 'pointer',
    width: '24px',
    height: '24px',
    borderRadius: '50%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  itemName: {
    fontSize: '18px',
    fontWeight: '600',
    color: '#2c3e50',
    margin: '0 0 16px 0',
  },
  quantitySection: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '16px',
  },
  quantity: {
    display: 'flex',
    alignItems: 'baseline',
    gap: '4px',
  },
  quantityNumber: {
    fontSize: '24px',
    fontWeight: '700',
    color: '#007bff',
  },
  quantityUnit: {
    fontSize: '14px',
    color: '#6c757d',
  },
  lowStockBadge: {
    backgroundColor: '#ffc107',
    color: '#856404',
    padding: '4px 8px',
    borderRadius: '12px',
    fontSize: '12px',
    fontWeight: '600',
  },
  itemDetails: {
    display: 'flex',
    flexDirection: 'column' as const,
    gap: '8px',
    marginBottom: '12px',
  },
  detailRow: {
    display: 'flex',
    justifyContent: 'space-between',
    fontSize: '14px',
  },
  detailLabel: {
    color: '#6c757d',
    fontWeight: '500',
  },
  detailValue: {
    color: '#495057',
  },
  itemNotes: {
    borderTop: '1px solid #e9ecef',
    paddingTop: '12px',
  },
  notesText: {
    fontSize: '12px',
    color: '#6c757d',
    margin: 0,
    fontStyle: 'italic',
  },
};

export default InventoryPage;