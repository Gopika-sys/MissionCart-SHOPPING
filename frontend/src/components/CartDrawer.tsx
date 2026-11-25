import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { X, Trash2, Plus, Minus, ExternalLink, AlertTriangle, ShoppingBag } from 'lucide-react';
import { useCart } from '../context/CartContext';
import { formatINR } from '../services/api';

export const CartDrawer: React.FC = () => {
  const { cart, drawerOpen, setDrawerOpen, updateCartItem, removeCartItem } = useCart();
  const navigate = useNavigate();

  if (!drawerOpen) return null;

  const activeItems = cart?.activeItems || [];
  const externalGroups = cart?.externalRetailerGroups || [];

  return (
    <div style={{ position: 'fixed', inset: 0, zIndex: 80, display: 'flex', justifyContent: 'flex-end' }}>
      <div
        onClick={() => setDrawerOpen(false)}
        style={{ position: 'absolute', inset: 0, background: 'rgba(43, 34, 32, 0.46)', backdropFilter: 'blur(3px)' }}
      />
      <aside
        aria-label="Shopping Bag"
        style={{
          position: 'relative',
          width: 'min(480px, 100vw)',
          height: '100%',
          background: 'var(--mc-ivory)',
          borderLeft: '1px solid var(--mc-border)',
          display: 'flex',
          flexDirection: 'column',
          zIndex: 81,
        }}
      >
        <div style={{ padding: '1.35rem 1.5rem', borderBottom: '1px solid var(--mc-border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'var(--mc-surface)' }}>
          <div>
            <h2 style={{ fontSize: '1.45rem' }}>Your Shopping Bag</h2>
            <p style={{ margin: 0, fontSize: '0.82rem', color: 'var(--mc-muted)' }}>
              {activeItems.length} item(s) • Native & external retailers separated cleanly
            </p>
          </div>
          <button type="button" className="mc-btn mc-btn-outline" style={{ padding: '0.5rem' }} onClick={() => setDrawerOpen(false)} aria-label="Close bag">
            <X size={18} />
          </button>
        </div>

        <div style={{ flex: 1, overflowY: 'auto', padding: '1.25rem 1.5rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {cart?.compatibilityReport && cart.compatibilityReport.issues.length > 0 && (
            <div style={{ padding: '0.85rem 1rem', borderRadius: 'var(--mc-radius-sm)', background: 'var(--mc-blush)', border: '1px solid var(--mc-burgundy)', fontSize: '0.84rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem', fontWeight: 700, color: 'var(--mc-burgundy)' }}>
                <AlertTriangle size={15} /> Compatibility Note ({cart.compatibilityReport.overallStatus})
              </div>
              <p style={{ margin: '0.35rem 0 0', color: 'var(--mc-chocolate)' }}>{cart.compatibilityReport.issues[0].reason}</p>
            </div>
          )}

          {activeItems.length === 0 ? (
            <div className="mc-linen-panel" style={{ padding: '2.2rem 1.5rem', textAlign: 'center' }}>
              <ShoppingBag size={28} style={{ color: 'var(--mc-burgundy)', marginBottom: '0.6rem' }} />
              <h3 style={{ fontSize: '1.2rem', marginBottom: '0.4rem' }}>Your bag is empty</h3>
              <p style={{ fontSize: '0.88rem', color: 'var(--mc-muted)', marginBottom: '1.1rem' }}>
                Start a purpose-driven shopping mission or browse verified essentials.
              </p>
              <Link to="/mission-builder" className="mc-btn mc-btn-accent" onClick={() => setDrawerOpen(false)}>
                Plan a Mission
              </Link>
            </div>
          ) : (
            activeItems.map((item) => (
              <div key={item.id} className="mc-card" style={{ padding: '0.9rem', display: 'flex', gap: '0.9rem', alignItems: 'center' }}>
                <div style={{ width: 68, height: 68, borderRadius: 'var(--mc-radius-sm)', overflow: 'hidden', background: 'var(--mc-card)', flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '0.25rem' }}>
                  {item.product.imageStatus === 'VERIFIED' && item.product.imageUrl ? (
                    <img src={item.product.imageUrl} alt={item.product.name} referrerPolicy="no-referrer" style={{ width: '100%', height: '100%', objectFit: 'contain' }} />
                  ) : (
                    <span style={{ fontSize: '0.66rem', color: 'var(--mc-muted)', textAlign: 'center' }}>Product</span>
                  )}
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: '0.72rem', color: 'var(--mc-burgundy)', fontWeight: 600, textTransform: 'uppercase' }}>
                    {item.product.sourcePlatform.replace(/_/g, ' ')} {item.nativeCheckoutEligible ? '• Native' : '• External'}
                  </div>
                  <Link to={`/products/${item.product.slug}`} onClick={() => setDrawerOpen(false)} style={{ fontWeight: 600, fontSize: '0.93rem', display: 'block', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {item.product.name}
                  </Link>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: '0.45rem' }}>
                    <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem', border: '1px solid var(--mc-border)', borderRadius: 'var(--mc-radius-pill)', padding: '0.15rem 0.4rem' }}>
                      <button type="button" onClick={() => updateCartItem(item.id, item.quantity - 1)} style={{ border: 'none', background: 'none', cursor: 'pointer', padding: '0.15rem' }}><Minus size={13} /></button>
                      <span style={{ fontSize: '0.84rem', fontWeight: 600 }}>{item.quantity}</span>
                      <button type="button" onClick={() => updateCartItem(item.id, item.quantity + 1)} style={{ border: 'none', background: 'none', cursor: 'pointer', padding: '0.15rem' }}><Plus size={13} /></button>
                    </div>
                    <span style={{ fontWeight: 700, fontSize: '0.94rem' }}>{item.unitPrice && item.unitPrice > 0 ? formatINR(item.lineTotal) : 'Price unavailable'}</span>
                  </div>
                </div>
                <button type="button" onClick={() => removeCartItem(item.id)} aria-label="Remove item" style={{ border: 'none', background: 'none', color: 'var(--mc-muted)', cursor: 'pointer' }}>
                  <Trash2 size={16} />
                </button>
              </div>
            ))
          )}

          {externalGroups.length > 0 && (
            <div style={{ marginTop: '0.5rem', padding: '0.95rem', background: 'var(--mc-card)', borderRadius: 'var(--mc-radius-md)', border: '1px solid var(--mc-border)' }}>
              <div style={{ fontSize: '0.78rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '0.4rem', color: 'var(--mc-burgundy)' }}>
                External Retailer Groups
              </div>
              <p style={{ margin: '0 0 0.7rem', fontSize: '0.8rem', color: 'var(--mc-muted)' }}>
                Each external retailer handles its own cart, shipping, and returns independently.
              </p>
              {externalGroups.map((g) => (
                <div key={g.sourceCode} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.45rem 0', borderTop: '1px solid var(--mc-border)' }}>
                  <span style={{ fontSize: '0.86rem', fontWeight: 600 }}>{g.sourceName} ({g.items.length})</span>
                  <span style={{ fontSize: '0.86rem' }}>{g.subtotal > 0 ? formatINR(g.subtotal) : 'Price pending'}</span>
                </div>
              ))}
            </div>
          )}
        </div>

        {activeItems.length > 0 && (
          <div style={{ padding: '1.25rem 1.5rem', borderTop: '1px solid var(--mc-border)', background: 'var(--mc-surface)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.35rem', fontSize: '0.9rem' }}>
              <span style={{ color: 'var(--mc-muted)' }}>MissionCart Native Subtotal</span>
              <strong>{formatINR(cart?.nativeSubtotal)}</strong>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem', fontSize: '1.05rem' }}>
              <span>Combined Estimated Total</span>
              <strong>{formatINR(cart?.grandTotal)}</strong>
            </div>
            <div style={{ display: 'flex', gap: '0.75rem' }}>
              <button type="button" className="mc-btn mc-btn-outline" style={{ flex: 1 }} onClick={() => { setDrawerOpen(false); navigate('/cart'); }}>
                Full Bag
              </button>
              <button type="button" className="mc-btn mc-btn-accent" style={{ flex: 1.4 }} onClick={() => { setDrawerOpen(false); navigate('/checkout'); }}>
                Checkout <ExternalLink size={14} />
              </button>
            </div>
          </div>
        )}
      </aside>
    </div>
  );
};
