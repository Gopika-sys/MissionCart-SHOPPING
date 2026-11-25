import React, { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { LogOut, MapPin, PackageCheck, Heart, ShoppingBag, Sparkles, Users, Recycle, Shield } from 'lucide-react';
import { api, unwrap } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';

export const LoginPage: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const from = (location.state as any)?.from || '/';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const user = await login(email, password);
      if (user.role === 'ADMIN') navigate('/admin');
      else if (user.role === 'SELLER') navigate('/seller');
      else navigate(from === '/login' ? '/dashboard' : from);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Sign in failed.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="mc-container" style={{ padding: '4rem 1.5rem', maxWidth: 480 }}>
      <div className="mc-card" style={{ padding: '2.2rem' }}>
        <h1 style={{ fontSize: '2.2rem', marginBottom: '0.4rem' }}>Welcome back</h1>
        <p style={{ color: 'var(--mc-muted)', marginBottom: '1.5rem' }}>Sign in to manage your missions, bag, orders, and lifecycle requests.</p>

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div>
            <label htmlFor="login-email" style={{ display: 'block', fontSize: '0.86rem', fontWeight: 600, marginBottom: '0.3rem' }}>Email</label>
            <input id="login-email" type="email" required className="mc-input" value={email} onChange={(e) => setEmail(e.target.value)} />
          </div>
          <div>
            <label htmlFor="login-pass" style={{ display: 'block', fontSize: '0.86rem', fontWeight: 600, marginBottom: '0.3rem' }}>Password</label>
            <input id="login-pass" type="password" required className="mc-input" value={password} onChange={(e) => setPassword(e.target.value)} />
          </div>
          {error && <div style={{ color: 'var(--mc-burgundy)', fontSize: '0.88rem' }}>{error}</div>}
          <button type="submit" disabled={loading} className="mc-btn mc-btn-accent">{loading ? 'Signing in...' : 'Sign In'}</button>
        </form>

        <div className="mc-linen-panel" style={{ padding: '0.9rem 1rem', marginTop: '1.25rem', fontSize: '0.82rem' }}>
          <strong>Demo Customer Account:</strong> ananya@missioncart.local / MissionCart@2026
          <br />
          <button type="button" className="mc-btn mc-btn-outline" style={{ padding: '0.25rem 0.7rem', fontSize: '0.75rem', marginTop: '0.45rem' }} onClick={() => { setEmail('ananya@missioncart.local'); setPassword('MissionCart@2026'); }}>
            Fill Demo Customer
          </button>
        </div>

        <p style={{ marginTop: '1.25rem', fontSize: '0.9rem', textAlign: 'center', color: 'var(--mc-muted)' }}>
          New to MissionCart? <Link to="/register" style={{ color: 'var(--mc-burgundy)', fontWeight: 600 }}>Create an account</Link>
        </p>
      </div>
    </div>
  );
};

export const RegisterPage: React.FC = () => {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [accountType, setAccountType] = useState<'CUSTOMER' | 'SELLER'>('CUSTOMER');
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      const user = await register({ fullName, email, phone, password, accountType });
      navigate(user.role === 'SELLER' ? '/seller' : '/dashboard');
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Registration failed.');
    }
  };

  return (
    <div className="mc-container" style={{ padding: '4rem 1.5rem', maxWidth: 500 }}>
      <div className="mc-card" style={{ padding: '2.2rem' }}>
        <h1 style={{ fontSize: '2.1rem', marginBottom: '0.4rem' }}>Create your account</h1>
        <p style={{ color: 'var(--mc-muted)', marginBottom: '1.5rem' }}>Start planning purpose-first bundles in INR.</p>
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '0.95rem' }}>
          <input required className="mc-input" placeholder="Full Name" value={fullName} onChange={(e) => setFullName(e.target.value)} />
          <input type="email" required className="mc-input" placeholder="Email Address" value={email} onChange={(e) => setEmail(e.target.value)} />
          <input className="mc-input" placeholder="Phone (+91...)" value={phone} onChange={(e) => setPhone(e.target.value)} />
          <input type="password" minLength={8} required className="mc-input" placeholder="Password (min 8 chars)" value={password} onChange={(e) => setPassword(e.target.value)} />
          <select className="mc-select" value={accountType} onChange={(e) => setAccountType(e.target.value as any)}>
            <option value="CUSTOMER">Customer Account</option>
            <option value="SELLER">Seller Studio Account</option>
          </select>
          {error && <div style={{ color: 'var(--mc-burgundy)', fontSize: '0.88rem' }}>{error}</div>}
          <button type="submit" className="mc-btn mc-btn-accent">Create Account</button>
        </form>
      </div>
    </div>
  );
};

