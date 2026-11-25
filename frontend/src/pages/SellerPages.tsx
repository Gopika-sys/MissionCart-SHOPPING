import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { api, unwrap, formatINR } from '../services/api';
import { useCart } from '../context/CartContext';

export const SellerDashboardPage: React.FC = () => {
  const { data: products } = useQuery({
    queryKey: ['seller-products'],
    queryFn: async () => unwrap<any[]>(await api.get('/seller/products')),
  });

  return (
    <div className="mc-container" style={{ padding: '3rem 1.5rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem', marginBottom: '2rem' }}>
        <div>
          <span className="mc-badge mc-badge-caramel">Seller Studio Portal</span>
          <h1 style={{ fontSize: '2.4rem', marginTop: '0.3rem' }}>Seller Dashboard</h1>
        </div>
        <Link to="/seller/submit" className="mc-btn mc-btn-accent">Submit New Product</Link>
      </div>

      <div className="mc-grid-4" style={{ marginBottom: '2rem' }}>
        <Link to="/seller/submit" className="mc-card" style={{ padding: '1.3rem' }}><h3>Product Submission</h3><p style={{ color: 'var(--mc-muted)', fontSize: '0.86rem' }}>List native MissionCart inventory</p></Link>
        <Link to="/seller/inventory" className="mc-card" style={{ padding: '1.3rem' }}><h3>Inventory ({(products || []).length})</h3><p style={{ color: 'var(--mc-muted)', fontSize: '0.86rem' }}>Manage stock & approval state</p></Link>
        <Link to="/seller/orders" className="mc-card" style={{ padding: '1.3rem' }}><h3>Seller Orders</h3><p style={{ color: 'var(--mc-muted)', fontSize: '0.86rem' }}>Fulfillment queue</p></Link>
        <Link to="/seller/earnings" className="mc-card" style={{ padding: '1.3rem' }}><h3>Sales & Earnings</h3><p style={{ color: 'var(--mc-muted)', fontSize: '0.86rem' }}>INR settlement overview</p></Link>
      </div>

      <div className="mc-card" style={{ padding: '1.4rem' }}>
        <h2 style={{ fontSize: '1.35rem', marginBottom: '0.8rem' }}>Your Submitted Products</h2>
        {(products || []).length === 0 ? (
          <p style={{ color: 'var(--mc-muted)' }}>No seller products submitted yet.</p>
        ) : (
          (products || []).map((p) => (
            <div key={p.id} style={{ display: 'flex', justifyContent: 'space-between', padding: '0.6rem 0', borderBottom: '1px solid var(--mc-border)' }}>
              <span><strong>{p.name}</strong> ({p.sku})</span>
              <span>{formatINR(p.price)} • {p.approved ? 'Approved' : 'Awaiting Admin Approval'}</span>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export const SellerSubmitProductPage: React.FC = () => {
  const qc = useQueryClient();
  const { showToast } = useCart();
  const [sku, setSku] = useState('');
  const [name, setName] = useState('');
  const [price, setPrice] = useState('');
  const [description, setDescription] = useState('');
  const [categoryId, setCategoryId] = useState('1');
  const [missionTags, setMissionTags] = useState('daily-essentials');

  const { data: categories } = useQuery({ queryKey: ['categories'], queryFn: async () => unwrap<any[]>(await api.get('/categories')) });

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    await api.post('/seller/products', {
      sku, name, price: Number(price), categoryId: Number(categoryId), description, missionTags, stockQuantity: 10, imageStatus: 'REQUIRES_IMAGE',
    });
    setSku(''); setName(''); setPrice(''); setDescription('');
    qc.invalidateQueries({ queryKey: ['seller-products'] });
    showToast('Submitted for admin verification & approval');
  };

  return (
    <div className="mc-container" style={{ padding: '3rem 1.5rem', maxWidth: 680 }}>
      <h1 style={{ fontSize: '2.3rem', marginBottom: '1.5rem' }}>Submit Seller Product</h1>
      <form onSubmit={submit} className="mc-card" style={{ padding: '1.6rem', display: 'flex', flexDirection: 'column', gap: '0.95rem' }}>
        <input required className="mc-input" placeholder="SKU (e.g. KORA-MAT-01)" value={sku} onChange={(e) => setSku(e.target.value)} />
        <input required className="mc-input" placeholder="Product Name" value={name} onChange={(e) => setName(e.target.value)} />
        <input required type="number" className="mc-input" placeholder="Selling Price (INR)" value={price} onChange={(e) => setPrice(e.target.value)} />
        <select className="mc-select" value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
          {(categories || []).map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <input className="mc-input" placeholder="Mission tags (comma separated)" value={missionTags} onChange={(e) => setMissionTags(e.target.value)} />
        <textarea required rows={3} className="mc-textarea" placeholder="Accurate product description" value={description} onChange={(e) => setDescription(e.target.value)} />
        <button type="submit" className="mc-btn mc-btn-accent">Submit for Admin Approval</button>
      </form>
    </div>
  );
};

export const SellerInventoryPage: React.FC = () => <SellerDashboardPage />;
export const SellerOrdersPage: React.FC = () => (
  <div className="mc-container" style={{ padding: '3rem 1.5rem' }}>
    <h1 style={{ fontSize: '2.2rem', marginBottom: '0.5rem' }}>Seller Order Fulfillment</h1>
    <p style={{ color: 'var(--mc-muted)' }}>Orders containing your approved MissionCart-managed SKUs appear here after native checkout confirmation.</p>
  </div>
);
export const SellerEarningsPage: React.FC = () => (
  <div className="mc-container" style={{ padding: '3rem 1.5rem' }}>
    <h1 style={{ fontSize: '2.2rem', marginBottom: '0.5rem' }}>Sales & Earnings (INR)</h1>
    <p style={{ color: 'var(--mc-muted)' }}>Track net settlements once native orders are confirmed and delivered.</p>
  </div>
);
