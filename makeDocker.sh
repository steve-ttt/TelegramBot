#!/usr/bin/bash

CONFIG_FILE="bot2.json" # The name of your configuration file

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Configuration file '$CONFIG_FILE' not found in the current directory."
    exit 1
fi

# Stop and remove the old container if it exists, suppressing errors if it doesn't.
echo "Stopping and removing old container..."
docker stop MyTgBot > /dev/null 2>&1 || true
docker rm MyTgBot > /dev/null 2>&1 || true

# Create a local directory for persistent data if it doesn't exist
mkdir -p data

echo "Building Docker image..."
docker build -t ada-telegram-bot .

echo "Running new container with persistent data volume..."
# Run the container, mounting:
# 1. The local 'data' directory for persistent history.
# 2. The local config file (as read-only).
# We also set the DATA_PATH environment variable for the Java app.
# The command for the container passes the path to the mounted config file.
docker run -d \
  --name MyTgBot \
  -v "$(pwd)/data:/data" \
  -v "$(pwd)/$CONFIG_FILE:/app/$CONFIG_FILE:ro" \
  -e "DATA_PATH=/data" \
  ada-telegram-bot \
  "/app/$CONFIG_FILE"
