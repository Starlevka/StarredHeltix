const express = require('express');
const { MongoClient } = require('mongodb');
const app = express();
const port = 3000;

// Middleware
app.use(express.json());

// MongoDB connection URL and database name
const url = process.env.MONGODB_URL || 'mongodb://localhost:27017';
const dbName = 'starredheltix';
let db;

// Connect to MongoDB
MongoClient.connect(url, { useUnifiedTopology: true })
  .then(client => {
    console.log('Connected to MongoDB');
    db = client.db(dbName);
    
    // Create index on playerName for faster lookups
    return db.collection('players').createIndex({ "playerName": 1 }, { unique: true });
  })
  .then(() => {
    console.log('Index created on playerName');
  })
  .catch(error => {
    console.error('Failed to connect to MongoDB:', error);
    process.exit(1);
  });

// Register player endpoint
app.post('/api/register', async (req, res) => {
  try {
    const { playerName, version, timestamp } = req.body;
    
    // Validate input
    if (!playerName || !version) {
      return res.status(400).json({ 
        status: 'error', 
        message: 'Missing playerName or version' 
      });
    }
    
    if (!db) {
      return res.status(500).json({ 
        status: 'error', 
        message: 'Database not available' 
      });
    }
    
    const collection = db.collection('players');
    
    // Insert or update player info
    const result = await collection.updateOne(
      { playerName: playerName },
      { 
        $set: {
          playerName: playerName,
          version: version,
          lastSeen: timestamp || Date.now()
        }
      },
      { upsert: true }
    );
    
    console.log(`Registered player: ${playerName} with version ${version}`);
    
    // Clean up old entries (older than 1 hour)
    const oneHourAgo = Date.now() - (60 * 60 * 1000);
    await collection.deleteMany({ lastSeen: { $lt: oneHourAgo } });
    
    res.json({ 
      status: 'success', 
      message: 'Player registered successfully' 
    });
  } catch (error) {
    console.error('Error registering player:', error);
    res.status(500).json({ 
      status: 'error', 
      message: 'Internal server error' 
    });
  }
});

// Get players endpoint
app.get('/api/players', async (req, res) => {
  try {
    if (!db) {
      return res.status(500).json({ 
        status: 'error', 
        message: 'Database not available' 
      });
    }
    
    const collection = db.collection('players');
    const players = await collection.find({}).toArray();
    
    // Convert to the format expected by the mod
    const playerList = {};
    players.forEach(player => {
      playerList[player.playerName] = player.version;
    });
    
    console.log(`Returning ${Object.keys(playerList).length} players`);
    
    res.json({ players: playerList });
  } catch (error) {
    console.error('Error retrieving players:', error);
    res.status(500).json({ 
      status: 'error', 
      message: 'Internal server error' 
    });
  }
});

// Health check endpoint
app.get('/api/health', async (req, res) => {
  try {
    if (!db) {
      return res.status(500).json({ 
        status: 'error', 
        message: 'Database not available' 
      });
    }
    
    const collection = db.collection('players');
    const playerCount = await collection.countDocuments();
    
    res.json({ 
      status: 'ok', 
      uptime: process.uptime(),
      players: playerCount
    });
  } catch (error) {
    console.error('Error in health check:', error);
    res.status(500).json({ 
      status: 'error', 
      message: 'Internal server error' 
    });
  }
});

// Start the server
app.listen(port, '0.0.0.0', () => {
  console.log(`StarredHeltix Network Server with MongoDB listening at http://0.0.0.0:${port}`);
  console.log('Server is ready to handle requests');
});