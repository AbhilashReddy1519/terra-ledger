import { useState, useEffect } from 'react'

export default function App() {
  const [activeTab, setActiveTab] = useState('verify') // 'verify', 'records', 'officer', 'audit', 'skeleton'
  
  // Auth state
  const [token, setToken] = useState(localStorage.getItem('terra_token') || '')
  const [currentUser, setCurrentUser] = useState(
    JSON.parse(localStorage.getItem('terra_user') || '{"email":"citizen@example.com","role":"CITIZEN"}')
  )
  const [authError, setAuthError] = useState(null)
  const [loginEmail, setLoginEmail] = useState('')
  const [loginPassword, setLoginPassword] = useState('')

  // Verification state (M6 centerpiece)
  const [verifyFile, setVerifyFile] = useState(null)
  const [verifyRecordId, setVerifyRecordId] = useState('')
  const [verifyLoading, setVerifyLoading] = useState(false)
  const [verifyResult, setVerifyResult] = useState(null)
  const [verifyError, setVerifyError] = useState(null)

  // Records state
  const [records, setRecords] = useState([])
  const [recordsLoading, setRecordsLoading] = useState(false)
  const [uploadFile, setUploadFile] = useState(null)
  const [uploadStatus, setUploadStatus] = useState(null)

  // Officer queue state
  const [officerQueue, setOfficerQueue] = useState([])
  const [officerLoading, setOfficerLoading] = useState(false)
  const [reconcileReport, setReconcileReport] = useState(null)

  // Audit state
  const [auditEvents, setAuditEvents] = useState([])

  // Skeleton state
  const [skeletonRecord, setSkeletonRecord] = useState(null)
  const [skeletonLoading, setSkeletonLoading] = useState(false)

  // Quick preset login helper
  async function quickLogin(role) {
    const email = `${role.toLowerCase()}_ui_${Date.now().toString().slice(-4)}@example.com`
    const password = 'password123'
    try {
      setAuthError(null)
      // Attempt register
      const regRes = await fetch('/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password, role })
      })
      if (!regRes.ok && regRes.status !== 409) {
        throw new Error(`Register failed: ${regRes.status}`)
      }
      // Login
      const loginRes = await fetch('/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      })
      if (!loginRes.ok) throw new Error(`Login failed: ${loginRes.status}`)
      const data = await loginRes.json()
      saveAuth(data.token, { email, role })
    } catch (err) {
      setAuthError(err.message)
    }
  }

  function saveAuth(jwt, user) {
    setToken(jwt)
    setCurrentUser(user)
    localStorage.setItem('terra_token', jwt)
    localStorage.setItem('terra_user', JSON.stringify(user))
  }

  function logout() {
    setToken('')
    setCurrentUser(null)
    localStorage.removeItem('terra_token')
    localStorage.removeItem('terra_user')
  }

  // Ensure an authenticated session exists on mount
  useEffect(() => {
    if (!token) {
      quickLogin('CITIZEN')
    }
  }, [])

  // Load records when switching tabs
  useEffect(() => {
    if (activeTab === 'records' && token) {
      loadRecords()
    } else if (activeTab === 'officer' && token) {
      loadOfficerQueue()
    } else if (activeTab === 'audit' && token) {
      loadAuditEvents()
    }
  }, [activeTab, token])

  // Verification action
  async function handleVerify(e) {
    e.preventDefault()
    if (!verifyFile) {
      setVerifyError('Please select a deed file to verify.')
      return
    }

    setVerifyLoading(true)
    setVerifyError(null)
    setVerifyResult(null)

    const formData = new FormData()
    formData.append('file', verifyFile)
    if (verifyRecordId && verifyRecordId.trim()) {
      formData.append('recordId', verifyRecordId.trim())
    }

    try {
      const res = await fetch('/api/verify', {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`
        },
        body: formData
      })

      const data = await res.json()
      if (!res.ok) {
        throw new Error(data.error || `HTTP error ${res.status}`)
      }
      setVerifyResult(data)
    } catch (err) {
      setVerifyError(err.message)
    } finally {
      setVerifyLoading(false)
    }
  }

  // Upload deed record
  async function handleUploadRecord(e) {
    e.preventDefault()
    if (!uploadFile) return
    setUploadStatus({ loading: true, message: 'Uploading and processing OCR...' })

    const formData = new FormData()
    formData.append('file', uploadFile)

    try {
      const res = await fetch('/api/records', {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        body: formData
      })
      const data = await res.json()
      if (!res.ok) throw new Error(data.error || 'Upload failed')
      setUploadStatus({ success: true, message: `Record uploaded! Survey No: ${data.surveyNo || 'N/A'}` })
      setUploadFile(null)
      loadRecords()
    } catch (err) {
      setUploadStatus({ error: true, message: err.message })
    }
  }

  async function loadRecords() {
    setRecordsLoading(true)
    try {
      const res = await fetch('/api/citizen/records', {
        headers: { Authorization: `Bearer ${token}` }
      })
      if (res.ok) {
        const data = await res.json()
        setRecords(data)
      }
    } catch (e) {
      console.error(e)
    } finally {
      setRecordsLoading(false)
    }
  }

  async function loadOfficerQueue() {
    setOfficerLoading(true)
    try {
      const res = await fetch('/api/officer/records/pending', {
        headers: { Authorization: `Bearer ${token}` }
      })
      if (res.ok) {
        const data = await res.json()
        setOfficerQueue(data)
      }
    } catch (e) {
      console.error(e)
    } finally {
      setOfficerLoading(false)
    }
  }

  async function handleApprove(recordId) {
    try {
      const res = await fetch(`/api/records/${recordId}/approve`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({ remarks: 'Approved from Officer Dashboard' })
      })
      if (res.ok) {
        loadOfficerQueue()
      } else {
        const err = await res.json()
        alert(`Approval error: ${err.error || res.status}`)
      }
    } catch (e) {
      alert(`Approval failed: ${e.message}`)
    }
  }

  async function handleReconcile() {
    try {
      const res = await fetch('/api/records/reconcile', {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` }
      })
      if (res.ok) {
        const report = await res.json()
        setReconcileReport(report)
      }
    } catch (e) {
      alert(`Reconciliation error: ${e.message}`)
    }
  }

  async function loadAuditEvents() {
    try {
      const res = await fetch('/api/audit', {
        headers: { Authorization: `Bearer ${token}` }
      })
      if (res.ok) {
        const data = await res.json()
        setAuditEvents(data)
      }
    } catch (e) {
      console.error(e)
    }
  }

  // M0 Skeleton helper
  async function runSkeletonCommit() {
    setSkeletonLoading(true)
    try {
      const res = await fetch('/api/skeleton/record', { method: 'POST' })
      const data = await res.json()
      setSkeletonRecord(data)
    } catch (e) {
      alert(e.message)
    } finally {
      setSkeletonLoading(false)
    }
  }

  return (
    <div className="container">
      {/* Header */}
      <header className="header">
        <div className="logo-group">
          <h1>🌏 Terra Ledger</h1>
          <p>Decentralized Land Deed Registry & Cryptographic Verification Platform</p>
        </div>

        <div className="auth-bar">
          {currentUser ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
              <span className={`badge badge-${currentUser.role.toLowerCase()}`}>
                {currentUser.role}
              </span>
              <span style={{ fontSize: '0.85rem', color: '#475569' }}>{currentUser.email}</span>
              <button className="btn btn-secondary" style={{ padding: '0.35rem 0.75rem', fontSize: '0.8rem' }} onClick={logout}>
                Sign Out
              </button>
            </div>
          ) : (
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button className="btn btn-primary" onClick={() => quickLogin('CITIZEN')}>Sign In as Citizen</button>
              <button className="btn btn-secondary" onClick={() => quickLogin('OFFICER')}>Sign In as Officer</button>
            </div>
          )}
        </div>
      </header>

      {/* Role Switcher Toolbar */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: '#e2e8f0', padding: '0.5rem 0.8rem', borderRadius: 6, marginBottom: '1.25rem', fontSize: '0.82rem' }}>
        <span><strong>Quick Persona Switch:</strong></span>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button className="btn btn-secondary" style={{ padding: '0.25rem 0.6rem', fontSize: '0.78rem' }} onClick={() => quickLogin('CITIZEN')}>
            Switch to Citizen
          </button>
          <button className="btn btn-secondary" style={{ padding: '0.25rem 0.6rem', fontSize: '0.78rem' }} onClick={() => quickLogin('OFFICER')}>
            Switch to Revenue Officer
          </button>
        </div>
      </div>

      {authError && (
        <div style={{ background: '#fef2f2', border: '1px solid #fca5a5', padding: '0.75rem', borderRadius: 6, color: '#dc2626', marginBottom: '1rem', fontSize: '0.85rem' }}>
          Auth error: {authError}
        </div>
      )}

      {/* Navigation Tabs */}
      <nav className="nav-tabs">
        <button
          className={`nav-tab ${activeTab === 'verify' ? 'active' : ''}`}
          onClick={() => setActiveTab('verify')}
        >
          🔍 Verify Document (M6)
        </button>
        <button
          className={`nav-tab ${activeTab === 'records' ? 'active' : ''}`}
          onClick={() => setActiveTab('records')}
        >
          📄 Citizen Records
        </button>
        {currentUser?.role === 'OFFICER' && (
          <button
            className={`nav-tab ${activeTab === 'officer' ? 'active' : ''}`}
            onClick={() => setActiveTab('officer')}
          >
            🏛️ Officer Review Queue
          </button>
        )}
        {(currentUser?.role === 'OFFICER' || currentUser?.role === 'ADMIN') && (
          <button
            className={`nav-tab ${activeTab === 'audit' ? 'active' : ''}`}
            onClick={() => setActiveTab('audit')}
          >
            📜 Audit Log
          </button>
        )}
        <button
          className={`nav-tab ${activeTab === 'skeleton' ? 'active' : ''}`}
          onClick={() => setActiveTab('skeleton')}
        >
          ⚡ Skeleton (M0)
        </button>
      </nav>

      {/* TAB 1: VERIFICATION (M6 CENTERPIECE) */}
      {activeTab === 'verify' && (
        <div className="card">
          <h2 className="card-title">Cryptographic Document Verification</h2>
          <p className="card-desc">
            Upload any digital deed scan to compute its SHA-256 digest and verify it bit-for-bit against the immutable Ethereum smart contract ledger.
          </p>

          <form onSubmit={handleVerify}>
            <div className="form-group">
              <label className="form-label">Select Deed Document to Verify *</label>
              <input
                type="file"
                className="form-input"
                accept="image/*,application/pdf"
                onChange={(e) => setVerifyFile(e.target.files[0])}
              />
              {verifyFile && (
                <div style={{ marginTop: '0.35rem', fontSize: '0.8rem', color: '#64748b' }}>
                  Selected: <strong>{verifyFile.name}</strong> ({(verifyFile.size / 1024).toFixed(1)} KB)
                </div>
              )}
            </div>

            <div className="form-group">
              <label className="form-label">Record ID (Optional UUID)</label>
              <input
                type="text"
                className="form-input"
                placeholder="Leave blank to auto-detect matching record from hash"
                value={verifyRecordId}
                onChange={(e) => setVerifyRecordId(e.target.value)}
              />
              <span style={{ fontSize: '0.78rem', color: '#64748b' }}>
                If specified, the file will be compared directly against the on-chain commit for this record ID.
              </span>
            </div>

            <div style={{ marginTop: '1.25rem' }}>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={verifyLoading || !verifyFile}
              >
                {verifyLoading ? 'Computing Hash & Querying Chain...' : 'Verify against Blockchain'}
              </button>
            </div>
          </form>

          {verifyError && (
            <div style={{ background: '#fef2f2', border: '1px solid #fca5a5', padding: '1rem', borderRadius: 6, color: '#dc2626', marginTop: '1.5rem' }}>
              ❌ {verifyError}
            </div>
          )}

          {/* VERIFICATION RESULT DISPLAY */}
          {verifyResult && (
            <div className={`verify-result ${verifyResult.status.toLowerCase()}`}>
              <div className="verify-header">
                {verifyResult.status === 'MATCH' && <span>✅ Identical to the Approved File</span>}
                {verifyResult.status === 'MISMATCH' && <span>❌ Different from the Approved File</span>}
                {verifyResult.status === 'NOT_FOUND' && <span>⚠️ Record Not Found on Blockchain</span>}
              </div>

              <div className="verify-details-grid">
                <div className="detail-item">
                  <div className="detail-item-label">Verification Result</div>
                  <div className="detail-item-value" style={{ fontWeight: 700 }}>
                    {verifyResult.status} ({verifyResult.message})
                  </div>
                </div>

                {verifyResult.recordId && (
                  <div className="detail-item">
                    <div className="detail-item-label">Record ID</div>
                    <div className="detail-item-value">{verifyResult.recordId}</div>
                  </div>
                )}

                {verifyResult.version && (
                  <div className="detail-item">
                    <div className="detail-item-label">Committed Version</div>
                    <div className="detail-item-value">v{verifyResult.version}</div>
                  </div>
                )}

                {verifyResult.approver && (
                  <div className="detail-item">
                    <div className="detail-item-label">Officer Approver Address</div>
                    <div className="detail-item-value">{verifyResult.approver}</div>
                  </div>
                )}

                {verifyResult.committedAt && (
                  <div className="detail-item">
                    <div className="detail-item-label">Committed Timestamp</div>
                    <div className="detail-item-value">{new Date(verifyResult.committedAt).toLocaleString()}</div>
                  </div>
                )}

                {verifyResult.cid && (
                  <div className="detail-item">
                    <div className="detail-item-label">Encrypted IPFS CID</div>
                    <div className="detail-item-value">{verifyResult.cid}</div>
                  </div>
                )}
              </div>

              {/* Hash Comparison View */}
              <div style={{ background: '#ffffff', padding: '1rem', borderRadius: 6, border: '1px solid #e2e8f0', marginBottom: '1rem' }}>
                <div style={{ marginBottom: '0.6rem' }}>
                  <div style={{ fontSize: '0.75rem', fontWeight: 600, color: '#64748b' }}>COMPUTED FILE SHA-256 HASH</div>
                  <div style={{ fontFamily: 'monospace', fontSize: '0.82rem', wordBreak: 'break-all', color: verifyResult.status === 'MISMATCH' ? '#dc2626' : '#0f172a' }}>
                    {verifyResult.computedHash}
                  </div>
                </div>

                {verifyResult.onChainHash && (
                  <div>
                    <div style={{ fontSize: '0.75rem', fontWeight: 600, color: '#64748b' }}>ON-CHAIN SMART CONTRACT HASH</div>
                    <div style={{ fontFamily: 'monospace', fontSize: '0.82rem', wordBreak: 'break-all', color: '#16a34a' }}>
                      {verifyResult.onChainHash}
                    </div>
                  </div>
                )}
              </div>

              {/* MANDATORY HONESTY DISCLAIMER */}
              <div className="disclaimer-banner">
                <strong>Notice:</strong> {verifyResult.disclaimer}
              </div>
            </div>
          )}
        </div>
      )}

      {/* TAB 2: CITIZEN RECORDS */}
      {activeTab === 'records' && (
        <div>
          {/* Upload Card */}
          <div className="card">
            <h2 className="card-title">Upload New Deed Document</h2>
            <p className="card-desc">
              Upload a scanned deed file (PNG, JPG, TIFF, PDF). Our Python OCR pipeline extracts key survey details and submits the record to the revenue officer review queue.
            </p>

            <form onSubmit={handleUploadRecord}>
              <div className="form-group">
                <input
                  type="file"
                  className="form-input"
                  accept="image/*,application/pdf"
                  onChange={(e) => setUploadFile(e.target.files[0])}
                />
              </div>

              <button
                type="submit"
                className="btn btn-primary"
                disabled={!uploadFile || uploadStatus?.loading}
              >
                {uploadStatus?.loading ? 'Uploading & Processing OCR...' : 'Upload Deed Scan'}
              </button>
            </form>

            {uploadStatus && (
              <div style={{
                marginTop: '1rem',
                padding: '0.75rem',
                borderRadius: 6,
                background: uploadStatus.error ? '#fef2f2' : '#f0fdf4',
                color: uploadStatus.error ? '#dc2626' : '#16a34a',
                border: `1px solid ${uploadStatus.error ? '#fca5a5' : '#86efac'}`
              }}>
                {uploadStatus.message}
              </div>
            )}
          </div>

          {/* Records List */}
          <div className="card">
            <h2 className="card-title">My Registered Land Records</h2>
            {recordsLoading ? (
              <p>Loading records...</p>
            ) : records.length === 0 ? (
              <p style={{ color: '#64748b' }}>No records uploaded yet. Upload a deed scan above to begin.</p>
            ) : (
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Survey No</th>
                    <th>Owner Name</th>
                    <th>Area</th>
                    <th>Status</th>
                    <th>Version</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {records.map((r) => (
                    <tr key={r.id}>
                      <td style={{ fontWeight: 600 }}>{r.surveyNo || 'N/A'}</td>
                      <td>{r.ownerName || 'N/A'}</td>
                      <td>{r.area || 'N/A'}</td>
                      <td>
                        <span className={`badge badge-${r.status}`}>{r.status}</span>
                      </td>
                      <td>{r.currentVersion ? `v${r.currentVersion}` : '—'}</td>
                      <td>
                        <button
                          className="btn btn-secondary"
                          style={{ padding: '0.25rem 0.6rem', fontSize: '0.75rem' }}
                          onClick={() => {
                            setVerifyRecordId(r.id)
                            setActiveTab('verify')
                          }}
                        >
                          Verify on Chain
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      )}

      {/* TAB 3: OFFICER REVIEW QUEUE */}
      {activeTab === 'officer' && (
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <h2 style={{ fontSize: '1.3rem', fontWeight: 600 }}>Officer Review Queue & Reconciliation</h2>
            <button className="btn btn-secondary" onClick={handleReconcile}>
              Audit Cache with Blockchain
            </button>
          </div>

          {reconcileReport && (
            <div className="card" style={{ background: '#f8fafc', borderLeft: '4px solid #2563eb' }}>
              <h3 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '0.5rem' }}>
                Blockchain Reconciliation Audit Result
              </h3>
              <p style={{ fontSize: '0.85rem' }}>
                Total Chain Events: <strong>{reconcileReport.totalChainEvents}</strong> |
                Matched: <strong>{reconcileReport.matchedCount}</strong> |
                Rebuilt: <strong>{reconcileReport.rebuiltCount}</strong> |
                Discrepancies: <strong>{reconcileReport.discrepancies?.length || 0}</strong>
              </p>
              <span className="badge badge-COMMITTED" style={{ marginTop: '0.5rem' }}>
                Cache Consistency: {reconcileReport.cacheMatchesChain ? 'PERFECT MATCH' : 'DRIFT DETECTED'}
              </span>
            </div>
          )}

          <div className="card">
            <h3 className="card-title">Pending Deed Submissions</h3>
            {officerLoading ? (
              <p>Loading queue...</p>
            ) : officerQueue.length === 0 ? (
              <p style={{ color: '#64748b' }}>No pending records in review queue.</p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginTop: '1rem' }}>
                {officerQueue.map((item) => (
                  <div key={item.id} style={{ border: '1px solid #e2e8f0', borderRadius: 8, padding: '1rem', background: '#fafafa' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
                      <span style={{ fontWeight: 600, fontSize: '1.05rem' }}>
                        Survey No: {item.surveyNo || 'N/A'} ({item.deedType || 'DEED'})
                      </span>
                      <span className="badge badge-PENDING_REVIEW">Pending Review</span>
                    </div>

                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '0.5rem', fontSize: '0.85rem', marginBottom: '1rem' }}>
                      <div><strong>Owner:</strong> {item.ownerName || 'N/A'}</div>
                      <div><strong>Area:</strong> {item.area || 'N/A'}</div>
                      <div><strong>Boundaries:</strong> {item.boundaries || 'N/A'}</div>
                      <div><strong>File:</strong> {item.fileName} ({(item.fileSize / 1024).toFixed(1)} KB)</div>
                    </div>

                    <div style={{ display: 'flex', gap: '0.75rem' }}>
                      <button
                        className="btn btn-success"
                        onClick={() => handleApprove(item.id)}
                      >
                        Approve & Commit to Blockchain
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* TAB 4: AUDIT LOG */}
      {activeTab === 'audit' && (
        <div className="card">
          <h2 className="card-title">System Audit Log</h2>
          <p className="card-desc">
            Immutable log of all user and administrative state modifications.
          </p>
          <table className="data-table">
            <thead>
              <tr>
                <th>Action</th>
                <th>Actor</th>
                <th>Entity</th>
                <th>Entity ID</th>
                <th>Timestamp</th>
              </tr>
            </thead>
            <tbody>
              {auditEvents.map((a) => (
                <tr key={a.id}>
                  <td><span className="badge badge-officer">{a.action}</span></td>
                  <td>{a.actor}</td>
                  <td>{a.entity}</td>
                  <td style={{ fontFamily: 'monospace', fontSize: '0.78rem' }}>{a.entityId}</td>
                  <td>{new Date(a.at).toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* TAB 5: SKELETON */}
      {activeTab === 'skeleton' && (
        <div className="card">
          <h2 className="card-title">M0 Walking Skeleton</h2>
          <p className="card-desc">
            Direct roundtrip: Browser → Spring Boot → Hardhat Smart Contract.
          </p>
          <div style={{ display: 'flex', gap: '1rem', marginBottom: '1rem' }}>
            <button className="btn btn-primary" onClick={runSkeletonCommit} disabled={skeletonLoading}>
              {skeletonLoading ? 'Working...' : 'Commit Hardcoded Record'}
            </button>
          </div>
          {skeletonRecord && (
            <pre style={{ background: '#f8fafc', padding: '1rem', borderRadius: 6, border: '1px solid #e2e8f0', fontSize: '0.8rem', overflowX: 'auto' }}>
              {JSON.stringify(skeletonRecord, null, 2)}
            </pre>
          )}
        </div>
      )}

      {/* Footer */}
      <footer style={{ marginTop: '3rem', textAlign: 'center', color: '#94a3b8', fontSize: '0.82rem' }}>
        Terra Ledger · M6 Complete MVP · Spring Boot, Solidity on Hardhat, AES-256-GCM, Kubo IPFS, Tesseract OCR, React + Vite
      </footer>
    </div>
  )
}
