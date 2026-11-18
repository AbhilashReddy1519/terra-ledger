/**
 * components/Button.jsx
 *
 * Reusable button with:
 *  - variant: 'primary' | 'ghost' | 'danger'
 *  - size:    'sm' | 'md' (default)
 *  - full:    boolean (full width)
 *  - loading: boolean (shows spinner + disables)
 */

import Spinner from './Spinner.jsx';

export default function Button({
  children,
  variant = 'primary',
  size,
  full = false,
  loading = false,
  disabled = false,
  type = 'button',
  onClick,
  className = '',
  ...rest
}) {
  const classes = [
    'btn',
    `btn-${variant}`,
    size === 'sm' ? 'btn-sm' : '',
    full ? 'btn-full' : '',
    className,
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <button
      type={type}
      className={classes}
      disabled={disabled || loading}
      onClick={onClick}
      {...rest}
    >
      {loading && <Spinner />}
      {children}
    </button>
  );
}
