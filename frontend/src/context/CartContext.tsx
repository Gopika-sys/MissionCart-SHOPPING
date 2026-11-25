import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { api, unwrap } from '../services/api';
import { CartSummary, Product } from '../types';
import { useAuth } from './AuthContext';

interface CartCtx {
  cart: CartSummary | null;
  wishlist: Product[];
  drawerOpen: boolean;
  setDrawerOpen: (open: boolean) => void;
  refreshCart: () => Promise<void>;
  addToCart: (productId: number, quantity?: number, missionId?: number) => Promise<void>;
  updateCartItem: (itemId: number, quantity?: number, savedForLater?: boolean) => Promise<void>;
  removeCartItem: (itemId: number) => Promise<void>;
  toggleWishlist: (productId: number) => Promise<void>;
  toast: string | null;
  showToast: (msg: string) => void;
}

const CartContext = createContext<CartCtx | null>(null);

export function CartProvider({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  const [cart, setCart] = useState<CartSummary | null>(null);
  const [wishlist, setWishlist] = useState<Product[]>([]);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  const showToast = useCallback((msg: string) => {
    setToast(msg);
    setTimeout(() => setToast((curr) => (curr === msg ? null : curr)), 3200);
  }, []);

  const refreshCart = useCallback(async () => {
    if (!user) {
      setCart(null);
      setWishlist([]);
      return;
    }
    try {
      const [cRes, wRes] = await Promise.all([api.get('/cart'), api.get('/wishlist')]);
      setCart(unwrap<CartSummary>(cRes));
      setWishlist(unwrap<Product[]>(wRes));
    } catch {
      // ignore unauthenticated
    }
  }, [user]);

  useEffect(() => {
    refreshCart();
  }, [refreshCart]);

  const addToCart = async (productId: number, quantity = 1, missionId?: number) => {
    if (!user) {
      showToast('Please sign in to save items to your shopping bag.');
      return;
    }
    const res = await api.post('/cart/items', { productId, quantity, missionId });
    setCart(unwrap<CartSummary>(res));
    setDrawerOpen(true);
    showToast('Added to your MissionCart bag');
  };

  const updateCartItem = async (itemId: number, quantity?: number, savedForLater?: boolean) => {
    const res = await api.patch(`/cart/items/${itemId}`, { quantity, savedForLater });
    setCart(unwrap<CartSummary>(res));
  };

  const removeCartItem = async (itemId: number) => {
    const res = await api.delete(`/cart/items/${itemId}`);
    setCart(unwrap<CartSummary>(res));
  };

  const toggleWishlist = async (productId: number) => {
    if (!user) {
      showToast('Sign in to save items to your wishlist.');
      return;
    }
    const res = await api.post(`/wishlist/${productId}`);
    setWishlist(unwrap<Product[]>(res));
    showToast('Wishlist updated');
  };

  return (
    <CartContext.Provider
      value={{
        cart,
        wishlist,
        drawerOpen,
        setDrawerOpen,
        refreshCart,
        addToCart,
        updateCartItem,
        removeCartItem,
        toggleWishlist,
        toast,
        showToast,
      }}
    >
      {children}
      {toast && (
        <div
          role="status"
          style={{
            position: 'fixed',
            bottom: '1.5rem',
            right: '1.5rem',
            zIndex: 90,
            background: 'var(--mc-ink)',
            color: 'var(--mc-white)',
            padding: '0.8rem 1.3rem',
            borderRadius: 'var(--mc-radius-pill)',
            boxShadow: 'var(--mc-shadow-lift)',
            fontSize: '0.9rem',
            fontWeight: 500,
          }}
        >
          {toast}
        </div>
      )}
    </CartContext.Provider>
  );
}

export function useCart() {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error('useCart must be used inside CartProvider');
  return ctx;
}
