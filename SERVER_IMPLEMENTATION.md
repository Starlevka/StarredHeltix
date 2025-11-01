# StarredHeltix Network Server Implementation Guide

This document explains how to implement a server for the StarredHeltix mod network functionality.

## Overview

The StarredHeltix mod includes a network manager that can communicate with a custom server to:
1. Register players who have the mod installed
2. Discover other players with the mod across different servers

## Server API Endpoints

The mod expects the server to implement two main endpoints:

### 1. Player Registration Endpoint

**Endpoint**: `POST /api/register`
**Content-Type**: `application/json`

**Request Body**:
```json
{
  "playerName": "PlayerName",
  "version": "0.0.7",
  "timestamp": 1234567890000
}
```

**Response**:
```json
{
  "status": "success",
  "message": "Player registered"
}
```

### 2. Player Discovery Endpoint

**Endpoint**: `GET /api/players`
**Content-Type**: `application/json`

**Response**:
```json
{
  "players": {
    "Player1": "0.0.7",
    "Player2": "0.0.6",
    "Player3": "0.0.7"
  }
}
```

## Sample Server Implementation

Here's a simple Node.js implementation using Express:

```javascript
const express = require('express');
const app = express();
const port = 3000;

// Middleware
app.use(express.json());

// In-memory storage (use a database in production)
let players = {};

// Register player endpoint
app.post('/api/register', (req, res) => {
  const { playerName, version, timestamp } = req.body;
  
  if (!playerName || !version) {
    return res.status(400).json({ 
      status: 'error', 
      message: 'Missing playerName or version' 
    });
  }
  
  // Store player info
  players[playerName] = {
    version: version,
    lastSeen: timestamp || Date.now()
  };
  
  // Clean up old entries (older than 1 hour)
  const oneHourAgo = Date.now() - (60 * 60 * 1000);
  Object.keys(players).forEach(player => {
    if (players[player].lastSeen < oneHourAgo) {
      delete players[player];
    }
  });
  
  res.json({ 
    status: 'success', 
    message: 'Player registered' 
  });
});

// Get players endpoint
app.get('/api/players', (req, res) => {
  // Convert to the format expected by the mod
  const playerList = {};
  Object.keys(players).forEach(player => {
    playerList[player] = players[player].version;
  });
  
  res.json({ players: playerList });
});

app.listen(port, () => {
  console.log(`StarredHeltix Network Server listening at http://localhost:${port}`);
});
```

## Configuration

To use your custom server, you need to update the `SERVER_URL` constant in the [ModNetworkManager.java](file:///C:/Users/lev/Documents/StarredHeltix/src/client/java/set/starlev/starredheltix/util/ModNetworkManager.java#L17-L17) file:

```java
private static final String SERVER_URL = "https://your-server-url.com/api";
```

Replace `https://your-server-url.com/api` with your actual server URL.

## Security Considerations

1. **Authentication**: Consider adding authentication to prevent unauthorized player registrations
2. **Rate Limiting**: Implement rate limiting to prevent abuse
3. **Data Validation**: Validate all incoming data
4. **HTTPS**: Use HTTPS in production to encrypt data in transit

## Database Integration

For production use, replace the in-memory storage with a database solution like:
- MongoDB
- PostgreSQL
- MySQL
- Redis

Example with MongoDB:

```javascript
const { MongoClient } = require('mongodb');

// Connection URL
const url = 'mongodb://localhost:27017';
const dbName = 'starredheltix';
let db;

// Connect to MongoDB
MongoClient.connect(url, { useUnifiedTopology: true }, (err, client) => {
  if (err) return console.error(err);
  console.log('Connected to MongoDB');
  db = client.db(dbName);
});

// Register player endpoint with MongoDB
app.post('/api/register', async (req, res) => {
  const { playerName, version, timestamp } = req.body;
  
  if (!playerName || !version) {
    return res.status(400).json({ 
      status: 'error', 
      message: 'Missing playerName or version' 
    });
  }
  
  try {
    const collection = db.collection('players');
    await collection.updateOne(
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
    
    res.json({ 
      status: 'success', 
      message: 'Player registered' 
    });
  } catch (err) {
    res.status(500).json({ 
      status: 'error', 
      message: 'Database error' 
    });
  }
});
```

## Usage in the Mod

Players with the mod can now use the following commands:

1. `/sh_check <player>` - Check if a specific player has the mod
2. `/sh_check find` - Find all online players with the mod

Only moderators (as defined in the mod code) can use these commands.