// ⚠️ Tracing setup: Uncomment the line below after configuring OpenTelemetry
// import './tracing';

import express, { Express } from 'express';
import cors from 'cors';
import bodyParser from 'body-parser';
import dotenv from 'dotenv';
import { initDatabase } from './config/database';

// Routes
import authRoutes from './routes/auth';
import taskRoutes from './routes/tasks';
import inventoryRoutes from './routes/inventory';
import shoppingRoutes from './routes/shopping';
import budgetRoutes from './routes/budget';
import messagesRoutes from './routes/messages';
import emailTestRoutes from './routes/emailTest';

dotenv.config();

const app: Express = express();
const PORT = process.env.PORT || 5000;

// Initialize database
const startServer = async () => {
  try {
    await initDatabase();
    console.log('Database initialized successfully');
    
    // Middleware
    app.use(cors());
    app.use(bodyParser.json());
    app.use(bodyParser.urlencoded({ extended: true }));

    // Routes
    app.use('/api/auth', authRoutes);
    app.use('/api/tasks', taskRoutes);
    app.use('/api/inventory', inventoryRoutes);
    app.use('/api/shopping', shoppingRoutes);
    app.use('/api/budget', budgetRoutes);
    app.use('/api/messages', messagesRoutes);
    app.use('/api/email', emailTestRoutes);

    // Health check
    app.get('/api/health', (req, res) => {
      res.json({ status: 'ok', timestamp: new Date().toISOString() });
    });

    app.listen(PORT, () => {
      console.log(`Server running on port ${PORT}`);
    });
  } catch (error) {
    console.error('Failed to start server:', error);
    process.exit(1);
  }
};

startServer();
