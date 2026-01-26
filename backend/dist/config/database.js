"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.getDatabase = exports.initDatabase = void 0;
const sqlite3_1 = __importDefault(require("sqlite3"));
const sqlite_1 = require("sqlite");
const path_1 = __importDefault(require("path"));
const dotenv_1 = __importDefault(require("dotenv"));
dotenv_1.default.config();
let db = null;
const initDatabase = async () => {
    if (db)
        return db;
    try {
        db = await (0, sqlite_1.open)({
            filename: path_1.default.join(__dirname, '../../database.sqlite'),
            driver: sqlite3_1.default.Database
        });
        console.log('Connected to SQLite database');
        // Initialize tables
        await initTables();
        return db;
    }
    catch (error) {
        console.error('Database connection error:', error);
        throw error;
    }
};
exports.initDatabase = initDatabase;
const initTables = async () => {
    if (!db)
        return;
    // Users table
    await db.exec(`
    CREATE TABLE IF NOT EXISTS users (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      email TEXT UNIQUE NOT NULL,
      password_hash TEXT,
      full_name TEXT,
      family_id INTEGER,
      role TEXT DEFAULT 'member',
      status TEXT DEFAULT 'pending',
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )
  `);
    // Families table
    await db.exec(`
    CREATE TABLE IF NOT EXISTS families (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT NOT NULL,
      created_by INTEGER NOT NULL,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (created_by) REFERENCES users(id)
    )
  `);
    // Login codes table
    await db.exec(`
    CREATE TABLE IF NOT EXISTS login_codes (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      email TEXT NOT NULL,
      code TEXT NOT NULL,
      expires_at DATETIME NOT NULL,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )
  `);
    // Tasks table
    await db.exec(`
    CREATE TABLE IF NOT EXISTS tasks (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      family_id INTEGER NOT NULL,
      title TEXT NOT NULL,
      description TEXT,
      assigned_to INTEGER,
      created_by INTEGER NOT NULL,
      frequency TEXT NOT NULL,
      due_date DATE,
      completed BOOLEAN DEFAULT FALSE,
      completed_by INTEGER,
      completed_at DATETIME,
      priority TEXT DEFAULT 'medium',
      status TEXT DEFAULT 'pending',
      category TEXT,
      estimated_cost DECIMAL(10, 2),
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (family_id) REFERENCES families(id),
      FOREIGN KEY (assigned_to) REFERENCES users(id),
      FOREIGN KEY (created_by) REFERENCES users(id)
    )
  `);
    // Inventory table
    await db.exec(`
    CREATE TABLE IF NOT EXISTS inventory (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      family_id INTEGER NOT NULL,
      item_name TEXT NOT NULL,
      quantity DECIMAL(10, 2) NOT NULL,
      unit TEXT,
      min_threshold DECIMAL(10, 2),
      category TEXT,
      location TEXT,
      notes TEXT,
      last_updated INTEGER,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (family_id) REFERENCES families(id),
      FOREIGN KEY (last_updated) REFERENCES users(id)
    )
  `);
    // Shopping Lists table
    await db.exec(`
    CREATE TABLE IF NOT EXISTS shopping_lists (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      family_id INTEGER NOT NULL,
      created_by INTEGER NOT NULL,
      title TEXT,
      auto_generated BOOLEAN DEFAULT FALSE,
      sent_via_email BOOLEAN DEFAULT FALSE,
      sent_at DATETIME,
      status TEXT DEFAULT 'draft',
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (family_id) REFERENCES families(id),
      FOREIGN KEY (created_by) REFERENCES users(id)
    )
  `);
    // Shopping List Items
    await db.exec(`
    CREATE TABLE IF NOT EXISTS shopping_list_items (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      shopping_list_id INTEGER NOT NULL,
      item_name TEXT NOT NULL,
      quantity DECIMAL(10, 2),
      unit TEXT,
      estimated_cost DECIMAL(10, 2),
      checked BOOLEAN DEFAULT FALSE,
      checked_by INTEGER,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (shopping_list_id) REFERENCES shopping_lists(id) ON DELETE CASCADE,
      FOREIGN KEY (checked_by) REFERENCES users(id)
    )
  `);
    // Budget table
    await db.exec(`
    CREATE TABLE IF NOT EXISTS budgets (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      family_id INTEGER NOT NULL,
      category TEXT NOT NULL,
      monthly_limit DECIMAL(10, 2),
      spent_amount DECIMAL(10, 2) DEFAULT 0,
      month_year TEXT,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (family_id) REFERENCES families(id),
      UNIQUE(family_id, category, month_year)
    )
  `);
    // Expenses table
    await db.exec(`
    CREATE TABLE IF NOT EXISTS expenses (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      family_id INTEGER NOT NULL,
      recorded_by INTEGER NOT NULL,
      amount DECIMAL(10, 2) NOT NULL,
      category TEXT,
      description TEXT,
      task_id INTEGER,
      expense_date DATE NOT NULL,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (family_id) REFERENCES families(id),
      FOREIGN KEY (recorded_by) REFERENCES users(id),
      FOREIGN KEY (task_id) REFERENCES tasks(id)
    )
  `);
    // Messages table
    await db.exec(`
    CREATE TABLE IF NOT EXISTS messages (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      family_id INTEGER NOT NULL,
      sender_id INTEGER NOT NULL,
      recipient_id INTEGER,
      message TEXT NOT NULL,
      message_type TEXT DEFAULT 'text',
      is_group_message BOOLEAN DEFAULT FALSE,
      read_by TEXT DEFAULT '[]',
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (family_id) REFERENCES families(id),
      FOREIGN KEY (sender_id) REFERENCES users(id),
      FOREIGN KEY (recipient_id) REFERENCES users(id)
    )
  `);
    console.log('Database tables initialized');
    // Add test user if not exists
    const existingUser = await db.get('SELECT id FROM users WHERE email = ?', ['test@example.com']);
    if (!existingUser) {
        // Create test family first
        const familyResult = await db.run('INSERT INTO families (name, created_by) VALUES (?, ?)', ['Test Ailesi', 1]);
        // Create test user
        await db.run('INSERT INTO users (email, full_name, family_id, role, status) VALUES (?, ?, ?, ?, ?)', ['test@example.com', 'Test Kullanıcı', familyResult.lastID, 'admin', 'active']);
        console.log('Test user created: test@example.com');
        // Add sample tasks
        const sampleTasks = [
            {
                title: '🍽️ Bulaşık Makinesi Boşalt',
                description: 'Temiz bulaşıkları yerlerine koy ve makineyi yeniden hazırla',
                frequency: 'daily',
                priority: 'medium',
                category: 'household',
                due_date: new Date().toISOString().slice(0, 10)
            },
            {
                title: '🛒 Haftalık Market Alışverişi',
                description: 'Temel gıda ihtiyaçları ve ev eşyaları için market alışverişi',
                frequency: 'weekly',
                priority: 'high',
                category: 'shopping',
                due_date: new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10)
            },
            {
                title: '⚡ Elektrik Faturası Öde',
                description: 'Aylık elektrik faturasını kontrol et ve öde',
                frequency: 'monthly',
                priority: 'high',
                category: 'external',
                due_date: new Date(Date.now() + 5 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10)
            },
            {
                title: '👕 Çamaşır Yıka ve As',
                description: 'Kirli çamaşırları yıka, kurutma makinesine at veya as',
                frequency: 'weekly',
                priority: 'medium',
                category: 'household',
                due_date: new Date(Date.now() + 1 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10)
            },
            {
                title: '🧹 Genel Ev Temizliği',
                description: 'Süpürme, silme, toz alma ve genel düzenleme',
                frequency: 'weekly',
                priority: 'medium',
                category: 'household',
                due_date: new Date(Date.now() + 3 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10)
            },
            {
                title: '🗄️ Mutfak Dolabı Düzenle',
                description: 'Mutfak dolabındaki ürünleri kontrol et, tarihleri kontrol et ve düzenle',
                frequency: 'weekly',
                priority: 'low',
                category: 'household',
                due_date: new Date(Date.now() + 4 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10)
            },
            {
                title: '🚿 Banyo ve Tuvalet Temizliği',
                description: 'Banyo ve tuvalet temizliği, dezenfeksiyon ve hijyen kontrolü',
                frequency: 'weekly',
                priority: 'medium',
                category: 'household',
                due_date: new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10)
            },
            {
                title: '🗑️ Çöp Torbaları Değiştir',
                description: 'Tüm odaların çöp torbalarını değiştir ve çöpü dışarı çıkar',
                frequency: 'weekly',
                priority: 'medium',
                category: 'household',
                due_date: new Date(Date.now() + 1 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10)
            },
            {
                title: '💧 Su Faturası Öde',
                description: 'Aylık su faturasını kontrol et ve zamanında öde',
                frequency: 'monthly',
                priority: 'high',
                category: 'external',
                due_date: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10)
            },
            {
                title: '👨‍⚕️ Doktor Randevusu Al',
                description: 'Yıllık genel sağlık kontrolü için doktor randevusu al',
                frequency: 'once',
                priority: 'medium',
                category: 'external',
                due_date: new Date(Date.now() + 10 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10)
            },
            {
                title: '🌱 Balkon Bitkileri Sula',
                description: 'Balkon ve iç mekan bitkilerini sula, yaprakları kontrol et',
                frequency: 'daily',
                priority: 'low',
                category: 'household',
                due_date: new Date().toISOString().slice(0, 10)
            },
            {
                title: '💊 Eczane Alışverişi',
                description: 'İlaç, vitamin ve sağlık ürünleri alışverişi',
                frequency: 'monthly',
                priority: 'medium',
                category: 'shopping',
                due_date: new Date(Date.now() + 6 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10)
            },
            {
                title: '🛏️ Yatak Çarşafları Değiştir',
                description: 'Tüm yatak çarşaflarını değiştir, yıka ve ütüle',
                frequency: 'weekly',
                priority: 'medium',
                category: 'household',
                due_date: new Date(Date.now() + 3 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10)
            },
            {
                title: '❄️ Buzdolabı Temizliği',
                description: 'Buzdolabını temizle, son kullanma tarihlerini kontrol et',
                frequency: 'monthly',
                priority: 'medium',
                category: 'household',
                due_date: new Date(Date.now() + 8 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10)
            },
            {
                title: '🚗 Araç Bakımı ve Kontrol',
                description: 'Araç periyodik bakımı, yağ değişimi ve genel kontrol',
                frequency: 'monthly',
                priority: 'high',
                category: 'maintenance',
                due_date: new Date(Date.now() + 12 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10)
            },
            {
                title: '🧽 Mutfak Derinlemesine Temizlik',
                description: 'Ocak, fırın, mikrodalga ve mutfak tezgahlarının detaylı temizliği',
                frequency: 'monthly',
                priority: 'medium',
                category: 'household',
                due_date: new Date(Date.now() + 9 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10)
            },
            {
                title: '📱 Telefon Faturası Öde',
                description: 'Aylık telefon ve internet faturalarını öde',
                frequency: 'monthly',
                priority: 'high',
                category: 'external',
                due_date: new Date(Date.now() + 11 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10)
            },
            {
                title: '🧴 Temizlik Malzemeleri Stok Kontrolü',
                description: 'Deterjan, sabun ve temizlik malzemelerinin stok durumunu kontrol et',
                frequency: 'weekly',
                priority: 'low',
                category: 'household',
                due_date: new Date(Date.now() + 5 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10)
            },
            {
                title: '🏠 Ev Güvenlik Kontrolü',
                description: 'Kapı kilitleri, pencere güvenliği ve alarm sistemini kontrol et',
                frequency: 'monthly',
                priority: 'medium',
                category: 'maintenance',
                due_date: new Date(Date.now() + 15 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10)
            },
            {
                title: '🎂 Doğum Günü Hazırlığı',
                description: 'Aile üyesinin doğum günü için hediye ve kutlama hazırlığı',
                frequency: 'once',
                priority: 'medium',
                category: 'external',
                due_date: new Date(Date.now() + 20 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10)
            }
        ];
        for (const task of sampleTasks) {
            await db.run('INSERT INTO tasks (family_id, title, description, created_by, frequency, due_date, priority, category, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)', [familyResult.lastID, task.title, task.description, 1, task.frequency, task.due_date, task.priority, task.category, 'pending']);
        }
        // Add sample inventory items
        const sampleInventory = [
            // Gıda Ürünleri
            { item_name: '🥛 Süt', quantity: 2, unit: 'lt', min_threshold: 1, category: 'food', location: 'Buzdolabı' },
            { item_name: '🍞 Ekmek', quantity: 1, unit: 'adet', min_threshold: 2, category: 'food', location: 'Mutfak' },
            { item_name: '🥚 Yumurta', quantity: 6, unit: 'adet', min_threshold: 12, category: 'food', location: 'Buzdolabı' },
            { item_name: '🫖 Çay', quantity: 1, unit: 'kutu', min_threshold: 2, category: 'food', location: 'Mutfak dolabı' },
            { item_name: '🍯 Şeker', quantity: 0.5, unit: 'kg', min_threshold: 1, category: 'food', location: 'Mutfak dolabı' },
            { item_name: '🍝 Makarna', quantity: 2, unit: 'paket', min_threshold: 3, category: 'food', location: 'Mutfak dolabı' },
            { item_name: '🍚 Pirinç', quantity: 1, unit: 'kg', min_threshold: 2, category: 'food', location: 'Mutfak dolabı' },
            { item_name: '🫒 Zeytinyağı', quantity: 0.8, unit: 'lt', min_threshold: 1, category: 'food', location: 'Mutfak dolabı' },
            { item_name: '🧂 Tuz', quantity: 0.3, unit: 'kg', min_threshold: 0.5, category: 'food', location: 'Mutfak dolabı' },
            { item_name: '🍅 Domates Salçası', quantity: 1, unit: 'kutu', min_threshold: 2, category: 'food', location: 'Mutfak dolabı' },
            { item_name: '🧀 Peynir', quantity: 0.2, unit: 'kg', min_threshold: 0.5, category: 'food', location: 'Buzdolabı' },
            { item_name: '🥛 Yogurt', quantity: 2, unit: 'kutu', min_threshold: 3, category: 'food', location: 'Buzdolabı' },
            { item_name: '🧈 Tereyağı', quantity: 1, unit: 'paket', min_threshold: 2, category: 'food', location: 'Buzdolabı' },
            { item_name: '🧅 Soğan', quantity: 1.5, unit: 'kg', min_threshold: 2, category: 'food', location: 'Mutfak' },
            { item_name: '🥔 Patates', quantity: 2, unit: 'kg', min_threshold: 3, category: 'food', location: 'Mutfak' },
            { item_name: '🍎 Elma', quantity: 1, unit: 'kg', min_threshold: 2, category: 'food', location: 'Mutfak' },
            { item_name: '🍌 Muz', quantity: 6, unit: 'adet', min_threshold: 10, category: 'food', location: 'Mutfak' },
            { item_name: '🥕 Havuç', quantity: 0.5, unit: 'kg', min_threshold: 1, category: 'food', location: 'Buzdolabı' },
            { item_name: '🥒 Salatalık', quantity: 3, unit: 'adet', min_threshold: 5, category: 'food', location: 'Buzdolabı' },
            { item_name: '🍋 Limon', quantity: 4, unit: 'adet', min_threshold: 6, category: 'food', location: 'Mutfak' },
            // Temizlik Ürünleri
            { item_name: '🧴 Deterjan', quantity: 0.5, unit: 'lt', min_threshold: 1, category: 'cleaning', location: 'Banyo' },
            { item_name: '🧽 Bulaşık Deterjanı', quantity: 0.3, unit: 'lt', min_threshold: 0.5, category: 'cleaning', location: 'Mutfak' },
            { item_name: '🧴 Çamaşır Suyu', quantity: 1, unit: 'lt', min_threshold: 2, category: 'cleaning', location: 'Banyo' },
            { item_name: '✨ Cam Temizleyici', quantity: 0.5, unit: 'lt', min_threshold: 1, category: 'cleaning', location: 'Temizlik dolabı' },
            { item_name: '🧽 Yer Temizleyici', quantity: 0.8, unit: 'lt', min_threshold: 1, category: 'cleaning', location: 'Temizlik dolabı' },
            { item_name: '🧻 Tuvalet Kağıdı', quantity: 2, unit: 'paket', min_threshold: 4, category: 'cleaning', location: 'Banyo' },
            { item_name: '🧻 Kağıt Havlu', quantity: 1, unit: 'paket', min_threshold: 2, category: 'cleaning', location: 'Mutfak' },
            { item_name: '🗑️ Çöp Torbası', quantity: 10, unit: 'adet', min_threshold: 20, category: 'cleaning', location: 'Temizlik dolabı' },
            { item_name: '🧹 Süpürge', quantity: 1, unit: 'adet', min_threshold: 1, category: 'cleaning', location: 'Temizlik dolabı' },
            { item_name: '🪣 Temizlik Kovası', quantity: 1, unit: 'adet', min_threshold: 1, category: 'cleaning', location: 'Temizlik dolabı' },
            // Kişisel Bakım
            { item_name: '🧼 Sabun', quantity: 1, unit: 'adet', min_threshold: 2, category: 'personal', location: 'Banyo' },
            { item_name: '🧴 Şampuan', quantity: 0.5, unit: 'şişe', min_threshold: 1, category: 'personal', location: 'Banyo' },
            { item_name: '🦷 Diş Macunu', quantity: 1, unit: 'tüp', min_threshold: 2, category: 'personal', location: 'Banyo' },
            { item_name: '🪥 Diş Fırçası', quantity: 2, unit: 'adet', min_threshold: 4, category: 'personal', location: 'Banyo' },
            { item_name: '🪒 Traş Bıçağı', quantity: 3, unit: 'adet', min_threshold: 5, category: 'personal', location: 'Banyo' },
            { item_name: '🧴 Duş Jeli', quantity: 1, unit: 'şişe', min_threshold: 2, category: 'personal', location: 'Banyo' },
            { item_name: '🧴 Saç Kremi', quantity: 0.7, unit: 'şişe', min_threshold: 1, category: 'personal', location: 'Banyo' },
            { item_name: '🧻 Islak Mendil', quantity: 2, unit: 'paket', min_threshold: 3, category: 'personal', location: 'Banyo' },
            // Ev Eşyaları
            { item_name: '💡 Ampul', quantity: 2, unit: 'adet', min_threshold: 4, category: 'household', location: 'Depo' },
            { item_name: '🔋 Pil AA', quantity: 4, unit: 'adet', min_threshold: 8, category: 'household', location: 'Depo' },
            { item_name: '🔋 Pil AAA', quantity: 2, unit: 'adet', min_threshold: 6, category: 'household', location: 'Depo' },
            { item_name: '🕯️ Mum', quantity: 3, unit: 'adet', min_threshold: 5, category: 'household', location: 'Depo' },
            { item_name: '🔦 El Feneri', quantity: 1, unit: 'adet', min_threshold: 2, category: 'household', location: 'Depo' },
            { item_name: '🧰 Tamir Seti', quantity: 1, unit: 'set', min_threshold: 1, category: 'household', location: 'Depo' },
            { item_name: '📏 Metre', quantity: 1, unit: 'adet', min_threshold: 1, category: 'household', location: 'Depo' },
            // Sağlık Ürünleri
            { item_name: '💊 Ağrı Kesici', quantity: 1, unit: 'kutu', min_threshold: 2, category: 'health', location: 'İlaç dolabı' },
            { item_name: '🍊 Vitamin C', quantity: 0.5, unit: 'kutu', min_threshold: 1, category: 'health', location: 'İlaç dolabı' },
            { item_name: '🩹 Yara Bandı', quantity: 1, unit: 'kutu', min_threshold: 2, category: 'health', location: 'İlaç dolabı' },
            { item_name: '🧴 Antiseptik', quantity: 1, unit: 'şişe', min_threshold: 2, category: 'health', location: 'İlaç dolabı' },
            { item_name: '🌡️ Ateş Ölçer', quantity: 1, unit: 'adet', min_threshold: 1, category: 'health', location: 'İlaç dolabı' },
            { item_name: '💊 Vitamin D', quantity: 1, unit: 'kutu', min_threshold: 1, category: 'health', location: 'İlaç dolabı' },
            { item_name: '🧴 El Dezenfektanı', quantity: 2, unit: 'şişe', min_threshold: 3, category: 'health', location: 'Giriş' }
        ];
        for (const item of sampleInventory) {
            await db.run('INSERT INTO inventory (family_id, item_name, quantity, unit, min_threshold, category, location, last_updated) VALUES (?, ?, ?, ?, ?, ?, ?, ?)', [familyResult.lastID, item.item_name, item.quantity, item.unit, item.min_threshold, item.category, item.location, 1]);
        }
        // Add sample budget categories
        const currentMonth = new Date().toISOString().slice(0, 7);
        const sampleBudgets = [
            { category: '🍽️ food', monthly_limit: 2000, spent_amount: 650 },
            { category: '🚗 transport', monthly_limit: 800, spent_amount: 320 },
            { category: '⚡ utilities', monthly_limit: 600, spent_amount: 480 },
            { category: '🎬 entertainment', monthly_limit: 400, spent_amount: 150 },
            { category: '🛒 shopping', monthly_limit: 500, spent_amount: 280 },
            { category: '🏥 health', monthly_limit: 300, spent_amount: 120 },
            { category: '👕 clothing', monthly_limit: 400, spent_amount: 200 }
        ];
        for (const budget of sampleBudgets) {
            await db.run('INSERT INTO budgets (family_id, category, monthly_limit, spent_amount, month_year) VALUES (?, ?, ?, ?, ?)', [familyResult.lastID, budget.category, budget.monthly_limit, budget.spent_amount, currentMonth]);
        }
        // Add sample expenses
        const sampleExpenses = [
            { amount: 150, category: '🍽️ food', description: 'Market alışverişi - temel gıdalar', expense_date: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10) },
            { amount: 80, category: '🚗 transport', description: 'Benzin - haftalık', expense_date: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10) },
            { amount: 200, category: '⚡ utilities', description: 'Elektrik faturası - aylık', expense_date: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10) },
            { amount: 120, category: '🍽️ food', description: 'Online market - meyve sebze', expense_date: new Date().toISOString().slice(0, 10) },
            { amount: 50, category: '🎬 entertainment', description: 'Sinema bileti - aile', expense_date: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10) },
            { amount: 75, category: '🏥 health', description: 'Eczane - vitamin ve ilaç', expense_date: new Date(Date.now() - 4 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10) },
            { amount: 180, category: '👕 clothing', description: 'Kış kıyafetleri', expense_date: new Date(Date.now() - 5 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10) }
        ];
        for (const expense of sampleExpenses) {
            await db.run('INSERT INTO expenses (family_id, recorded_by, amount, category, description, expense_date) VALUES (?, ?, ?, ?, ?, ?)', [familyResult.lastID, 1, expense.amount, expense.category, expense.description, expense.expense_date]);
        }
        console.log('✅ Sample data added successfully!');
        console.log('📊 Added: 20 tasks, 50 inventory items, 7 budget categories, 7 expenses');
        // Add sample messages
        const sampleMessages = [
            {
                message: '👋 Merhaba aile! Bugün market alışverişi yapacağım, eksik bir şey var mı?',
                is_group_message: true,
                created_at: new Date(Date.now() - 3 * 60 * 60 * 1000).toISOString()
            },
            {
                message: '🥛 Süt bitmiş, alır mısın lütfen?',
                is_group_message: true,
                created_at: new Date(Date.now() - 2.5 * 60 * 60 * 1000).toISOString()
            },
            {
                message: '✅ Tamam, süt ve ekmek alacağım. Başka bir şey?',
                is_group_message: true,
                created_at: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString()
            },
            {
                message: '🧻 Tuvalet kağıdı da bitmiş galiba',
                is_group_message: true,
                created_at: new Date(Date.now() - 1.5 * 60 * 60 * 1000).toISOString()
            },
            {
                message: '🛒 Market listesi hazır! Herkese teşekkürler 😊',
                is_group_message: true,
                created_at: new Date(Date.now() - 1 * 60 * 60 * 1000).toISOString()
            },
            {
                message: '🏠 Ev temizliği bugün yapılacak, yardım edebilir misiniz?',
                is_group_message: true,
                created_at: new Date(Date.now() - 4 * 60 * 60 * 1000).toISOString()
            },
            {
                message: '💡 Elektrik faturası gelmiş, bu ay biraz yüksek çıkmış',
                is_group_message: true,
                created_at: new Date(Date.now() - 6 * 60 * 60 * 1000).toISOString()
            },
            {
                message: '🌱 Balkon bitkilerini suladım, çok güzel büyüyorlar! 🌿',
                is_group_message: true,
                created_at: new Date(Date.now() - 30 * 60 * 1000).toISOString()
            },
            {
                message: '🍽️ Akşam yemeği için ne pişirelim? Önerilerinizi bekliyorum 👨‍🍳',
                is_group_message: true,
                created_at: new Date(Date.now() - 45 * 60 * 1000).toISOString()
            },
            {
                message: '🎉 Bu hafta sonu piknik yapalım mı? Hava çok güzel olacakmış! ☀️',
                is_group_message: true,
                created_at: new Date(Date.now() - 15 * 60 * 1000).toISOString()
            }
        ];
        for (const msg of sampleMessages) {
            await db.run('INSERT INTO messages (family_id, sender_id, message, is_group_message, created_at) VALUES (?, ?, ?, ?, ?)', [familyResult.lastID, 1, msg.message, msg.is_group_message, msg.created_at]);
        }
        console.log('💬 Added 10 sample messages with emojis');
    }
};
const getDatabase = async () => {
    if (!db) {
        await (0, exports.initDatabase)();
    }
    return db;
};
exports.getDatabase = getDatabase;
exports.default = { initDatabase: exports.initDatabase, getDatabase: exports.getDatabase };
//# sourceMappingURL=database.js.map