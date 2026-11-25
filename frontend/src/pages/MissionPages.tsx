import React, { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Client } from '@stomp/stompjs';
import {
  Sparkles, CheckCircle2, AlertTriangle, XCircle, RefreshCw, Plus, Minus,
  Trash2, Share2, Users, ShoppingBag, ArrowRight, PackageCheck, Sliders
} from 'lucide-react';
import { api, unwrap, formatINR } from '../services/api';
import { MissionDetail, MissionItem, Product } from '../types';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';

export const MissionBuilderPage: React.FC = () => {
  const navigate = useNavigate();
  const [prompt, setPrompt] = useState('');
  const [budget, setBudget] = useState('');
  const [timeframe, setTimeframe] = useState('');
  const [location, setLocation] = useState('');
  const [alreadyOwn, setAlreadyOwn] = useState('');
  const [constraints, setConstraints] = useState('');
  const [selectedPrefs, setSelectedPrefs] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const prefsList = ['Budget-conscious', 'Minimalist aesthetic', 'Eco / Reusable', 'Travel-compact', 'Beginner-friendly', 'Fast essentials'];

  const togglePref = (p: string) => {
    setSelectedPrefs((curr) => (curr.includes(p) ? curr.filter((x) => x !== p) : [...curr, p]));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!prompt.trim()) {
      setError('Describe what you are trying to achieve.');
      return;
    }
    setError(null);
    setLoading(true);
    try {
      const res = await api.post('/missions/plan', {
        prompt: prompt.trim(),
        budget: budget ? Number(budget) : null,
        timeframe: timeframe || null,
        location: location || null,
        alreadyOwn: alreadyOwn || null,
        preferences: selectedPrefs,
        constraints: constraints || null,
      });
      const mission = unwrap<MissionDetail>(res);
      navigate(`/mission-result/${mission.id}`);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Could not plan mission.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="mc-container" style={{ padding: '3rem 1.5rem', maxWidth: 940 }}>
      <span className="mc-badge mc-badge-burgundy" style={{ marginBottom: '0.6rem' }}><Sparkles size={13} /> Universal AI Mission Planner</span>
      <h1 style={{ fontSize: 'clamp(2.2rem, 4vw, 3.2rem)', marginBottom: '0.6rem' }}>Tell us what you&apos;re preparing for.</h1>
      <p style={{ color: 'var(--mc-muted)', marginBottom: '2rem', fontSize: '1.02rem' }}>
        Enter any real-life situation — from moving into an apartment or planning a 15-friend birthday dinner to assembling a skincare routine or remote desk setup.
      </p>

      <form onSubmit={handleSubmit} className="mc-card" style={{ padding: '2rem', display: 'flex', flexDirection: 'column', gap: '1.3rem' }}>
        <div>
          <label htmlFor="mb-prompt" style={{ display: 'block', fontWeight: 600, marginBottom: '0.45rem' }}>Your Situation & Goals *</label>
          <textarea
            id="mb-prompt"
            rows={4}
            className="mc-textarea"
            value={prompt}
            onChange={(e) => setPrompt(e.target.value)}
            placeholder="e.g., I'm organizing a small birthday party for 15 friends. I need decorations, tableware, snacks, and return gifts within ₹4,500."
          />
        </div>

        <div className="mc-grid-3">
          <div>
            <label htmlFor="mb-budget" style={{ display: 'block', fontWeight: 600, fontSize: '0.88rem', marginBottom: '0.35rem' }}>Budget (INR ₹)</label>
            <input id="mb-budget" type="number" min={100} className="mc-input" value={budget} onChange={(e) => setBudget(e.target.value)} placeholder="e.g., 5000" />
          </div>
          <div>
            <label htmlFor="mb-time" style={{ display: 'block', fontWeight: 600, fontSize: '0.88rem', marginBottom: '0.35rem' }}>Timeline</label>
            <input id="mb-time" type="text" className="mc-input" value={timeframe} onChange={(e) => setTimeframe(e.target.value)} placeholder="e.g., First month, 5 days" />
          </div>
          <div>
            <label htmlFor="mb-loc" style={{ display: 'block', fontWeight: 600, fontSize: '0.88rem', marginBottom: '0.35rem' }}>City / Delivery Area</label>
            <input id="mb-loc" type="text" className="mc-input" value={location} onChange={(e) => setLocation(e.target.value)} placeholder="e.g., Bengaluru, Manali" />
          </div>
        </div>

        <div>
          <label htmlFor="mb-own" style={{ display: 'block', fontWeight: 600, fontSize: '0.88rem', marginBottom: '0.35rem' }}>Items You Already Own (Excluded Automatically)</label>
          <input
            id="mb-own"
            type="text"
            className="mc-input"
            value={alreadyOwn}
            onChange={(e) => setAlreadyOwn(e.target.value)}
            placeholder="e.g., I already have a laptop, kettle, charger, and water bottle"
          />
        </div>

        <div>
          <div style={{ fontWeight: 600, fontSize: '0.88rem', marginBottom: '0.5rem' }}>Preferences</div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.45rem' }}>
            {prefsList.map((p) => {
              const active = selectedPrefs.includes(p);
              return (
                <button
                  key={p}
                  type="button"
                  onClick={() => togglePref(p)}
                  className={`mc-badge ${active ? 'mc-badge-burgundy' : ''}`}
                  style={{ cursor: 'pointer', padding: '0.4rem 0.9rem' }}
                >
                  {p}
                </button>
              );
            })}
          </div>
        </div>

        <div>
          <label htmlFor="mb-constraints" style={{ display: 'block', fontWeight: 600, fontSize: '0.88rem', marginBottom: '0.35rem' }}>Space, Voltage, Skin, or Size Constraints</label>
          <input
            id="mb-constraints"
            type="text"
            className="mc-input"
            value={constraints}
            onChange={(e) => setConstraints(e.target.value)}
            placeholder="e.g., Desk width 90cm, oily sensitive skin, size M"
          />
        </div>

        {error && <div style={{ color: 'var(--mc-burgundy)', fontSize: '0.9rem' }}>{error}</div>}

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
          <button type="submit" disabled={loading} className="mc-btn mc-btn-accent" style={{ padding: '0.85rem 1.8rem' }}>
            {loading ? 'Analyzing Purpose & Matching Catalogue...' : 'Generate My Mission'} <ArrowRight size={16} />
          </button>
        </div>
      </form>
    </div>
  );
};

