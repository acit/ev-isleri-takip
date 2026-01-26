import express from 'express';
import { authMiddleware } from '../middleware/auth';
import * as budgetController from '../controllers/budget';

const router = express.Router();

router.use(authMiddleware);

router.get('/', async (req, res) => {
  await budgetController.getBudget(req as any, res);
});

router.post('/set-limit', async (req, res) => {
  await budgetController.setBudgetLimit(req as any, res);
});

router.post('/expenses', async (req, res) => {
  await budgetController.recordExpense(req as any, res);
});

router.get('/expenses', async (req, res) => {
  await budgetController.getExpenses(req as any, res);
});

export default router;
