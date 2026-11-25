import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '../context/AuthContext';
import { CartProvider } from '../context/CartContext';
import { ProductCard } from '../components/ProductCard';
import { HomePage } from '../pages/HomePage';
import { formatINR } from '../services/api';
import { Product } from '../types';

function renderWithProviders(ui: React.ReactElement) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <AuthProvider>
        <CartProvider>
          <MemoryRouter>{ui}</MemoryRouter>
        </CartProvider>
      </AuthProvider>
    </QueryClientProvider>
  );
}

const sampleVerifiedProduct: Product = {
  id: 1,
  sku: 'MC-ELEC-001',
  name: '1.5L Stainless Electric Kettle',
  slug: 'electric-kettle-mcelec001',
  brandName: 'Philips Home',
  categoryId: 1,
  categoryName: 'Electronics',
  sourcePlatform: 'MISSIONCART_DIRECT',
  productType: 'MISSIONCART_DIRECT',
  description: 'Quick-boil 1500W kettle',
  price: 1299,
  originalPrice: 1699,
  currency: 'INR',
  priceVerified: true,
  availabilityStatus: 'IN_STOCK',
  stockQuantity: 24,
  ratingVerified: false,
  dataVerificationStatus: 'MISSIONCART_CATALOG_VERIFIED',
  specifications: { wattage: 1500 },
  attributes: {},
  missionTags: ['hostel', 'kitchen'],
  priorityTier: 'BUDGET',
  lifecycleOptions: ['REPAIR'],
  imageUrl: '/verified-media/elec_kettle.jpg',
  imageSource: 'Wikimedia Commons',
  imageLicense: 'CC BY-SA',
  imageStatus: 'VERIFIED',
  additionalImages: [],
  published: true,
  approved: true,
};

const sampleOpenCatalogProduct: Product = {
  ...sampleVerifiedProduct,
  id: 2,
  sku: 'OBF-8904417306521',
  name: 'Sali-cinamide Anti-acne Serum',
  brandName: 'The Derma Co',
  slug: 'salicinamide-serum-obf890',
  sourcePlatform: 'OPEN_BEAUTY_FACTS',
  productType: 'EXTERNAL_CATALOG',
  price: 0,
  priceVerified: false,
  imageUrl: 'https://images.openbeautyfacts.org/images/products/890/441/730/6521/front_en.3.400.jpg',
  imageStatus: 'VERIFIED',
};

describe('MissionCart Redesigned Frontend Suite', () => {
  it('formats INR currency properly', () => {
    expect(formatINR(5000)).toContain('5,000');
  });

  it('renders genuine product photos without pending-verification banners on customer cards', () => {
    renderWithProviders(
      <>
        <ProductCard product={sampleVerifiedProduct} />
        <ProductCard product={sampleOpenCatalogProduct} />
      </>
    );

    const img = screen.getByAltText('1.5L Stainless Electric Kettle') as HTMLImageElement;
    expect(img).toHaveAttribute('referrerpolicy', 'no-referrer');
    expect(screen.queryByText(/PHOTOGRAPH PENDING VERIFICATION/i)).not.toBeInTheDocument();
    expect(screen.getByText(/Price pending retailer feed/i)).toBeInTheDocument();
  });

  it('renders homepage editorial mission planner headline and example prompts', () => {
    renderWithProviders(<HomePage />);
    expect(screen.getByText(/Life has plans\./i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Describe what you're trying to do/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Generate My Mission/i })).toBeInTheDocument();
    expect(screen.getByText(/Moving into a hostel/i)).toBeInTheDocument();
  });
});
