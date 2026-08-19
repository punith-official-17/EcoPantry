import { Router } from 'express';
import multer from 'multer';
import { scanReceipt, suggestRecipes } from '../controllers/aiController.js';
import { authenticateToken } from '../middleware/authMiddleware.js';

const upload = multer({
    storage: multer.memoryStorage(),
    limits: { fileSize: 10 * 1024 * 1024 } // 10MB limit
});

const router = Router();

// All AI endpoints require authentication
router.use(authenticateToken);

router.post('/scan-receipt', upload.single('image'), scanReceipt);
router.post('/suggest-recipes', suggestRecipes);

export default router;
