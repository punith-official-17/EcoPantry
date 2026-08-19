import cron from 'node-cron';
import { query } from '../db/db.js';

export interface ExpiryAlert {
    user_id: string;
    user_name: string;
    user_email: string;
    item_id: string;
    item_name: string;
    category: string;
    quantity: string;
    expiry_date: string;
    hours_until_expiry: number;
}

/**
 * Query database for all active items expiring in the next 24-48 hours across all users
 */
export async function getExpiringAlerts(hoursMin: number = 0, hoursMax: number = 48): Promise<ExpiryAlert[]> {
    try {
        const sql = `
            SELECT 
                u.id AS user_id,
                u.name AS user_name,
                u.email AS user_email,
                i.id AS item_id,
                i.name AS item_name,
                i.category,
                i.quantity,
                i.expiry_date,
                ROUND(EXTRACT(EPOCH FROM (i.expiry_date::timestamp - CURRENT_DATE::timestamp)) / 3600) AS hours_until_expiry
            FROM items i
            JOIN users u ON i.user_id = u.id
            WHERE i.status = 'active'
              AND i.expiry_date >= CURRENT_DATE
              AND i.expiry_date <= (CURRENT_DATE + INTERVAL '2 days')
            ORDER BY i.expiry_date ASC, u.id ASC
        `;

        const result = await query<ExpiryAlert>(sql);
        return result.rows;
    } catch (error) {
        console.error('Error fetching expiring alerts:', error);
        return [];
    }
}

/**
 * Execute the daily 08:00 AM notification job
 */
export async function runDailyExpiryNotificationJob(): Promise<{ triggeredCount: number; alerts: ExpiryAlert[] }> {
    console.log('[CRON] Running daily expiry notification scan at 08:00 AM...');
    const alerts = await getExpiringAlerts(0, 48);

    // Group alerts by user to simulate sending consolidated notifications or emails
    const alertsByUser = new Map<string, ExpiryAlert[]>();
    for (const alert of alerts) {
        const list = alertsByUser.get(alert.user_email) || [];
        list.push(alert);
        alertsByUser.set(alert.user_email, list);
    }

    for (const [email, userAlerts] of alertsByUser.entries()) {
        console.log(`[ALERT] Notification dispatched to ${email}: ${userAlerts.length} item(s) expiring within 48h! [${userAlerts.map(a => a.item_name).join(', ')}]`);
    }

    return {
        triggeredCount: alerts.length,
        alerts
    };
}

/**
 * Start the cron scheduler (Daily at 08:00 AM: '0 8 * * *')
 */
export function startExpiryCronScheduler() {
    // Standard cron expression for 8:00 AM every day
    const cronSchedule = '0 8 * * *';

    cron.schedule(cronSchedule, async () => {
        try {
            await runDailyExpiryNotificationJob();
        } catch (err) {
            console.error('[CRON] Daily notification job failed:', err);
        }
    });

    console.log(`[CRON] Expiry notification scheduler registered with pattern "${cronSchedule}" (Daily at 08:00 AM).`);
}
