"use strict";
// ⚠️ Tracing setup: Uncomment the line below after configuring OpenTelemetry
// import './tracing';
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = __importDefault(require("express"));
const cors_1 = __importDefault(require("cors"));
const body_parser_1 = __importDefault(require("body-parser"));
const dotenv_1 = __importDefault(require("dotenv"));
const database_1 = require("./config/database");
// Routes
const auth_1 = __importDefault(require("./routes/auth"));
const tasks_1 = __importDefault(require("./routes/tasks"));
const inventory_1 = __importDefault(require("./routes/inventory"));
const shopping_1 = __importDefault(require("./routes/shopping"));
const budget_1 = __importDefault(require("./routes/budget"));
const messages_1 = __importDefault(require("./routes/messages"));
const emailTest_1 = __importDefault(require("./routes/emailTest"));
dotenv_1.default.config();
const app = (0, express_1.default)();
const PORT = process.env.PORT || 5000;
// Initialize database
const startServer = async () => {
    try {
        await (0, database_1.initDatabase)();
        console.log('Database initialized successfully');
        // Middleware
        app.use((0, cors_1.default)());
        app.use(body_parser_1.default.json());
        app.use(body_parser_1.default.urlencoded({ extended: true }));
        // Routes
        app.use('/api/auth', auth_1.default);
        app.use('/api/tasks', tasks_1.default);
        app.use('/api/inventory', inventory_1.default);
        app.use('/api/shopping', shopping_1.default);
        app.use('/api/budget', budget_1.default);
        app.use('/api/messages', messages_1.default);
        app.use('/api/email', emailTest_1.default);
        // Health check
        app.get('/api/health', (req, res) => {
            res.json({ status: 'ok', timestamp: new Date().toISOString() });
        });
        app.listen(PORT, () => {
            console.log(`Server running on port ${PORT}`);
        });
    }
    catch (error) {
        console.error('Failed to start server:', error);
        process.exit(1);
    }
};
startServer();
//# sourceMappingURL=index.js.map