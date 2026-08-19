import { initDb, pool } from './db.js';

async function run() {
    try {
        console.log('Connecting to database and applying schema...');
        await initDb();
        console.log('Schema migration complete!');
        process.exit(0);
    } catch (error) {
        console.error('Migration failed:', error);
        process.exit(1);
    } finally {
        await pool.end();
    }
}

run();
