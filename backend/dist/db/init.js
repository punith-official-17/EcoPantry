"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const db_js_1 = require("./db.js");
async function run() {
    try {
        console.log('Connecting to database and applying schema...');
        await (0, db_js_1.initDb)();
        console.log('Schema migration complete!');
        process.exit(0);
    }
    catch (error) {
        console.error('Migration failed:', error);
        process.exit(1);
    }
    finally {
        await db_js_1.pool.end();
    }
}
run();
