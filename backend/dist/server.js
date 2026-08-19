"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = __importDefault(require("express"));
const cors_1 = __importDefault(require("cors"));
const dotenv_1 = __importDefault(require("dotenv"));
const authRoutes_js_1 = __importDefault(require("./routes/authRoutes.js"));
const itemsRoutes_js_1 = __importDefault(require("./routes/itemsRoutes.js"));
const aiRoutes_js_1 = __importDefault(require("./routes/aiRoutes.js"));
const notificationRoutes_js_1 = __importDefault(require("./routes/notificationRoutes.js"));
const db_js_1 = require("./db/db.js");
const cronService_js_1 = require("./services/cronService.js");
dotenv_1.default.config();
const app = (0, express_1.default)();
const PORT = process.env.PORT || 5000;
// Middlewares
app.use((0, cors_1.default)({
    origin: '*',
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization']
}));
// Generous payload size for base64 receipt scans
app.use(express_1.default.json({ limit: '20mb' }));
app.use(express_1.default.urlencoded({ extended: true, limit: '20mb' }));
// Request Logger (Development)
if (process.env.NODE_ENV !== 'production') {
    app.use((req, res, next) => {
        console.log(`[${new Date().toISOString()}] ${req.method} ${req.originalUrl}`);
        next();
    });
}
// Health check
app.get('/health', async (req, res) => {
    try {
        const dbCheck = await (0, db_js_1.query)('SELECT NOW()');
        res.status(200).json({
            status: 'healthy',
            service: 'Smart Pantry Backend API',
            timestamp: new Date().toISOString(),
            db_connected: true,
            db_time: dbCheck.rows[0].now
        });
    }
    catch (err) {
        res.status(500).json({
            status: 'unhealthy',
            service: 'Smart Pantry Backend API',
            db_connected: false,
            error: err.message
        });
    }
});
// API Routes
app.use('/api/auth', authRoutes_js_1.default);
app.use('/api/items', itemsRoutes_js_1.default);
app.use('/api/ai', aiRoutes_js_1.default);
app.use('/api/notifications', notificationRoutes_js_1.default);
// 404 Route Handler
app.use((req, res) => {
    res.status(404).json({
        success: false,
        error: `Endpoint '${req.method} ${req.originalUrl}' not found.`
    });
});
// Global Error Handler
app.use((err, req, res, next) => {
    console.error('Unhandled Application Error:', err);
    res.status(err.status || 500).json({
        success: false,
        error: err.message || 'Internal server error occurred.'
    });
});
// Server Initialization
async function bootstrap() {
    try {
        console.log('Initializing Smart Pantry Backend...');
        // Initialize DB schema
        await (0, db_js_1.initDb)().catch((err) => {
            console.warn('Database initialization warning (will retry on incoming queries):', err.message);
        });
        // Start 08:00 AM Cron Notification Scheduler
        (0, cronService_js_1.startExpiryCronScheduler)();
        app.listen(PORT, () => {
            console.log(`🚀 Smart Pantry Backend Server running on port ${PORT}`);
            console.log(`📡 Health Check: http://localhost:${PORT}/health`);
            console.log(`🔐 Auth API:    http://localhost:${PORT}/api/auth`);
            console.log(`📦 Items API:   http://localhost:${PORT}/api/items`);
            console.log(`✨ AI API:      http://localhost:${PORT}/api/ai`);
            console.log(`⏰ Alerts API:  http://localhost:${PORT}/api/notifications`);
        });
    }
    catch (error) {
        console.error('Failed to start server:', error);
        process.exit(1);
    }
}
bootstrap();
exports.default = app;
