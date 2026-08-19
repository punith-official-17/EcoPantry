import { Router } from 'express';
import { getItems, createItem, updateItem, deleteItem } from '../controllers/itemsController.js';
import { authenticateToken } from '../middleware/authMiddleware.js';

const router = Router();

// All item endpoints require authentication
router.use(authenticateToken);

router.get('/', getItems);
router.post('/', createItem);
router.put('/:id', updateItem);
router.delete('/:id', deleteItem);

export default router;
