import express from 'express';
import { authMiddleware } from '../middleware/auth';
import * as inventoryController from '../controllers/inventory';

const router = express.Router();

router.use(authMiddleware);

router.post('/', async (req, res) => {
  await inventoryController.addInventoryItem(req as any, res);
});

router.get('/low-stock', async (req, res) => {
  await inventoryController.getLowStockItems(req as any, res);
});

router.get('/', async (req, res) => {
  await inventoryController.getInventory(req as any, res);
});

router.patch('/:itemId', async (req, res) => {
  await inventoryController.updateInventoryItem(req as any, res);
});

router.delete('/:itemId', async (req, res) => {
  await inventoryController.deleteInventoryItem(req as any, res);
});

export default router;