export const CustomerDashboardPage: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <div className="mc-container" style={{ padding: '3rem 1.5rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem', marginBottom: '2rem' }}>
        <div>
          <span className="mc-badge mc-badge-blush" style={{ marginBottom: '0.4rem' }}>{user?.role} ACCOUNT</span>
          <h1 style={{ fontSize: '2.4rem' }}>Hello, {user?.fullName}</h1>
        </div>
        <button type="button" className="mc-btn mc-btn-outline" onClick={async () => { await logout(); navigate('/'); }}>
          <LogOut size={15} /> Sign Out
        </button>
      </div>

      <div className="mc-grid-4">
        {[
          { to: '/missions', icon: Sparkles, title: 'My Missions', desc: 'Revisit saved purpose plans & budgets' },
          { to: '/shared-missions', icon: Users, title: 'Shared Rooms', desc: 'Collaborate with roommates & friends' },
          { to: '/possessions', icon: PackageCheck, title: 'Already-Own', desc: 'Prevent duplicate spending automatically' },
          { to: '/orders', icon: ShoppingBag, title: 'Orders & Tracking', desc: 'View native purchases & tracking' },
          { to: '/wishlist', icon: Heart, title: 'Wishlist', desc: 'Saved products for future missions' },
          { to: '/lifecycle', icon: Recycle, title: 'Lifecycle Manager', desc: 'Repair, exchange, rent & resell' },
          { to: '/addresses', icon: MapPin, title: 'Addresses', desc: 'Manage Indian delivery addresses' },
          { to: '/profile', icon: Shield, title: 'Profile & Settings', desc: 'Account details & preferences' },
        ].map((card) => (
          <Link key={card.to} to={card.to} className="mc-card" style={{ padding: '1.35rem' }}>
            <card.icon size={20} color="var(--mc-burgundy)" style={{ marginBottom: '0.7rem' }} />
            <h2 style={{ fontSize: '1.2rem', marginBottom: '0.3rem' }}>{card.title}</h2>
            <p style={{ margin: 0, fontSize: '0.84rem', color: 'var(--mc-muted)' }}>{card.desc}</p>
          </Link>
        ))}
      </div>
    </div>
  );
};

