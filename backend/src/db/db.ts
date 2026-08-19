import pg from 'pg';
import dotenv from 'dotenv';
import fs from 'fs';
import path from 'path';

dotenv.config();

const { Pool } = pg;

const connectionString = process.env.DATABASE_URL || 'postgresql://postgres:postgres@localhost:5432/smart_pantry';

export const pool = new Pool({
    connectionString,
    ssl: process.env.NODE_ENV === 'production' && !connectionString.includes('localhost')
        ? { rejectUnauthorized: false }
        : false,
    max: 20,
    idleTimeoutMillis: 30000,
    connectionTimeoutMillis: 10000,
});

pool.on('error', (err) => {
    console.error('Unexpected error on idle PostgreSQL client', err);
});

export const query = async <T extends pg.QueryResultRow = any>(text: string, params?: any[]): Promise<pg.QueryResult<T>> => {
    const start = Date.now();
    try {
        const res = await pool.query<T>(text, params);
        const duration = Date.now() - start;
        if (process.env.NODE_ENV !== 'production') {
            console.log('Executed query', { text: text.substring(0, 100), duration, rows: res.rowCount });
        }
        return res;
    } catch (error) {
        console.error('Database query error:', error);
        throw error;
    }
};

export const initDb = async () => {
    try {
        const schemaPath = path.join(process.cwd(), 'src', 'db', 'schema.sql');
        if (fs.existsSync(schemaPath)) {
            const schemaSql = fs.readFileSync(schemaPath, 'utf8');
            await pool.query(schemaSql);
            console.log('Database tables & schema initialized successfully.');
        }
    } catch (err) {
        console.error('Failed to initialize database schema:', err);
        throw err;
    }
};
