const express = require('express');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(express.json({ limit: '10mb' }));

const userBackups = {};
const accessRequests = [];

app.get('/', (req, res) => {
  res.json({
    status: "online",
    server: "AGNP Logistics Mini Cloud Server",
    timestamp: new Date().toISOString()
  });
});

app.post('/api/backup', (req, res) => {
  const { email, backupData } = req.body;
  if (!email || !backupData) {
    return res.status(400).json({ success: false, message: "Missing email or backup data" });
  }

  userBackups[email] = {
    data: backupData,
    timestamp: new Date().toISOString()
  };

  console.log(`[BACKUP SAVED] Account: ${email}`);
  res.json({ success: true, message: `Cloud backup saved for ${email}` });
});

app.get('/api/backup/:email', (req, res) => {
  const email = req.params.email;
  const backup = userBackups[email];

  if (backup) {
    res.json({ success: true, backup: backup.data, timestamp: backup.timestamp });
  } else {
    res.status(404).json({ success: false, message: "No cloud backup found for this account" });
  }
});

app.post('/api/access-request', (req, res) => {
  const { name, email, department, reason } = req.body;
  if (!name || !email) {
    return res.status(400).json({ success: false, message: "Missing required fields" });
  }

  const requestEntry = {
    id: Date.now(),
    name,
    email,
    department: department || "Logistics",
    reason: reason || "Field Access",
    timestamp: new Date().toISOString()
  };

  accessRequests.push(requestEntry);
  console.log(`[ACCESS REQUEST] ${name} (${email})`);
  res.json({ success: true, message: "Access application submitted directly to mini server!" });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, '0.0.0.0', () => {
  console.log(`🚀 AGNP Mini Cloud Server running on port ${PORT}`);
});
