import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Heart, ShoppingBag, ExternalLink, Star, CheckCircle2, ArrowUpRight } from 'lucide-react';
import { Product } from '../types';
import { formatINR } from '../services/api';
import { useCart } from '../context/CartContext';

interface ProductCardProps {
  product: Product;
  variant?: 'editorial' | 'compact';
  showCompareToggle?: boolean;
  isCompared?: boolean;
  onToggleCompare?: (product: Product) => void;
}

export const ProductCard: React.FC<ProductCardProps> = ({
  product,
  variant = 'editorial',
  showCompareToggle = false,
  isCompared = false,
  onToggleCompare,
}) => {
  const { addToCart, toggleWishlist, wishlist } = useCart();
  const [hovered, setHovered] = useState(false);
  const [imgError, setImgError] = useState(false);

  const hasPhoto = Boolean(product.imageUrl) && product.imageStatus === 'VERIFIED' && !imgError;
  const altImage = product.additionalImages?.find((img) => img.imageUrl !== product.imageUrl)?.imageUrl;
  const activeUrl = hovered && altImage ? altImage : product.imageUrl;
  const isSaved = wishlist.some((w) => w.id === product.id);
  const isNative = product.productType === 'MISSIONCART_DIRECT' || product.productType === 'SELLER_MANAGED';

  const hasVerifiedPrice =
    product.price !== null &&
    product.price > 0 &&
    product.priceStatus !== 'UNAVAILABLE' &&
    product.priceStatus !== 'EXPIRED';
  const hasVerifiedDiscount =
    hasVerifiedPrice &&
    product.priceVerified &&
    Boolean(product.originalPrice) &&
    Number(product.originalPrice) > Number(product.price);
  const discountPct = hasVerifiedDiscount
    ? Math.round(((Number(product.originalPrice) - Number(product.price)) / Number(product.originalPrice)) * 100)
    : 0;

  return (
    <article
      className="mc-card"
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        overflow: 'hidden',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        background: 'var(--mc-surface)',
        borderColor: hovered ? 'var(--mc-caramel)' : 'var(--mc-border)',
        boxShadow: hovered ? 'var(--mc-shadow-lift)' : 'var(--mc-shadow-soft)',
        transform: hovered ? 'translateY(-4px)' : 'translateY(0)',
      }}
    >
      <div>
        <Link
          to={`/products/${product.slug}`}
          style={{
            display: 'block',
            position: 'relative',
            overflow: 'hidden',
            aspectRatio: variant === 'editorial' ? '1 / 1' : '4 / 3',
            background: 'linear-gradient(180deg, #FAF6F0 0%, var(--mc-card) 100%)',
            padding: '0.9rem',
          }}
        >
          {hasPhoto && activeUrl ? (
            <img
              src={activeUrl}
              alt={product.name}
              loading="lazy"
              referrerPolicy="no-referrer"
              onError={() => setImgError(true)}
              style={{
                width: '100%',
                height: '100%',
                objectFit: 'contain',
                borderRadius: 'var(--mc-radius-sm)',
                transition: 'transform 420ms cubic-bezier(0.22, 1, 0.36, 1)',
                transform: hovered ? 'scale(1.05)' : 'scale(1)',
              }}
            />
          ) : (
            <div
              style={{
                width: '100%',
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                textAlign: 'center',
                color: 'var(--mc-muted)',
              }}
            >
              <span className="mc-serif" style={{ fontSize: '1.1rem', color: 'var(--mc-chocolate)' }}>
                {product.brandName || product.categoryName}
              </span>
              <span style={{ fontSize: '0.75rem', marginTop: '0.25rem' }}>Admin image queue</span>
            </div>
          )}

          {/* Top source & priority badges */}
          <div
            style={{
              position: 'absolute',
              top: '0.65rem',
              left: '0.65rem',
              right: '0.65rem',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'flex-start',
              gap: '0.35rem',
              pointerEvents: 'none',
            }}
          >
            <span className="mc-badge mc-badge-ivory" style={{ fontSize: '0.68rem', textTransform: 'uppercase', backdropFilter: 'blur(6px)' }}>
              {product.sourcePlatform.replace(/_/g, ' ')}
            </span>
            <span className="mc-badge mc-badge-blush" style={{ fontSize: '0.68rem' }}>
              {product.priorityTier}
            </span>
          </div>

          {/* Wishlist Floating Heart */}
          <button
            type="button"
            aria-label="Toggle wishlist"
            onClick={(e) => {
              e.preventDefault();
              toggleWishlist(product.id);
            }}
            style={{
              position: 'absolute',
              bottom: '0.65rem',
              right: '0.65rem',
              width: 34,
              height: 34,
              borderRadius: '50%',
              border: '1px solid var(--mc-border)',
              background: 'rgba(255,255,255,0.95)',
              display: 'grid',
              placeItems: 'center',
              cursor: 'pointer',
              boxShadow: 'var(--mc-shadow-soft)',
            }}
          >
            <Heart
              size={15}
              fill={isSaved ? 'var(--mc-burgundy)' : 'none'}
              color={isSaved ? 'var(--mc-burgundy)' : 'var(--mc-chocolate)'}
            />
          </button>
        </Link>

        <div style={{ padding: '0.95rem 1.05rem 0.5rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '0.4rem', fontSize: '0.74rem', color: 'var(--mc-muted)', marginBottom: '0.25rem' }}>
            <span style={{ fontWeight: 600, color: 'var(--mc-caramel)', textTransform: 'uppercase', letterSpacing: '0.04em', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {product.brandName || product.categoryName || 'Curated'}
            </span>
            {product.ratingVerified && product.rating ? (
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.2rem', color: 'var(--mc-chocolate)', fontWeight: 600 }}>
                <Star size={12} fill="var(--mc-caramel)" color="var(--mc-caramel)" /> {product.rating}
              </span>
            ) : (
              <span style={{ fontSize: '0.7rem', color: 'var(--mc-burgundy)', fontWeight: 600 }}>
                {product.availabilityStatus === 'IN_STOCK' ? 'Available' : 'Check Stock'}
              </span>
            )}
          </div>

          <Link to={`/products/${product.slug}`}>
            <h3
              style={{
                fontSize: '1.04rem',
                lineHeight: 1.28,
                marginBottom: '0.35rem',
                color: 'var(--mc-chocolate)',
                display: '-webkit-box',
                WebkitLineClamp: 2,
                WebkitBoxOrient: 'vertical',
                overflow: 'hidden',
                minHeight: '2.66rem',
              }}
            >
              {product.name}
            </h3>
          </Link>

          <p
            style={{
              margin: 0,
              fontSize: '0.82rem',
              color: 'var(--mc-muted)',
              display: '-webkit-box',
              WebkitLineClamp: 2,
              WebkitBoxOrient: 'vertical',
              overflow: 'hidden',
              minHeight: '2.6rem',
            }}
          >
            {product.description}
          </p>

          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.35rem', marginTop: '0.55rem' }}>
            {product.specifications && Object.keys(product.specifications).length > 0 && (
              <span className="mc-badge mc-badge-caramel" style={{ fontSize: '0.67rem' }}>
                <CheckCircle2 size={10} /> Spec Checked
              </span>
            )}
            {showCompareToggle && onToggleCompare && (
              <button
                type="button"
                onClick={() => onToggleCompare(product)}
                className={`mc-badge ${isCompared ? 'mc-badge-burgundy' : ''}`}
                style={{ cursor: 'pointer', fontSize: '0.67rem' }}
              >
                {isCompared ? 'Comparing' : '+ Compare'}
              </button>
            )}
          </div>
        </div>
      </div>

      <div style={{ padding: '0.75rem 1.05rem 1.05rem', borderTop: '1px solid var(--mc-border)', marginTop: '0.6rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', gap: '0.4rem', marginBottom: '0.65rem' }}>
          <div>
            {hasVerifiedPrice ? (
              <>
                <span style={{ fontSize: '1.08rem', fontWeight: 700, color: 'var(--mc-chocolate)' }}>
                  {formatINR(product.price)}
                </span>
                {hasVerifiedDiscount && (
                  <>
                    <span style={{ fontSize: '0.8rem', color: 'var(--mc-muted)', textDecoration: 'line-through', marginLeft: '0.4rem' }}>
                      {formatINR(product.originalPrice)}
                    </span>
                    <span style={{ fontSize: '0.72rem', color: 'var(--mc-burgundy)', fontWeight: 700, marginLeft: '0.35rem' }}>
                      {discountPct}% off
                    </span>
                  </>
                )}
              </>
            ) : (
              <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--mc-muted)' }}>
                Price pending retailer feed
              </span>
            )}
          </div>

          {isNative ? (
            <span className="mc-badge mc-badge-caramel" style={{ fontSize: '0.67rem' }}>Native Stock</span>
          ) : product.sourceUrl ? (
            <a href={product.sourceUrl} target="_blank" rel="noopener noreferrer" style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--mc-burgundy)', display: 'inline-flex', alignItems: 'center', gap: '0.2rem' }}>
              Source <ExternalLink size={11} />
            </a>
          ) : null}
        </div>

        <div style={{ display: 'flex', gap: '0.45rem' }}>
          <button
            type="button"
            className="mc-btn mc-btn-accent"
            style={{ flex: 1, padding: '0.52rem 0.75rem', fontSize: '0.8rem' }}
            onClick={() => addToCart(product.id, 1)}
          >
            <ShoppingBag size={13} /> Add to Bag
          </button>
          <Link
            to={`/products/${product.slug}`}
            className="mc-btn mc-btn-outline"
            style={{ padding: '0.52rem 0.75rem', fontSize: '0.8rem' }}
            aria-label={`View details for ${product.name}`}
          >
            Details <ArrowUpRight size={13} />
          </Link>
        </div>
      </div>
    </article>
  );
};
