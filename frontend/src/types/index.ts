export interface UserProfile {
  id: number;
  email: string;
  fullName: string;
  phone?: string;
  role: 'CUSTOMER' | 'ADMIN' | 'SELLER';
  avatarUrl?: string;
}

export interface Category {
  id: number;
  name: string;
  slug: string;
  parentId?: number;
  description?: string;
  purposeGroup?: string;
  displayOrder: number;
}

export interface RetailerSource {
  id: number;
  code: string;
  name: string;
  officialWebsite: string;
  logoUrl?: string;
  categoryFocus: string;
  supportedCategories: string;
  importMethod: string;
  productUrlPattern?: string;
  imageSourcingMethod: string;
  connectionStatus: string;
  nativeCheckout: boolean;
  lastSuccessfulImport?: string;
  lastPriceVerification?: string;
}

export interface ProductImage {
  id: number;
  imageUrl: string;
  imageSource: string;
  imageAttribution?: string;
  imageLicense?: string;
  imageStatus: string;
  imageAltText: string;
  primaryImage: boolean;
}

export interface Product {
  id: number;
  sku: string;
  name: string;
  slug: string;
  brandId?: number;
  brandName?: string;
  categoryId: number;
  categoryName?: string;
  categorySlug?: string;
  sourcePlatform: string;
  sourceProductId?: string;
  sourceUrl?: string | null;
  productType: string;
  description: string;
  price: number | null;
  originalPrice?: number | null;
  currency: string;
  priceVerified: boolean;
  priceStatus?: 'VERIFIED' | 'ESTIMATED' | 'UNAVAILABLE' | 'EXPIRED';
  taxRate?: number;
  shippingEstimate?: number;
  priceLastVerifiedAt?: string;
  availabilityVerifiedAt?: string;
  availabilityStatus: string;
  stockQuantity: number;
  rating?: number;
  reviewCount?: number;
  ratingVerified: boolean;
  dataVerificationStatus: string;
  specifications: Record<string, unknown>;
  attributes: Record<string, unknown>;
  missionTags: string[];
  priorityTier: string;
  lifecycleOptions: string[];
  imageUrl?: string | null;
  imageSource?: string;
  imageAttribution?: string;
  imageLicense?: string;
  imageStatus: 'VERIFIED' | 'PENDING_REVIEW' | 'REQUIRES_IMAGE' | 'INVALID';
  additionalImages: ProductImage[];
  published: boolean;
  approved: boolean;
}

export interface ProductCounts {
  totalImportedProducts: number;
  publishedProducts: number;
  imageVerifiedProducts: number;
  availableProducts: number;
  pendingImageProducts: number;
  byRetailer: Record<string, number>;
  byCategory: Record<string, number>;
}

export interface BudgetSummary {
  totalBudget: number;
  essentialSpending: number;
  optionalSpending: number;
  productSubtotal: number;
  totalSelected: number;
  verifiedDiscount: number;
  shippingCost: number;
  applicableTaxes: number;
  couponDiscount: number;
  finalPayable: number;
  remainingBudget: number;
  utilizationPercent: number;
  excludedOwnedSavings: number;
  unpricedItemCount: number;
  overBudget: boolean;
  affordableConfirmed: boolean;
}

export interface CompatibilityIssue {
  ruleCode: string;
  domain: string;
  status: 'COMPATIBLE' | 'WARNING' | 'INCOMPATIBLE' | 'NOT_VERIFIED';
  reason: string;
  recommendedAction: string;
  affectedProductIds: number[];
}

export interface CompatibilityReport {
  overallStatus: 'COMPATIBLE' | 'WARNING' | 'INCOMPATIBLE' | 'NOT_VERIFIED';
  issues: CompatibilityIssue[];
}

export interface MissionItem {
  id: number;
  productId: number;
  product: Product | null;
  requirementLabel: string;
  priority: 'ESSENTIAL' | 'OPTIONAL';
  quantity: number;
  unitPrice: number | null;
  lineTotal: number | null;
  selected: boolean;
  excludedOwned: boolean;
  compatibilityStatus: string;
  compatibilityReason?: string;
  recommendationReason?: string;
  assignedToUserId?: number;
  claimedByName?: string;
}

export interface MissionDetail {
  id: number;
  userId?: number;
  title: string;
  slug: string;
  rawPrompt: string;
  goal: string;
  situationSummary: string;
  purpose: string;
  budget?: number;
  currency: string;
  timeframe?: string;
  peopleCount: number;
  aiProviderUsed: string;
  aiExplanation: string;
  assumptions: string[];
  existingPossessions: string[];
  unmatchedRequirements: string[];
  followUpQuestion?: string;
  status: string;
  shared: boolean;
  shareToken?: string;
  budgetSummary: BudgetSummary;
  compatibilityReport: CompatibilityReport;
  items: MissionItem[];
  progressPercent: number;
  createdAt: string;
}

export interface CartItemView {
  id: number;
  productId: number;
  missionId?: number;
  quantity: number;
  savedForLater: boolean;
  unitPrice: number | null;
  lineTotal: number | null;
  nativeCheckoutEligible: boolean;
  product: Product;
}

export interface RetailerCartGroup {
  sourceCode: string;
  sourceName: string;
  officialWebsite: string;
  nativeCheckout: boolean;
  subtotal: number;
  items: CartItemView[];
}

export interface CartSummary {
  cartId: number;
  activeItems: CartItemView[];
  savedForLaterItems: CartItemView[];
  externalRetailerGroups: RetailerCartGroup[];
  nativeSubtotal: number;
  externalSubtotal: number;
  grandTotal: number;
  currency: string;
  compatibilityReport: CompatibilityReport;
}
