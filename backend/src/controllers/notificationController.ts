import { Request, Response } from 'express';
import { query } from '../db/db.js';
import { runDailyExpiryNotificationJob } from '../services/cronService.js';

export async function getUserExpiryAlerts(req: Request, res: Response) {
    try {
        const userId = req.user!.id;

        const sql = `
            SELECT 
                i.id AS item_id,
                i.name AS item_name,
                i.category,
                i.quantity,
                i.expiry_date,
                (i.expiry_date - CURRENT_DATE) AS days_until_expiry,
                CASE
                    WHEN i.expiry_date < CURRENT_DATE THEN 'Expired'
                    WHEN i.expiry_date = CURRENT_DATE THEN 'Expires Today'
                    WHEN i.expiry_date = CURRENT_DATE + 1 THEN 'Expires Tomorrow (24h)'
                    ELSE 'Expires in 48h'
                END AS urgency_label
            FROM items i
            WHERE i.user_id = $1
              AND i.status = 'active'
              AND i.expiry_date <= (CURRENT_DATE + INTERVAL '2 days')
            ORDER BY i.expiry_date ASC
        `;

        const result = await query(sql, [userId]);

        return res.status(200).json({
            success: true,
            alert_count: result.rows.length,
            data: result.rows
        });
    } catch (error) {
        console.error('Fetch alerts error:', error);
        return res.status(500).json({
            success: false,
            error: 'Failed to fetch expiration alerts'
        });
    }
}

export async function triggerDailyCronJob(req: Request, res: Response) {
    try {
        const result = await runDailyExpiryNotificationJob();
        return res.status(200).json({
            success: true,
            message: `Daily 08:00 AM notification job executed successfully.`,
            triggered_alerts_count: result.triggeredCount,
            alerts: result.alerts
        });
    } catch (error) {
        console.error('Trigger cron error:', error);
        return res.status(500).json({
            success: false,
            error: 'Failed to execute notification cron job'
        });
    }
}
