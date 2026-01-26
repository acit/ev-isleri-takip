import React, { useState, useEffect, useRef } from 'react';
import { useAuthStore } from '../store/authStore';
import { messagesAPI } from '../utils/api';

interface Message {
  id: number;
  sender_id: number;
  recipient_id: number | null;
  message: string;
  message_type: string;
  is_read: boolean;
  created_at: string;
  sender_name: string;
  sender_email: string;
  recipient_name?: string;
  recipient_email?: string;
}

interface FamilyMember {
  id: number;
  full_name: string;
  email: string;
  role: string;
  status: string;
}

const MessagesPage: React.FC = () => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [familyMembers, setFamilyMembers] = useState<FamilyMember[]>([]);
  const [selectedRecipient, setSelectedRecipient] = useState<number | null>(null);
  const [newMessage, setNewMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const { userId } = useAuthStore();

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    fetchFamilyMembers();
    fetchMessages();
  }, [selectedRecipient]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const fetchFamilyMembers = async () => {
    try {
      const response = await messagesAPI.getFamilyMembers();
      setFamilyMembers(response.data);
    } catch (error) {
      console.error('Error fetching family members:', error);
    }
  };

  const fetchMessages = async () => {
    try {
      const params = selectedRecipient 
        ? { recipientId: selectedRecipient, limit: 50 }
        : { limit: 50 };
        
      const response = await messagesAPI.getAll(params);
      setMessages(response.data);
    } catch (error) {
      console.error('Error fetching messages:', error);
    }
  };

  const sendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newMessage.trim()) return;

    setLoading(true);
    setError('');

    try {
      const response = await messagesAPI.send({
        recipientId: selectedRecipient,
        message: newMessage.trim(),
        messageType: 'text'
      });

      setMessages(prev => [...prev, response.data]);
      setNewMessage('');
    } catch (error: any) {
      console.error('Error sending message:', error);
      setError(error.response?.data?.error || 'Mesaj gönderilemedi');
    }
    setLoading(false);
  };

  const formatTime = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffInHours = (now.getTime() - date.getTime()) / (1000 * 60 * 60);

    if (diffInHours < 24) {
      return date.toLocaleTimeString('tr-TR', { 
        hour: '2-digit', 
        minute: '2-digit' 
      });
    } else {
      return date.toLocaleDateString('tr-TR', { 
        day: '2-digit', 
        month: '2-digit',
        hour: '2-digit', 
        minute: '2-digit' 
      });
    }
  };

  const getRecipientName = () => {
    if (!selectedRecipient) return 'Aile Grubu';
    const member = familyMembers.find(m => m.id === selectedRecipient);
    return member?.full_name || 'Bilinmeyen Kullanıcı';
  };

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h1 style={styles.title}>💬 Aile Mesajları</h1>
        <p style={styles.subtitle}>Aile üyeleriyle iletişim kurun</p>
      </div>

      <div style={styles.content}>
        {/* Sidebar - Recipients */}
        <div style={styles.sidebar}>
          <div style={styles.sidebarHeader}>
            <h3 style={styles.sidebarTitle}>Konuşmalar</h3>
          </div>
          
          <div style={styles.recipientsList}>
            <button
              style={{
                ...styles.recipientItem,
                ...(selectedRecipient === null ? styles.recipientItemActive : {})
              }}
              onClick={() => setSelectedRecipient(null)}
            >
              <div style={styles.recipientAvatar}>👨‍👩‍👧‍👦</div>
              <div style={styles.recipientInfo}>
                <div style={styles.recipientName}>Aile Grubu</div>
                <div style={styles.recipientStatus}>Herkese mesaj</div>
              </div>
            </button>

            {familyMembers.map(member => (
              <button
                key={member.id}
                style={{
                  ...styles.recipientItem,
                  ...(selectedRecipient === member.id ? styles.recipientItemActive : {})
                }}
                onClick={() => setSelectedRecipient(member.id)}
              >
                <div style={styles.recipientAvatar}>👤</div>
                <div style={styles.recipientInfo}>
                  <div style={styles.recipientName}>{member.full_name}</div>
                  <div style={styles.recipientStatus}>{member.role}</div>
                </div>
              </button>
            ))}
          </div>
        </div>

        {/* Chat Area */}
        <div style={styles.chatArea}>
          <div style={styles.chatHeader}>
            <div style={styles.chatHeaderInfo}>
              <h3 style={styles.chatTitle}>{getRecipientName()}</h3>
              <p style={styles.chatSubtitle}>
                {selectedRecipient ? 'Özel konuşma' : 'Aile grubu konuşması'}
              </p>
            </div>
          </div>

          <div style={styles.messagesContainer}>
            {messages.length === 0 ? (
              <div style={styles.emptyState}>
                <div style={styles.emptyIcon}>💬</div>
                <h3 style={styles.emptyTitle}>Henüz mesaj yok</h3>
                <p style={styles.emptyText}>
                  {selectedRecipient 
                    ? 'İlk mesajı göndererek konuşmaya başlayın'
                    : 'Aile grubunda ilk mesajı gönderin'
                  }
                </p>
              </div>
            ) : (
              <div style={styles.messagesList}>
                {messages.map(message => (
                  <div
                    key={message.id}
                    style={{
                      ...styles.messageItem,
                      ...(message.sender_id === userId ? styles.messageItemOwn : styles.messageItemOther)
                    }}
                  >
                    <div style={{
                      ...styles.messageContent,
                      backgroundColor: message.sender_id === userId 
                        ? 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
                        : '#f1f3f4',
                      background: message.sender_id === userId 
                        ? 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
                        : '#f1f3f4',
                      color: message.sender_id === userId ? '#fff' : '#2c3e50',
                      boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
                    }}>
                      {message.sender_id !== userId && !selectedRecipient && (
                        <div style={{
                          ...styles.messageSender,
                          color: '#2196f3'
                        }}>{message.sender_name}</div>
                      )}
                      <div style={{
                        ...styles.messageText,
                        color: message.sender_id === userId ? '#fff' : '#2c3e50',
                        fontSize: '15px',
                        lineHeight: '1.4'
                      }}>{message.message}</div>
                      <div style={{
                        ...styles.messageTime,
                        color: message.sender_id === userId ? 'rgba(255,255,255,0.8)' : '#6c757d'
                      }}>{formatTime(message.created_at)}</div>
                    </div>
                  </div>
                ))}
                <div ref={messagesEndRef} />
              </div>
            )}
          </div>

          {/* Message Input */}
          <div style={styles.messageInput}>
            {error && (
              <div style={styles.error}>
                ⚠️ {error}
              </div>
            )}
            <form onSubmit={sendMessage} style={styles.inputForm}>
              <input
                type="text"
                value={newMessage}
                onChange={(e) => setNewMessage(e.target.value)}
                placeholder={`${getRecipientName()}'a mesaj yazın... 😊`}
                style={{
                  ...styles.textInput,
                  borderColor: newMessage.trim() ? '#2196f3' : '#e9ecef'
                }}
                disabled={loading}
              />
              <button
                type="submit"
                style={{
                  ...styles.sendButton,
                  backgroundColor: newMessage.trim() ? '#2196f3' : '#ccc',
                  transform: loading ? 'scale(0.95)' : 'scale(1)',
                  cursor: loading || !newMessage.trim() ? 'not-allowed' : 'pointer'
                }}
                disabled={loading || !newMessage.trim()}
              >
                {loading ? '⏳' : '🚀'}
              </button>
            </form>
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
    height: 'calc(100vh - 120px)',
    display: 'flex',
    flexDirection: 'column' as const,
  },
  header: {
    marginBottom: '24px',
  },
  title: {
    fontSize: '28px',
    fontWeight: '700',
    color: '#2c3e50',
    margin: '0 0 8px 0',
  },
  subtitle: {
    fontSize: '16px',
    color: '#6c757d',
    margin: 0,
  },
  content: {
    flex: 1,
    display: 'flex',
    backgroundColor: '#fff',
    borderRadius: '12px',
    boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
    overflow: 'hidden',
  },
  sidebar: {
    width: '300px',
    borderRight: '1px solid #e9ecef',
    display: 'flex',
    flexDirection: 'column' as const,
  },
  sidebarHeader: {
    padding: '20px',
    borderBottom: '1px solid #e9ecef',
    backgroundColor: '#f8f9fa',
  },
  sidebarTitle: {
    fontSize: '18px',
    fontWeight: '600',
    color: '#2c3e50',
    margin: 0,
  },
  recipientsList: {
    flex: 1,
    overflow: 'auto',
  },
  recipientItem: {
    width: '100%',
    display: 'flex',
    alignItems: 'center',
    padding: '16px 20px',
    border: 'none',
    backgroundColor: 'transparent',
    cursor: 'pointer',
    transition: 'background-color 0.2s ease',
    textAlign: 'left' as const,
  },
  recipientItemActive: {
    backgroundColor: '#e3f2fd',
    borderRight: '3px solid #2196f3',
  },
  recipientAvatar: {
    width: '40px',
    height: '40px',
    borderRadius: '50%',
    backgroundColor: '#e9ecef',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '18px',
    marginRight: '12px',
  },
  recipientInfo: {
    flex: 1,
  },
  recipientName: {
    fontSize: '14px',
    fontWeight: '600',
    color: '#2c3e50',
    marginBottom: '2px',
  },
  recipientStatus: {
    fontSize: '12px',
    color: '#6c757d',
  },
  chatArea: {
    flex: 1,
    display: 'flex',
    flexDirection: 'column' as const,
  },
  chatHeader: {
    padding: '20px',
    borderBottom: '1px solid #e9ecef',
    backgroundColor: '#f8f9fa',
  },
  chatHeaderInfo: {},
  chatTitle: {
    fontSize: '18px',
    fontWeight: '600',
    color: '#2c3e50',
    margin: '0 0 4px 0',
  },
  chatSubtitle: {
    fontSize: '14px',
    color: '#6c757d',
    margin: 0,
  },
  messagesContainer: {
    flex: 1,
    overflow: 'auto',
    position: 'relative' as const,
  },
  emptyState: {
    display: 'flex',
    flexDirection: 'column' as const,
    alignItems: 'center',
    justifyContent: 'center',
    height: '100%',
    padding: '40px',
    textAlign: 'center' as const,
  },
  emptyIcon: {
    fontSize: '64px',
    marginBottom: '16px',
    opacity: 0.5,
  },
  emptyTitle: {
    fontSize: '20px',
    fontWeight: '600',
    color: '#2c3e50',
    margin: '0 0 8px 0',
  },
  emptyText: {
    fontSize: '14px',
    color: '#6c757d',
    margin: 0,
  },
  messagesList: {
    padding: '20px',
    display: 'flex',
    flexDirection: 'column' as const,
    gap: '12px',
  },
  messageItem: {
    display: 'flex',
    maxWidth: '70%',
  },
  messageItemOwn: {
    alignSelf: 'flex-end',
  },
  messageItemOther: {
    alignSelf: 'flex-start',
  },
  messageContent: {
    padding: '12px 16px',
    borderRadius: '18px',
    maxWidth: '100%',
    wordBreak: 'break-word' as const,
    fontSize: '16px',
    lineHeight: '1.5',
  },
  messageSender: {
    fontSize: '12px',
    fontWeight: '600',
    color: '#2196f3',
    marginBottom: '4px',
  },
  messageText: {
    fontSize: '14px',
    color: '#2c3e50',
    lineHeight: '1.4',
    marginBottom: '4px',
  },
  messageTime: {
    fontSize: '11px',
    color: '#6c757d',
    textAlign: 'right' as const,
  },
  messageInput: {
    padding: '20px',
    borderTop: '1px solid #e9ecef',
    backgroundColor: '#f8f9fa',
  },
  error: {
    backgroundColor: '#f8d7da',
    color: '#721c24',
    padding: '8px 12px',
    borderRadius: '6px',
    fontSize: '14px',
    marginBottom: '12px',
  },
  inputForm: {
    display: 'flex',
    gap: '12px',
    alignItems: 'center',
  },
  textInput: {
    flex: 1,
    padding: '12px 16px',
    border: '2px solid #e9ecef',
    borderRadius: '24px',
    fontSize: '14px',
    outline: 'none',
    transition: 'border-color 0.3s ease',
  },
  sendButton: {
    width: '48px',
    height: '48px',
    borderRadius: '50%',
    border: 'none',
    backgroundColor: '#2196f3',
    color: '#fff',
    fontSize: '18px',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    transition: 'background-color 0.3s ease',
  },
};

export default MessagesPage;