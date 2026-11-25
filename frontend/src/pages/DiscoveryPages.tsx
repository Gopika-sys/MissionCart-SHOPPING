import React, { useEffect, useState } from 'react';
import { Link, useParams, useSearchParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Search, ExternalLink, Heart, ShoppingBag, ArrowLeft, ShieldCheck, CheckCircle2, Recycle, X } from 'lucide-react';
import { api, unwrap, formatINR } from '../services/api';
import { Category, Product, ProductCounts, RetailerSource } from '../types';
import { ProductCard } from '../components/ProductCard';
import { useCart } from '../context/CartContext';

export const ExplorePage: React.FC = () => {
  const { data: productsData } = useQuery({
    queryKey: ['explore-verified'],
    queryFn: async () => unwrap<{ items: Product[] }>(await api.get('/products?verifiedImagesOnly=true&size=12')),
  });
  const { data: categories } = useQuery({
    queryKey: ['categories'],
    queryFn: async () => unwrap<Category[]>(await api.get('/categories')),
  });
  const { data: counts } = useQuery({
    queryKey: ['product-counts'],
    queryFn: async () => unwrap<ProductCounts>(await api.get('/products/count')),
  });

  return (
    <div className="mc-container" style={{ padding: '3rem 1.5rem' }}>
      <div style={{ maxWidth: 740, marginBottom: '2.5rem' }}>
        <span className="mc-badge mc-badge-blush" style={{ marginBottom: '0.6rem' }}>Editorial Lookbook</span>
        <h1 style={{ fontSize: 'clamp(2.2rem, 4vw, 3.3rem)', marginBottom: '0.75rem' }}>Explore by Purpose & Aesthetic</h1>
        <p style={{ color: 'var(--mc-muted)', margin: 0 }}>
          Browse {counts ? counts.imageVerifiedProducts.toLocaleString('en-IN') : ''} verified-photo products paired with attribute-checked compatibility rules.
        </p>
      </div>

      {/* Editorial Asymmetric Mosaic */}
      <div className="mc-grid-3" style={{ marginBottom: '3.5rem' }}>
        {[
          { title: 'Calm Study & Remote Desk', img: '/verified-media/hero_workspace.jpg', query: 'study', bg: 'var(--mc-card)' },
          { title: 'Hostel & First Apartment', img: '/verified-media/hero_kitchen.jpg', query: 'hostel', bg: 'var(--mc-blush)' },
          { title: 'Oily-Skin Morning & Night', img: '/verified-media/hero_skincare.jpg', query: 'skincare', bg: '#F1E6DB' },
        ].map((card) => (
          <Link key={card.title} to={`/products?q=${card.query}`} className="mc-card" style={{ overflow: 'hidden', background: card.bg }}>
            <div style={{ height: 240, overflow: 'hidden' }}>
              <img src={card.img} alt={card.title} referrerPolicy="no-referrer" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
            </div>
            <div style={{ padding: '1.25rem' }}>
              <h3 style={{ fontSize: '1.35rem', marginBottom: '0.3rem' }}>{card.title}</h3>
              <span style={{ fontSize: '0.84rem', color: 'var(--mc-burgundy)', fontWeight: 600 }}>Explore matching catalogue →</span>
            </div>
          </Link>
        ))}
      </div>

      <h2 style={{ fontSize: '2rem', marginBottom: '1.25rem' }}>Verified Photograph Collection</h2>
      <div className="mc-grid-4" style={{ marginBottom: '3.5rem' }}>
        {(productsData?.items || []).map((p) => (
          <ProductCard key={p.id} product={p} />
        ))}
      </div>

      <h2 style={{ fontSize: '1.9rem', marginBottom: '1rem' }}>Browse All Categories</h2>
      <div className="mc-grid-4">
        {(categories || []).map((c) => (
          <Link key={c.id} to={`/products?category=${c.slug}`} className="mc-linen-panel" style={{ padding: '1.2rem' }}>
            <div style={{ fontSize: '0.72rem', color: 'var(--mc-burgundy)', fontWeight: 700, textTransform: 'uppercase' }}>{c.purposeGroup || 'Category'}</div>
            <h3 style={{ fontSize: '1.15rem', margin: '0.3rem 0' }}>{c.name}</h3>
            <p style={{ margin: 0, fontSize: '0.82rem', color: 'var(--mc-muted)' }}>{c.description}</p>
          </Link>
        ))}
      </div>
    </div>
  );
};

