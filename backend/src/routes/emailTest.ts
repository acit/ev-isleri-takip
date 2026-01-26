import express from 'express';
import * as emailTestController from '../controllers/emailTest';

const router = express.Router();

// Test email connection
router.post('/test', emailTestController.testEmailConnection);

// Get email configuration status
router.get('/status', emailTestController.getEmailStatus);

export default router;