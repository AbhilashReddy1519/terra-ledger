const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("LandRegistry", function () {
  let registry;
  let admin, officer, nonOfficer, stranger;
  const RECORD_ID = ethers.id("test-record-001"); // bytes32
  const CID = "bafybeigdyrzt5sfp7udm7hu76uh7y26nf3efuylqabf3oclgtqy55fbzdi";
  const SHA256 = ethers.id("file-content-hash"); // bytes32 stand-in

  beforeEach(async function () {
    [admin, officer, nonOfficer, stranger] = await ethers.getSigners();
    const LandRegistry = await ethers.getContractFactory("LandRegistry");
    registry = await LandRegistry.connect(admin).deploy();
    await registry.waitForDeployment();
  });

  // ── Role management ─────────────────────────────────────────────────────────

  describe("grantOfficer / revokeOfficer", function () {
    it("admin can grant officer", async function () {
      await registry.connect(admin).grantOfficer(officer.address);
      expect(await registry.officers(officer.address)).to.be.true;
    });

    it("non-admin cannot grant officer", async function () {
      await expect(
        registry.connect(stranger).grantOfficer(officer.address)
      ).to.be.revertedWithCustomError(registry, "NotAdmin");
    });

    it("admin can revoke officer", async function () {
      await registry.connect(admin).grantOfficer(officer.address);
      await registry.connect(admin).revokeOfficer(officer.address);
      expect(await registry.officers(officer.address)).to.be.false;
    });

    it("emits OfficerGranted event", async function () {
      await expect(registry.connect(admin).grantOfficer(officer.address))
        .to.emit(registry, "OfficerGranted")
        .withArgs(officer.address);
    });
  });

  // ── createRecord ────────────────────────────────────────────────────────────

  describe("createRecord", function () {
    beforeEach(async function () {
      await registry.connect(admin).grantOfficer(officer.address);
    });

    it("officer can create a record (happy path)", async function () {
      await registry.connect(officer).createRecord(RECORD_ID, CID, SHA256);
      const [version, cid, hash, approver] = await registry.getLatest(RECORD_ID);
      expect(version).to.equal(1n);
      expect(cid).to.equal(CID);
      expect(hash).to.equal(SHA256);
      expect(approver).to.equal(officer.address);
    });

    it("non-officer cannot commit — MUST revert with NotOfficer", async function () {
      await expect(
        registry.connect(nonOfficer).createRecord(RECORD_ID, CID, SHA256)
      ).to.be.revertedWithCustomError(registry, "NotOfficer");
    });

    it("duplicate recordId reverts with RecordAlreadyExists", async function () {
      await registry.connect(officer).createRecord(RECORD_ID, CID, SHA256);
      await expect(
        registry.connect(officer).createRecord(RECORD_ID, CID, SHA256)
      ).to.be.revertedWithCustomError(registry, "RecordAlreadyExists");
    });

    it("empty CID reverts", async function () {
      await expect(
        registry.connect(officer).createRecord(RECORD_ID, "", SHA256)
      ).to.be.revertedWithCustomError(registry, "EmptyCID");
    });

    it("zero hash reverts", async function () {
      await expect(
        registry.connect(officer).createRecord(RECORD_ID, CID, ethers.ZeroHash)
      ).to.be.revertedWithCustomError(registry, "InvalidHash");
    });

    it("emits RecordCommitted with correct args", async function () {
      await expect(
        registry.connect(officer).createRecord(RECORD_ID, CID, SHA256)
      )
        .to.emit(registry, "RecordCommitted")
        .withArgs(RECORD_ID, 1n, SHA256, CID, officer.address);
    });
  });

  // ── addVersion ──────────────────────────────────────────────────────────────

  describe("addVersion", function () {
    const CID2 = "bafkreihdwdcefgh4d4gevgjlvljr2f";
    const SHA2 = ethers.id("file-content-hash-v2");

    beforeEach(async function () {
      await registry.connect(admin).grantOfficer(officer.address);
      await registry.connect(officer).createRecord(RECORD_ID, CID, SHA256);
    });

    it("officer can append a version — versions only append", async function () {
      await registry.connect(officer).addVersion(RECORD_ID, CID2, SHA2);
      const [version] = await registry.getLatest(RECORD_ID);
      expect(version).to.equal(2n);
      expect(await registry.versionCount(RECORD_ID)).to.equal(2n);
    });

    it("non-officer cannot addVersion", async function () {
      await expect(
        registry.connect(nonOfficer).addVersion(RECORD_ID, CID2, SHA2)
      ).to.be.revertedWithCustomError(registry, "NotOfficer");
    });

    it("addVersion on nonexistent record reverts RecordNotFound", async function () {
      const bogus = ethers.id("does-not-exist");
      await expect(
        registry.connect(officer).addVersion(bogus, CID2, SHA2)
      ).to.be.revertedWithCustomError(registry, "RecordNotFound");
    });

    it("old version still readable after addVersion", async function () {
      await registry.connect(officer).addVersion(RECORD_ID, CID2, SHA2);
      const [cid, hash] = await registry.getVersion(RECORD_ID, 1);
      expect(cid).to.equal(CID);
      expect(hash).to.equal(SHA256);
    });
  });

  // ── getLatest ───────────────────────────────────────────────────────────────

  describe("getLatest on nonexistent record", function () {
    it("reverts RecordNotFound", async function () {
      await expect(
        registry.getLatest(ethers.id("no-such-record"))
      ).to.be.revertedWithCustomError(registry, "RecordNotFound");
    });
  });
});
