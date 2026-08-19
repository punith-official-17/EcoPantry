"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.initDb = exports.query = exports.pool = void 0;
const pg_1 = __importDefault(require("pg"));
const dotenv_1 = __importDefault(require("dotenv"));
const fs_1 = __importDefault(require("fs"));
const path_1 = __importDefault(require("path"));
dotenv_1.default.config();
const { Pool } = pg_1.default;
const connectionString = process.env.DATABASE_URL || 'postgresql://postgres:postgres@localhost:5432/smart_pantry';
exports.pool = new Pool({
    connectionString,
    ssl: process.env.NODE_ENV === 'production' && !connectionString.includes('localhost')
        ? { rejectUnauthorized: false }
        : false,
    max: 20,
    idleTimeoutMillis: 30000,
    connectionTimeoutMillis: 10000,
});
exports.pool.on('error', (err) => {
    console.error('Unexpected error on idle PostgreSQL client', err);
});
const query = async (text, params) => {
    const start = Date.now();
    try {
        const res = await exports.pool.query(text, params);
        const duration = Date.now() - start;
        if (process.env.NODE_ENV !== 'production') {
            console.log('Executed query', { text: text.substring(0, 100), duration, rows: res.rowCount });
        }
        return res;
    }
    catch (error) {
        console.error('Database query error:', error);
        throw error;
    }
};
exports.query = query;
const initDb = async () => {
    try {
        const schemaPath = path_1.default.join(process.cwd(), 'src', 'db', 'schema.sql');
        if (fs_1.default.existsSync(schemaPath)) {
            const schemaSql = fs_1.default.readFileSync(schemaPath, 'utf8');
            await exports.pool.query(schemaSql);
            console.log('Database tables & schema initialized successfully.');
        }
    }
    catch (err) {
        console.error('Failed to initialize database schema:', err);
        throw err;
    }
};
exports.initDb = initDb;
