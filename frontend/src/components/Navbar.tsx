import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Search, ShoppingBag, User, Menu, X, Sparkles } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';

export const Navbar: React.FC = () => {
  const { user } = useAuth();
  const { cart, setDrawerOpen } = useCart();
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState('');
  const [mobileOpen, setMobileOpen] = useState(false);

  const count = cart?.activeItems?.reduce((acc, i) => acc + i.quantity, 0) || 0;

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchTerm.trim()) {
      navigate(`/search?q=${encodeURIComponent(searchTerm.trim())}`);
      setMobileOpen(false);
    }
  };

  return (
    <header style={{ position: 'sticky', top: 0, zIndex: 60, background: 'rgba(243, 237, 227, 0.94)', backdropFilter: 'blur(12px)', borderBottom: '1px solid var(--mc-border)' }}>
      <div className="mc-container" style={{ height: 74, display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '1rem' }}>
        <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: '0.55rem' }}>
          <span style={{ width: 35, height: 35, borderRadius: '50%', background: 'var(--mc-burgundy)', color: 'var(--mc-white)', display: 'grid', placeItems: 'center' }}>
            <Sparkles size={16} />
          </span>
          <div>
            <div className="mc-serif" style={{ fontSize: '1.4rem', fontWeight: 600, letterSpacing: '-0.02em', lineHeight: 1, color: 'var(--mc-chocolate)' }}>MissionCart</div>
            <div style={{ fontSize: '0.64rem', letterSpacing: '0.09em', textTransform: 'uppercase', color: 'var(--mc-burgundy)', fontWeight: 600 }}>Shopping by Purpose</div>
          </div>
        </Link>

        <nav className="mc-desktop-nav" style={{ display: 'flex', alignItems: 'center', gap: '1.35rem', fontSize: '0.9rem', fontWeight: 500, color: 'var(--mc-chocolate)' }}>
          <Link to="/">Home</Link>
          <Link to="/explore">Discover</Link>
          <Link to="/products">Catalogue</Link>
          <Link to="/categories">Categories</Link>
          <Link to="/retailers">Retailers</Link>
          <Link to="/how-it-works">How it works</Link>
          <Link to="/missions">My missions</Link>
        </nav>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
          <form onSubmit={handleSearch} className="mc-desktop-search" style={{ position: 'relative', width: 205 }}>
            <Search size={15} style={{ position: 'absolute', left: 11, top: '50%', transform: 'translateY(-50%)', color: 'var(--mc-muted)' }} />
            <input
              type="search"
              placeholder="Search products..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="mc-input"
              style={{ padding: '0.45rem 0.75rem 0.45rem 2.1rem', borderRadius: 'var(--mc-radius-pill)', fontSize: '0.84rem' }}
            />
          </form>

          {user ? (
            <Link
              to={user.role === 'ADMIN' ? '/admin' : user.role === 'SELLER' ? '/seller' : '/dashboard'}
              className="mc-btn mc-btn-outline"
              style={{ padding: '0.45rem 0.9rem', fontSize: '0.84rem' }}
            >
              <User size={15} /> {user.fullName.split(' ')[0]}
            </Link>
          ) : (
            <Link to="/login" className="mc-btn mc-btn-outline" style={{ padding: '0.45rem 0.95rem', fontSize: '0.84rem' }}>
              <User size={15} /> Account
            </Link>
          )}

          <button
            type="button"
            aria-label="Open shopping bag"
            className="mc-btn mc-btn-accent"
            style={{ padding: '0.48rem 0.95rem', fontSize: '0.84rem', position: 'relative' }}
            onClick={() => setDrawerOpen(true)}
          >
            <ShoppingBag size={15} />
            <span>Bag ({count})</span>
          </button>

          <button
            type="button"
            aria-label="Toggle menu"
            className="mc-mobile-toggle mc-btn mc-btn-outline"
            style={{ padding: '0.45rem' }}
            onClick={() => setMobileOpen(!mobileOpen)}
          >
            {mobileOpen ? <X size={18} /> : <Menu size={18} />}
          </button>
        </div>
      </div>

      {mobileOpen && (
        <div style={{ padding: '1rem 1.5rem 1.4rem', borderTop: '1px solid var(--mc-border)', background: 'var(--mc-ivory)', display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          <form onSubmit={handleSearch}>
            <input type="search" placeholder="Search products..." value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} className="mc-input" />
          </form>
          <Link to="/" onClick={() => setMobileOpen(false)}>Home</Link>
          <Link to="/explore" onClick={() => setMobileOpen(false)}>Discover</Link>
          <Link to="/products" onClick={() => setMobileOpen(false)}>Catalogue</Link>
          <Link to="/categories" onClick={() => setMobileOpen(false)}>Categories</Link>
          <Link to="/retailers" onClick={() => setMobileOpen(false)}>Retailers</Link>
          <Link to="/how-it-works" onClick={() => setMobileOpen(false)}>How it works</Link>
          <Link to="/missions" onClick={() => setMobileOpen(false)}>My missions</Link>
          <Link to="/mission-builder" onClick={() => setMobileOpen(false)}>Mission Builder</Link>
        </div>
      )}

      <style>{`
        .mc-mobile-toggle { display: none; }
        @media (max-width: 1120px) {
          .mc-desktop-nav, .mc-desktop-search { display: none !important; }
          .mc-mobile-toggle { display: inline-flex; }
        }
      `}</style>
    </header>
  );
};
