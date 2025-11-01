# StarredHeltix Network Server

This is the server component for the StarredHeltix Minecraft mod that allows players with the mod to find each other across different servers.

## Features

- Player registration endpoint
- Player discovery endpoint
- In-memory storage (for development)
- MongoDB integration (for production)

## Setup

### Prerequisites

- Node.js 18 or higher
- npm (comes with Node.js)

### Installation

1. Clone or download this repository
2. Navigate to this directory in your terminal
3. Install dependencies:
   ```
   npm install
   ```

### Running the Server

#### Development Mode
```
npm run dev
```

#### Production Mode
```
npm start
```

The server will start on port 3000 by default. You can change the port by setting the `PORT` environment variable.

## API Endpoints

### Register a Player
```
POST /api/register
Content-Type: application/json

{
  "playerName": "PlayerName",
  "version": "0.0.7",
  "timestamp": 1234567890000
}
```

### Get All Players
```
GET /api/players
```

Response:
```json
{
  "players": {
    "Player1": "0.0.7",
    "Player2": "0.0.6"
  }
}
```

### Health Check
```
GET /api/health
```

## Deployment Options

### Free Options

1. **Replit** - Browser-based IDE and hosting
2. **Render** - Free tier with $7/month credit
3. **Railway** - Free tier with $5/month credit

### With Database (Recommended for Production)

To use MongoDB:
1. Install the MongoDB driver:
   ```
   npm install mongodb
   ```
2. Set the `MONGODB_URL` environment variable to your MongoDB connection string
3. Run the server with:
   ```
   node server-db.js
   ```

## Environment Variables

- `PORT` - Port to run the server on (default: 3000)
- `MONGODB_URL` - MongoDB connection string (for server-db.js)

## License

This project is licensed under the MIT License.