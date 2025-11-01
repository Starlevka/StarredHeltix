const express = require('express');
const app = express();
const port = process.env.PORT || 3000; // Use the port provided by the hosting service

// Middleware to parse JSON bodies
app.use(express.json());

// In-memory storage for players (in production, use a database)
let players = {};

// Register player endpoint
app.post('/api/register', (req, res) => {
  try {
    const { playerName, version, timestamp } = req.body;
    
    // Validate input
    if (!playerName || !version) {
      return res.status(400).json({ 
        status: 'error', 
        message: 'Missing playerName or version' 
      });
    }
    
    // Store player info with current timestamp if not provided
    players[playerName] = {
      version: version,
      lastSeen: timestamp || Date.now()
    };
    
    console.log(`Registered player: ${playerName} with version ${version}`);
    
    // Clean up old entries (older than 1 hour)
    const oneHourAgo = Date.now() - (60 * 60 * 1000);
    Object.keys(players).forEach(player => {
      if (players[player].lastSeen < oneHourAgo) {
        console.log(`Removed expired player: ${player}`);
        delete players[player];
      }
    });
    
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
app.get('/api/players', (req, res) => {
  try {
    // Convert to the format expected by the mod
    const playerList = {};
    Object.keys(players).forEach(player => {
      playerList[player] = players[player].version;
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
app.get('/api/health', (req, res) => {
  res.json({ 
    status: 'ok', 
    uptime: process.uptime(),
    players: Object.keys(players).length
  });
});

// Serve a simple page at the root
app.get('/', (req, res) => {
  res.send(`
    <h1>StarredHeltix Network Server</h1>
    <p>Server is running successfully!</p>
    <p>API endpoints:</p>
    <ul>
      <li>POST /api/register - Register a player</li>
      <li>GET /api/players - Get all players</li>
      <li>GET /api/health - Health check</li>
    </ul>
  `);
});

// Start the server
app.listen(port, '0.0.0.0', () => {
  console.log(`StarredHeltix Network Server listening at http://0.0.0.0:${port}`);
  console.log('Server is ready to handle requests');
});