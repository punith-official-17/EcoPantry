import { Router } from 'express';
import { getUserExpiryAlerts, triggerDailyCronJob } from '../controllers/notificationController.js';
import { authenticateToken } from '../middleware/authMiddleware.js';

const router = Router();

router.get('/alerts', authenticateToken, getUserExpiryAlerts);
router.post('/run-cron', triggerDailyCronJob);

export default router;
