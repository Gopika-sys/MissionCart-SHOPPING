import React, { Suspense, lazy } from 'react';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './context/AuthContext';
import { CartProvider } from './context/CartContext';
import { Navbar } from './components/Navbar';
import { Footer } from './components/Footer';
import { CartDrawer } from './components/CartDrawer';
import { ProtectedRoute } from './components/ProtectedRoute';

const HomePage = lazy(() => import('./pages/HomePage').then((m) => ({ default: m.HomePage })));
const ExplorePage = lazy(() => import('./pages/DiscoveryPages').then((m) => ({ default: m.ExplorePage })));
const ProductsPage = lazy(() => import('./pages/DiscoveryPages').then((m) => ({ default: m.ProductsPage })));
const CategoriesPage = lazy(() => import('./pages/DiscoveryPages').then((m) => ({ default: m.CategoriesPage })));
const ProductDetailPage = lazy(() => import('./pages/DiscoveryPages').then((m) => ({ default: m.ProductDetailPage })));
const SearchResultsPage = lazy(() => import('./pages/DiscoveryPages').then((m) => ({ default: m.SearchResultsPage })));
const RetailersPage = lazy(() => import('./pages/DiscoveryPages').then((m) => ({ default: m.RetailersPage })));

const MissionBuilderPage = lazy(() => import('./pages/MissionPages').then((m) => ({ default: m.MissionBuilderPage })));
const MissionResultPage = lazy(() => import('./pages/MissionPages').then((m) => ({ default: m.MissionResultPage })));
const MyMissionsPage = lazy(() => import('./pages/MissionPages').then((m) => ({ default: m.MyMissionsPage })));
const PossessionsPage = lazy(() => import('./pages/MissionPages').then((m) => ({ default: m.PossessionsPage })));
const SharedMissionsPage = lazy(() => import('./pages/MissionPages').then((m) => ({ default: m.SharedMissionsPage })));
const GroupMissionRoomPage = lazy(() => import('./pages/MissionPages').then((m) => ({ default: m.GroupMissionRoomPage })));

const CartPage = lazy(() => import('./pages/CommercePages').then((m) => ({ default: m.CartPage })));
const CheckoutPage = lazy(() => import('./pages/CommercePages').then((m) => ({ default: m.CheckoutPage })));
const OrdersPage = lazy(() => import('./pages/CommercePages').then((m) => ({ default: m.OrdersPage })));
const OrderDetailPage = lazy(() => import('./pages/CommercePages').then((m) => ({ default: m.OrderDetailPage })));
const WishlistPage = lazy(() => import('./pages/CommercePages').then((m) => ({ default: m.WishlistPage })));
const LifecycleManagerPage = lazy(() => import('./pages/CommercePages').then((m) => ({ default: m.LifecycleManagerPage })));

const LoginPage = lazy(() => import('./pages/AccountAndInfoPages').then((m) => ({ default: m.LoginPage })));
const RegisterPage = lazy(() => import('./pages/AccountAndInfoPages').then((m) => ({ default: m.RegisterPage })));
const CustomerDashboardPage = lazy(() => import('./pages/AccountAndInfoPages').then((m) => ({ default: m.CustomerDashboardPage })));
const ProfilePage = lazy(() => import('./pages/AccountAndInfoPages').then((m) => ({ default: m.ProfilePage })));
const AddressesPage = lazy(() => import('./pages/AccountAndInfoPages').then((m) => ({ default: m.AddressesPage })));
const SettingsPage = lazy(() => import('./pages/AccountAndInfoPages').then((m) => ({ default: m.SettingsPage })));
const HowItWorksPage = lazy(() => import('./pages/AccountAndInfoPages').then((m) => ({ default: m.HowItWorksPage })));
const AboutPage = lazy(() => import('./pages/AccountAndInfoPages').then((m) => ({ default: m.AboutPage })));
const ContactPage = lazy(() => import('./pages/AccountAndInfoPages').then((m) => ({ default: m.ContactPage })));

