/**
 * components/Spinner.jsx
 *
 * CSS-only spinning ring.
 *
 * Props:
 *  - size: 'sm' (default) | 'lg'
 *  - center: boolean — wrap in a flex-center div
 */

export default function Spinner({ size, center = false }) {
  const cls = ['spinner', size === 'lg' ? 'spinner-lg' : ''].filter(Boolean).join(' ');

  if (center) {
    return (
      <div className="spinner-center">
        <div className={cls} role="status" aria-label="Loading" />
      </div>
    );
  }

  return <div className={cls} role="status" aria-label="Loading" />;
}
