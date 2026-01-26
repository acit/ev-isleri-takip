-- Users table
CREATE TABLE IF NOT EXISTS users (
  id SERIAL PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(255) NOT NULL,
  family_id INTEGER,
  role VARCHAR(50) DEFAULT 'member', -- admin, member
  status VARCHAR(50) DEFAULT 'pending', -- pending, active, inactive
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Family Groups table
CREATE TABLE IF NOT EXISTS families (
  id SERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  created_by INTEGER NOT NULL REFERENCES users(id),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Invite Tokens (for email invitations)
CREATE TABLE IF NOT EXISTS invite_tokens (
  id SERIAL PRIMARY KEY,
  email VARCHAR(255) NOT NULL,
  token VARCHAR(255) UNIQUE NOT NULL,
  family_id INTEGER NOT NULL REFERENCES families(id),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP NOT NULL
);

-- Tasks table
CREATE TABLE IF NOT EXISTS tasks (
  id SERIAL PRIMARY KEY,
  family_id INTEGER NOT NULL REFERENCES families(id),
  title VARCHAR(255) NOT NULL,
  description TEXT,
  assigned_to INTEGER REFERENCES users(id),
  created_by INTEGER NOT NULL REFERENCES users(id),
  frequency VARCHAR(50) NOT NULL, -- daily, weekly, monthly, quarterly, biannual, yearly, once
  recurrence_data JSONB, -- stores recurrence details
  due_date DATE,
  completed BOOLEAN DEFAULT FALSE,
  completed_by INTEGER REFERENCES users(id),
  completed_at TIMESTAMP,
  priority VARCHAR(50) DEFAULT 'medium', -- low, medium, high
  status VARCHAR(50) DEFAULT 'pending', -- pending, in_progress, completed, cancelled
  category VARCHAR(50), -- household, external, shopping, maintenance, etc.
  estimated_cost DECIMAL(10, 2),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Inventory/Stock table
CREATE TABLE IF NOT EXISTS inventory (
  id SERIAL PRIMARY KEY,
  family_id INTEGER NOT NULL REFERENCES families(id),
  item_name VARCHAR(255) NOT NULL,
  quantity DECIMAL(10, 2) NOT NULL,
  unit VARCHAR(50), -- pieces, kg, liter, etc.
  min_threshold DECIMAL(10, 2),
  category VARCHAR(100),
  location VARCHAR(255),
  notes TEXT,
  last_updated INTEGER REFERENCES users(id),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Shopping Lists table
CREATE TABLE IF NOT EXISTS shopping_lists (
  id SERIAL PRIMARY KEY,
  family_id INTEGER NOT NULL REFERENCES families(id),
  created_by INTEGER NOT NULL REFERENCES users(id),
  title VARCHAR(255),
  auto_generated BOOLEAN DEFAULT FALSE,
  sent_via_email BOOLEAN DEFAULT FALSE,
  sent_at TIMESTAMP,
  status VARCHAR(50) DEFAULT 'draft', -- draft, sent, completed
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Shopping List Items
CREATE TABLE IF NOT EXISTS shopping_list_items (
  id SERIAL PRIMARY KEY,
  shopping_list_id INTEGER NOT NULL REFERENCES shopping_lists(id) ON DELETE CASCADE,
  item_name VARCHAR(255) NOT NULL,
  quantity DECIMAL(10, 2),
  unit VARCHAR(50),
  estimated_cost DECIMAL(10, 2),
  checked BOOLEAN DEFAULT FALSE,
  checked_by INTEGER REFERENCES users(id),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Budget/Financial Tracking table
CREATE TABLE IF NOT EXISTS budgets (
  id SERIAL PRIMARY KEY,
  family_id INTEGER NOT NULL REFERENCES families(id),
  category VARCHAR(100) NOT NULL,
  monthly_limit DECIMAL(10, 2),
  spent_amount DECIMAL(10, 2) DEFAULT 0,
  month_year VARCHAR(7), -- YYYY-MM format
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(family_id, category, month_year)
);

-- Expenses table
CREATE TABLE IF NOT EXISTS expenses (
  id SERIAL PRIMARY KEY,
  family_id INTEGER NOT NULL REFERENCES families(id),
  recorded_by INTEGER NOT NULL REFERENCES users(id),
  amount DECIMAL(10, 2) NOT NULL,
  category VARCHAR(100),
  description TEXT,
  task_id INTEGER REFERENCES tasks(id),
  expense_date DATE NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Task History (for tracking completion)
CREATE TABLE IF NOT EXISTS task_history (
  id SERIAL PRIMARY KEY,
  task_id INTEGER NOT NULL REFERENCES tasks(id),
  completed_by INTEGER REFERENCES users(id),
  completed_date DATE NOT NULL,
  notes TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Login codes table for email authentication
CREATE TABLE IF NOT EXISTS login_codes (
  id SERIAL PRIMARY KEY,
  email VARCHAR(255) NOT NULL,
  code VARCHAR(6) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Messages table for family communication
CREATE TABLE IF NOT EXISTS messages (
  id SERIAL PRIMARY KEY,
  family_id INTEGER NOT NULL REFERENCES families(id),
  sender_id INTEGER NOT NULL REFERENCES users(id),
  recipient_id INTEGER REFERENCES users(id), -- NULL for family-wide messages
  message TEXT NOT NULL,
  message_type VARCHAR(50) DEFAULT 'text', -- text, system, notification
  is_read BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_users_family_id ON users(family_id);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_tasks_family_id ON tasks(family_id);
CREATE INDEX IF NOT EXISTS idx_tasks_assigned_to ON tasks(assigned_to);
CREATE INDEX IF NOT EXISTS idx_tasks_due_date ON tasks(due_date);
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);
CREATE INDEX IF NOT EXISTS idx_tasks_created_by ON tasks(created_by);
CREATE INDEX IF NOT EXISTS idx_inventory_family_id ON inventory(family_id);
CREATE INDEX IF NOT EXISTS idx_shopping_lists_family_id ON shopping_lists(family_id);
CREATE INDEX IF NOT EXISTS idx_shopping_lists_status ON shopping_lists(status);
CREATE INDEX IF NOT EXISTS idx_budgets_family_id ON budgets(family_id);
CREATE INDEX IF NOT EXISTS idx_expenses_family_id ON expenses(family_id);
CREATE INDEX IF NOT EXISTS idx_expenses_category ON expenses(category);
CREATE INDEX IF NOT EXISTS idx_login_codes_email ON login_codes(email);
CREATE INDEX IF NOT EXISTS idx_login_codes_expires_at ON login_codes(expires_at);
