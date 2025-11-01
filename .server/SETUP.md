# Server Setup Guide

This guide will help you set up the StarredHeltix Network Server.

## Quick Start (Development)

1. Make sure you have Node.js installed (version 18 or higher)
2. Open a terminal in the `server` directory
3. Install dependencies:
   ```
   npm install
   ```
4. Start the server:
   ```
   npm start
   ```

Your server should now be running at `http://localhost:3000`

## Deploying to Replit (Free)

1. Go to [replit.com](https://replit.com) and sign up
2. Create a new repl
3. Choose "Import from GitHub" and use this repository, or:
   - Create a new Node.js repl
   - Copy all files from the `server` directory to your repl
4. Click "Run"

Replit will automatically detect it's a Node.js app and start the server.

## Deploying to Render (Free Tier)

1. Push this code to a GitHub repository
2. Sign up at [render.com](https://render.com)
3. Create a new Web Service
4. Connect your GitHub repository
5. Set:
   - Build command: `npm install`
   - Start command: `npm start`
6. Deploy

## Using with MongoDB (Recommended for Production)

1. Get a free MongoDB database:
   - Go to [mongodb.com/cloud/atlas](https://www.mongodb.com/cloud/atlas)
   - Sign up and create a free cluster
   - Get your connection string
2. Set the `MONGODB_URL` environment variable to your connection string
3. Run the server with:
   ```
   node server-db.js
   ```

## Updating the Mod

Once your server is running, you'll need to update the server URL in the mod:

1. Open `src/client/java/set/starlev/starredheltix/util/ModNetworkManager.java`
2. Change the `SERVER_URL` value to your server's URL:
   ```java
   private static final String SERVER_URL = "https://your-server-url.com/api";
   ```
3. Rebuild the mod

## Testing the Server

You can test your server using curl or any HTTP client:

### Register a player:
```bash
curl -X POST http://localhost:3000/api/register \
  -H "Content-Type: application/json" \
  -d '{"playerName": "TestPlayer", "version": "0.0.7"}'
```

### Get all players:
```bash
curl http://localhost:3000/api/players
```

## Troubleshooting

### Server won't start
- Make sure Node.js is installed
- Check that port 3000 is not in use
- Check the console for error messages

### Players aren't registering
- Check that the server URL in the mod is correct
- Check the server logs for errors
- Make sure the mod can reach the server (firewall, etc.)

### Server returns errors
- Check the server logs for detailed error messages
- Make sure all dependencies are installed