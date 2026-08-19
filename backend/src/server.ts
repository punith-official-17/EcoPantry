import express, { Request, Response, NextFunction } from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import authRoutes from './routes/authRoutes.js';
import itemsRoutes from './routes/itemsRoutes.js';
import aiRoutes from './routes/aiRoutes.js';
import notificationRoutes from './routes/notificationRoutes.js';
import { initDb, query } from './db/db.js';
import { startExpiryCronScheduler } from './services/cronService.js';

dotenv.config();

const app = express();
const PORT = process.env.PORT || 5000;

// Middlewares
app.use(cors({
    origin: '*',
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization']
}));

// Generous payload size for base64 receipt scans
app.use(express.json({ limit: '20mb' }));
app.use(express.urlencoded({ extended: true, limit: '20mb' }));

// Request Logger (Development)
if (process.env.NODE_ENV !== 'production') {
    app.use((req, res, next) => {
        console.log(`[${new Date().toISOString()}] ${req.method} ${req.originalUrl}`);
        next();
    });
}

// Health check
app.get('/health', async (req: Request, res: Response) => {
    try {
        const dbCheck = await query('SELECT NOW()');
        res.status(200).json({
            status: 'healthy',
            service: 'Smart Pantry Backend API',
            timestamp: new Date().toISOString(),
            db_connected: true,
            db_time: dbCheck.rows[0].now
        });
    } catch (err: any) {
        res.status(500).json({
            status: 'unhealthy',
            service: 'Smart Pantry Backend API',
            db_connected: false,
            error: err.message
        });
    }
});

// API Routes
app.use('/api/auth', authRoutes);
app.use('/api/items', itemsRoutes);
app.use('/api/ai', aiRoutes);
app.use('/api/notifications', notificationRoutes);

// 404 Route Handler
app.use((req: Request, res: Response) => {
    res.status(404).json({
        success: false,
        error: `Endpoint '${req.method} ${req.originalUrl}' not found.`
    });
});

// Global Error Handler
app.use((err: any, req: Request, res: Response, next: NextFunction) => {
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
        await initDb().catch((err) => {
            console.warn('Database initialization warning (will retry on incoming queries):', err.message);
        });

        // Start 08:00 AM Cron Notification Scheduler
        startExpiryCronScheduler();

        app.listen(PORT, () => {
            console.log(`🚀 Smart Pantry Backend Server running on port ${PORT}`);
            console.log(`📡 Health Check: http://localhost:${PORT}/health`);
            console.log(`🔐 Auth API:    http://localhost:${PORT}/api/auth`);
            console.log(`📦 Items API:   http://localhost:${PORT}/api/items`);
            console.log(`✨ AI API:      http://localhost:${PORT}/api/ai`);
            console.log(`⏰ Alerts API:  http://localhost:${PORT}/api/notifications`);
        });
    } catch (error) {
        console.error('Failed to start server:', error);
        process.exit(1);
    }
}

bootstrap();

export default app;
