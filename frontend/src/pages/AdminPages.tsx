import React, { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { api, unwrap, formatINR } from '../services/api';
import { useCart } from '../context/CartContext';
import { ProductCounts } from '../types';

const ADMIN_TABS = [
  'overview', 'products', 'images', 'importer', 'categories', 'sources',
  'inventory', 'orders', 'users', 'lifecycle', 'compatibility', 'analytics', 'reviews', 'settings'
] as const;

type AdminTab = typeof ADMIN_TABS[number];

export const AdminDashboardPage: React.FC<{ initialTab?: AdminTab }> = ({ initialTab = 'overview' }) => {
  const [tab, setTab] = useState<AdminTab>(initialTab);
  const qc = useQueryClient();
  const { showToast } = useCart();

  const [newSku, setNewSku] = useState('');
  const [newName, setNewName] = useState('');
  const [newPrice, setNewPrice] = useState('');
  const [newCatId, setNewCatId] = useState('1');
  const [newSource, setNewSource] = useState('MISSIONCART_DIRECT');

  const [impSource, setImpSource] = useState('AMAZON_IN');
  const [impFormat, setImpFormat] = useState<'JSON' | 'CSV'>('JSON');
  const [impContent, setImpContent] = useState('');
  const [impUpdateExisting, setImpUpdateExisting] = useState(false);
  const [impResult, setImpResult] = useState<any>(null);
  const [impBusy, setImpBusy] = useState(false);

  const [catName, setCatName] = useState('');
  const [catGroup, setCatGroup] = useState('Everyday essentials');
  const [ruleCode, setRuleCode] = useState('');
  const [ruleTitle, setRuleTitle] = useState('');
  const [ruleAction, setRuleAction] = useState('');
  const [imageFilter, setImageFilter] = useState<'PENDING' | 'ALL'>('PENDING');
  const [customImageUrls, setCustomImageUrls] = useState<Record<number, string>>({});

  const { data: overview } = useQuery({ queryKey: ['admin-overview'], queryFn: async () => unwrap<any>(await api.get('/admin/overview')) });
  const { data: counts } = useQuery({ queryKey: ['product-counts'], queryFn: async () => unwrap<ProductCounts>(await api.get('/products/count')) });
  const { data: products } = useQuery({
    queryKey: ['admin-products', tab, imageFilter],
    queryFn: async () => unwrap<any[]>(await api.get(`/admin/products?size=100${tab === 'images' && imageFilter === 'PENDING' ? '&imageStatus=REQUIRES_IMAGE' : ''}`)),
    enabled: tab === 'products' || tab === 'images' || tab === 'inventory',
  });
  const { data: categories } = useQuery({ queryKey: ['categories'], queryFn: async () => unwrap<any[]>(await api.get('/categories')) });
  const { data: sources } = useQuery({ queryKey: ['sources'], queryFn: async () => unwrap<any[]>(await api.get('/sources')) });
  const { data: orders } = useQuery({ queryKey: ['admin-orders'], queryFn: async () => unwrap<any[]>(await api.get('/admin/orders')), enabled: tab === 'orders' });
  const { data: users } = useQuery({ queryKey: ['admin-users'], queryFn: async () => unwrap<any[]>(await api.get('/admin/users')), enabled: tab === 'users' });
  const { data: lifecycle } = useQuery({ queryKey: ['admin-lifecycle'], queryFn: async () => unwrap<any[]>(await api.get('/admin/lifecycle')), enabled: tab === 'lifecycle' });
  const { data: jobs } = useQuery({ queryKey: ['admin-jobs'], queryFn: async () => unwrap<any[]>(await api.get('/admin/catalog/jobs')), enabled: tab === 'importer' || tab === 'overview' });
  const { data: rules } = useQuery({ queryKey: ['compat-rules'], queryFn: async () => unwrap<any[]>(await api.get('/compatibility/rules')), enabled: tab === 'compatibility' });
  const { data: analytics } = useQuery({ queryKey: ['admin-analytics'], queryFn: async () => unwrap<any>(await api.get('/admin/analytics')), enabled: tab === 'analytics' });
  const { data: reviews } = useQuery({ queryKey: ['admin-reviews'], queryFn: async () => unwrap<any[]>(await api.get('/admin/reviews')), enabled: tab === 'reviews' });
  const { data: settings } = useQuery({ queryKey: ['admin-settings'], queryFn: async () => unwrap<any[]>(await api.get('/admin/settings')), enabled: tab === 'settings' });
  const { data: paymentConfig } = useQuery({ queryKey: ['payment-config'], queryFn: async () => unwrap<any>(await api.get('/payments/config')) });

  const runImport = async (previewOnly: boolean) => {
    setImpBusy(true);
    try {
      const res = await api.post('/admin/catalog/import', {
        sourcePlatform: impSource,
        format: impFormat,
        fileName: `admin-upload.${impFormat.toLowerCase()}`,
        content: impContent,
        previewOnly,
        updateExisting: impUpdateExisting,
      });
      setImpResult(unwrap(res));
      if (!previewOnly) {
        qc.invalidateQueries({ queryKey: ['admin-jobs'] });
        qc.invalidateQueries({ queryKey: ['admin-products'] });
        qc.invalidateQueries({ queryKey: ['admin-overview'] });
        qc.invalidateQueries({ queryKey: ['product-counts'] });
        showToast('Bulk catalog import executed');
      }
    } catch (err: any) {
      showToast(err?.response?.data?.message || 'Import validation error');
    } finally {
      setImpBusy(false);
    }
  };

  return (
    <div className="mc-container" style={{ padding: '2.5rem 1.5rem' }}>
      <div style={{ marginBottom: '1.5rem' }}>
        <span className="mc-badge mc-badge-burgundy">Protected Admin Operations</span>
        <h1 style={{ fontSize: '2.4rem', marginTop: '0.35rem' }}>MissionCart Admin Console</h1>
      </div>

      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem', marginBottom: '2rem', paddingBottom: '1rem', borderBottom: '1px solid var(--mc-border)' }}>
        {ADMIN_TABS.map((t) => (
          <button key={t} type="button" onClick={() => setTab(t)} className={`mc-btn ${tab === t ? 'mc-btn-accent' : 'mc-btn-outline'}`} style={{ padding: '0.4rem 0.9rem', fontSize: '0.8rem', textTransform: 'capitalize' }}>
            {t}
          </button>
        ))}
      </div>

      {tab === 'overview' && overview && (
        <div className="mc-grid-4">
          {[
            ['Total DB Products', (counts?.totalImportedProducts ?? overview.totalProducts).toLocaleString('en-IN')],
            ['Image-Verified Listings', (counts?.imageVerifiedProducts ?? overview.verifiedImagesCount).toLocaleString('en-IN')],
            ['Pending Image Queue', (counts?.pendingImageProducts ?? overview.requiresImageCount).toLocaleString('en-IN')],
            ['Registered Users', overview.totalUsers],
            ['Total Orders', overview.totalOrders],
            ['Native Revenue', formatINR(overview.totalRevenue)],
            ['Active Missions', overview.activeMissions],
            ['Import Jobs', overview.importJobsCount],
          ].map(([label, val]) => (
            <div key={String(label)} className="mc-card" style={{ padding: '1.3rem' }}>
              <div style={{ fontSize: '0.78rem', color: 'var(--mc-muted)', textTransform: 'uppercase' }}>{label}</div>
              <div className="mc-serif" style={{ fontSize: '2rem', marginTop: '0.3rem', color: 'var(--mc-burgundy)' }}>{val}</div>
            </div>
          ))}
        </div>
      )}

      {tab === 'products' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              await api.post('/admin/products', {
                sku: newSku, name: newName, price: Number(newPrice), categoryId: Number(newCatId),
                sourcePlatform: newSource, productType: newSource === 'MISSIONCART_DIRECT' ? 'MISSIONCART_DIRECT' : 'EXTERNAL_CATALOG',
                description: newName, stockQuantity: 15, imageStatus: 'REQUIRES_IMAGE', published: true, approved: true,
              });
              setNewSku(''); setNewName(''); setNewPrice('');
              qc.invalidateQueries({ queryKey: ['admin-products'] });
              qc.invalidateQueries({ queryKey: ['product-counts'] });
              showToast('Product created (added to image verification queue)');
            }}
            className="mc-linen-panel"
            style={{ padding: '1.25rem', display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: '0.65rem' }}
          >
            <input required className="mc-input" placeholder="SKU (e.g. MC-NEW-01)" value={newSku} onChange={(e) => setNewSku(e.target.value)} />
            <input required className="mc-input" placeholder="Product Name" value={newName} onChange={(e) => setNewName(e.target.value)} />
            <input required type="number" className="mc-input" placeholder="Price INR" value={newPrice} onChange={(e) => setNewPrice(e.target.value)} />
            <select className="mc-select" value={newCatId} onChange={(e) => setNewCatId(e.target.value)}>
              {(categories || []).map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
            <select className="mc-select" value={newSource} onChange={(e) => setNewSource(e.target.value)}>
              {(sources || []).map((s) => <option key={s.code} value={s.code}>{s.name}</option>)}
            </select>
            <button type="submit" className="mc-btn mc-btn-accent">Add Product</button>
          </form>

          <div className="mc-card" style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.86rem' }}>
              <thead>
                <tr style={{ textAlign: 'left', borderBottom: '1px solid var(--mc-border)', background: 'var(--mc-card)' }}>
                  <th style={{ padding: '0.75rem' }}>SKU</th>
                  <th style={{ padding: '0.75rem' }}>Name</th>
                  <th style={{ padding: '0.75rem' }}>Source</th>
                  <th style={{ padding: '0.75rem' }}>Price</th>
                  <th style={{ padding: '0.75rem' }}>Image Status</th>
                  <th style={{ padding: '0.75rem' }}>Status</th>
                  <th style={{ padding: '0.75rem' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {(products || []).map((p) => (
                  <tr key={p.id} style={{ borderBottom: '1px solid var(--mc-border)' }}>
                    <td style={{ padding: '0.65rem' }}>{p.sku}</td>
                    <td style={{ padding: '0.65rem', fontWeight: 600 }}>{p.name}</td>
                    <td style={{ padding: '0.65rem' }}>{p.sourcePlatform}</td>
                    <td style={{ padding: '0.65rem' }}>{p.price > 0 ? formatINR(p.price) : 'Pending'}</td>
                    <td style={{ padding: '0.65rem' }}>{p.imageStatus}</td>
                    <td style={{ padding: '0.65rem' }}>{p.published && p.approved ? 'Live' : 'Pending'}</td>
                    <td style={{ padding: '0.65rem', display: 'flex', gap: '0.4rem' }}>
                      <button type="button" className="mc-btn mc-btn-outline" style={{ padding: '0.25rem 0.6rem', fontSize: '0.75rem' }} onClick={async () => {
                        await api.put(`/admin/products/${p.id}`, { approved: !p.approved, published: !p.published });
                        qc.invalidateQueries({ queryKey: ['admin-products'] });
                      }}>Toggle Live</button>
                      <button type="button" className="mc-btn mc-btn-outline" style={{ padding: '0.25rem 0.6rem', fontSize: '0.75rem' }} onClick={async () => {
                        await api.delete(`/admin/products/${p.id}`);
                        qc.invalidateQueries({ queryKey: ['admin-products'] });
                      }}>Delete</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {tab === 'images' && (
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem', flexWrap: 'wrap', gap: '0.75rem' }}>
            <p style={{ margin: 0, color: 'var(--mc-muted)' }}>
              Products without verified photos are isolated here and excluded from customer-facing product cards.
            </p>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button type="button" className={`mc-btn ${imageFilter === 'PENDING' ? 'mc-btn-accent' : 'mc-btn-outline'}`} style={{ padding: '0.4rem 0.9rem', fontSize: '0.8rem' }} onClick={() => setImageFilter('PENDING')}>
                Pending Queue ({counts?.pendingImageProducts ?? 0})
              </button>
              <button type="button" className={`mc-btn ${imageFilter === 'ALL' ? 'mc-btn-accent' : 'mc-btn-outline'}`} style={{ padding: '0.4rem 0.9rem', fontSize: '0.8rem' }} onClick={() => setImageFilter('ALL')}>
                First 100 All
              </button>
            </div>
          </div>
          <div className="mc-grid-3">
            {(products || []).map((p) => (
              <div key={p.id} className="mc-card" style={{ padding: '1rem', display: 'flex', flexDirection: 'column', gap: '0.6rem' }}>
                <div style={{ fontSize: '0.76rem', color: 'var(--mc-burgundy)', fontWeight: 600 }}>{p.sku} • {p.imageStatus}</div>
                <strong>{p.name}</strong>
                {p.imageUrl && <img src={p.imageUrl} alt={p.name} referrerPolicy="no-referrer" style={{ width: '100%', height: 150, objectFit: 'contain', background: 'var(--mc-card)', borderRadius: 8 }} />}
                <input
                  className="mc-input"
                  style={{ padding: '0.45rem 0.7rem', fontSize: '0.8rem' }}
                  placeholder="Paste authorized image URL (https://...)"
                  value={customImageUrls[p.id] ?? p.imageUrl ?? ''}
                  onChange={(e) => setCustomImageUrls({ ...customImageUrls, [p.id]: e.target.value })}
                />
                <div style={{ display: 'flex', gap: '0.4rem', flexWrap: 'wrap' }}>
                  <button type="button" className="mc-btn mc-btn-accent" style={{ padding: '0.3rem 0.75rem', fontSize: '0.75rem' }} onClick={async () => {
                    const url = customImageUrls[p.id] ?? p.imageUrl ?? '';
                    if (!url) { showToast('Provide an authorized image URL first'); return; }
                    await api.patch(`/admin/products/${p.id}/image`, { imageUrl: url, imageStatus: 'VERIFIED', imageSource: 'Admin Verified' });
                    qc.invalidateQueries({ queryKey: ['admin-products'] });
                    qc.invalidateQueries({ queryKey: ['product-counts'] });
                    showToast('Image verified and published to storefront');
                  }}>Verify & Approve</button>
                  <button type="button" className="mc-btn mc-btn-outline" style={{ padding: '0.3rem 0.65rem', fontSize: '0.75rem' }} onClick={async () => {
                    await api.patch(`/admin/products/${p.id}/image`, { imageStatus: 'REQUIRES_IMAGE', imageUrl: '' });
                    qc.invalidateQueries({ queryKey: ['admin-products'] });
                    qc.invalidateQueries({ queryKey: ['product-counts'] });
                  }}>Flag Pending</button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {tab === 'importer' && (
        <div className="mc-grid-2" style={{ gap: '2rem', alignItems: 'start' }}>
          <div className="mc-card" style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '0.9rem' }}>
            <h2 style={{ fontSize: '1.4rem' }}>Bulk Catalog Importer (JSON / CSV)</h2>
            <div className="mc-grid-2">
              <select className="mc-select" value={impSource} onChange={(e) => setImpSource(e.target.value)}>
                {(sources || []).map((s) => <option key={s.code} value={s.code}>{s.name}</option>)}
              </select>
              <select className="mc-select" value={impFormat} onChange={(e) => setImpFormat(e.target.value as any)}>
                <option value="JSON">JSON</option>
                <option value="CSV">CSV</option>
              </select>
            </div>
            <input
              type="file"
              accept=".json,.csv"
              className="mc-input"
              onChange={async (e) => {
                const f = e.target.files?.[0];
                if (f) {
                  setImpFormat(f.name.toLowerCase().endsWith('.csv') ? 'CSV' : 'JSON');
                  setImpContent(await f.text());
                }
              }}
            />
            <textarea
              rows={8}
              className="mc-textarea"
              value={impContent}
              onChange={(e) => setImpContent(e.target.value)}
              placeholder={impFormat === 'JSON'
                ? '[{"sku":"AMZ-901","name":"Study Booklight","categorySlug":"study-workspace","price":499}]'
                : 'sku,name,categorySlug,price\nNYK-901,Gentle Gel Cleanser,skincare,399'}
            />
            <label style={{ display: 'flex', gap: '0.45rem', alignItems: 'center', fontSize: '0.86rem' }}>
              <input type="checkbox" checked={impUpdateExisting} onChange={(e) => setImpUpdateExisting(e.target.checked)} />
              Update existing products on SKU / sourceProductId match
            </label>
            <div style={{ display: 'flex', gap: '0.65rem' }}>
              <button type="button" disabled={impBusy} className="mc-btn mc-btn-outline" onClick={() => runImport(true)}>Preview & Validate</button>
              <button type="button" disabled={impBusy} className="mc-btn mc-btn-accent" onClick={() => runImport(false)}>{impBusy ? 'Importing...' : 'Approve & Batch Import'}</button>
            </div>

            {impResult && (
              <div className="mc-linen-panel" style={{ padding: '1rem', fontSize: '0.84rem' }}>
                <strong>Result ({impResult.previewOnly ? 'Preview' : 'Committed'}):</strong> Total {impResult.totalRecords} • Inserted {impResult.insertedCount} • Updated {impResult.updatedCount} • Skipped Dup {impResult.skippedDuplicateCount} • Errors {impResult.errorCount}
                {(impResult.rows || []).slice(0, 50).map((r: any) => (
                  <div key={r.rowIndex} style={{ borderTop: '1px solid var(--mc-border)', paddingTop: '0.3rem', marginTop: '0.3rem' }}>
                    Row {r.rowIndex} [{r.sku}]: <strong>{r.status}</strong> — {r.message}
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="mc-card" style={{ padding: '1.5rem' }}>
            <h2 style={{ fontSize: '1.4rem', marginBottom: '0.8rem' }}>Import Job History</h2>
            {(jobs || []).map((j) => (
              <div key={j.id} style={{ padding: '0.65rem 0', borderTop: '1px solid var(--mc-border)', fontSize: '0.85rem' }}>
                <strong>{j.sourcePlatform} ({j.format})</strong> — {j.status}
                <div style={{ color: 'var(--mc-muted)' }}>Inserted: {j.insertedCount}, Skipped Dup: {j.skippedDuplicateCount}, Errors: {j.errorCount}</div>
              </div>
            ))}
          </div>
        </div>
      )}

      {tab === 'categories' && (
        <div className="mc-grid-2" style={{ gap: '1.5rem', alignItems: 'start' }}>
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              await api.post('/admin/categories', { name: catName, purposeGroup: catGroup, description: catName });
              setCatName('');
              qc.invalidateQueries({ queryKey: ['categories'] });
              showToast('Category created');
            }}
            className="mc-card"
            style={{ padding: '1.4rem', display: 'flex', flexDirection: 'column', gap: '0.8rem' }}
          >
            <h2 style={{ fontSize: '1.3rem' }}>Add Category</h2>
            <input required className="mc-input" placeholder="Category Name" value={catName} onChange={(e) => setCatName(e.target.value)} />
            <input className="mc-input" placeholder="Purpose Group" value={catGroup} onChange={(e) => setCatGroup(e.target.value)} />
            <button type="submit" className="mc-btn mc-btn-accent">Create Category</button>
          </form>
          <div className="mc-card" style={{ padding: '1.4rem' }}>
            {(categories || []).map((c) => (
              <div key={c.id} style={{ padding: '0.5rem 0', borderBottom: '1px solid var(--mc-border)', display: 'flex', justifyContent: 'space-between' }}>
                <strong>{c.name}</strong>
                <span className="mc-badge">{c.slug}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {tab === 'sources' && (
        <div className="mc-grid-3">
          {(sources || []).map((s) => (
            <div key={s.id} className="mc-card" style={{ padding: '1.2rem' }}>
              <strong>{s.name}</strong>
              <div style={{ fontSize: '0.82rem', color: 'var(--mc-muted)', margin: '0.3rem 0 0.7rem' }}>
                {s.connectionStatus} • {s.importMethod} • {(counts?.byRetailer?.[s.code] ?? 0).toLocaleString('en-IN')} listings
              </div>
              <button type="button" className="mc-btn mc-btn-outline" style={{ padding: '0.3rem 0.7rem', fontSize: '0.76rem' }} onClick={async () => {
                const next = s.connectionStatus === 'ACTIVE' ? 'CONFIGURED_FEED_PENDING' : 'ACTIVE';
                await api.patch(`/admin/sources/${s.id}`, { connectionStatus: next });
                qc.invalidateQueries({ queryKey: ['sources'] });
              }}>Toggle Status</button>
            </div>
          ))}
        </div>
      )}

      {tab === 'inventory' && (
        <div className="mc-card" style={{ padding: '1.2rem' }}>
          {(products || []).map((p) => (
            <div key={p.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.55rem 0', borderBottom: '1px solid var(--mc-border)' }}>
              <span><strong>{p.name}</strong> ({p.sku})</span>
              <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                <span>Stock: <strong>{p.stockQuantity}</strong></span>
                <button type="button" className="mc-btn mc-btn-outline" style={{ padding: '0.25rem 0.65rem', fontSize: '0.76rem' }} onClick={async () => {
                  await api.patch(`/admin/products/${p.id}/stock`, { stockQuantity: p.stockQuantity + 10, reason: 'Admin restock +10' });
                  qc.invalidateQueries({ queryKey: ['admin-products'] });
                }}>+10 Stock</button>
              </div>
            </div>
          ))}
        </div>
      )}

      {tab === 'orders' && (
        <div className="mc-card" style={{ padding: '1.2rem' }}>
          {(orders || []).map((o) => (
            <div key={o.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.65rem 0', borderBottom: '1px solid var(--mc-border)' }}>
              <div><strong>{o.orderNumber}</strong> • {o.status} • {formatINR(o.totalAmount)}</div>
              <button type="button" className="mc-btn mc-btn-outline" style={{ padding: '0.25rem 0.7rem', fontSize: '0.76rem' }} onClick={async () => {
                await api.patch(`/admin/orders/${o.id}`, { trackingStatus: 'SHIPPED' });
                qc.invalidateQueries({ queryKey: ['admin-orders'] });
              }}>Mark Shipped</button>
            </div>
          ))}
        </div>
      )}

      {tab === 'users' && (
        <div className="mc-card" style={{ padding: '1.2rem' }}>
          {(users || []).map((u) => (
            <div key={u.id} style={{ display: 'flex', justifyContent: 'space-between', padding: '0.55rem 0', borderBottom: '1px solid var(--mc-border)' }}>
              <span><strong>{u.fullName}</strong> ({u.email}) — {u.role}</span>
              <span className="mc-badge mc-badge-blush">{u.enabled ? 'Enabled' : 'Disabled'}</span>
            </div>
          ))}
        </div>
      )}

      {tab === 'lifecycle' && (
        <div className="mc-card" style={{ padding: '1.2rem' }}>
          {(lifecycle || []).map((lr) => (
            <div key={lr.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.65rem 0', borderBottom: '1px solid var(--mc-border)' }}>
              <span>#{lr.id} <strong>{lr.requestType}</strong> • Product #{lr.productId} • {lr.status}</span>
              <button type="button" className="mc-btn mc-btn-outline" style={{ padding: '0.25rem 0.7rem', fontSize: '0.76rem' }} onClick={async () => {
                await api.patch(`/admin/lifecycle/${lr.id}`, { status: 'REVIEWED_BY_ADMIN' });
                qc.invalidateQueries({ queryKey: ['admin-lifecycle'] });
              }}>Mark Reviewed</button>
            </div>
          ))}
        </div>
      )}

      {tab === 'compatibility' && (
        <div className="mc-grid-2" style={{ gap: '1.5rem', alignItems: 'start' }}>
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              await api.post('/admin/compatibility/rules', { ruleCode, domain: 'ELECTRONICS', title: ruleTitle, description: ruleTitle, conditionJson: '{}', severity: 'WARNING', recommendedAction: ruleAction, active: true });
              setRuleCode(''); setRuleTitle(''); setRuleAction('');
              qc.invalidateQueries({ queryKey: ['compat-rules'] });
            }}
            className="mc-card"
            style={{ padding: '1.4rem', display: 'flex', flexDirection: 'column', gap: '0.75rem' }}
          >
            <h2 style={{ fontSize: '1.3rem' }}>Add Compatibility Rule</h2>
            <input required className="mc-input" placeholder="Rule Code" value={ruleCode} onChange={(e) => setRuleCode(e.target.value)} />
            <input required className="mc-input" placeholder="Rule Title" value={ruleTitle} onChange={(e) => setRuleTitle(e.target.value)} />
            <input required className="mc-input" placeholder="Recommended Action" value={ruleAction} onChange={(e) => setRuleAction(e.target.value)} />
            <button type="submit" className="mc-btn mc-btn-accent">Save Rule</button>
          </form>
          <div className="mc-card" style={{ padding: '1.4rem' }}>
            {(rules || []).map((r) => (
              <div key={r.id} style={{ padding: '0.6rem 0', borderBottom: '1px solid var(--mc-border)' }}>
                <strong>{r.ruleCode}</strong> ({r.domain}) — {r.title}
              </div>
            ))}
          </div>
        </div>
      )}

      {tab === 'analytics' && analytics && (
        <div className="mc-grid-2" style={{ gap: '1.5rem' }}>
          <div className="mc-card" style={{ padding: '1.4rem' }}>
            <h2 style={{ fontSize: '1.3rem', marginBottom: '0.8rem' }}>Missions by Purpose ({analytics.totalMissions})</h2>
            {Object.entries(analytics.missionsByPurpose || {}).map(([k, v]) => (
              <div key={k} style={{ display: 'flex', justifyContent: 'space-between', padding: '0.4rem 0', borderBottom: '1px solid var(--mc-border)' }}>
                <span>{k}</span><strong>{String(v)}</strong>
              </div>
            ))}
          </div>
          <div className="mc-card" style={{ padding: '1.4rem' }}>
            <h2 style={{ fontSize: '1.3rem', marginBottom: '0.8rem' }}>AI Providers Used</h2>
            {Object.entries(analytics.missionsByAiProvider || {}).map(([k, v]) => (
              <div key={k} style={{ display: 'flex', justifyContent: 'space-between', padding: '0.4rem 0', borderBottom: '1px solid var(--mc-border)' }}>
                <span>{k}</span><strong>{String(v)}</strong>
              </div>
            ))}
          </div>
        </div>
      )}

      {tab === 'reviews' && (
        <div className="mc-card" style={{ padding: '1.2rem' }}>
          {(reviews || []).length === 0 ? <p style={{ color: 'var(--mc-muted)' }}>No customer reviews submitted yet.</p> : (reviews || []).map((r) => (
            <div key={r.id} style={{ padding: '0.6rem 0', borderBottom: '1px solid var(--mc-border)' }}>
              <strong>{r.rating}/5</strong> — {r.title}: {r.comment}
            </div>
          ))}
        </div>
      )}

      {tab === 'settings' && (
        <div className="mc-card" style={{ padding: '1.4rem' }}>
          <h2 style={{ fontSize: '1.3rem', marginBottom: '0.6rem' }}>System &amp; Payment Configuration</h2>
          {paymentConfig && (
            <div className="mc-linen-panel" style={{ padding: '1rem', marginBottom: '1rem', fontSize: '0.88rem' }}>
              <strong>Payment Gateway Mode:</strong> {paymentConfig.mode} • Razorpay Configured: {String(paymentConfig.razorpayConfigured)} • Webhook Configured: {String(paymentConfig.webhookConfigured)} • Methods: {(paymentConfig.methods || []).join(', ')}
            </div>
          )}
          <p style={{ color: 'var(--mc-muted)', fontSize: '0.88rem' }}>
            AI provider, Razorpay credentials, and admin bootstrap are driven securely via environment variables (`.env`). Secrets are never exposed to the frontend.
          </p>
          {(settings || []).map((s) => (
            <div key={s.settingKey} style={{ padding: '0.5rem 0', borderBottom: '1px solid var(--mc-border)' }}>
              <strong>{s.settingKey}</strong>: {s.settingValue}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
