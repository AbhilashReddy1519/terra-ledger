const { ethers } = require("hardhat");

async function main() {
  const [deployer] = await ethers.getSigners();
  console.log("Deploying with:", deployer.address);

  const LandRegistry = await ethers.getContractFactory("LandRegistry");
  const registry = await LandRegistry.deploy();
  await registry.waitForDeployment();

  const address = await registry.getAddress();
  console.log("LandRegistry deployed to:", address);

  // Grant deployer and second Hardhat account officer & registrar roles
  await registry.grantOfficer(deployer.address);
  await registry.grantRegistrar(deployer.address);
  console.log("Officer and Registrar granted to deployer:", deployer.address);

  const signers = await ethers.getSigners();
  if (signers.length > 1) {
    const officerAddr = signers[1].address;
    await registry.grantOfficer(officerAddr);
    await registry.grantRegistrar(officerAddr);
    console.log("Officer and Registrar granted to:", officerAddr);
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
