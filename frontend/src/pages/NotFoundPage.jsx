/**
 * pages/NotFoundPage.jsx
 *
 * Friendly 404 shown for any unmatched route.
 */

import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div className="error-page">
      <div className="error-page-code">404</div>
      <h1 className="error-page-title">Page Not Found</h1>
      <p className="error-page-desc">
        The page you're looking for doesn't exist or has been moved.
      </p>
      <Link to="/" className="btn btn-primary" style={{ marginTop: '1rem' }}>
        Go home
      </Link>
    </div>
  );
}
