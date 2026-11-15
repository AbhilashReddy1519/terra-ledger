const { ethers } = require("hardhat");

async function main() {
  const [deployer] = await ethers.getSigners();
  console.log("Deploying with:", deployer.address);

  const LandRegistry = await ethers.getContractFactory("LandRegistry");
  const registry = await LandRegistry.deploy();
  await registry.waitForDeployment();

  const address = await registry.getAddress();
  console.log("LandRegistry deployed to:", address);

  // Grant the second Hardhat account officer role (used by backend in dev)
  const signers = await ethers.getSigners();
  if (signers.length > 1) {
    const officerAddr = signers[1].address;
    await registry.grantOfficer(officerAddr);
    console.log("Officer granted:", officerAddr);
  }

  // Write address to a file so the backend can read it
  const fs = require("fs");
  const path = require("path");
  const out = { address, network: "hardhat", deployedAt: new Date().toISOString() };
  const outPath = path.join(__dirname, "..", "deployments", "localhost.json");
  fs.mkdirSync(path.dirname(outPath), { recursive: true });
  fs.writeFileSync(outPath, JSON.stringify(out, null, 2));
  console.log("Deployment info written to", outPath);
}

main().catch((e) => { console.error(e); process.exit(1); });