export const MissionResultPage: React.FC = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const { user } = useAuth();
  const { addToCart, showToast } = useCart();

  const [filter, setFilter] = useState<'ALL' | 'ESSENTIAL' | 'OPTIONAL' | 'WITHIN_BUDGET'>('ALL');
  const [retailerFilter, setRetailerFilter] = useState('');
  const [replacingItem, setReplacingItem] = useState<MissionItem | null>(null);
  const [alternatives, setAlternatives] = useState<{ budget: Product[]; balanced: Product[]; premium: Product[] } | null>(null);
  const [optimization, setOptimization] = useState<any>(null);
  const [customOwnedInput, setCustomOwnedInput] = useState('');

  const { data: mission, isLoading } = useQuery({
    queryKey: ['mission', id],
    queryFn: async () => unwrap<MissionDetail>(await api.get(`/missions/${id}`)),
    enabled: Boolean(id),
  });

  const patchMission = async (itemActions: any[]) => {
    const res = await api.patch(`/missions/${id}`, { itemActions });
    qc.setQueryData(['mission', id], unwrap<MissionDetail>(res));
    setOptimization(null);
  };

  const openReplace = async (item: MissionItem) => {
    setReplacingItem(item);
    const res = await api.get(`/products/${item.productId}/alternatives`);
    setAlternatives(unwrap(res));
  };

  const applyReplacement = async (newProductId: number) => {
    if (!replacingItem) return;
    await patchMission([{ itemId: replacingItem.id, replaceWithProductId: newProductId }]);
    setReplacingItem(null);
    showToast('Product swapped — budget & compatibility recalculated');
  };

  const handleOptimize = async () => {
    const res = await api.post(`/missions/${id}/optimize`);
    setOptimization(unwrap(res));
  };

  const handleShare = async () => {
    if (!user) {
      showToast('Sign in to create a shareable group mission room.');
      return;
    }
    const res = await api.post(`/missions/${id}/share`);
    const room = unwrap<{ shareToken: string }>(res);
    navigate(`/shared/${room.shareToken}`);
  };

  const addAllSelectedToBag = async () => {
    if (!user) {
      showToast('Please sign in to add mission items to your bag.');
      return;
    }
    const selected = (mission?.items || []).filter((i) => i.selected && !i.excludedOwned);
    for (const item of selected) {
      await addToCart(item.productId, item.quantity, mission!.id);
    }
  };

  if (isLoading) return <div className="mc-container" style={{ padding: '3rem 1.5rem' }}>Building mission plan...</div>;
  if (!mission) return <div className="mc-container" style={{ padding: '3rem 1.5rem' }}>Mission not found.</div>;

  const filteredItems = mission.items.filter((item) => {
    if (filter === 'ESSENTIAL' && item.priority !== 'ESSENTIAL') return false;
    if (filter === 'OPTIONAL' && item.priority !== 'OPTIONAL') return false;
    if (filter === 'WITHIN_BUDGET' && mission.budget && item.lineTotal > mission.budget * 0.45) return false;
    if (retailerFilter && item.product?.sourcePlatform !== retailerFilter) return false;
    return true;
  });

  const essentialItems = filteredItems.filter((i) => i.priority === 'ESSENTIAL');
  const optionalItems = filteredItems.filter((i) => i.priority !== 'ESSENTIAL');
  const uniqueRetailers = Array.from(new Set(mission.items.map((i) => i.product?.sourcePlatform).filter(Boolean))) as string[];

  return (
    <div className="mc-container" style={{ padding: '2.5rem 1.5rem' }}>
      {/* Mission Header */}
      <div className="mc-linen-panel" style={{ padding: '2rem', marginBottom: '2rem' }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', gap: '1.25rem', alignItems: 'flex-start' }}>
          <div style={{ maxWidth: 740 }}>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', marginBottom: '0.75rem' }}>
              <span className="mc-badge mc-badge-burgundy">{mission.purpose}</span>
              <span className="mc-badge">{mission.aiProviderUsed.replace('_', ' ')}</span>
              <span className="mc-badge mc-badge-blush">{mission.progressPercent}% Covered</span>
            </div>
            <h1 style={{ fontSize: 'clamp(1.85rem, 3.2vw, 2.7rem)', marginBottom: '0.5rem' }}>{mission.title}</h1>
            <p style={{ margin: '0 0 0.85rem', color: 'var(--mc-muted)' }}>{mission.aiExplanation}</p>
            {mission.assumptions?.length > 0 && (
              <div style={{ fontSize: '0.82rem', color: 'var(--mc-muted)' }}>
                <strong>Assumptions:</strong> {mission.assumptions.join(' • ')}
              </div>
            )}
          </div>

          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.6rem' }}>
            <button type="button" className="mc-btn mc-btn-outline" onClick={handleShare}>
              <Share2 size={15} /> Share Mission
            </button>
            <button type="button" className="mc-btn mc-btn-accent" onClick={addAllSelectedToBag}>
              <ShoppingBag size={15} /> Add Selected to Bag
            </button>
          </div>
        </div>
      </div>

      {/* SMART BUDGET PLANNER + COMPATIBILITY PANEL */}
      <div className="mc-grid-2" style={{ marginBottom: '2rem', alignItems: 'stretch' }}>
        <div className="mc-card" style={{ padding: '1.4rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.9rem' }}>
            <h2 style={{ fontSize: '1.35rem' }}>Smart Budget Planner</h2>
            {mission.budgetSummary.overBudget && (
              <button type="button" className="mc-btn mc-btn-accent" style={{ padding: '0.4rem 0.9rem', fontSize: '0.8rem' }} onClick={handleOptimize}>
                <Sliders size={14} /> Optimize Budget
              </button>
            )}
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '0.75rem', marginBottom: '0.85rem' }}>
            <div style={{ padding: '0.75rem', background: 'var(--mc-card)', borderRadius: 'var(--mc-radius-sm)' }}>
              <div style={{ fontSize: '0.74rem', color: 'var(--mc-muted)' }}>Target Budget</div>
              <strong style={{ fontSize: '1.15rem' }}>{formatINR(mission.budgetSummary.totalBudget)}</strong>
            </div>
            <div style={{ padding: '0.75rem', background: 'var(--mc-card)', borderRadius: 'var(--mc-radius-sm)' }}>
              <div style={{ fontSize: '0.74rem', color: 'var(--mc-muted)' }}>Final Payable (incl. Tax &amp; Ship)</div>
              <strong style={{ fontSize: '1.15rem', color: mission.budgetSummary.overBudget ? 'var(--mc-burgundy)' : 'var(--mc-chocolate)' }}>
                {formatINR(mission.budgetSummary.finalPayable ?? mission.budgetSummary.totalSelected)}
              </strong>
            </div>
            <div style={{ padding: '0.75rem', background: 'var(--mc-card)', borderRadius: 'var(--mc-radius-sm)' }}>
              <div style={{ fontSize: '0.74rem', color: 'var(--mc-muted)' }}>Remaining</div>
              <strong style={{ fontSize: '1.15rem', color: mission.budgetSummary.remainingBudget < 0 ? 'var(--mc-burgundy)' : 'var(--mc-caramel)' }}>
                {formatINR(mission.budgetSummary.remainingBudget)}
              </strong>
            </div>
          </div>

          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.75rem', fontSize: '0.8rem', color: 'var(--mc-muted)', marginBottom: '0.75rem' }}>
            <span>Subtotal: <strong>{formatINR(mission.budgetSummary.productSubtotal)}</strong></span>
            <span>Shipping: <strong>{formatINR(mission.budgetSummary.shippingCost)}</strong></span>
            <span>Est. GST: <strong>{formatINR(mission.budgetSummary.applicableTaxes)}</strong></span>
            {mission.budgetSummary.verifiedDiscount > 0 && (
              <span>Verified Savings: <strong>{formatINR(mission.budgetSummary.verifiedDiscount)}</strong></span>
            )}
          </div>
          {mission.budgetSummary.unpricedItemCount > 0 && (
            <div style={{ fontSize: '0.8rem', color: 'var(--mc-burgundy)', fontWeight: 600, marginBottom: '0.6rem' }}>
              {mission.budgetSummary.unpricedItemCount} selected item(s) have unavailable prices and are excluded from the verified total above.
            </div>
          )}

          <div style={{ height: 10, borderRadius: 999, background: 'var(--mc-surface)', overflow: 'hidden', marginBottom: '0.65rem' }}>
            <div
              style={{
                height: '100%',
                width: `${Math.min(100, mission.budgetSummary.utilizationPercent)}%`,
                background: mission.budgetSummary.overBudget ? 'var(--mc-burgundy)' : 'var(--mc-caramel)',
                transition: 'width 300ms ease',
              }}
            />
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.82rem', color: 'var(--mc-muted)' }}>
            <span>Essential: {formatINR(mission.budgetSummary.essentialSpending)} • Optional: {formatINR(mission.budgetSummary.optionalSpending)}</span>
            <span>Owned savings: {formatINR(mission.budgetSummary.excludedOwnedSavings)}</span>
          </div>

          {optimization && optimization.suggestions?.length > 0 && (
            <div style={{ marginTop: '1rem', padding: '0.9rem', background: 'var(--mc-card)', borderRadius: 'var(--mc-radius-sm)', border: '1px solid var(--mc-border)' }}>
              <div style={{ fontWeight: 700, fontSize: '0.88rem', marginBottom: '0.4rem' }}>
                Suggested Optimization (Save {formatINR(optimization.totalPotentialSavings)})
              </div>
              {optimization.suggestions.map((s: any, idx: number) => (
                <div key={idx} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '0.5rem', padding: '0.45rem 0', borderTop: '1px solid var(--mc-border)', fontSize: '0.82rem' }}>
                  <div>
                    <strong>{s.currentProductName}</strong> → {s.actionType === 'REPLACE_CHEAPER' ? s.suggestedProductName : 'Defer optional item'}
                    <div style={{ color: 'var(--mc-muted)' }}>{s.reason}</div>
                  </div>
                  <button
                    type="button"
                    className="mc-btn mc-btn-outline"
                    style={{ padding: '0.3rem 0.7rem', fontSize: '0.76rem' }}
                    onClick={() =>
                      s.actionType === 'REPLACE_CHEAPER'
                        ? patchMission([{ itemId: s.missionItemId, replaceWithProductId: s.suggestedProductId }])
                        : patchMission([{ itemId: s.missionItemId, selected: false }])
                    }
                  >
                    Apply (-{formatINR(s.savings)})
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* COMPATIBILITY & ALREADY-OWN PANEL */}
        <div className="mc-card" style={{ padding: '1.4rem', display: 'flex', flexDirection: 'column', justifyContent: 'space-between', gap: '1rem' }}>
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
              <h2 style={{ fontSize: '1.35rem' }}>Compatibility Engine</h2>
              <span className={`mc-badge ${mission.compatibilityReport.overallStatus === 'COMPATIBLE' ? 'mc-badge-blush' : mission.compatibilityReport.overallStatus === 'INCOMPATIBLE' ? 'mc-badge-blush' : 'mc-badge-caramel'}`}>
                {mission.compatibilityReport.overallStatus}
              </span>
            </div>
            {mission.compatibilityReport.issues.length === 0 ? (
              <p style={{ margin: 0, fontSize: '0.9rem', color: 'var(--mc-muted)' }}>
                <CheckCircle2 size={15} style={{ verticalAlign: 'text-bottom', color: 'var(--mc-caramel)', marginRight: '0.3rem' }} />
                All active items pass electronics voltage/wattage, space, size, and skincare routine layering rules.
              </p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem' }}>
                {mission.compatibilityReport.issues.map((iss, idx) => (
                  <div key={idx} style={{ padding: '0.75rem', borderRadius: 'var(--mc-radius-sm)', background: iss.status === 'INCOMPATIBLE' ? 'var(--mc-blush)' : '#F4E8DA', fontSize: '0.84rem' }}>
                    <div style={{ fontWeight: 700, display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                      {iss.status === 'INCOMPATIBLE' ? <XCircle size={15} /> : <AlertTriangle size={15} />}
                      {iss.domain}: {iss.reason}
                    </div>
                    <div style={{ color: 'var(--mc-muted)', marginTop: '0.25rem' }}>Action: {iss.recommendedAction}</div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div style={{ borderTop: '1px solid var(--mc-border)', paddingTop: '0.85rem' }}>
            <div style={{ fontSize: '0.82rem', fontWeight: 600, marginBottom: '0.4rem' }}>
              Already-Own Exclusions ({mission.existingPossessions?.length || 0})
            </div>
            <form
              onSubmit={async (e) => {
                e.preventDefault();
                if (!customOwnedInput.trim()) return;
                const needle = customOwnedInput.trim().toLowerCase();
                const matches = mission.items.filter((i) => i.product?.name.toLowerCase().includes(needle) || i.requirementLabel.toLowerCase().includes(needle));
                if (matches.length > 0) {
                  await patchMission(matches.map((m) => ({ itemId: m.id, excludedOwned: true })));
                  showToast(`Marked ${matches.length} matching item(s) as already owned`);
                } else {
                  showToast('No item in this mission matched that possession name.');
                }
                setCustomOwnedInput('');
              }}
              style={{ display: 'flex', gap: '0.5rem' }}
            >
              <input value={customOwnedInput} onChange={(e) => setCustomOwnedInput(e.target.value)} placeholder="Mark item already owned (e.g., kettle)" className="mc-input" style={{ padding: '0.5rem 0.8rem', fontSize: '0.84rem' }} />
              <button type="submit" className="mc-btn mc-btn-outline" style={{ padding: '0.5rem 0.9rem', fontSize: '0.82rem' }}>Exclude</button>
            </form>
          </div>
        </div>
      </div>

      {/* Unmatched Honest Disclosure */}
      {mission.unmatchedRequirements && mission.unmatchedRequirements.length > 0 && (
        <div className="mc-linen-panel" style={{ padding: '1rem 1.25rem', marginBottom: '1.5rem', fontSize: '0.88rem' }}>
          <strong>Catalogue Gap Notice:</strong> We couldn&apos;t find a verified matching product for:{' '}
          {mission.unmatchedRequirements.join(', ')}. Explore <Link to="/retailers" style={{ textDecoration: 'underline' }}>our 13 retailer sources</Link> or check back after catalog import.
        </div>
      )}

      {/* Filter Bar */}
      <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem' }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.45rem' }}>
          {(['ALL', 'ESSENTIAL', 'OPTIONAL', 'WITHIN_BUDGET'] as const).map((f) => (
            <button key={f} type="button" onClick={() => setFilter(f)} className={`mc-btn ${filter === f ? 'mc-btn-primary' : 'mc-btn-outline'}`} style={{ padding: '0.45rem 1rem', fontSize: '0.82rem' }}>
              {f.replace('_', ' ')}
            </button>
          ))}
        </div>
        <select value={retailerFilter} onChange={(e) => setRetailerFilter(e.target.value)} className="mc-select" style={{ width: 220 }}>
          <option value="">All Retailers in Plan</option>
          {uniqueRetailers.map((r) => <option key={r} value={r}>{r.replace('_', ' ')}</option>)}
        </select>
      </div>

      {/* Essential & Optional Lists */}
      {[{ heading: 'Essential Products', list: essentialItems }, { heading: 'Optional Upgrades & Conveniences', list: optionalItems }].map(
        (section) =>
          section.list.length > 0 && (
            <section key={section.heading} style={{ marginBottom: '2.5rem' }}>
              <h2 style={{ fontSize: '1.65rem', marginBottom: '1rem' }}>{section.heading} ({section.list.length})</h2>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.9rem' }}>
                {section.list.map((item) => (
                  <div key={item.id} className="mc-card" style={{ padding: '1.1rem', display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', gap: '1rem', alignItems: 'center', opacity: item.excludedOwned ? 0.65 : 1 }}>
                    <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', flex: '1 1 360px' }}>
                      <div style={{ width: 82, height: 82, borderRadius: 'var(--mc-radius-sm)', overflow: 'hidden', background: 'var(--mc-surface)', flexShrink: 0, display: 'grid', placeItems: 'center' }}>
                        {item.product?.imageStatus === 'VERIFIED' && item.product.imageUrl ? (
                          <img src={item.product.imageUrl} alt={item.product.name} referrerPolicy="no-referrer" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                        ) : (
                          <span style={{ fontSize: '0.68rem', color: 'var(--mc-muted)', textAlign: 'center', padding: '0.25rem' }}>Pending Photo</span>
                        )}
                      </div>
                      <div>
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem', marginBottom: '0.3rem' }}>
                          <span className={`mc-badge ${item.priority === 'ESSENTIAL' ? 'mc-badge-burgundy' : 'mc-badge-caramel'}`}>{item.priority}</span>
                          <span className="mc-badge">{item.requirementLabel}</span>
                          {item.product && <span className="mc-badge">{item.product.sourcePlatform.replace('_', ' ')}</span>}
                          <span className={`mc-badge ${item.compatibilityStatus === 'COMPATIBLE' ? 'mc-badge-blush' : 'mc-badge-blush'}`}>{item.compatibilityStatus}</span>
                        </div>
                        <Link to={item.product ? `/products/${item.product.slug}` : '#'} style={{ fontWeight: 700, fontSize: '1.05rem' }}>
                          {item.product?.name || `Product #${item.productId}`}
                        </Link>
                        <p style={{ margin: '0.25rem 0 0', fontSize: '0.84rem', color: 'var(--mc-muted)' }}>
                          {item.recommendationReason} {item.compatibilityReason && item.compatibilityStatus !== 'COMPATIBLE' ? `• ${item.compatibilityReason}` : ''}
                        </p>
                      </div>
                    </div>

                    <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '0.75rem' }}>
                      <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem', border: '1px solid var(--mc-border)', borderRadius: 'var(--mc-radius-pill)', padding: '0.2rem 0.5rem' }}>
                        <button type="button" onClick={() => patchMission([{ itemId: item.id, quantity: Math.max(1, item.quantity - 1) }])} style={{ border: 'none', background: 'none', cursor: 'pointer' }}><Minus size={13} /></button>
                        <span style={{ fontWeight: 600, fontSize: '0.85rem' }}>{item.quantity}</span>
                        <button type="button" onClick={() => patchMission([{ itemId: item.id, quantity: item.quantity + 1 }])} style={{ border: 'none', background: 'none', cursor: 'pointer' }}><Plus size={13} /></button>
                      </div>
                      <strong style={{ minWidth: 80, textAlign: 'right' }}>{item.lineTotal && item.lineTotal > 0 ? formatINR(item.lineTotal) : 'Price unavailable'}</strong>
                      <button type="button" className="mc-btn mc-btn-outline" style={{ padding: '0.4rem 0.85rem', fontSize: '0.8rem' }} onClick={() => openReplace(item)}>
                        <RefreshCw size={13} /> Replace
                      </button>
                      <button
                        type="button"
                        className="mc-btn mc-btn-outline"
                        style={{ padding: '0.4rem 0.85rem', fontSize: '0.8rem' }}
                        onClick={() => patchMission([{ itemId: item.id, selected: !item.selected, excludedOwned: false }])}
                      >
                        {item.excludedOwned ? 'Restore' : item.selected ? 'Remove' : 'Include'}
                      </button>
                      <button type="button" className="mc-btn mc-btn-primary" style={{ padding: '0.4rem 0.85rem', fontSize: '0.8rem' }} onClick={() => addToCart(item.productId, item.quantity, mission.id)}>
                        <ShoppingBag size={13} /> Bag
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )
      )}

      {/* SMART REPLACEMENTS MODAL */}
      {replacingItem && (
        <div style={{ position: 'fixed', inset: 0, zIndex: 85, background: 'rgba(28,25,23,0.45)', display: 'grid', placeItems: 'center', padding: '1rem' }}>
          <div className="mc-card" style={{ width: 'min(760px, 100%)', maxHeight: '85vh', overflowY: 'auto', padding: '1.6rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <h3 style={{ fontSize: '1.45rem' }}>Replace: {replacingItem.product?.name}</h3>
              <button type="button" className="mc-btn mc-btn-outline" onClick={() => setReplacingItem(null)}>Close</button>
            </div>
            {alternatives && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
                {(['budget', 'balanced', 'premium'] as const).map((tier) => (
                  <div key={tier}>
                    <h4 style={{ textTransform: 'uppercase', fontSize: '0.78rem', letterSpacing: '0.07em', color: 'var(--mc-burgundy)', marginBottom: '0.5rem' }}>{tier} Alternatives</h4>
                    {alternatives[tier].length === 0 ? (
                      <p style={{ fontSize: '0.84rem', color: 'var(--mc-muted)', margin: 0 }}>No alternatives in this tier for this category.</p>
                    ) : (
                      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: '0.75rem' }}>
                        {alternatives[tier].map((alt) => (
                          <div key={alt.id} className="mc-linen-panel" style={{ padding: '0.9rem', display: 'flex', flexDirection: 'column', justifyContent: 'space-between', gap: '0.5rem' }}>
                            <div>
                              <div style={{ fontSize: '0.74rem', color: 'var(--mc-muted)' }}>{alt.sourcePlatform.replace('_', ' ')}</div>
                              <div style={{ fontWeight: 600, fontSize: '0.92rem' }}>{alt.name}</div>
                              <strong style={{ fontSize: '0.95rem' }}>{alt.price && alt.price > 0 ? formatINR(alt.price) : 'Price unavailable'}</strong>
                            </div>
                            <button type="button" className="mc-btn mc-btn-primary" style={{ padding: '0.4rem 0.8rem', fontSize: '0.8rem' }} onClick={() => applyReplacement(alt.id)}>
                              Select Swap
                            </button>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export const MyMissionsPage: React.FC = () => {
  const qc = useQueryClient();
  const { data: missions, isLoading } = useQuery({
    queryKey: ['my-missions'],
    queryFn: async () => unwrap<MissionDetail[]>(await api.get('/missions')),
  });

  const handleDelete = async (id: number) => {
    await api.delete(`/missions/${id}`);
    qc.invalidateQueries({ queryKey: ['my-missions'] });
  };

  return (
    <div className="mc-container" style={{ padding: '3rem 1.5rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem', marginBottom: '2rem' }}>
        <div>
          <h1 style={{ fontSize: '2.5rem' }}>My Shopping Missions</h1>
          <p style={{ color: 'var(--mc-muted)', margin: 0 }}>All saved plans with live INR budgets and compatibility records.</p>
        </div>
        <Link to="/mission-builder" className="mc-btn mc-btn-accent"><Sparkles size={15} /> New Mission</Link>
      </div>

      {isLoading ? (
        <p>Loading your missions...</p>
      ) : (missions || []).length === 0 ? (
        <div className="mc-linen-panel" style={{ padding: '3rem', textAlign: 'center' }}>
          <h3>No saved missions yet</h3>
          <p style={{ color: 'var(--mc-muted)' }}>Create your first purpose-driven plan to see smart recommendations here.</p>
          <Link to="/mission-builder" className="mc-btn mc-btn-primary">Build a Mission</Link>
        </div>
      ) : (
        <div className="mc-grid-2">
          {(missions || []).map((m) => (
            <div key={m.id} className="mc-card" style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', justifyContent: 'space-between', gap: '1rem' }}>
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                  <span className="mc-badge mc-badge-burgundy">{m.purpose}</span>
                  <span className="mc-badge">{m.items.length} items</span>
                </div>
                <h2 style={{ fontSize: '1.4rem', marginBottom: '0.4rem' }}>{m.title}</h2>
                <p style={{ margin: 0, fontSize: '0.88rem', color: 'var(--mc-muted)' }}>{m.aiExplanation}</p>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: '0.85rem', borderTop: '1px solid var(--mc-border)' }}>
                <div>
                  <span style={{ fontSize: '0.78rem', color: 'var(--mc-muted)' }}>Selected Total: </span>
                  <strong>{formatINR(m.budgetSummary?.totalSelected)}</strong>
                </div>
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <Link to={`/mission-result/${m.id}`} className="mc-btn mc-btn-primary" style={{ padding: '0.45rem 1rem', fontSize: '0.82rem' }}>Open Plan</Link>
                  <button type="button" onClick={() => handleDelete(m.id)} className="mc-btn mc-btn-outline" style={{ padding: '0.45rem 0.7rem' }} aria-label="Delete mission"><Trash2 size={15} /></button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export const PossessionsPage: React.FC = () => {
  const qc = useQueryClient();
  const [text, setText] = useState('');
  const { data: items } = useQuery({
    queryKey: ['possessions'],
    queryFn: async () => unwrap<any[]>(await api.get('/possessions')),
  });

  const handleAdd = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!text.trim()) return;
    await api.post('/possessions', { itemText: text.trim() });
    setText('');
    qc.invalidateQueries({ queryKey: ['possessions'] });
  };

  return (
    <div className="mc-container" style={{ padding: '3rem 1.5rem', maxWidth: 880 }}>
      <h1 style={{ fontSize: '2.4rem', marginBottom: '0.45rem' }}>Already-Own Inventory</h1>
      <p style={{ color: 'var(--mc-muted)', marginBottom: '1.8rem' }}>
        Tell MissionCart what you already own in natural language so future missions skip duplicate purchases and reallocate your budget.
      </p>

      <form onSubmit={handleAdd} className="mc-card" style={{ padding: '1.25rem', display: 'flex', gap: '0.75rem', marginBottom: '2rem' }}>
        <input
          className="mc-input"
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder="e.g., I already have a laptop, kettle, charger, and water bottle"
        />
        <button type="submit" className="mc-btn mc-btn-accent" style={{ whiteSpace: 'nowrap' }}>Save Possessions</button>
      </form>

      <div className="mc-grid-3">
        {(items || []).map((p) => (
          <div key={p.id} className="mc-linen-panel" style={{ padding: '1rem 1.15rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <div style={{ fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.4rem' }}><PackageCheck size={16} color="var(--mc-caramel)" /> {p.itemName}</div>
              <div style={{ fontSize: '0.78rem', color: 'var(--mc-muted)' }}>Keywords: {p.keywords}</div>
            </div>
            <button
              type="button"
              onClick={async () => {
                await api.delete(`/possessions/${p.id}`);
                qc.invalidateQueries({ queryKey: ['possessions'] });
              }}
              style={{ border: 'none', background: 'none', cursor: 'pointer', color: 'var(--mc-muted)' }}
              aria-label="Delete possession"
            >
              <Trash2 size={15} />
            </button>
          </div>
        ))}
      </div>
    </div>
  );
};

export const SharedMissionsPage: React.FC = () => {
  const { data: rooms } = useQuery({
    queryKey: ['shared-my'],
    queryFn: async () => unwrap<MissionDetail[]>(await api.get('/shared/my')),
  });

  return (
    <div className="mc-container" style={{ padding: '3rem 1.5rem' }}>
      <h1 style={{ fontSize: '2.4rem', marginBottom: '0.4rem' }}>Shared Group Missions</h1>
      <p style={{ color: 'var(--mc-muted)', marginBottom: '2rem' }}>Coordinate apartment setups, group trips, and events with real-time WebSocket updates.</p>
      {(rooms || []).length === 0 ? (
        <div className="mc-linen-panel" style={{ padding: '2.5rem', textAlign: 'center' }}>
          <Users size={28} style={{ color: 'var(--mc-burgundy)', marginBottom: '0.5rem' }} />
          <h3>No shared missions yet</h3>
          <p style={{ color: 'var(--mc-muted)' }}>Open any mission and click &ldquo;Share Mission&rdquo; to invite roommates or friends.</p>
          <Link to="/missions" className="mc-btn mc-btn-primary">View My Missions</Link>
        </div>
      ) : (
        <div className="mc-grid-2">
          {(rooms || []).map((m) => (
            <Link key={m.id} to={`/shared/${m.shareToken}`} className="mc-card" style={{ padding: '1.4rem' }}>
              <span className="mc-badge mc-badge-blush" style={{ marginBottom: '0.5rem' }}>Live Room • {m.shareToken}</span>
              <h3 style={{ fontSize: '1.35rem', marginBottom: '0.35rem' }}>{m.title}</h3>
              <p style={{ margin: 0, color: 'var(--mc-muted)', fontSize: '0.88rem' }}>{m.items.length} shared items • Total {formatINR(m.budgetSummary.totalSelected)}</p>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
};

export const GroupMissionRoomPage: React.FC = () => {
  const { token } = useParams();
  const qc = useQueryClient();
  const { user } = useAuth();
  const { showToast } = useCart();
  const [pledgeAmount, setPledgeAmount] = useState('');

  const { data: room, isLoading } = useQuery({
    queryKey: ['shared-room', token],
    queryFn: async () => unwrap<any>(await api.get(`/shared/${token}`)),
    enabled: Boolean(token),
  });

  useEffect(() => {
    if (!token) return;
    const wsBase = import.meta.env.VITE_WS_URL || `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}/ws`;
    const client = new Client({
      brokerURL: wsBase.startsWith('http') ? wsBase.replace(/^http/, 'ws') : wsBase,
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/topic/shared/${token}`, (msg) => {
          try {
            qc.setQueryData(['shared-room', token], JSON.parse(msg.body));
          } catch {}
        });
      },
    });
    client.activate();
    return () => { client.deactivate(); };
  }, [token, qc]);

  if (isLoading) return <div className="mc-container" style={{ padding: '3rem 1.5rem' }}>Loading shared mission room...</div>;
  if (!room) return <div className="mc-container" style={{ padding: '3rem 1.5rem' }}>Shared mission room not found.</div>;

  const mission: MissionDetail = room.mission;

  const joinRoom = async () => {
    if (!user) { showToast('Sign in to join this shared mission.'); return; }
    const res = await api.post(`/shared/${token}/join`, { displayName: user.fullName });
    qc.setQueryData(['shared-room', token], unwrap(res));
    showToast('Joined shared room');
  };

  const itemAction = async (itemId: number, action: 'CLAIM' | 'UNCLAIM') => {
    if (!user) { showToast('Sign in to claim items.'); return; }
    const res = await api.patch(`/shared/${token}/items/${itemId}`, { action, claimName: user.fullName });
    qc.setQueryData(['shared-room', token], unwrap(res));
  };

  return (
    <div className="mc-container" style={{ padding: '2.5rem 1.5rem' }}>
      <div className="mc-linen-panel" style={{ padding: '1.8rem', marginBottom: '2rem', display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', gap: '1rem', alignItems: 'center' }}>
        <div>
          <span className="mc-badge mc-badge-blush" style={{ marginBottom: '0.5rem' }}>Real-Time Shared Mission Room</span>
          <h1 style={{ fontSize: '2.2rem', marginBottom: '0.35rem' }}>{mission.title}</h1>
          <p style={{ margin: 0, color: 'var(--mc-muted)' }}>
            Members ({room.members?.length || 0}): {(room.members || []).map((m: any) => `${m.memberName} (${m.role})`).join(', ')}
          </p>
        </div>
        <div style={{ display: 'flex', gap: '0.6rem' }}>
          <button type="button" className="mc-btn mc-btn-outline" onClick={() => { navigator.clipboard?.writeText(window.location.href); showToast('Invite link copied'); }}>
            Copy Invite Link
          </button>
          <button type="button" className="mc-btn mc-btn-accent" onClick={joinRoom}>Join Room</button>
        </div>
      </div>

      <div className="mc-grid-2" style={{ gap: '2rem', alignItems: 'start' }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.9rem' }}>
          <h2 style={{ fontSize: '1.5rem' }}>Shared Items & Claims</h2>
          {mission.items.map((item) => (
            <div key={item.id} className="mc-card" style={{ padding: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '1rem' }}>
              <div>
                <div style={{ fontWeight: 700 }}>{item.product?.name || item.requirementLabel}</div>
                <div style={{ fontSize: '0.84rem', color: 'var(--mc-muted)' }}>
                  {formatINR(item.lineTotal)} • {item.claimedByName ? `Claimed by ${item.claimedByName}` : 'Unclaimed'}
                </div>
              </div>
              {item.claimedByName ? (
                <button type="button" className="mc-btn mc-btn-outline" style={{ padding: '0.4rem 0.85rem', fontSize: '0.8rem' }} onClick={() => itemAction(item.id, 'UNCLAIM')}>
                  Unclaim
                </button>
              ) : (
                <button type="button" className="mc-btn mc-btn-primary" style={{ padding: '0.4rem 0.85rem', fontSize: '0.8rem' }} onClick={() => itemAction(item.id, 'CLAIM')}>
                  Claim Item
                </button>
              )}
            </div>
          ))}
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          <div className="mc-card" style={{ padding: '1.3rem' }}>
            <h3 style={{ fontSize: '1.25rem', marginBottom: '0.4rem' }}>Group Contribution Tracking (Demo)</h3>
            <p style={{ fontSize: '0.82rem', color: 'var(--mc-muted)', marginBottom: '0.85rem' }}>{room.contributionNote}</p>
            <form
              onSubmit={async (e) => {
                e.preventDefault();
                if (!user || !pledgeAmount) return;
                const res = await api.post(`/shared/${token}/contributions`, { amount: Number(pledgeAmount), note: 'Roommate share pledge' });
                qc.setQueryData(['shared-room', token], unwrap(res));
                setPledgeAmount('');
              }}
              style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.9rem' }}
            >
              <input type="number" min={50} className="mc-input" placeholder="Amount (₹)" value={pledgeAmount} onChange={(e) => setPledgeAmount(e.target.value)} />
              <button type="submit" className="mc-btn mc-btn-primary">Log Pledge</button>
            </form>
            {(room.contributions || []).map((c: any) => (
              <div key={c.id} style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', padding: '0.35rem 0', borderTop: '1px solid var(--mc-border)' }}>
                <span>{c.note} ({c.status})</span>
                <strong>{formatINR(c.amount)}</strong>
              </div>
            ))}
          </div>

          <div className="mc-card" style={{ padding: '1.3rem' }}>
            <h3 style={{ fontSize: '1.2rem', marginBottom: '0.6rem' }}>Activity History</h3>
            {(room.activityHistory || []).slice(0, 8).map((a: any) => (
              <div key={a.id} style={{ fontSize: '0.82rem', padding: '0.35rem 0', borderTop: '1px solid var(--mc-border)', color: 'var(--mc-muted)' }}>
                <strong>{a.action}</strong>: {a.details}
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
