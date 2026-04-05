#!/bin/bash
# Docker Cleanup Script for Biashara360
# Use this script to forcefully clean up stuck containers

set -e

echo "🧹 Starting Biashara360 Docker Cleanup..."

# Step 1: Try graceful shutdown
echo "📍 Step 1: Attempting graceful container shutdown..."
cd /home/sirpatrick/Downloads/biashara360-COMPLETE\(8\)/b360-complete/backend 2>/dev/null || cd .

docker-compose down --remove-orphans 2>/dev/null || echo "⚠️  docker-compose down failed (this may be expected)"

# Step 2: Wait a bit
echo "⏳ Waiting 5 seconds..."
sleep 5

# Step 3: Prune containers
echo "📍 Step 2: Pruning Docker containers..."
docker container prune -f 2>/dev/null || echo "⚠️  Prune failed"

# Step 4: Restart Docker daemon
echo "📍 Step 3: Restarting Docker daemon..."
sudo systemctl restart docker
sleep 3

# Step 5: Verify cleanup
echo "📍 Step 4: Verifying cleanup..."
RUNNING=$(docker ps -a 2>/dev/null | grep -c "biashara" || echo "0")
if [ "$RUNNING" -eq "0" ]; then
    echo "✅ All Biashara360 containers cleaned up!"
else
    echo "⚠️  Warning: Some containers still exist. Attempting aggressive removal..."
    CONTAINER_IDS=$(docker ps -a | grep "biashara" | awk '{print $1}')
    for id in $CONTAINER_IDS; do
        echo "   Removing container: $id"
        docker rm -f "$id" 2>/dev/null || echo "   ⚠️  Could not remove $id (may require sudo)"
    done
fi

echo ""
echo "🚀 Cleanup complete! You can now run:"
echo "   cd /home/sirpatrick/Downloads/biashara360-COMPLETE\(8\)/b360-complete/backend"
echo "   docker-compose up --build"
echo ""

