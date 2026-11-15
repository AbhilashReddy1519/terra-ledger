#!/bin/sh
set -e

npx hardhat node --hostname 0.0.0.0 &
NODE_PID=$!

echo "Waiting for Hardhat node to listen on port 8545..."
until wget -qO- --post-data='{"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id":1}' --header='Content-Type: application/json' http://127.0.0.1:8545 >/dev/null 2>&1; do
  sleep 1
done

echo "Hardhat node ready! Deploying contracts..."
npx hardhat run scripts/deploy.js --network localhost

echo "LandRegistry deployed. Keeping Hardhat node alive."
wait $NODE_PID
