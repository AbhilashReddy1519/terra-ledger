/**
 * components/StatusBadge.jsx
 *
 * Coloured pill for land-record status values from RecordStatus enum:
 *   PENDING_REVIEW | APPROVED | REJECTED | COMMITTED
 *
 * Props:
 *  - status: string (one of the four values above, case-insensitive)
 *  - label:  optional override for display text
 */

const LABELS = {
  PENDING_REVIEW: 'Pending Review',
  APPROVED:       'Approved',
  REJECTED:       'Rejected',
  COMMITTED:      'Committed',
};

export default function StatusBadge({ status, label }) {
  const upper = (status ?? '').toUpperCase();
  const cssClass = upper.toLowerCase().replace('_', '_'); // keep underscores for CSS
  const displayLabel = label ?? LABELS[upper] ?? upper;

  return (
    <span className={`status-badge ${upper.toLowerCase()}`}>
      {displayLabel}
    </span>
  );
}