export const ProductsPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [q, setQ] = useState(searchParams.get('q') || '');
  const [pageSize, setPageSize] = useState(24);
  const [compareList, setCompareList] = useState<Product[]>([]);

  const selectedCategories = (searchParams.get('categories') || searchParams.get('category') || '').split(',').filter(Boolean);
  const selectedSources = (searchParams.get('sources') || searchParams.get('source') || '').split(',').filter(Boolean);
  const sort = searchParams.get('sort') || 'relevance';
  const minPrice = searchParams.get('minPrice') || '';
  const maxPrice = searchParams.get('maxPrice') || '';

  useEffect(() => {
    setPageSize(24);
  }, [searchParams.toString()]);

  const { data: counts } = useQuery({
    queryKey: ['product-counts'],
    queryFn: async () => unwrap<ProductCounts>(await api.get('/products/count')),
  });
  const { data: categories } = useQuery({
    queryKey: ['categories'],
    queryFn: async () => unwrap<Category[]>(await api.get('/categories')),
  });
  const { data: sources } = useQuery({
    queryKey: ['sources'],
    queryFn: async () => unwrap<RetailerSource[]>(await api.get('/sources')),
  });

  const { data: result, isLoading } = useQuery({
    queryKey: ['products', searchParams.toString(), pageSize],
    queryFn: async () => {
      const params = new URLSearchParams();
      if (searchParams.get('q')) params.set('q', searchParams.get('q')!);
      if (selectedCategories.length > 0) params.set('categories', selectedCategories.join(','));
      if (selectedSources.length > 0) params.set('sources', selectedSources.join(','));
      if (minPrice) params.set('minPrice', minPrice);
      if (maxPrice) params.set('maxPrice', maxPrice);
      params.set('verifiedImagesOnly', 'true');
      params.set('sort', sort);
      params.set('page', '0');
      params.set('size', String(pageSize));
      return unwrap<{ items: Product[]; totalElements: number }>(await api.get(`/products?${params.toString()}`));
    },
  });

  const toggleMulti = (paramKey: string, legacyKey: string, value: string, current: string[]) => {
    const next = new URLSearchParams(searchParams);
    next.delete(legacyKey);
    const updated = current.includes(value) ? current.filter((v) => v !== value) : [...current, value];
    if (updated.length > 0) next.set(paramKey, updated.join(','));
    else next.delete(paramKey);
    setSearchParams(next);
  };

  const updateParam = (k: string, v: string) => {
    const next = new URLSearchParams(searchParams);
    if (v) next.set(k, v);
    else next.delete(k);
    setSearchParams(next);
  };

  const toggleCompare = (p: Product) => {
    setCompareList((curr) => {
      if (curr.some((x) => x.id === p.id)) return curr.filter((x) => x.id !== p.id);
      if (curr.length >= 3) return [...curr.slice(1), p];
      return [...curr, p];
    });
  };

  const items = result?.items || [];
  const totalMatched = result?.totalElements ?? 0;

  return (
    <div className="mc-container" style={{ padding: '2.5rem 1.5rem' }}>
      <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'flex-end', gap: '1rem', marginBottom: '1.6rem' }}>
        <div>
          <span className="mc-badge mc-badge-blush" style={{ marginBottom: '0.45rem' }}>
            {counts ? `${counts.imageVerifiedProducts.toLocaleString('en-IN')} Verified-Photo Products in DB` : 'Live Catalogue'}
          </span>
          <h1 style={{ fontSize: 'clamp(2rem, 3.5vw, 2.8rem)', marginBottom: '0.35rem' }}>Unified Multi-Source Catalogue</h1>
          <p style={{ margin: 0, color: 'var(--mc-muted)' }}>
            Showing {items.length.toLocaleString('en-IN')} of {totalMatched.toLocaleString('en-IN')} matching verified-photo listings.
          </p>
        </div>

        <form
          onSubmit={(e) => {
            e.preventDefault();
            updateParam('q', q.trim());
          }}
          style={{ display: 'flex', gap: '0.5rem', minWidth: 280 }}
        >
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search title, brand, tag..." className="mc-input" />
          <button type="submit" className="mc-btn mc-btn-accent"><Search size={15} /></button>
        </form>
      </div>

      {/* Category & Retailer Multi-Select Chips + Price/Sort Controls */}
      <div className="mc-linen-panel" style={{ padding: '1.2rem', marginBottom: '2rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(170px, 1fr))', gap: '0.75rem', alignItems: 'center' }}>
          <input type="number" min={0} className="mc-input" placeholder="Min Price (₹)" value={minPrice} onChange={(e) => updateParam('minPrice', e.target.value)} />
          <input type="number" min={0} className="mc-input" placeholder="Max Price (₹)" value={maxPrice} onChange={(e) => updateParam('maxPrice', e.target.value)} />
          <select value={sort} onChange={(e) => updateParam('sort', e.target.value)} className="mc-select" aria-label="Sort order">
            <option value="relevance">Sort: Relevance</option>
            <option value="price_asc">Price: Low to High</option>
            <option value="price_desc">Price: High to Low</option>
            <option value="newest">Newest Arrivals</option>
          </select>
          <button type="button" className="mc-btn mc-btn-outline" onClick={() => { setQ(''); setSearchParams({}); }}>
            Clear All Filters
          </button>
        </div>

        <div>
          <div style={{ fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.06em', color: 'var(--mc-burgundy)', marginBottom: '0.45rem' }}>
            Categories (Multi-Select)
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem' }}>
            {(categories || []).map((c) => {
              const active = selectedCategories.includes(c.slug);
              return (
                <button
                  key={c.slug}
                  type="button"
                  onClick={() => toggleMulti('categories', 'category', c.slug, selectedCategories)}
                  className={`mc-badge ${active ? 'mc-badge-burgundy' : ''}`}
                  style={{ cursor: 'pointer', background: active ? undefined : 'var(--mc-surface)' }}
                >
                  {c.name}
                </button>
              );
            })}
          </div>
        </div>

        <div>
          <div style={{ fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.06em', color: 'var(--mc-burgundy)', marginBottom: '0.45rem' }}>
            Retailer & Catalog Sources (Multi-Select)
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem' }}>
            {(sources || []).map((s) => {
              const active = selectedSources.includes(s.code);
              const count = counts?.byRetailer?.[s.code];
              return (
                <button
                  key={s.code}
                  type="button"
                  onClick={() => toggleMulti('sources', 'source', s.code, selectedSources)}
                  className={`mc-badge ${active ? 'mc-badge-burgundy' : ''}`}
                  style={{ cursor: 'pointer', background: active ? undefined : 'var(--mc-surface)' }}
                >
                  {s.name}{count ? ` (${count.toLocaleString('en-IN')})` : ''}
                </button>
              );
            })}
          </div>
        </div>
      </div>

      {/* Product Grid */}
      {isLoading ? (
        <div className="mc-grid-4">
          {Array.from({ length: 8 }).map((_, i) => (
            <div key={i} className="mc-card" style={{ height: 380, background: 'var(--mc-card)', opacity: 0.7 }} />
          ))}
        </div>
      ) : items.length === 0 ? (
        <div className="mc-linen-panel" style={{ padding: '3rem', textAlign: 'center' }}>
          <h3>No matching verified-photo items</h3>
          <p style={{ color: 'var(--mc-muted)' }}>Try adjusting your search keywords, category, or retailer chips.</p>
          <button type="button" className="mc-btn mc-btn-accent" onClick={() => { setQ(''); setSearchParams({}); }}>Reset Filters</button>
        </div>
      ) : (
        <>
          <div className="mc-grid-4">
            {items.map((p) => (
              <ProductCard
                key={p.id}
                product={p}
                showCompareToggle
                isCompared={compareList.some((c) => c.id === p.id)}
                onToggleCompare={toggleCompare}
              />
            ))}
          </div>

          {items.length < totalMatched && (
            <div style={{ textAlign: 'center', marginTop: '2.5rem' }}>
              <button type="button" className="mc-btn mc-btn-primary" onClick={() => setPageSize((s) => s + 24)}>
                Load 24 More Products ({(totalMatched - items.length).toLocaleString('en-IN')} remaining)
              </button>
            </div>
          )}
        </>
      )}

      {/* Side-by-side Compare Strip */}
      {compareList.length > 0 && (
        <div style={{ position: 'fixed', bottom: '1rem', left: '50%', transform: 'translateX(-50%)', zIndex: 70, width: 'min(920px, 94vw)', background: 'var(--mc-surface)', border: '2px solid var(--mc-burgundy)', borderRadius: 'var(--mc-radius-md)', padding: '1rem', boxShadow: 'var(--mc-shadow-lift)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.6rem' }}>
            <strong style={{ color: 'var(--mc-burgundy)' }}>Product Comparison ({compareList.length}/3)</strong>
            <button type="button" onClick={() => setCompareList([])} className="mc-btn mc-btn-outline" style={{ padding: '0.25rem 0.65rem', fontSize: '0.75rem' }}>
              <X size={13} /> Clear
            </button>
          </div>
          <div className="mc-grid-3" style={{ gap: '0.75rem' }}>
            {compareList.map((c) => (
              <div key={c.id} className="mc-linen-panel" style={{ padding: '0.7rem', fontSize: '0.8rem' }}>
                <div style={{ fontWeight: 700 }}>{c.name}</div>
                <div style={{ color: 'var(--mc-muted)' }}>{c.brandName} • {c.sourcePlatform.replace(/_/g, ' ')}</div>
                <div style={{ fontWeight: 700, color: 'var(--mc-burgundy)', marginTop: '0.25rem' }}>
                  {c.price > 0 ? formatINR(c.price) : 'Price pending feed'}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export const SearchResultsPage: React.FC = () => <ProductsPage />;

export const CategoriesPage: React.FC = () => {
  const { data: categories } = useQuery({
    queryKey: ['categories'],
    queryFn: async () => unwrap<Category[]>(await api.get('/categories')),
  });
  const { data: counts } = useQuery({
    queryKey: ['product-counts'],
    queryFn: async () => unwrap<ProductCounts>(await api.get('/products/count')),
  });

  return (
    <div className="mc-container" style={{ padding: '3rem 1.5rem' }}>
      <h1 style={{ fontSize: '2.6rem', marginBottom: '0.5rem' }}>Catalogue Categories</h1>
      <p style={{ color: 'var(--mc-muted)', marginBottom: '2rem' }}>
        Purpose-grouped categories spanning {counts ? counts.totalImportedProducts.toLocaleString('en-IN') : ''} catalogue records.
      </p>
      <div className="mc-grid-3">
        {(categories || []).map((c) => (
          <Link key={c.id} to={`/products?category=${c.slug}`} className="mc-card" style={{ padding: '1.6rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.7rem' }}>
              <span className="mc-badge mc-badge-blush">{c.purposeGroup}</span>
              {counts?.byCategory?.[c.name] !== undefined && (
                <span className="mc-badge mc-badge-caramel">{counts.byCategory[c.name].toLocaleString('en-IN')} items</span>
              )}
            </div>
            <h2 style={{ fontSize: '1.45rem', marginBottom: '0.4rem' }}>{c.name}</h2>
            <p style={{ margin: 0, fontSize: '0.9rem', color: 'var(--mc-muted)' }}>{c.description}</p>
          </Link>
        ))}
      </div>
    </div>
  );
};

export const ProductDetailPage: React.FC = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addToCart, toggleWishlist, wishlist } = useCart();

  const { data: product, isLoading } = useQuery({
    queryKey: ['product', id],
    queryFn: async () => unwrap<Product>(await api.get(`/products/${id}`)),
    enabled: Boolean(id),
  });

  const { data: alternatives } = useQuery({
    queryKey: ['product-alts', product?.id],
    queryFn: async () => unwrap<{ budget: Product[]; balanced: Product[]; premium: Product[] }>(await api.get(`/products/${product!.id}/alternatives`)),
    enabled: Boolean(product?.id),
  });

  if (isLoading) return <div className="mc-container" style={{ padding: '3rem 1.5rem' }}>Loading product details...</div>;
  if (!product) return <div className="mc-container" style={{ padding: '3rem 1.5rem' }}>Product not found.</div>;

  const isSaved = wishlist.some((w) => w.id === product.id);
  const isNative = product.productType === 'MISSIONCART_DIRECT' || product.productType === 'SELLER_MANAGED';

  return (
    <div className="mc-container" style={{ padding: '2.5rem 1.5rem' }}>
      <button type="button" onClick={() => navigate(-1)} className="mc-btn mc-btn-outline" style={{ marginBottom: '1.5rem', padding: '0.45rem 1rem' }}>
        <ArrowLeft size={15} /> Back
      </button>

      <div className="mc-grid-2" style={{ gap: '2.5rem', alignItems: 'start', marginBottom: '3.5rem' }}>
        <div className="mc-card" style={{ overflow: 'hidden', aspectRatio: '1 / 1', background: '#FAF6F0', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1.5rem' }}>
          {product.imageStatus === 'VERIFIED' && product.imageUrl ? (
            <img src={product.imageUrl} alt={product.name} referrerPolicy="no-referrer" style={{ width: '100%', height: '100%', objectFit: 'contain' }} />
          ) : (
            <div style={{ textAlign: 'center', padding: '2rem', color: 'var(--mc-muted)' }}>
              <div style={{ fontWeight: 600 }}>Awaiting Verified Product Photo</div>
              <div style={{ fontSize: '0.84rem', marginTop: '0.3rem' }}>Administrators can upload or approve a photo in the Admin Image Queue.</div>
            </div>
          )}
        </div>

        <div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', marginBottom: '0.75rem' }}>
            <span className="mc-badge mc-badge-ivory">{product.sourcePlatform.replace(/_/g, ' ')}</span>
            <span className="mc-badge mc-badge-blush">{product.priorityTier}</span>
            <span className="mc-badge mc-badge-caramel">{product.dataVerificationStatus.replace(/_/g, ' ')}</span>
          </div>

          <h1 style={{ fontSize: 'clamp(1.85rem, 3vw, 2.65rem)', marginBottom: '0.5rem' }}>{product.name}</h1>
          <div style={{ fontSize: '0.92rem', color: 'var(--mc-muted)', marginBottom: '1.1rem' }}>
            Brand: <strong>{product.brandName || 'Curated'}</strong> • SKU: {product.sku} • Category: {product.categoryName}
          </div>

          <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.75rem', marginBottom: '1.25rem', flexWrap: 'wrap' }}>
            {product.price !== null && product.price > 0 && product.priceStatus !== 'UNAVAILABLE' && product.priceStatus !== 'EXPIRED' ? (
              <>
                <span style={{ fontSize: '2rem', fontWeight: 700 }}>{formatINR(product.price)}</span>
                {product.priceVerified && product.originalPrice && product.originalPrice > product.price && (
                  <span style={{ fontSize: '1.1rem', color: 'var(--mc-muted)', textDecoration: 'line-through' }}>{formatINR(product.originalPrice)}</span>
                )}
                <span className="mc-badge mc-badge-blush">{product.priceStatus || (product.priceVerified ? 'VERIFIED' : 'ESTIMATED')}</span>
              </>
            ) : (
              <span style={{ fontSize: '1.1rem', fontWeight: 600, color: 'var(--mc-burgundy)' }}>Price unavailable — requires retailer confirmation</span>
            )}
            <span style={{ fontSize: '0.82rem', color: 'var(--mc-caramel)', fontWeight: 700 }}>{product.availabilityStatus.replace('_', ' ')}</span>
            {product.priceLastVerifiedAt && (
              <span style={{ fontSize: '0.76rem', color: 'var(--mc-muted)' }}>
                Last price verified: {new Date(product.priceLastVerifiedAt).toLocaleDateString('en-IN')}
              </span>
            )}
          </div>

          <p style={{ fontSize: '1rem', color: 'var(--mc-chocolate)', marginBottom: '1.4rem' }}>{product.description}</p>

          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.75rem', marginBottom: '1.6rem' }}>
            <button type="button" className="mc-btn mc-btn-accent" onClick={() => addToCart(product.id, 1)}>
              <ShoppingBag size={16} /> Add to MissionCart Bag
            </button>
            <button type="button" className="mc-btn mc-btn-outline" onClick={() => toggleWishlist(product.id)}>
              <Heart size={16} fill={isSaved ? 'var(--mc-burgundy)' : 'none'} /> {isSaved ? 'Saved' : 'Wishlist'}
            </button>
            {!isNative && product.sourceUrl && (
              <a href={product.sourceUrl} target="_blank" rel="noopener noreferrer" className="mc-btn mc-btn-outline">
                View on {product.sourcePlatform.replace(/_/g, ' ')} <ExternalLink size={15} />
              </a>
            )}
          </div>

          {!isNative && !product.sourceUrl && (
            <div className="mc-linen-panel" style={{ padding: '0.9rem 1rem', fontSize: '0.84rem', marginBottom: '1.4rem' }}>
              <ShieldCheck size={15} style={{ verticalAlign: 'text-bottom', marginRight: '0.35rem', color: 'var(--mc-burgundy)' }} />
              External sample record pending live {product.sourcePlatform.replace(/_/g, ' ')} affiliate/API feed connection.
            </div>
          )}

          {product.specifications && Object.keys(product.specifications).length > 0 && (
            <div className="mc-card" style={{ padding: '1.1rem', marginBottom: '1.2rem' }}>
              <h3 style={{ fontSize: '1.08rem', marginBottom: '0.6rem' }}>Structured Specifications & Attributes</h3>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem', fontSize: '0.86rem' }}>
                {Object.entries(product.specifications).map(([k, v]) => (
                  <div key={k} style={{ padding: '0.35rem 0', borderBottom: '1px solid var(--mc-border)' }}>
                    <span style={{ color: 'var(--mc-muted)' }}>{k}: </span>
                    <strong>{String(v)}</strong>
                  </div>
                ))}
              </div>
            </div>
          )}

          {product.imageStatus === 'VERIFIED' && (
            <div style={{ fontSize: '0.8rem', color: 'var(--mc-muted)', marginBottom: '1rem' }}>
              <CheckCircle2 size={13} style={{ verticalAlign: 'text-bottom', marginRight: '0.25rem', color: 'var(--mc-burgundy)' }} />
              Image Source: {product.imageSource} • License: {product.imageLicense} • Attribution: {product.imageAttribution}
            </div>
          )}

          {product.lifecycleOptions && product.lifecycleOptions.length > 0 && (
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', alignItems: 'center' }}>
              <span style={{ fontSize: '0.84rem', fontWeight: 600, display: 'inline-flex', alignItems: 'center', gap: '0.35rem' }}>
                <Recycle size={15} color="var(--mc-burgundy)" /> Lifecycle eligible:
              </span>
              {product.lifecycleOptions.map((opt) => (
                <Link key={opt} to={`/lifecycle?productId=${product.id}&type=${opt}`} className="mc-badge mc-badge-blush" style={{ cursor: 'pointer' }}>
                  {opt}
                </Link>
              ))}
            </div>
          )}
        </div>
      </div>

      {alternatives && (alternatives.budget.length > 0 || alternatives.balanced.length > 0 || alternatives.premium.length > 0) && (
        <section>
          <h2 style={{ fontSize: '1.9rem', marginBottom: '1.2rem' }}>Comparable Alternatives in {product.categoryName}</h2>
          <div className="mc-grid-4">
            {[...alternatives.budget, ...alternatives.balanced, ...alternatives.premium].slice(0, 4).map((alt) => (
              <ProductCard key={alt.id} product={alt} variant="compact" />
            ))}
          </div>
        </section>
      )}
    </div>
  );
};

export const RetailersPage: React.FC = () => {
  const { data: sources } = useQuery({
    queryKey: ['sources'],
    queryFn: async () => unwrap<RetailerSource[]>(await api.get('/sources')),
  });
  const { data: counts } = useQuery({
    queryKey: ['product-counts'],
    queryFn: async () => unwrap<ProductCounts>(await api.get('/products/count')),
  });

  return (
    <div className="mc-container" style={{ padding: '3rem 1.5rem' }}>
      <div style={{ maxWidth: 760, marginBottom: '2.5rem' }}>
        <span className="mc-badge mc-badge-blush" style={{ marginBottom: '0.6rem' }}>Multi-Source Marketplace Architecture</span>
        <h1 style={{ fontSize: '2.7rem', marginBottom: '0.6rem' }}>13 Partner Retailer Ecosystems + Open Catalog Sources</h1>
        <p style={{ color: 'var(--mc-muted)', margin: 0 }}>
          MissionCart integrates configuration-driven catalog adapters for India&apos;s leading general, fashion, skincare, desk, and electronics ecosystems alongside active open barcode catalogues.
        </p>
      </div>

      <div className="mc-grid-3">
        {(sources || []).map((s) => {
          const itemCount = counts?.byRetailer?.[s.code] || 0;
          const isActive = s.nativeCheckout || s.connectionStatus.startsWith('ACTIVE');
          return (
            <div key={s.id} className="mc-card" style={{ padding: '1.4rem', display: 'flex', flexDirection: 'column', justifyContent: 'space-between', gap: '1rem' }}>
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.6rem', gap: '0.5rem' }}>
                  <h2 style={{ fontSize: '1.25rem' }}>{s.name}</h2>
                  <span className={`mc-badge ${isActive ? 'mc-badge-burgundy' : 'mc-badge-caramel'}`}>
                    {s.nativeCheckout ? 'Native Active' : s.connectionStatus === 'ACTIVE_OPEN_DATASET' ? 'Open Feed Active' : 'Feed Pending'}
                  </span>
                </div>
                <div style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--mc-burgundy)', marginBottom: '0.4rem' }}>
                  {s.categoryFocus} • {itemCount.toLocaleString('en-IN')} listing(s)
                </div>
                <p style={{ fontSize: '0.86rem', color: 'var(--mc-muted)', margin: '0 0 0.7rem' }}>{s.supportedCategories}</p>
                <div style={{ fontSize: '0.78rem', color: 'var(--mc-muted)' }}>
                  <div><strong>Import:</strong> {s.importMethod}</div>
                  <div><strong>Images:</strong> {s.imageSourcingMethod}</div>
                </div>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: '0.75rem', borderTop: '1px solid var(--mc-border)' }}>
                <Link to={`/products?source=${s.code}`} className="mc-btn mc-btn-outline" style={{ padding: '0.4rem 0.9rem', fontSize: '0.82rem' }}>
                  Filter Products
                </Link>
                {s.officialWebsite.startsWith('http') && !s.officialWebsite.includes('missioncart.local') && (
                  <a href={s.officialWebsite} target="_blank" rel="noopener noreferrer" style={{ fontSize: '0.82rem', display: 'inline-flex', alignItems: 'center', gap: '0.25rem', color: 'var(--mc-burgundy)', fontWeight: 600 }}>
                    Official Site <ExternalLink size={13} />
                  </a>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
