// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

/**
 * @title LandRegistry
 * @notice Append-only land record registry.  No update or delete functions exist,
 *         so history cannot be rewritten even by an admin.
 *
 * Roles
 * -----
 *   admin   – the account that deployed the contract; can grant/revoke officers.
 *   officer – authorised to create records and add versions.
 *
 * On-chain data
 * -------------
 *   recordId  bytes32   – opaque identifier (keccak256 of the MySQL UUID)
 *   version   uint256   – monotonically increasing, starts at 1
 *   cid       string    – IPFS CID of the AES-256-GCM encrypted document
 *   sha256Hash bytes32  – SHA-256 of the original plaintext bytes (for verification)
 *   approver  address   – officer address that submitted the transaction
 *   at        uint256   – block timestamp
 *
 * No names, survey numbers, areas, or personal data are stored on-chain.
 */
contract LandRegistry {
    // ── State ──────────────────────────────────────────────────────────────────

    address public immutable admin;

    mapping(address => bool) public officers;
    mapping(address => bool) public registrars;

    struct Version {
        uint256 version;
        string  cid;
        bytes32 sha256Hash;
        address approver;
        uint256 at;
    }

    // recordId => ordered list of versions (index 0 = version 1)
    mapping(bytes32 => Version[]) private _versions;

    // ── Events ─────────────────────────────────────────────────────────────────

    event OfficerGranted(address indexed account);
    event OfficerRevoked(address indexed account);
    event RegistrarGranted(address indexed account);
    event RegistrarRevoked(address indexed account);
    event RecordCommitted(
        bytes32 indexed recordId,
        uint256 version,
        bytes32 sha256Hash,
        string  cid,
        address approver
    );

    // ── Errors ─────────────────────────────────────────────────────────────────

    error NotAdmin();
    error NotOfficer();
    error NotRegistrar();
    error NotAuthorized();
    error RecordNotFound();
    error RecordAlreadyExists();
    error InvalidHash();
    error EmptyCID();

    // ── Modifiers ──────────────────────────────────────────────────────────────

    modifier onlyAdmin() {
        if (msg.sender != admin) revert NotAdmin();
        _;
    }

    modifier onlyOfficer() {
        if (!officers[msg.sender]) revert NotOfficer();
        _;
    }

    modifier onlyRegistrar() {
        if (!registrars[msg.sender]) revert NotRegistrar();
        _;
    }

    modifier onlyOfficerOrRegistrar() {
        if (!officers[msg.sender] && !registrars[msg.sender]) revert NotAuthorized();
        _;
    }

    // ── Constructor ────────────────────────────────────────────────────────────

    constructor() {
        admin = msg.sender;
    }

    // ── Admin functions ────────────────────────────────────────────────────────

    function grantOfficer(address a) external onlyAdmin {
        officers[a] = true;
        emit OfficerGranted(a);
    }

    function revokeOfficer(address a) external onlyAdmin {
        officers[a] = false;
        emit OfficerRevoked(a);
    }

    function grantRegistrar(address a) external onlyAdmin {
        registrars[a] = true;
        emit RegistrarGranted(a);
    }

    function revokeRegistrar(address a) external onlyAdmin {
        registrars[a] = false;
        emit RegistrarRevoked(a);
    }

    // ── Officer functions ──────────────────────────────────────────────────────

    /**
     * @notice Create the first version of a record.
     * @dev Reverts if the record already exists.
     */
    function createRecord(
        bytes32 recordId,
        string calldata cid,
        bytes32 sha256Hash
    ) external onlyOfficer {
        if (_versions[recordId].length != 0) revert RecordAlreadyExists();
        if (bytes(cid).length == 0) revert EmptyCID();
        if (sha256Hash == bytes32(0)) revert InvalidHash();

        _versions[recordId].push(Version({
            version:    1,
            cid:        cid,
            sha256Hash: sha256Hash,
            approver:   msg.sender,
            at:         block.timestamp
        }));

        emit RecordCommitted(recordId, 1, sha256Hash, cid, msg.sender);
    }

    /**
     * @notice Append a new version to an existing record.
     * @dev Reverts if the record does not exist.
     */
    function addVersion(
        bytes32 recordId,
        string calldata cid,
        bytes32 sha256Hash
    ) external onlyOfficerOrRegistrar {
        uint256 len = _versions[recordId].length;
        if (len == 0) revert RecordNotFound();
        if (bytes(cid).length == 0) revert EmptyCID();
        if (sha256Hash == bytes32(0)) revert InvalidHash();

        uint256 nextVersion = len + 1;
        _versions[recordId].push(Version({
            version:    nextVersion,
            cid:        cid,
            sha256Hash: sha256Hash,
            approver:   msg.sender,
            at:         block.timestamp
        }));

        emit RecordCommitted(recordId, nextVersion, sha256Hash, cid, msg.sender);
    }

    // ── View functions ─────────────────────────────────────────────────────────

    /**
     * @notice Get the latest version of a record.
     */
    function getLatest(bytes32 recordId)
        external
        view
        returns (
            uint256 version,
            string memory cid,
            bytes32 sha256Hash,
            address approver,
            uint256 at
        )
    {
        uint256 len = _versions[recordId].length;
        if (len == 0) revert RecordNotFound();
        Version storage v = _versions[recordId][len - 1];
        return (v.version, v.cid, v.sha256Hash, v.approver, v.at);
    }

    /**
     * @notice Get a specific version of a record (1-indexed).
     */
    function getVersion(bytes32 recordId, uint256 versionNumber)
        external
        view
        returns (
            string memory cid,
            bytes32 sha256Hash,
            address approver,
            uint256 at
        )
    {
        uint256 len = _versions[recordId].length;
        if (len == 0) revert RecordNotFound();
        if (versionNumber == 0 || versionNumber > len) revert RecordNotFound();
        Version storage v = _versions[recordId][versionNumber - 1];
        return (v.cid, v.sha256Hash, v.approver, v.at);
    }

    /**
     * @notice Return the total number of versions for a record.
     */
    function versionCount(bytes32 recordId) external view returns (uint256) {
        return _versions[recordId].length;
    }
}