const AdminDashboardPage = lazy(() => import('./pages/AdminPages').then((m) => ({ default: m.AdminDashboardPage })));
const SellerDashboardPage = lazy(() => import('./pages/SellerPages').then((m) => ({ default: m.SellerDashboardPage })));
const SellerSubmitProductPage = lazy(() => import('./pages/SellerPages').then((m) => ({ default: m.SellerSubmitProductPage })));
const SellerInventoryPage = lazy(() => import('./pages/SellerPages').then((m) => ({ default: m.SellerInventoryPage })));
const SellerOrdersPage = lazy(() => import('./pages/SellerPages').then((m) => ({ default: m.SellerOrdersPage })));
const SellerEarningsPage = lazy(() => import('./pages/SellerPages').then((m) => ({ default: m.SellerEarningsPage })));

const queryClient = new QueryClient({
  defaultOptions: { queries: { staleTime: 30_000, retry: 1 } },
});

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <CartProvider>
          <BrowserRouter>
            <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
              <Navbar />
              <CartDrawer />
              <main style={{ flex: 1 }}>
                <Suspense fallback={<div className="mc-container" style={{ padding: '4rem 1.5rem', color: 'var(--mc-muted)' }}>Loading MissionCart...</div>}>
                  <Routes>
                    {/* Public Pages */}
                    <Route path="/" element={<HomePage />} />
                    <Route path="/explore" element={<ExplorePage />} />
                    <Route path="/products" element={<ProductsPage />} />
                    <Route path="/categories" element={<CategoriesPage />} />
                    <Route path="/products/:id" element={<ProductDetailPage />} />
                    <Route path="/search" element={<SearchResultsPage />} />
                    <Route path="/mission-builder" element={<MissionBuilderPage />} />
                    <Route path="/mission-result/:id" element={<MissionResultPage />} />
                    <Route path="/retailers" element={<RetailersPage />} />
                    <Route path="/how-it-works" element={<HowItWorksPage />} />
                    <Route path="/about" element={<AboutPage />} />
                    <Route path="/contact" element={<ContactPage />} />
                    <Route path="/login" element={<LoginPage />} />
                    <Route path="/register" element={<RegisterPage />} />

                    {/* Customer Pages */}
                    <Route path="/dashboard" element={<ProtectedRoute><CustomerDashboardPage /></ProtectedRoute>} />
                    <Route path="/missions" element={<ProtectedRoute><MyMissionsPage /></ProtectedRoute>} />
                    <Route path="/missions/:id" element={<MissionResultPage />} />
                    <Route path="/possessions" element={<ProtectedRoute><PossessionsPage /></ProtectedRoute>} />
                    <Route path="/wishlist" element={<ProtectedRoute><WishlistPage /></ProtectedRoute>} />
                    <Route path="/cart" element={<ProtectedRoute><CartPage /></ProtectedRoute>} />
                    <Route path="/checkout" element={<ProtectedRoute><CheckoutPage /></ProtectedRoute>} />
                    <Route path="/orders" element={<ProtectedRoute><OrdersPage /></ProtectedRoute>} />
                    <Route path="/orders/:id" element={<ProtectedRoute><OrderDetailPage /></ProtectedRoute>} />
                    <Route path="/orders/:id/tracking" element={<ProtectedRoute><OrderDetailPage /></ProtectedRoute>} />
                    <Route path="/shared-missions" element={<ProtectedRoute><SharedMissionsPage /></ProtectedRoute>} />
                    <Route path="/shared/:token" element={<GroupMissionRoomPage />} />
                    <Route path="/lifecycle" element={<ProtectedRoute><LifecycleManagerPage /></ProtectedRoute>} />
                    <Route path="/profile" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
                    <Route path="/addresses" element={<ProtectedRoute><AddressesPage /></ProtectedRoute>} />
                    <Route path="/settings" element={<ProtectedRoute><SettingsPage /></ProtectedRoute>} />

                    {/* Admin Pages */}
                    <Route path="/admin" element={<ProtectedRoute roles={['ADMIN']}><AdminDashboardPage initialTab="overview" /></ProtectedRoute>} />
                    <Route path="/admin/products" element={<ProtectedRoute roles={['ADMIN']}><AdminDashboardPage initialTab="products" /></ProtectedRoute>} />
                    <Route path="/admin/products/new" element={<ProtectedRoute roles={['ADMIN']}><AdminDashboardPage initialTab="products" /></ProtectedRoute>} />
                    <Route path="/admin/categories" element={<ProtectedRoute roles={['ADMIN']}><AdminDashboardPage initialTab="categories" /></ProtectedRoute>} />
                    <Route path="/admin/sources" element={<ProtectedRoute roles={['ADMIN']}><AdminDashboardPage initialTab="sources" /></ProtectedRoute>} />
                    <Route path="/admin/images" element={<ProtectedRoute roles={['ADMIN']}><AdminDashboardPage initialTab="images" /></ProtectedRoute>} />
                    <Route path="/admin/importer" element={<ProtectedRoute roles={['ADMIN']}><AdminDashboardPage initialTab="importer" /></ProtectedRoute>} />
                    <Route path="/admin/inventory" element={<ProtectedRoute roles={['ADMIN']}><AdminDashboardPage initialTab="inventory" /></ProtectedRoute>} />
                    <Route path="/admin/customers" element={<ProtectedRoute roles={['ADMIN']}><AdminDashboardPage initialTab="users" /></ProtectedRoute>} />
                    <Route path="/admin/orders" element={<ProtectedRoute roles={['ADMIN']}><AdminDashboardPage initialTab="orders" /></ProtectedRoute>} />
                    <Route path="/admin/analytics" element={<ProtectedRoute roles={['ADMIN']}><AdminDashboardPage initialTab="analytics" /></ProtectedRoute>} />
                    <Route path="/admin/compatibility" element={<ProtectedRoute roles={['ADMIN']}><AdminDashboardPage initialTab="compatibility" /></ProtectedRoute>} />
                    <Route path="/admin/lifecycle" element={<ProtectedRoute roles={['ADMIN']}><AdminDashboardPage initialTab="lifecycle" /></ProtectedRoute>} />
                    <Route path="/admin/reviews" element={<ProtectedRoute roles={['ADMIN']}><AdminDashboardPage initialTab="reviews" /></ProtectedRoute>} />
                    <Route path="/admin/settings" element={<ProtectedRoute roles={['ADMIN']}><AdminDashboardPage initialTab="settings" /></ProtectedRoute>} />

                    {/* Seller Pages */}
                    <Route path="/seller" element={<ProtectedRoute roles={['SELLER', 'ADMIN']}><SellerDashboardPage /></ProtectedRoute>} />
                    <Route path="/seller/register" element={<RegisterPage />} />
                    <Route path="/seller/submit" element={<ProtectedRoute roles={['SELLER', 'ADMIN']}><SellerSubmitProductPage /></ProtectedRoute>} />
                    <Route path="/seller/inventory" element={<ProtectedRoute roles={['SELLER', 'ADMIN']}><SellerInventoryPage /></ProtectedRoute>} />
                    <Route path="/seller/orders" element={<ProtectedRoute roles={['SELLER', 'ADMIN']}><SellerOrdersPage /></ProtectedRoute>} />
                    <Route path="/seller/earnings" element={<ProtectedRoute roles={['SELLER', 'ADMIN']}><SellerEarningsPage /></ProtectedRoute>} />
                  </Routes>
                </Suspense>
              </main>
              <Footer />
            </div>
          </BrowserRouter>
        </CartProvider>
      </AuthProvider>
    </QueryClientProvider>
  );
}

export default App;
