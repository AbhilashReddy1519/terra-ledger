/**
 * components/Card.jsx
 *
 * Simple white card container.
 *
 * Props:
 *  - title:     string (optional heading inside the card)
 *  - className: extra CSS classes
 *  - children
 */

export default function Card({ title, children, className = '', ...rest }) {
  return (
    <div className={`card ${className}`} {...rest}>
      {title && <h2 className="card-title">{title}</h2>}
      {children}
    </div>
  );
}
