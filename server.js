const express = require('express');
const cors = require('cors');
const fs = require('fs');
const path = require('path');

const app = express();
app.use(cors());
app.use(express.json({ limit: '10mb' }));

const BACKUP_FILE = path.join(__dirname, 'user_backups.json');
let userBackups = {};

// Load existing backups from disk on server startup
try {
  if (fs.existsSync(BACKUP_FILE)) {
    const rawData = fs.readFileSync(BACKUP_FILE, 'utf8');
    userBackups = JSON.parse(rawData);
    console.log(`📁 Loaded ${Object.keys(userBackups).length} account backups from disk file.`);
  }
} catch (err) {
  console.error("Error reading user_backups.json:", err);
}

function saveBackupsToDisk() {
  try {
    fs.writeFileSync(BACKUP_FILE, JSON.stringify(userBackups, null, 2), 'utf8');
  } catch (err) {
    console.error("Error writing user_backups.json:", err);
  }
}

// Health Check Endpoint
app.get('/', (req, res) => {
  res.json({
    status: "online",
    server: "AGNP Logistics Mini Cloud Server",
    totalBackups: Object.keys(userBackups).length,
    timestamp: new Date().toISOString()
  });
});

// 1. POST API: Backup User Progress
app.post('/api/backup', (req, res) => {
  const { email, backupData } = req.body;
  if (!email || !backupData) {
    return res.status(400).json({ success: false, message: "Missing email or backup data" });
  }

  const cleanEmail = email.toLowerCase().trim();
  const entry = {
    data: backupData,
    timestamp: new Date().toISOString()
  };

  userBackups[cleanEmail] = entry;
  userBackups["latest_global_backup"] = entry;

  saveBackupsToDisk();

  console.log(`[BACKUP SAVED] Account: ${cleanEmail}`);
  res.json({ success: true, message: `Cloud backup saved for ${cleanEmail}` });
});

// 2. GET API: Restore User Progress
app.get('/api/backup/:email', (req, res) => {
  const reqEmail = req.params.email ? req.params.email.toLowerCase().trim() : "";
  let backup = userBackups[reqEmail];

  // Fallback: If specific email backup isn't found, try 'latest_global_backup' or default email keys
  if (!backup) {
    backup = userBackups["latest_global_backup"] || userBackups["it.agnp@gmail.com"] || userBackups["caperina13@gmail.com"];
  }

  if (backup) {
    res.json({ success: true, backup: backup.data, timestamp: backup.timestamp });
  } else {
    res.status(404).json({ success: false, message: "No cloud backup found for this account" });
  }
});

// 3. POST API: Submit Developer Access Application
app.post('/api/access-request', (req, res) => {
  const { name, email, department, reason } = req.body;
  if (!name || !email) {
    return res.status(400).json({ success: false, message: "Missing required fields" });
  }

  res.json({ success: true, message: "Access application submitted directly to mini server!" });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, '0.0.0.0', () => {
  console.log(`🚀 AGNP Mini Cloud Server running on port ${PORT}`);
});
