import express from 'express';
import { authMiddleware } from '../middleware/auth';
import * as taskController from '../controllers/tasks';

const router = express.Router();

router.use(authMiddleware);

router.post('/', async (req, res) => {
  await taskController.createTask(req as any, res);
});

router.get('/by-frequency', async (req, res) => {
  await taskController.getTasksByFrequency(req as any, res);
});

router.get('/', async (req, res) => {
  await taskController.getTasks(req as any, res);
});

router.patch('/:taskId/complete', async (req, res) => {
  await taskController.completeTask(req as any, res);
});

router.patch('/:taskId', async (req, res) => {
  await taskController.updateTask(req as any, res);
});

export default router;
