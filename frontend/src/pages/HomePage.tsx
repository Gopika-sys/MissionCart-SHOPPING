import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion, useReducedMotion } from 'framer-motion';
import { useQuery } from '@tanstack/react-query';
import { Sparkles, ArrowRight, CheckCircle2, ShieldCheck, Recycle, Layers, Compass, Sliders } from 'lucide-react';
import { api, unwrap } from '../services/api';
import { Product, ProductCounts, RetailerSource } from '../types';
import { ProductCard } from '../components/ProductCard';

const EXAMPLE_PROMPTS = [
  {
    label: 'Moving into a hostel',
    prompt: "I'm moving to a hostel. My budget is ₹5,000. I need everything necessary for my first month.",
    budget: '5000',
    timeframe: 'First month',
  },
  {
    label: 'Building a study setup',
    prompt: "I'm a college student and need a productive study setup, including a desk organizer, lamp, laptop accessories, and notebooks within ₹8,000.",
    budget: '8000',
    timeframe: 'This semester',
  },
  {
    label: 'Preparing for a trip',
    prompt: "I'm going to Manali for five days. I need suitable outfits, shoes, skincare, travel accessories, and essentials within ₹7,000.",
    budget: '7000',
    timeframe: '5 days',
  },
  {
    label: 'Starting skincare',
    prompt: 'I have oily skin and need a simple morning and night skincare routine within ₹2,000.',
    budget: '2000',
    timeframe: 'Daily routine',
  },
  {
    label: 'Planning a wedding look',
    prompt: "I'm attending a wedding. I need an outfit, footwear, accessories, and grooming products within ₹6,000.",
    budget: '6000',
    timeframe: 'This weekend',
  },
  {
    label: 'Creating a home gym',
    prompt: 'I want to start working out at home. I need beginner-friendly equipment within ₹3,000.',
    budget: '3000',
    timeframe: 'Ongoing',
  },
];

const PURPOSE_COLLECTIONS = [
  { title: 'New beginnings', tag: 'hostel', desc: 'Hostel rooms, first apartments, and fresh chapters.', tint: '#F7F1E8' },
  { title: 'Home and living', tag: 'home-setup', desc: 'Ceramics, kitchenware, and calm everyday textiles.', tint: '#EFE5DA' },
  { title: 'Everyday essentials', tag: 'daily-essentials', desc: 'Reliable utilities built for daily rhythm.', tint: '#F3E5D8' },
  { title: 'Style and self-expression', tag: 'office', desc: 'Intentional wardrobes for work and celebration.', tint: '#E7C8D0' },
  { title: 'Work and study', tag: 'study', desc: 'Ergonomic desk tools and quiet-focus gear.', tint: '#EFE7DE' },
  { title: 'Travel', tag: 'travel', desc: 'Packable layers, luggage, and transit adapters.', tint: '#F5ECE2' },
  { title: 'Self-care', tag: 'skincare', desc: 'Balanced routines with ingredient-aware checks.', tint: '#EEDAE0' },
  { title: 'Fitness', tag: 'fitness', desc: 'Beginner-friendly mats, weights, and mobility gear.', tint: '#EFE6DA' },
];

const MARQUEE_ASSETS = [
  { src: '/verified-media/hero_workspace.jpg', label: 'Workspace & Study', alt: 'Minimal workspace desk with coffee and notebook' },
  { src: '/verified-media/elec_kettle.jpg', label: 'Hostel Kitchen', alt: 'Stainless electric kettle' },
  { src: '/verified-media/fash_sneakers.png', label: 'Footwear Edit', alt: 'Minimal white leather sneakers' },
  { src: '/verified-media/skin_dropper.jpg', label: 'Skincare Actives', alt: 'Glass dropper skincare bottle' },
  { src: '/verified-media/home_vase.jpg', label: 'Artisanal Ceramics', alt: 'Hand-glazed porcelain vase' },
  { src: '/verified-media/elec_headphones.jpg', label: 'Deep Focus Audio', alt: 'Over-ear wireless headphones' },
  { src: '/verified-media/fit_mat.jpg', label: 'Home Fitness', alt: 'Rolled yoga mat' },
  { src: '/verified-media/kit_mug.jpg', label: 'Morning Rituals', alt: 'Stoneware coffee cup' },
];

