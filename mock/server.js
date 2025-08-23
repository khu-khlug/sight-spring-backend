const express = require('express');
const app = express();

app.use(express.json());

// Mock auth endpoint
app.post('/internal/auth', (req, res) => {
  const cookies = req.headers.cookie;
  const apiKey = req.headers['x-api-key'];
  
  console.log('Auth request received:', { cookies, apiKey });
  
  // Mock successful authentication
  if (cookies && apiKey === 'test-api-key') {
    res.json({
      login: true,
      userId: 123
    });
  } else {
    res.json({
      login: false
    });
  }
});

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

const port = 3000;
app.listen(port, '0.0.0.0', () => {
  console.log(`Mock auth server running on port ${port}`);
});