export const ProfilePage: React.FC = () => {
  const { user, refreshUser } = useAuth();
  const { showToast } = useCart();
  const [fullName, setFullName] = useState(user?.fullName || '');
  const [phone, setPhone] = useState(user?.phone || '');

  const save = async (e: React.FormEvent) => {
    e.preventDefault();
    await api.patch('/users/profile', { fullName, phone });
    await refreshUser();
    showToast('Profile saved');
  };

  return (
    <div className="mc-container" style={{ padding: '3rem 1.5rem', maxWidth: 640 }}>
      <h1 style={{ fontSize: '2.2rem', marginBottom: '1.5rem' }}>Profile & Settings</h1>
      <form onSubmit={save} className="mc-card" style={{ padding: '1.6rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        <input disabled className="mc-input" value={user?.email || ''} />
        <input className="mc-input" value={fullName} onChange={(e) => setFullName(e.target.value)} placeholder="Full Name" />
        <input className="mc-input" value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="Phone" />
        <button type="submit" className="mc-btn mc-btn-accent">Save Profile</button>
      </form>
    </div>
  );
};

export const SettingsPage: React.FC = () => {
  const [theme, setTheme] = useState(document.documentElement.getAttribute('data-theme') || 'burgundy-ivory');
  const applyTheme = (t: string) => {
    setTheme(t);
    document.documentElement.setAttribute('data-theme', t);
  };
  return (
    <div className="mc-container" style={{ padding: '3rem 1.5rem', maxWidth: 680 }}>
      <h1 style={{ fontSize: '2.2rem', marginBottom: '1rem' }}>Editorial Theme & Preferences</h1>
      <div className="mc-card" style={{ padding: '1.5rem', display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
        <button type="button" className={`mc-btn ${theme === 'burgundy-ivory' ? 'mc-btn-primary' : 'mc-btn-outline'}`} onClick={() => applyTheme('burgundy-ivory')}>
          Warm Linen (Default)
        </button>
        <button type="button" className={`mc-btn ${theme === 'chocolate-blush' ? 'mc-btn-primary' : 'mc-btn-outline'}`} onClick={() => applyTheme('chocolate-blush')}>
          Espresso & Clay
        </button>
      </div>
    </div>
  );
};

export const AddressesPage: React.FC = () => {
  const qc = useQueryClient();
  const [label, setLabel] = useState('Home');
  const [recipientName, setRecipientName] = useState('');
  const [phone, setPhone] = useState('');
  const [line1, setLine1] = useState('');
  const [city, setCity] = useState('');
  const [stateName, setStateName] = useState('');
  const [postalCode, setPostalCode] = useState('');

  const { data: addresses } = useQuery({
    queryKey: ['addresses'],
    queryFn: async () => unwrap<any[]>(await api.get('/addresses')),
  });

  const add = async (e: React.FormEvent) => {
    e.preventDefault();
    await api.post('/addresses', { label, recipientName, phone, line1, city, state: stateName, postalCode, defaultAddress: false });
    qc.invalidateQueries({ queryKey: ['addresses'] });
    setLine1('');
  };

  return (
    <div className="mc-container" style={{ padding: '3rem 1.5rem' }}>
      <h1 style={{ fontSize: '2.2rem', marginBottom: '1.5rem' }}>Saved Addresses</h1>
      <div className="mc-grid-2" style={{ gap: '2rem', alignItems: 'start' }}>
        <form onSubmit={add} className="mc-card" style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
          <h2 style={{ fontSize: '1.3rem' }}>Add New Address</h2>
          <input className="mc-input" value={label} onChange={(e) => setLabel(e.target.value)} placeholder="Label (Home, Hostel, Office)" />
          <input required className="mc-input" value={recipientName} onChange={(e) => setRecipientName(e.target.value)} placeholder="Recipient Name" />
          <input required className="mc-input" value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="Phone" />
          <input required className="mc-input" value={line1} onChange={(e) => setLine1(e.target.value)} placeholder="Street / Hostel / Flat" />
          <div className="mc-grid-3">
            <input required className="mc-input" value={city} onChange={(e) => setCity(e.target.value)} placeholder="City" />
            <input required className="mc-input" value={stateName} onChange={(e) => setStateName(e.target.value)} placeholder="State" />
            <input required className="mc-input" value={postalCode} onChange={(e) => setPostalCode(e.target.value)} placeholder="PIN" />
          </div>
          <button type="submit" className="mc-btn mc-btn-accent">Save Address</button>
        </form>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
          {(addresses || []).map((a) => (
            <div key={a.id} className="mc-linen-panel" style={{ padding: '1.1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <span className="mc-badge" style={{ marginBottom: '0.35rem' }}>{a.label}</span>
                <div style={{ fontWeight: 700 }}>{a.recipientName}</div>
                <div style={{ fontSize: '0.86rem', color: 'var(--mc-muted)' }}>{a.line1}, {a.city}, {a.state} {a.postalCode}</div>
              </div>
              <button type="button" className="mc-btn mc-btn-outline" style={{ padding: '0.35rem 0.75rem', fontSize: '0.8rem' }} onClick={async () => { await api.delete(`/addresses/${a.id}`); qc.invalidateQueries({ queryKey: ['addresses'] }); }}>
                Delete
              </button>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export const HowItWorksPage: React.FC = () => (
  <div className="mc-container" style={{ padding: '3.5rem 1.5rem', maxWidth: 920 }}>
    <h1 style={{ fontSize: '2.7rem', marginBottom: '0.75rem' }}>How MissionCart Works</h1>
    <p style={{ fontSize: '1.08rem', color: 'var(--mc-muted)', marginBottom: '2.5rem' }}>
      Instead of forcing you to open 14 browser tabs across marketplaces, MissionCart starts with your real-life goal and orchestrates verified catalogue matching, budget optimization, compatibility checks, and post-purchase lifecycle support.
    </p>
    <div className="mc-grid-2">
      {[
        { t: '1. Universal Natural-Language Understanding', d: 'Type any situation — hostel move-in, Manali trek, oily skincare, or hosting 15 friends. Runs via Gemini/OpenAI when configured, or our zero-key deterministic rule engine.' },
        { t: '2. Grounded Multi-Source Catalogue', d: 'Every recommendation maps strictly to real database records across MissionCart Direct and 13 Indian partner ecosystems. Never invented products or fake links.' },
        { t: '3. Smart Budget & Already-Own Exclusion', d: 'List items you already own to eliminate duplicates, see essential vs optional spend in INR, and get one-click budget swaps.' },
        { t: '4. Attribute-Driven Compatibility Engine', d: 'Checks Indian 230V/16A electrical load limits, room dimensions, fashion sizes, and skincare active ingredient conflicts.' },
      ].map((b) => (
        <div key={b.t} className="mc-linen-panel" style={{ padding: '1.5rem' }}>
          <h2 style={{ fontSize: '1.35rem', marginBottom: '0.45rem' }}>{b.t}</h2>
          <p style={{ margin: 0, color: 'var(--mc-muted)' }}>{b.d}</p>
        </div>
      ))}
    </div>
  </div>
);

export const AboutPage: React.FC = () => (
  <div className="mc-container" style={{ padding: '3.5rem 1.5rem', maxWidth: 860 }}>
    <h1 style={{ fontSize: '2.7rem', marginBottom: '0.8rem' }}>About MissionCart</h1>
    <p style={{ fontSize: '1.06rem', color: 'var(--mc-muted)' }}>
      MissionCart was designed around a simple observation: people do not wake up wanting to browse 400 search filters — they wake up preparing for life chapters. Moving cities, starting a first job, organizing a study corner, or building a gentle routine all deserve calm, honest, purpose-first commerce.
    </p>
  </div>
);

export const ContactPage: React.FC = () => {
  const [sent, setSent] = useState(false);
  return (
    <div className="mc-container" style={{ padding: '3.5rem 1.5rem', maxWidth: 640 }}>
      <h1 style={{ fontSize: '2.5rem', marginBottom: '0.6rem' }}>Contact MissionCart</h1>
      <p style={{ color: 'var(--mc-muted)', marginBottom: '1.5rem' }}>Questions about catalog partnerships, retailer feeds, or orders?</p>
      {sent ? (
        <div className="mc-linen-panel" style={{ padding: '2rem' }}>Message received — thank you for reaching out.</div>
      ) : (
        <form onSubmit={(e) => { e.preventDefault(); setSent(true); }} className="mc-card" style={{ padding: '1.6rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <input required className="mc-input" placeholder="Your Name" />
          <input type="email" required className="mc-input" placeholder="Email Address" />
          <textarea required rows={4} className="mc-textarea" placeholder="How can we help?" />
          <button type="submit" className="mc-btn mc-btn-accent">Send Message</button>
        </form>
      )}
    </div>
  );
};
