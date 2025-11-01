#!/bin/bash
# start-server.sh
# Script to start the StarredHeltix Network Server

echo "Starting StarredHeltix Network Server..."

# Check if Node.js is installed
if ! command -v node &> /dev/null
then
    echo "Node.js is not installed. Please install Node.js to run the server."
    exit 1
fi

# Check if npm is installed
if ! command -v npm &> /dev/null
then
    echo "npm is not installed. Please install Node.js (which includes npm) to run the server."
    exit 1
fi

# Install dependencies if node_modules doesn't exist
if [ ! -d "node_modules" ]; then
    echo "Installing dependencies..."
    npm install
fi

# Start the server
echo "Server starting on http://localhost:3000"
echo "Press Ctrl+C to stop the server"
node server.js