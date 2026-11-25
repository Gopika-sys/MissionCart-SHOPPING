import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export const ProtectedRoute: React.FC<{ children: React.ReactNode; roles?: Array<'CUSTOMER' | 'ADMIN' | 'SELLER'> }> = ({ children, roles }) => {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="mc-container" style={{ padding: '4rem 1.5rem', textAlign: 'center', color: 'var(--mc-muted)' }}>
        Verifying session...
      </div>
    );
  }
  if (!user) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }
  if (roles && !roles.includes(user.role)) {
    return (
      <div className="mc-container" style={{ padding: '5rem 1.5rem', textAlign: 'center' }}>
        <h2 style={{ fontSize: '1.8rem', marginBottom: '0.6rem' }}>Access Restricted</h2>
        <p style={{ color: 'var(--mc-muted)' }}>Your account role ({user.role}) does not have permission to view this page.</p>
      </div>
    );
  }
  return <>{children}</>;
};
