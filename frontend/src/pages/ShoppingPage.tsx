import React, { useState, useEffect } from 'react';
import { shoppingAPI } from '../utils/api';

interface ShoppingList {
  id: number;
  title: string;
  status: string;
  auto_generated: boolean;
  sent_via_email: boolean;
  created_at: string;
}

interface ShoppingItem {
  id: number;
  item_name: string;
  quantity: number;
  unit: string;
  estimated_cost: number;
  checked: boolean;
}

interface ShoppingListDetail {
  list: ShoppingList;
  items: ShoppingItem[];
}

const ShoppingPage: React.FC = () => {
  const [lists, setLists] = useState<ShoppingList[]>([]);
  const [selectedList, setSelectedList] = useState<ShoppingList | null>(null);
  const [listDetail, setListDetail] = useState<ShoppingListDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [activeTab, setActiveTab] = useState<'all' | 'draft' | 'sent' | 'completed'>('all');
  const [newList, setNewList] = useState({
    title: '',
    items: [{ item_name: '', quantity: 1, unit: 'adet', estimated_cost: 0 }]
  });

  useEffect(() => {
    loadShoppingLists();
  }, []);

  const loadShoppingLists = async () => {
    try {
      const response = await shoppingAPI.getAll();
      setLists(response.data);
    } catch (error) {
      console.error('Error loading shopping lists:', error);
    } finally {
      setLoading(false);
    }
  };

  const generateFromInventory = async () => {
    try {
      setLoading(true);
      await shoppingAPI.generateFromInventory();
      await loadShoppingLists();
      showNotification('✅ Düşük stoklu ürünlerden alışveriş listesi oluşturuldu!', 'success');
    } catch (error) {
      console.error('Error generating list:', error);
      showNotification('❌ Liste oluşturulurken hata oluştu', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await shoppingAPI.create(newList);
      setNewList({
        title: '',
        items: [{ item_name: '', quantity: 1, unit: 'adet', estimated_cost: 0 }]
      });
      setShowForm(false);
      await loadShoppingLists();
      showNotification('✅ Alışveriş listesi oluşturuldu!', 'success');
    } catch (error) {
      console.error('Error creating shopping list:', error);
      showNotification('❌ Liste oluşturulurken hata oluştu', 'error');
    }
  };

  const loadShoppingListDetail = async (list: ShoppingList) => {
    setDetailLoading(true);
    try {
      const response = await shoppingAPI.getItems(list.id);
      setListDetail(response.data);
      setSelectedList(list);
    } catch (error) {
      console.error('Error loading shopping list detail:', error);
      showNotification('❌ Liste detayları yüklenemedi', 'error');
    } finally {
      setDetailLoading(false);
    }
  };

  const toggleItemCheck = async (itemId: number, checked: boolean) => {
    try {
      await shoppingAPI.updateItem(itemId, { checked });
      if (listDetail) {
        const updatedItems = listDetail.items.map(item =>
          item.id === itemId ? { ...item, checked } : item
        );
        setListDetail({ ...listDetail, items: updatedItems });
      }
    } catch (error) {
      console.error('Error updating item:', error);
      showNotification('❌ Öğe güncellenemedi', 'error');
    }
  };

  const sendByEmail = async (listId: number) => {
    // Create a more user-friendly email input dialog
    const emailDialog = document.createElement('div');
    emailDialog.style.cssText = `
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0,0,0,0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 10001;
      padding: 20px;
    `;

    emailDialog.innerHTML = `
      <div style="
        background: white;
        border-radius: 16px;
        padding: 32px;
        max-width: 500px;
        width: 100%;
        box-shadow: 0 20px 40px rgba(0,0,0,0.15);
      ">
        <h2 style="
          color: #2c3e50;
          margin: 0 0 8px 0;
          font-size: 24px;
          font-weight: 700;
        ">📧 Alışveriş Listesi Gönder</h2>
        <p style="
          color: #6c757d;
          margin: 0 0 24px 0;
          font-size: 16px;
        ">E-posta adreslerini virgülle ayırarak girin</p>
        
        <textarea 
          id="emailInput"
          placeholder="ornek@email.com, diger@email.com"
          style="
            width: 100%;
            height: 100px;
            padding: 16px;
            border: 2px solid #e9ecef;
            border-radius: 8px;
            font-size: 16px;
            font-family: inherit;
            resize: vertical;
            outline: none;
            margin-bottom: 24px;
            box-sizing: border-box;
          "
        ></textarea>
        
        <div style="
          background: #e3f2fd;
          padding: 16px;
          border-radius: 8px;
          margin-bottom: 24px;
        ">
          <h4 style="color: #1976d2; margin: 0 0 8px 0; font-size: 14px;">💡 E-posta İçeriği:</h4>
          <ul style="color: #424242; margin: 0; padding-left: 20px; font-size: 14px;">
            <li>Tüm ürünler ve miktarları</li>
            <li>Tahmini toplam maliyet</li>
            <li>Yazdırılabilir format</li>
            <li>İşaretleme kutuları</li>
          </ul>
        </div>
        
        <div style="
          display: flex;
          gap: 12px;
          justify-content: flex-end;
        ">
          <button 
            id="cancelEmail"
            style="
              background: #6c757d;
              color: white;
              border: none;
              border-radius: 8px;
              padding: 12px 20px;
              font-size: 14px;
              font-weight: 600;
              cursor: pointer;
            "
          >İptal</button>
          <button 
            id="sendEmail"
            style="
              background: #007bff;
              color: white;
              border: none;
              border-radius: 8px;
              padding: 12px 20px;
              font-size: 14px;
              font-weight: 600;
              cursor: pointer;
            "
          >📧 Gönder</button>
        </div>
      </div>
    `;

    document.body.appendChild(emailDialog);

    const emailInput = document.getElementById('emailInput') as HTMLTextAreaElement;
    const sendButton = document.getElementById('sendEmail');
    const cancelButton = document.getElementById('cancelEmail');

    // Focus on input
    emailInput.focus();

    // Handle send
    sendButton?.addEventListener('click', async () => {
      const emails = emailInput.value.trim();
      if (!emails) {
        showNotification('❌ Lütfen en az bir e-posta adresi girin', 'error');
        return;
      }

      // Validate email format
      const emailList = emails.split(',').map(e => e.trim()).filter(e => e);
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      const invalidEmails = emailList.filter(email => !emailRegex.test(email));
      
      if (invalidEmails.length > 0) {
        showNotification(`❌ Geçersiz e-posta adresleri: ${invalidEmails.join(', ')}`, 'error');
        return;
      }

      try {
        document.body.removeChild(emailDialog);
        showNotification('📧 E-posta gönderiliyor...', 'success');
        
        await shoppingAPI.sendByEmail(listId, emailList);
        await loadShoppingLists();
        showNotification(`✅ Alışveriş listesi ${emailList.length} kişiye gönderildi!`, 'success');
      } catch (error) {
        console.error('Error sending email:', error);
        showNotification('❌ E-posta gönderilemedi', 'error');
      }
    });

    // Handle cancel
    cancelButton?.addEventListener('click', () => {
      document.body.removeChild(emailDialog);
    });

    // Handle escape key
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        document.body.removeChild(emailDialog);
        document.removeEventListener('keydown', handleEscape);
      }
    };
    document.addEventListener('keydown', handleEscape);

    // Handle click outside
    emailDialog.addEventListener('click', (e) => {
      if (e.target === emailDialog) {
        document.body.removeChild(emailDialog);
      }
    });
  };

  const addItem = () => {
    setNewList({
      ...newList,
      items: [...newList.items, { item_name: '', quantity: 1, unit: 'adet', estimated_cost: 0 }]
    });
  };

  const removeItem = (index: number) => {
    const updatedItems = newList.items.filter((_, i) => i !== index);
    setNewList({ ...newList, items: updatedItems });
  };

  const updateItem = (index: number, field: string, value: any) => {
    const updatedItems = newList.items.map((item, i) => 
      i === index ? { ...item, [field]: value } : item
    );
    setNewList({ ...newList, items: updatedItems });
  };

  const closeModal = () => {
    setSelectedList(null);
    setListDetail(null);
  };

  const showNotification = (message: string, type: 'success' | 'error') => {
    const notification = document.createElement('div');
    notification.textContent = message;
    notification.style.cssText = `
      position: fixed;
      top: 20px;
      right: 20px;
      padding: 16px 24px;
      border-radius: 8px;
      color: white;
      font-weight: 600;
      z-index: 10000;
      background-color: ${type === 'success' ? '#28a745' : '#dc3545'};
      box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    `;
    document.body.appendChild(notification);
    setTimeout(() => document.body.removeChild(notification), 3000);
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'draft': return '#6c757d';
      case 'sent': return '#007bff';
      case 'completed': return '#28a745';
      default: return '#6c757d';
    }
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case 'draft': return 'Taslak';
      case 'sent': return 'Gönderildi';
      case 'completed': return 'Tamamlandı';
      default: return status;
    }
  };

  const getTotalCost = () => {
    if (!listDetail) return 0;
    return listDetail.items.reduce((sum, item) => sum + (item.estimated_cost || 0), 0);
  };

  const getCompletedCount = () => {
    if (!listDetail) return 0;
    return listDetail.items.filter(item => item.checked).length;
  };

  const getCompletionPercentage = () => {
    if (!listDetail || listDetail.items.length === 0) return 0;
    return Math.round((getCompletedCount() / listDetail.items.length) * 100);
  };

  const getFilteredLists = () => {
    if (activeTab === 'all') return lists;
    return lists.filter(list => list.status === activeTab);
  };

  const getTabCount = (status: string) => {
    if (status === 'all') return lists.length;
    return lists.filter(list => list.status === status).length;
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
          .shopping-card {
            animation: slideIn 0.4s ease-out;
          }
          .shopping-card:hover {
            transform: translateY(-4px);
            box-shadow: 0 8px 25px rgba(0,0,0,0.15);
          }
        `}
      </style>

      {/* Header */}
      <header style={styles.header}>
        <div style={styles.headerLeft}>
          <h1 style={styles.title}>🛒 Alışveriş Listeleri</h1>
          <p style={styles.subtitle}>Akıllı alışveriş planlama sistemi</p>
        </div>
        <div style={styles.headerActions}>
          <button 
            style={styles.generateButton}
            onClick={generateFromInventory}
            disabled={loading}
          >
            📦 Stoktan Oluştur
          </button>
          <button 
            style={styles.addButton}
            onClick={() => setShowForm(!showForm)}
          >
            {showForm ? '❌ İptal' : '➕ Yeni Liste'}
          </button>
        </div>
      </header>

      {/* Tabs */}
      <div style={styles.tabsContainer}>
        {[
          { id: 'all', label: 'Tümü', icon: '📋' },
          { id: 'draft', label: 'Taslaklar', icon: '📝' },
          { id: 'sent', label: 'Gönderildi', icon: '📧' },
          { id: 'completed', label: 'Tamamlandı', icon: '✅' }
        ].map(tab => (
          <button
            key={tab.id}
            style={{
              ...styles.tab,
              ...(activeTab === tab.id ? styles.tabActive : {})
            }}
            onClick={() => setActiveTab(tab.id as any)}
          >
            <span style={styles.tabIcon}>{tab.icon}</span>
            <span style={styles.tabLabel}>{tab.label}</span>
            <span style={styles.tabCount}>({getTabCount(tab.id)})</span>
          </button>
        ))}
      </div>

      {/* New List Form */}
      {showForm && (
        <div style={styles.formCard} className="shopping-card">
          <div style={styles.formHeader}>
            <h2 style={styles.formTitle}>✨ Yeni Alışveriş Listesi</h2>
            <p style={styles.formSubtitle}>Ürünlerinizi organize edin</p>
          </div>
          
          <form onSubmit={handleSubmit} style={styles.form}>
            <div style={styles.formGroup}>
              <label style={styles.label}>📝 Liste Adı</label>
              <input
                type="text"
                style={styles.input}
                value={newList.title}
                onChange={(e) => setNewList({...newList, title: e.target.value})}
                placeholder="Örn: Haftalık Market Alışverişi"
                required
              />
            </div>

            <div style={styles.itemsSection}>
              <div style={styles.itemsHeader}>
                <h3 style={styles.itemsTitle}>🛍️ Ürünler</h3>
                <button type="button" style={styles.addItemButton} onClick={addItem}>
                  ➕ Ürün Ekle
                </button>
              </div>

              <div style={styles.itemsGrid}>
                {newList.items.map((item, index) => (
                  <div key={index} style={styles.itemRow}>
                    <input
                      type="text"
                      style={styles.itemInput}
                      placeholder="Ürün adı"
                      value={item.item_name}
                      onChange={(e) => updateItem(index, 'item_name', e.target.value)}
                      required
                    />
                    <input
                      type="number"
                      style={styles.quantityInput}
                      placeholder="Miktar"
                      value={item.quantity}
                      onChange={(e) => updateItem(index, 'quantity', Number(e.target.value))}
                      min="0.1"
                      step="0.1"
                      required
                    />
                    <select
                      style={styles.unitSelect}
                      value={item.unit}
                      onChange={(e) => updateItem(index, 'unit', e.target.value)}
                    >
                      <option value="adet">Adet</option>
                      <option value="kg">Kg</option>
                      <option value="lt">Litre</option>
                      <option value="paket">Paket</option>
                      <option value="kutu">Kutu</option>
                      <option value="gram">Gram</option>
                    </select>
                    <input
                      type="number"
                      style={styles.priceInput}
                      placeholder="₺ Fiyat"
                      value={item.estimated_cost}
                      onChange={(e) => updateItem(index, 'estimated_cost', Number(e.target.value))}
                      min="0"
                      step="0.01"
                    />
                    {newList.items.length > 1 && (
                      <button
                        type="button"
                        style={styles.removeButton}
                        onClick={() => removeItem(index)}
                        title="Ürünü kaldır"
                      >
                        🗑️
                      </button>
                    )}
                  </div>
                ))}
              </div>
            </div>

            <div style={styles.formActions}>
              <button type="submit" style={styles.submitButton}>
                ✅ Liste Oluştur
              </button>
              <button 
                type="button" 
                style={styles.cancelButton}
                onClick={() => setShowForm(false)}
              >
                ❌ İptal
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Shopping Lists Grid */}
      <div style={styles.listsContainer}>
        {loading ? (
          <div style={styles.loadingState}>
            <div style={styles.spinner}></div>
            <p>Alışveriş listeleri yükleniyor...</p>
          </div>
        ) : getFilteredLists().length === 0 ? (
          <div style={styles.emptyState}>
            <div style={styles.emptyIcon}>🛒</div>
            <h3 style={styles.emptyTitle}>
              {activeTab === 'all' ? 'Henüz alışveriş listesi yok' : `${getStatusText(activeTab)} liste bulunamadı`}
            </h3>
            <p style={styles.emptyText}>
              {activeTab === 'all' 
                ? 'Yeni liste oluşturun veya stoktan otomatik oluşturun'
                : 'Farklı bir sekmeyi kontrol edin'
              }
            </p>
            {activeTab === 'all' && (
              <button 
                style={styles.emptyActionButton}
                onClick={() => setShowForm(true)}
              >
                ➕ İlk Listenizi Oluşturun
              </button>
            )}
          </div>
        ) : (
          <div style={styles.listsGrid}>
            {getFilteredLists().map((list, index) => (
              <div 
                key={list.id} 
                style={{...styles.listCard, animationDelay: `${index * 0.1}s`}}
                className="shopping-card"
              >
                <div style={styles.listHeader}>
                  <div style={styles.listTitleSection}>
                    <h3 style={styles.listTitle}>{list.title}</h3>
                    <div style={styles.listMeta}>
                      <span style={styles.listDate}>
                        📅 {new Date(list.created_at).toLocaleDateString('tr-TR')}
                      </span>
                    </div>
                  </div>
                  <div style={styles.listBadges}>
                    {list.auto_generated && (
                      <span style={styles.autoBadge}>🤖 Otomatik</span>
                    )}
                    <span 
                      style={{
                        ...styles.statusBadge,
                        backgroundColor: getStatusColor(list.status)
                      }}
                    >
                      {getStatusText(list.status)}
                    </span>
                  </div>
                </div>

                <div style={styles.listFooter}>
                  {list.sent_via_email && (
                    <div style={styles.emailSent}>
                      📧 E-posta gönderildi
                    </div>
                  )}
                  
                  <div style={styles.listActions}>
                    <button 
                      style={styles.viewButton}
                      onClick={() => loadShoppingListDetail(list)}
                    >
                      👁️ Görüntüle
                    </button>
                    {!list.sent_via_email && (
                      <button 
                        style={styles.emailButton}
                        onClick={() => sendByEmail(list.id)}
                      >
                        📧 Gönder
                      </button>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* List Detail Modal */}
      {selectedList && (
        <div style={styles.modal}>
          <div style={styles.modalContent}>
            <div style={styles.modalHeader}>
              <div style={styles.modalTitleSection}>
                <h2 style={styles.modalTitle}>{selectedList.title}</h2>
                <div style={styles.modalBadges}>
                  {selectedList.auto_generated && (
                    <span style={styles.autoBadge}>🤖 Otomatik</span>
                  )}
                  <span 
                    style={{
                      ...styles.statusBadge,
                      backgroundColor: getStatusColor(selectedList.status)
                    }}
                  >
                    {getStatusText(selectedList.status)}
                  </span>
                </div>
              </div>
              <button 
                style={styles.closeButton}
                onClick={closeModal}
              >
                ✕
              </button>
            </div>
            
            <div style={styles.modalBody}>
              {detailLoading ? (
                <div style={styles.modalLoading}>
                  <div style={styles.spinner}></div>
                  <p>Liste detayları yükleniyor...</p>
                </div>
              ) : listDetail ? (
                <>
                  {/* Progress Summary */}
                  <div style={styles.progressSummary}>
                    <div style={styles.progressInfo}>
                      <div style={styles.progressStats}>
                        <span style={styles.progressText}>
                          {getCompletedCount()} / {listDetail.items.length} tamamlandı
                        </span>
                        <span style={styles.progressPercentage}>
                          %{getCompletionPercentage()}
                        </span>
                      </div>
                      <div style={styles.progressBarContainer}>
                        <div 
                          style={{
                            ...styles.progressBarFill,
                            width: `${getCompletionPercentage()}%`
                          }}
                        />
                      </div>
                    </div>
                    {getTotalCost() > 0 && (
                      <div style={styles.totalCost}>
                        <span style={styles.costLabel}>Tahmini Toplam:</span>
                        <span style={styles.costAmount}>₺{getTotalCost().toLocaleString('tr-TR')}</span>
                      </div>
                    )}
                  </div>

                  {/* Shopping Items */}
                  <div style={styles.itemsList}>
                    <h3 style={styles.itemsListTitle}>🛍️ Alışveriş Listesi</h3>
                    {listDetail.items.length === 0 ? (
                      <div style={styles.emptyItems}>
                        <p>Bu listede henüz ürün yok</p>
                      </div>
                    ) : (
                      <div style={styles.itemsModalGrid}>
                        {listDetail.items.map((item) => (
                          <div 
                            key={item.id} 
                            style={{
                              ...styles.shoppingItem,
                              ...(item.checked ? styles.shoppingItemChecked : {})
                            }}
                          >
                            <div style={styles.itemCheckbox}>
                              <input
                                type="checkbox"
                                checked={item.checked}
                                onChange={(e) => toggleItemCheck(item.id, e.target.checked)}
                                style={styles.checkbox}
                              />
                            </div>
                            <div style={styles.itemDetails}>
                              <div style={{
                                ...styles.itemName,
                                ...(item.checked ? styles.itemNameChecked : {})
                              }}>
                                {item.item_name}
                              </div>
                              <div style={styles.itemMeta}>
                                <span style={styles.itemQuantity}>
                                  {item.quantity} {item.unit}
                                </span>
                                {item.estimated_cost > 0 && (
                                  <span style={styles.itemCost}>
                                    ₺{item.estimated_cost.toLocaleString('tr-TR')}
                                  </span>
                                )}
                              </div>
                            </div>
                            {item.checked && (
                              <div style={styles.checkMark}>✓</div>
                            )}
                          </div>
                        ))}
                      </div>
                    )}
                  </div>

                  {/* List Info */}
                  <div style={styles.listInfo}>
                    <div style={styles.infoItem}>
                      <span style={styles.infoLabel}>Oluşturulma:</span>
                      <span style={styles.infoValue}>
                        {new Date(selectedList.created_at).toLocaleDateString('tr-TR', {
                          year: 'numeric',
                          month: 'long',
                          day: 'numeric',
                          hour: '2-digit',
                          minute: '2-digit'
                        })}
                      </span>
                    </div>
                    {selectedList.sent_via_email && (
                      <div style={styles.infoItem}>
                        <span style={styles.infoLabel}>Durum:</span>
                        <span style={styles.emailSentInfo}>📧 E-posta ile gönderildi</span>
                      </div>
                    )}
                  </div>

                  {/* Action Buttons */}
                  <div style={styles.modalActions}>
                    {!selectedList.sent_via_email && (
                      <button 
                        style={styles.modalEmailButton}
                        onClick={() => sendByEmail(selectedList.id)}
                      >
                        📧 E-posta ile Gönder
                      </button>
                    )}
                    <button 
                      style={styles.modalCloseButton}
                      onClick={closeModal}
                    >
                      Kapat
                    </button>
                  </div>
                </>
              ) : (
                <div style={styles.errorState}>
                  <p>Liste detayları yüklenemedi</p>
                  <button onClick={closeModal} style={styles.modalCloseButton}>
                    Kapat
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
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
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: '32px',
    padding: '0 4px',
  },
  headerLeft: {
    flex: 1,
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
  headerActions: {
    display: 'flex',
    gap: '12px',
    alignItems: 'center',
  },
  generateButton: {
    backgroundColor: '#17a2b8',
    color: '#fff',
    border: 'none',
    borderRadius: '12px',
    padding: '14px 20px',
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'all 0.3s ease',
    boxShadow: '0 2px 8px rgba(23, 162, 184, 0.3)',
  },
  addButton: {
    backgroundColor: '#007bff',
    color: '#fff',
    border: 'none',
    borderRadius: '12px',
    padding: '14px 24px',
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'all 0.3s ease',
    boxShadow: '0 2px 8px rgba(0, 123, 255, 0.3)',
  },
  tabsContainer: {
    display: 'flex',
    gap: '8px',
    marginBottom: '32px',
    padding: '4px',
    backgroundColor: '#f8f9fa',
    borderRadius: '12px',
    overflow: 'auto',
  },
  tab: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    padding: '12px 16px',
    border: 'none',
    backgroundColor: 'transparent',
    color: '#6c757d',
    cursor: 'pointer',
    borderRadius: '8px',
    fontSize: '14px',
    fontWeight: '500',
    transition: 'all 0.3s ease',
    whiteSpace: 'nowrap' as const,
  },
  tabActive: {
    backgroundColor: '#fff',
    color: '#007bff',
    boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
  },
  tabIcon: {
    fontSize: '16px',
  },
  tabLabel: {},
  tabCount: {
    fontSize: '12px',
    opacity: 0.7,
  },
  formCard: {
    backgroundColor: '#fff',
    borderRadius: '16px',
    padding: '32px',
    marginBottom: '32px',
    boxShadow: '0 4px 20px rgba(0,0,0,0.08)',
    border: '1px solid #e9ecef',
  },
  formHeader: {
    marginBottom: '24px',
    textAlign: 'center' as const,
  },
  formTitle: {
    fontSize: '24px',
    fontWeight: '700',
    color: '#2c3e50',
    margin: '0 0 8px 0',
  },
  formSubtitle: {
    fontSize: '16px',
    color: '#6c757d',
    margin: 0,
  },
  form: {
    display: 'flex',
    flexDirection: 'column' as const,
    gap: '24px',
  },
  formGroup: {
    display: 'flex',
    flexDirection: 'column' as const,
    gap: '8px',
  },
  label: {
    fontSize: '14px',
    fontWeight: '600',
    color: '#495057',
  },
  input: {
    padding: '14px 16px',
    border: '2px solid #e9ecef',
    borderRadius: '8px',
    fontSize: '16px',
    transition: 'border-color 0.3s ease',
    outline: 'none',
  },
  itemsSection: {
    border: '2px solid #e9ecef',
    borderRadius: '12px',
    padding: '20px',
    backgroundColor: '#f8f9fa',
  },
  itemsHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '20px',
  },
  itemsTitle: {
    fontSize: '18px',
    fontWeight: '600',
    color: '#2c3e50',
    margin: 0,
  },
  addItemButton: {
    backgroundColor: '#28a745',
    color: '#fff',
    border: 'none',
    borderRadius: '8px',
    padding: '10px 16px',
    fontSize: '12px',
    fontWeight: '600',
    cursor: 'pointer',
  },
  itemsGrid: {
    display: 'flex',
    flexDirection: 'column' as const,
    gap: '12px',
  },
  itemRow: {
    display: 'grid',
    gridTemplateColumns: '2fr 100px 100px 120px 40px',
    gap: '12px',
    alignItems: 'center',
    padding: '12px',
    backgroundColor: '#fff',
    borderRadius: '8px',
    border: '1px solid #dee2e6',
  },
  itemInput: {
    padding: '10px 12px',
    border: '1px solid #ced4da',
    borderRadius: '6px',
    fontSize: '14px',
  },
  quantityInput: {
    padding: '10px 12px',
    border: '1px solid #ced4da',
    borderRadius: '6px',
    fontSize: '14px',
  },
  unitSelect: {
    padding: '10px 12px',
    border: '1px solid #ced4da',
    borderRadius: '6px',
    fontSize: '14px',
    backgroundColor: '#fff',
  },
  priceInput: {
    padding: '10px 12px',
    border: '1px solid #ced4da',
    borderRadius: '6px',
    fontSize: '14px',
  },
  removeButton: {
    backgroundColor: '#dc3545',
    color: '#fff',
    border: 'none',
    borderRadius: '6px',
    width: '32px',
    height: '32px',
    cursor: 'pointer',
    fontSize: '14px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  formActions: {
    display: 'flex',
    gap: '12px',
    justifyContent: 'center',
  },
  submitButton: {
    backgroundColor: '#28a745',
    color: '#fff',
    border: 'none',
    borderRadius: '12px',
    padding: '16px 32px',
    fontSize: '16px',
    fontWeight: '600',
    cursor: 'pointer',
    boxShadow: '0 2px 8px rgba(40, 167, 69, 0.3)',
  },
  cancelButton: {
    backgroundColor: '#6c757d',
    color: '#fff',
    border: 'none',
    borderRadius: '12px',
    padding: '16px 32px',
    fontSize: '16px',
    fontWeight: '600',
    cursor: 'pointer',
  },
  listsContainer: {
    minHeight: '400px',
  },
  loadingState: {
    display: 'flex',
    flexDirection: 'column' as const,
    alignItems: 'center',
    justifyContent: 'center',
    padding: '80px 20px',
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
  emptyState: {
    textAlign: 'center' as const,
    padding: '80px 20px',
    color: '#95a5a6',
  },
  emptyIcon: {
    fontSize: '64px',
    marginBottom: '16px',
  },
  emptyTitle: {
    fontSize: '24px',
    fontWeight: '600',
    color: '#2c3e50',
    marginBottom: '8px',
  },
  emptyText: {
    fontSize: '16px',
    marginBottom: '24px',
  },
  emptyActionButton: {
    backgroundColor: '#007bff',
    color: '#fff',
    border: 'none',
    borderRadius: '12px',
    padding: '16px 32px',
    fontSize: '16px',
    fontWeight: '600',
    cursor: 'pointer',
  },
  listsGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(350px, 1fr))',
    gap: '24px',
  },
  listCard: {
    backgroundColor: '#fff',
    borderRadius: '16px',
    padding: '24px',
    boxShadow: '0 2px 12px rgba(0,0,0,0.08)',
    border: '1px solid #e9ecef',
    transition: 'all 0.3s ease',
    cursor: 'pointer',
  },
  listHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: '16px',
  },
  listTitleSection: {
    flex: 1,
    marginRight: '16px',
  },
  listTitle: {
    fontSize: '20px',
    fontWeight: '600',
    color: '#2c3e50',
    margin: '0 0 8px 0',
    lineHeight: '1.3',
  },
  listMeta: {
    fontSize: '14px',
    color: '#6c757d',
  },
  listDate: {},
  listBadges: {
    display: 'flex',
    flexDirection: 'column' as const,
    gap: '6px',
    alignItems: 'flex-end',
  },
  autoBadge: {
    backgroundColor: '#6f42c1',
    color: '#fff',
    padding: '4px 8px',
    borderRadius: '12px',
    fontSize: '11px',
    fontWeight: '600',
  },
  statusBadge: {
    color: '#fff',
    padding: '6px 12px',
    borderRadius: '16px',
    fontSize: '12px',
    fontWeight: '500',
  },
  listFooter: {
    borderTop: '1px solid #f1f3f4',
    paddingTop: '16px',
  },
  emailSent: {
    color: '#28a745',
    fontSize: '12px',
    fontWeight: '500',
    marginBottom: '12px',
  },
  listActions: {
    display: 'flex',
    gap: '8px',
  },
  viewButton: {
    backgroundColor: '#007bff',
    color: '#fff',
    border: 'none',
    borderRadius: '8px',
    padding: '10px 16px',
    fontSize: '12px',
    fontWeight: '600',
    cursor: 'pointer',
    flex: 1,
  },
  emailButton: {
    backgroundColor: '#17a2b8',
    color: '#fff',
    border: 'none',
    borderRadius: '8px',
    padding: '10px 16px',
    fontSize: '12px',
    fontWeight: '600',
    cursor: 'pointer',
    flex: 1,
  },
  modal: {
    position: 'fixed' as const,
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0,0,0,0.5)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1000,
    padding: '20px',
  },
  modalContent: {
    backgroundColor: '#fff',
    borderRadius: '16px',
    width: '100%',
    maxWidth: '700px',
    maxHeight: '90vh',
    overflow: 'hidden',
    boxShadow: '0 20px 40px rgba(0,0,0,0.15)',
  },
  modalHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    padding: '24px 24px 16px 24px',
    borderBottom: '1px solid #e9ecef',
  },
  modalTitleSection: {
    flex: 1,
  },
  modalTitle: {
    fontSize: '24px',
    fontWeight: '700',
    color: '#2c3e50',
    margin: '0 0 8px 0',
  },
  modalBadges: {
    display: 'flex',
    gap: '8px',
  },
  closeButton: {
    backgroundColor: 'transparent',
    border: 'none',
    fontSize: '24px',
    cursor: 'pointer',
    color: '#6c757d',
    padding: '4px',
    width: '32px',
    height: '32px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: '50%',
    transition: 'background-color 0.2s',
  },
  modalBody: {
    padding: '24px',
    overflow: 'auto',
    maxHeight: 'calc(90vh - 120px)',
  },
  modalLoading: {
    display: 'flex',
    flexDirection: 'column' as const,
    alignItems: 'center',
    padding: '40px',
    color: '#6c757d',
  },
  progressSummary: {
    backgroundColor: '#f8f9fa',
    borderRadius: '12px',
    padding: '20px',
    marginBottom: '24px',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  progressInfo: {
    flex: 1,
    marginRight: '20px',
  },
  progressStats: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '8px',
  },
  progressText: {
    fontSize: '16px',
    fontWeight: '600',
    color: '#2c3e50',
  },
  progressPercentage: {
    fontSize: '18px',
    fontWeight: '700',
    color: '#007bff',
  },
  progressBarContainer: {
    width: '100%',
    height: '8px',
    backgroundColor: '#e9ecef',
    borderRadius: '4px',
    overflow: 'hidden',
  },
  progressBarFill: {
    height: '100%',
    backgroundColor: '#28a745',
    transition: 'width 0.3s ease',
  },
  totalCost: {
    textAlign: 'right' as const,
  },
  costLabel: {
    display: 'block',
    fontSize: '14px',
    color: '#6c757d',
    marginBottom: '4px',
  },
  costAmount: {
    fontSize: '20px',
    fontWeight: '700',
    color: '#28a745',
  },
  itemsList: {
    marginBottom: '24px',
  },
  itemsListTitle: {
    fontSize: '18px',
    fontWeight: '600',
    color: '#2c3e50',
    marginBottom: '16px',
  },
  emptyItems: {
    textAlign: 'center' as const,
    padding: '40px',
    color: '#95a5a6',
  },
  itemsModalGrid: {
    display: 'flex',
    flexDirection: 'column' as const,
    gap: '12px',
  },
  shoppingItem: {
    display: 'flex',
    alignItems: 'center',
    padding: '16px',
    backgroundColor: '#fff',
    border: '2px solid #e9ecef',
    borderRadius: '12px',
    transition: 'all 0.3s ease',
    cursor: 'pointer',
  },
  shoppingItemChecked: {
    backgroundColor: '#f8f9fa',
    borderColor: '#28a745',
    opacity: 0.8,
  },
  itemCheckbox: {
    marginRight: '16px',
  },
  checkbox: {
    width: '20px',
    height: '20px',
    cursor: 'pointer',
  },
  itemDetails: {
    flex: 1,
  },
  itemName: {
    fontSize: '16px',
    fontWeight: '600',
    color: '#2c3e50',
    marginBottom: '4px',
    transition: 'all 0.3s ease',
  },
  itemNameChecked: {
    textDecoration: 'line-through',
    color: '#6c757d',
  },
  itemMeta: {
    display: 'flex',
    gap: '16px',
    fontSize: '14px',
    color: '#6c757d',
  },
  itemQuantity: {},
  itemCost: {
    fontWeight: '600',
    color: '#28a745',
  },
  checkMark: {
    fontSize: '24px',
    color: '#28a745',
    fontWeight: 'bold',
  },
  listInfo: {
    backgroundColor: '#f8f9fa',
    borderRadius: '8px',
    padding: '16px',
    marginBottom: '24px',
  },
  infoItem: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '8px',
  },
  infoLabel: {
    fontSize: '14px',
    color: '#6c757d',
    fontWeight: '500',
  },
  infoValue: {
    fontSize: '14px',
    color: '#2c3e50',
    fontWeight: '600',
  },
  emailSentInfo: {
    fontSize: '14px',
    color: '#28a745',
    fontWeight: '600',
  },
  modalActions: {
    display: 'flex',
    gap: '12px',
    justifyContent: 'flex-end',
  },
  modalEmailButton: {
    backgroundColor: '#007bff',
    color: '#fff',
    border: 'none',
    borderRadius: '8px',
    padding: '12px 20px',
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
  },
  modalCloseButton: {
    backgroundColor: '#6c757d',
    color: '#fff',
    border: 'none',
    borderRadius: '8px',
    padding: '12px 20px',
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
  },
  errorState: {
    textAlign: 'center' as const,
    padding: '40px',
    color: '#dc3545',
  },
};

export default ShoppingPage;