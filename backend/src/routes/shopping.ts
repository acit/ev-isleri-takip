import express from 'express';
import { authMiddleware } from '../middleware/auth';
import * as shoppingController from '../controllers/shopping';

const router = express.Router();

router.use(authMiddleware);

router.post('/', async (req, res) => {
  await shoppingController.createShoppingList(req as any, res);
});

router.get('/', async (req, res) => {
  await shoppingController.getShoppingLists(req as any, res);
});

router.get('/:listId/items', async (req, res) => {
  await shoppingController.getShoppingListItems(req as any, res);
});

router.patch('/items/:itemId', async (req, res) => {
  await shoppingController.updateShoppingListItem(req as any, res);
});

router.post('/generate-from-inventory', async (req, res) => {
  await shoppingController.generateShoppingListFromLowStock(req as any, res);
});

router.post('/:listId/send-email', async (req, res) => {
  await shoppingController.sendShoppingListByEmail(req as any, res);
});

export default router;