const HERO_PANELS = [
  { src: '/verified-media/hero_workspace.jpg', caption: 'Work & Study Curations', sub: 'Desk • Lamp • Ergonomics' },
  { src: '/verified-media/hero_kitchen.jpg', caption: 'Living & Tableware', sub: 'Kitchen • Ceramics • Pantry' },
  { src: '/verified-media/hero_skincare.jpg', caption: 'Intentional Self-Care', sub: 'Skincare • Routine Order' },
];

export const HomePage: React.FC = () => {
  const navigate = useNavigate();
  const shouldReduceMotion = useReducedMotion();
  const [prompt, setPrompt] = useState('');
  const [budget, setBudget] = useState('');
  const [timeframe, setTimeframe] = useState('');
  const [planning, setPlanning] = useState(false);
  const [planError, setPlanError] = useState<string | null>(null);
  const [heroSlide, setHeroSlide] = useState(0);
  const [newsletterEmail, setNewsletterEmail] = useState('');
  const [newsletterSaved, setNewsletterSaved] = useState(false);

  useEffect(() => {
    if (shouldReduceMotion) return;
    const t = setInterval(() => setHeroSlide((s) => (s + 1) % HERO_PANELS.length), 5200);
    return () => clearInterval(t);
  }, [shouldReduceMotion]);

  const { data: countsData } = useQuery({
    queryKey: ['product-counts'],
    queryFn: async () => unwrap<ProductCounts>(await api.get('/products/count')),
  });

  const { data: featuredData } = useQuery({
    queryKey: ['featured-verified-products'],
    queryFn: async () => unwrap<{ items: Product[] }>(await api.get('/products?verifiedImagesOnly=true&size=8')),
  });

  const { data: sourcesData } = useQuery({
    queryKey: ['retailer-sources'],
    queryFn: async () => unwrap<RetailerSource[]>(await api.get('/sources')),
  });

  const handlePlanSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!prompt.trim()) {
      setPlanError('Tell us what you are preparing for so we can build your mission.');
      return;
    }
    setPlanError(null);
    setPlanning(true);
    try {
      const res = await api.post('/missions/plan', {
        prompt: prompt.trim(),
        budget: budget ? Number(budget) : null,
        timeframe: timeframe || null,
      });
      const mission = unwrap<{ id: number }>(res);
      navigate(`/mission-result/${mission.id}`);
    } catch (err: any) {
      setPlanError(err?.response?.data?.message || 'Unable to generate mission right now.');
    } finally {
      setPlanning(false);
    }
  };

  const featuredProducts = featuredData?.items || [];
  const externalSources = (sourcesData || []).filter(
    (s) => s.code !== 'MISSIONCART_DIRECT' && s.code !== 'OPEN_BEAUTY_FACTS' && s.code !== 'OPEN_PRODUCTS_FACTS'
  );
  const verifiedTotal = countsData?.imageVerifiedProducts ?? 0;

  return (
    <div>
      {/* HERO EDITORIAL SECTION */}
      <section style={{ padding: '3.5rem 0 4.5rem', background: 'linear-gradient(180deg, var(--mc-ivory) 0%, #ECE3D5 100%)', overflow: 'hidden' }}>
        <div className="mc-container" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '3rem', alignItems: 'center' }}>
          <motion.div
            initial={shouldReduceMotion ? false : { opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.65 }}
          >
            <span className="mc-badge mc-badge-blush" style={{ marginBottom: '1.15rem' }}>
              <Sparkles size={13} /> Purpose-First Shopping Intelligence
              {verifiedTotal > 0 ? ` • ${verifiedTotal.toLocaleString('en-IN')} Verified Listings` : ''}
            </span>
            <h1 style={{ fontSize: 'clamp(2.55rem, 5vw, 4.15rem)', lineHeight: 1.04, marginBottom: '1.15rem', color: 'var(--mc-chocolate)' }}>
              Life has plans.
              <br />
              <em className="mc-editorial" style={{ fontStyle: 'italic', color: 'var(--mc-burgundy)' }}>We&apos;ve got your cart.</em>
            </h1>
            <p style={{ fontSize: '1.08rem', color: 'var(--mc-muted)', maxWidth: 560, marginBottom: '1.75rem' }}>
              From everyday essentials to your biggest plans, tell us what you need and let MissionCart make shopping simpler.
            </p>

            {/* NATURAL LANGUAGE MISSION FORM */}
            <form onSubmit={handlePlanSubmit} className="mc-card" style={{ padding: '1.25rem', background: 'rgba(255,255,255,0.96)', boxShadow: 'var(--mc-shadow-lift)', borderColor: 'var(--mc-border)' }}>
              <label htmlFor="hero-mission-prompt" style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.07em', color: 'var(--mc-burgundy)', marginBottom: '0.45rem' }}>
                What are you trying to achieve?
              </label>
              <textarea
                id="hero-mission-prompt"
                rows={3}
                className="mc-textarea"
                value={prompt}
                onChange={(e) => setPrompt(e.target.value)}
                placeholder="Describe what you're trying to do, what you need, and your budget..."
                style={{ resize: 'vertical', marginBottom: '0.85rem' }}
              />
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr auto', gap: '0.65rem', alignItems: 'center' }} className="mc-hero-controls">
                <input
                  type="number"
                  min={100}
                  className="mc-input"
                  value={budget}
                  onChange={(e) => setBudget(e.target.value)}
                  placeholder="Budget (₹ INR)"
                  aria-label="Budget in INR"
                />
                <input
                  type="text"
                  className="mc-input"
                  value={timeframe}
                  onChange={(e) => setTimeframe(e.target.value)}
                  placeholder="Timeframe (optional)"
                  aria-label="Timeframe"
                />
                <button type="submit" disabled={planning} className="mc-btn mc-btn-accent" style={{ whiteSpace: 'nowrap' }}>
                  {planning ? 'Planning...' : 'Generate My Mission'} <ArrowRight size={16} />
                </button>
              </div>
              {planError && <p style={{ color: 'var(--mc-burgundy)', fontSize: '0.85rem', margin: '0.6rem 0 0' }}>{planError}</p>}
            </form>

            {/* EXAMPLE PROMPT CHIPS */}
            <div style={{ marginTop: '1.15rem', display: 'flex', flexWrap: 'wrap', gap: '0.45rem' }}>
              {EXAMPLE_PROMPTS.map((ex) => (
                <button
                  key={ex.label}
                  type="button"
                  onClick={() => {
                    setPrompt(ex.prompt);
                    setBudget(ex.budget);
                    setTimeframe(ex.timeframe);
                  }}
                  className="mc-badge"
                  style={{ cursor: 'pointer', padding: '0.38rem 0.85rem', background: 'var(--mc-surface)' }}
                >
                  {ex.label}
                </button>
              ))}
            </div>
          </motion.div>

          {/* OVERLAPPING EDITORIAL PHOTO COMPOSITION */}
          <div style={{ position: 'relative', minHeight: 480, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <motion.div
              key={heroSlide}
              initial={shouldReduceMotion ? false : { opacity: 0.7, scale: 1.04 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ duration: 1.1 }}
              style={{
                width: '82%',
                height: 430,
                borderRadius: 'var(--mc-radius-lg)',
                overflow: 'hidden',
                boxShadow: 'var(--mc-shadow-lift)',
                border: '2px solid var(--mc-caramel)',
                position: 'relative',
                zIndex: 2,
              }}
            >
              <img
                src={HERO_PANELS[heroSlide].src}
                alt={HERO_PANELS[heroSlide].caption}
                referrerPolicy="no-referrer"
                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
              />
              <div style={{ position: 'absolute', bottom: '1rem', left: '1rem', background: 'rgba(243,237,227,0.95)', padding: '0.6rem 1rem', borderRadius: 'var(--mc-radius-sm)', border: '1px solid var(--mc-caramel)' }}>
                <div className="mc-serif" style={{ fontSize: '1.05rem', fontWeight: 600, color: 'var(--mc-chocolate)' }}>{HERO_PANELS[heroSlide].caption}</div>
                <div style={{ fontSize: '0.75rem', color: 'var(--mc-burgundy)', fontWeight: 600 }}>{HERO_PANELS[heroSlide].sub}</div>
              </div>
            </motion.div>

            {/* Overlapping Top-Left Panel */}
            <motion.div
              animate={shouldReduceMotion ? undefined : { x: [0, 8, 0], y: [0, -6, 0] }}
              transition={{ duration: 9, repeat: Infinity, ease: 'easeInOut' }}
              style={{
                position: 'absolute',
                top: 0,
                left: 0,
                width: 175,
                height: 195,
                borderRadius: 'var(--mc-radius-md)',
                overflow: 'hidden',
                border: '4px solid var(--mc-ivory)',
                boxShadow: 'var(--mc-shadow-lift)',
                zIndex: 3,
              }}
            >
              <img src="/verified-media/hero_study.jpg" alt="Student studying at desk" referrerPolicy="no-referrer" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
            </motion.div>

            {/* Overlapping Bottom-Right Panel */}
            <motion.div
              animate={shouldReduceMotion ? undefined : { x: [0, -8, 0], y: [0, 6, 0] }}
              transition={{ duration: 10, repeat: Infinity, ease: 'easeInOut' }}
              style={{
                position: 'absolute',
                bottom: 0,
                right: 0,
                width: 185,
                height: 165,
                borderRadius: 'var(--mc-radius-md)',
                overflow: 'hidden',
                border: '4px solid var(--mc-ivory)',
                boxShadow: 'var(--mc-shadow-lift)',
                zIndex: 3,
              }}
            >
              <img src="/verified-media/home_vase.jpg" alt="Handcrafted porcelain ceramic vase" referrerPolicy="no-referrer" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
            </motion.div>
          </div>
        </div>
      </section>

      {/* INFINITE MOVING PHOTOGRAPHY MARQUEE */}
      <section aria-label="Curated Visual Showcase" style={{ padding: '2.2rem 0', borderTop: '1px solid var(--mc-border)', borderBottom: '1px solid var(--mc-border)', background: 'var(--mc-surface)', overflow: 'hidden' }}>
        <div className="mc-marquee-wrap" tabIndex={0}>
          <div className="mc-marquee-track">
            {[...MARQUEE_ASSETS, ...MARQUEE_ASSETS].map((item, idx) => (
              <div
                key={`${item.label}-${idx}`}
                style={{
                  width: idx % 2 === 0 ? 220 : 275,
                  height: idx % 3 === 0 ? 185 : 165,
                  borderRadius: 'var(--mc-radius-md)',
                  overflow: 'hidden',
                  position: 'relative',
                  flexShrink: 0,
                  border: '1px solid var(--mc-border)',
                }}
              >
                <img src={item.src} alt={item.alt} referrerPolicy="no-referrer" loading="lazy" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                <span style={{ position: 'absolute', bottom: '0.6rem', left: '0.6rem', background: 'rgba(43,34,32,0.84)', color: 'var(--mc-ivory)', fontSize: '0.72rem', padding: '0.2rem 0.65rem', borderRadius: 'var(--mc-radius-pill)', border: '1px solid rgba(198,155,109,0.5)' }}>
                  {item.label}
                </span>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* SHOP BY PURPOSE */}
      <section style={{ padding: '4.5rem 0' }}>
        <div className="mc-container">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', flexWrap: 'wrap', gap: '1rem', marginBottom: '2.2rem' }}>
            <div>
              <span style={{ fontSize: '0.78rem', textTransform: 'uppercase', letterSpacing: '0.09em', color: 'var(--mc-burgundy)', fontWeight: 700 }}>Curated Entry Points</span>
              <h2 style={{ fontSize: 'clamp(1.9rem, 3.2vw, 2.75rem)', marginTop: '0.3rem' }}>Shop by purpose, not aisles.</h2>
            </div>
            <Link to="/mission-builder" className="mc-btn mc-btn-outline">Open Full Mission Builder <ArrowRight size={15} /></Link>
          </div>

          <div className="mc-grid-4">
            {PURPOSE_COLLECTIONS.map((col, idx) => (
              <motion.div
                key={col.title}
                initial={shouldReduceMotion ? false : { opacity: 0, y: 16 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true, amount: 0.2 }}
                transition={{ duration: 0.45, delay: idx * 0.04 }}
              >
                <Link
                  to={`/products?q=${encodeURIComponent(col.tag)}`}
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'space-between',
                    minHeight: 175,
                    padding: '1.4rem',
                    borderRadius: 'var(--mc-radius-md)',
                    background: col.tint,
                    border: '1px solid var(--mc-border)',
                  }}
                >
                  <div>
                    <div style={{ fontSize: '0.72rem', textTransform: 'uppercase', letterSpacing: '0.08em', color: 'var(--mc-burgundy)', fontWeight: 700, marginBottom: '0.4rem' }}>0{idx + 1}</div>
                    <h3 style={{ fontSize: '1.3rem', marginBottom: '0.45rem' }}>{col.title}</h3>
                    <p style={{ margin: 0, fontSize: '0.86rem', color: 'var(--mc-muted)' }}>{col.desc}</p>
                  </div>
                  <div style={{ marginTop: '1rem', fontSize: '0.82rem', fontWeight: 600, color: 'var(--mc-burgundy)', display: 'inline-flex', alignItems: 'center', gap: '0.35rem' }}>
                    Explore collection <ArrowRight size={14} />
                  </div>
                </Link>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* FEATURED VERIFIED-PHOTO PRODUCTS */}
      <section style={{ padding: '4rem 0', background: 'var(--mc-background-secondary)', borderTop: '1px solid var(--mc-border)', borderBottom: '1px solid var(--mc-border)' }}>
        <div className="mc-container">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', flexWrap: 'wrap', gap: '1rem', marginBottom: '2rem' }}>
            <div>
              <span className="mc-badge mc-badge-blush" style={{ marginBottom: '0.5rem' }}><CheckCircle2 size={12} /> Verified Product Photography</span>
              <h2 style={{ fontSize: 'clamp(1.9rem, 3.2vw, 2.75rem)' }}>Curated Catalogue Highlights</h2>
            </div>
            <Link to="/products" className="mc-btn mc-btn-accent">
              Browse {verifiedTotal > 0 ? `${verifiedTotal.toLocaleString('en-IN')} ` : ''}Products <ArrowRight size={15} />
            </Link>
          </div>
          <div className="mc-grid-4">
            {featuredProducts.slice(0, 8).map((p) => (
              <ProductCard key={p.id} product={p} />
            ))}
          </div>
        </div>
      </section>

      {/* SIX-STEP HOW IT WORKS */}
      <section style={{ padding: '4.5rem 0' }}>
        <div className="mc-container">
          <div style={{ maxWidth: 640, marginBottom: '2.5rem' }}>
            <span style={{ fontSize: '0.78rem', textTransform: 'uppercase', letterSpacing: '0.09em', color: 'var(--mc-burgundy)', fontWeight: 700 }}>The MissionCart Method</span>
            <h2 style={{ fontSize: 'clamp(1.9rem, 3.2vw, 2.75rem)', marginTop: '0.35rem' }}>Six thoughtful steps from life plan to doorstep.</h2>
          </div>

          <div className="mc-grid-3">
            {[
              { step: '01', icon: Compass, title: 'Tell us your purpose', desc: 'Describe any real-life situation, budget in INR, timeline, and what you already own.' },
              { step: '02', icon: Sparkles, title: 'We understand your situation', desc: 'Our AI intent parser & deterministic rule engine extract essential and optional needs.' },
              { step: '03', icon: Layers, title: 'Find products that fit', desc: 'Matches real MissionCart & partner catalog records with attribute compatibility checks.' },
              { step: '04', icon: Sliders, title: 'Optimize your budget', desc: 'Swap between budget, balanced, and premium tiers with instant INR recalculation.' },
              { step: '05', icon: ShieldCheck, title: 'Shop your selected items', desc: 'Native checkout for MissionCart stock plus transparent grouped handoff for external retailers.' },
              { step: '06', icon: Recycle, title: 'Manage products beyond purchase', desc: 'Track repairs, exchanges, rentals, and resale listings inside the Lifecycle Manager.' },
            ].map((item) => (
              <div key={item.step} className="mc-linen-panel" style={{ padding: '1.6rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.9rem' }}>
                  <span className="mc-serif" style={{ fontSize: '1.6rem', color: 'var(--mc-burgundy)' }}>{item.step}</span>
                  <item.icon size={20} color="var(--mc-caramel)" />
                </div>
                <h3 style={{ fontSize: '1.22rem', marginBottom: '0.45rem' }}>{item.title}</h3>
                <p style={{ margin: 0, fontSize: '0.9rem', color: 'var(--mc-muted)' }}>{item.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* 13 TRUSTED RETAILERS + NEWSLETTER */}
      <section style={{ padding: '3.5rem 0', background: 'var(--mc-card)', borderTop: '1px solid var(--mc-border)' }}>
        <div className="mc-container">
          <div style={{ textAlign: 'center', maxWidth: 680, margin: '0 auto 2rem' }}>
            <h2 style={{ fontSize: '2.1rem', marginBottom: '0.5rem' }}>Shop across 13 curated source ecosystems</h2>
            <p style={{ color: 'var(--mc-muted)', margin: 0 }}>
              Every external retailer source is transparently tracked with feed status and honest checkout separation.
            </p>
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'center', gap: '0.65rem', marginBottom: '3.5rem' }}>
            {externalSources.map((s) => (
              <Link key={s.code} to={`/products?source=${encodeURIComponent(s.code)}`} className="mc-badge" style={{ padding: '0.5rem 1rem', background: 'var(--mc-surface)', fontSize: '0.84rem' }}>
                {s.name}
              </Link>
            ))}
          </div>

          <div className="mc-card" style={{ padding: '2.2rem', display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: '1.5rem', borderColor: 'var(--mc-caramel)' }}>
            <div style={{ maxWidth: 500 }}>
              <h3 style={{ fontSize: '1.5rem', marginBottom: '0.35rem' }}>Curated purpose guides, once a month.</h3>
              <p style={{ margin: 0, color: 'var(--mc-muted)', fontSize: '0.92rem' }}>Seasonal checklists for hostel move-ins, travel packing, and home setups.</p>
            </div>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                if (newsletterEmail.trim()) setNewsletterSaved(true);
              }}
              style={{ display: 'flex', gap: '0.6rem', flex: '1 1 320px', maxWidth: 420 }}
            >
              <input type="email" required value={newsletterEmail} onChange={(e) => setNewsletterEmail(e.target.value)} placeholder="you@example.com" className="mc-input" />
              <button type="submit" className="mc-btn mc-btn-accent">{newsletterSaved ? 'Subscribed' : 'Subscribe'}</button>
            </form>
          </div>
        </div>
      </section>

      <style>{`
        @media (max-width: 680px) {
          .mc-hero-controls { grid-template-columns: 1fr !important; }
        }
      `}</style>
    </div>
  );
};
