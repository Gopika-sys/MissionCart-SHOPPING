import React from 'react';
import { Link } from 'react-router-dom';

export const Footer: React.FC = () => {
  return (
    <footer style={{ background: 'var(--mc-chocolate)', color: 'var(--mc-ivory)', padding: '4rem 0 2.5rem', marginTop: '5rem' }}>
      <div className="mc-container">
        <div className="mc-grid-4" style={{ gap: '2.5rem', paddingBottom: '3rem', borderBottom: '1px solid rgba(231,200,208,0.18)' }}>
          <div>
            <h3 className="mc-serif" style={{ fontSize: '1.75rem', color: 'var(--mc-white)', marginBottom: '0.75rem' }}>MissionCart</h3>
            <p style={{ fontSize: '0.9rem', color: '#D7CBBF', margin: 0 }}>
              Shopping by Purpose, Not Products. Tell us what life chapter you are preparing for and let verified catalogue intelligence build your plan.
            </p>
          </div>
          <div>
            <h4 style={{ fontSize: '0.8rem', textTransform: 'uppercase', letterSpacing: '0.09em', color: 'var(--mc-caramel)', marginBottom: '0.9rem' }}>Discover</h4>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', fontSize: '0.9rem' }}>
              <Link to="/mission-builder">AI Mission Builder</Link>
              <Link to="/explore">Explore Collections</Link>
              <Link to="/products">All Catalogue Products</Link>
              <Link to="/categories">Purpose Categories</Link>
              <Link to="/retailers">13 Partner Retailers</Link>
            </div>
          </div>
          <div>
            <h4 style={{ fontSize: '0.8rem', textTransform: 'uppercase', letterSpacing: '0.09em', color: 'var(--mc-caramel)', marginBottom: '0.9rem' }}>Platform</h4>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', fontSize: '0.9rem' }}>
              <Link to="/how-it-works">How It Works</Link>
              <Link to="/shared-missions">Shared Group Missions</Link>
              <Link to="/lifecycle">Product Lifecycle Manager</Link>
              <Link to="/possessions">Already-Own Inventory</Link>
              <Link to="/seller/register">Sell on MissionCart</Link>
            </div>
          </div>
          <div>
            <h4 style={{ fontSize: '0.8rem', textTransform: 'uppercase', letterSpacing: '0.09em', color: 'var(--mc-caramel)', marginBottom: '0.9rem' }}>About</h4>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', fontSize: '0.9rem' }}>
              <Link to="/about">About MissionCart</Link>
              <Link to="/contact">Contact & Support</Link>
              <Link to="/login">Customer Sign In</Link>
              <Link to="/register">Create Account</Link>
            </div>
          </div>
        </div>
        <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'center', paddingTop: '1.5rem', fontSize: '0.82rem', color: '#BFAEA3', gap: '1rem' }}>
          <span>© {new Date().getFullYear()} MissionCart. Warm Editorial Purpose-First Commerce.</span>
          <span>All native prices in INR (₹). External retailer records clearly separated from native checkout.</span>
        </div>
      </div>
    </footer>
  );
};
