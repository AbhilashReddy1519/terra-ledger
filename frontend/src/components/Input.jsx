/**
 * components/Input.jsx
 *
 * Labelled text/email/password input with error state.
 *
 * Props:
 *  - id:          string (required, ties label to input for a11y)
 *  - label:       string
 *  - error:       string (shows red border + error message below)
 *  - type:        'text' | 'email' | 'password' (default 'text')
 *  - value, onChange, placeholder, disabled, autoComplete, ...rest
 */

export default function Input({
  id,
  label,
  error,
  type = 'text',
  className = '',
  ...rest
}) {
  const inputClass = ['input-field', error ? 'input-error' : '', className]
    .filter(Boolean)
    .join(' ');

  return (
    <div className="input-group">
      {label && (
        <label htmlFor={id} className="input-label">
          {label}
        </label>
      )}
      <input id={id} type={type} className={inputClass} {...rest} />
      {error && (
        <span className="input-error-text" role="alert">
          {error}
        </span>
      )}
    </div>
  );
}
