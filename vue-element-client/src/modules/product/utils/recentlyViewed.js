const RECENTLY_VIEWED_KEY = "clothing_recently_viewed_products";
const MAX_RECENTLY_VIEWED = 12;

const readStorage = () => {
  try {
    const raw = localStorage.getItem(RECENTLY_VIEWED_KEY);
    const parsed = JSON.parse(raw || "[]");
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
};

const writeStorage = (items) => {
  localStorage.setItem(RECENTLY_VIEWED_KEY, JSON.stringify(items));
};

const toSummary = (product) => {
  if (!product?.id) return null;
  return {
    id: product.id,
    slug: product.slug || null,
    name: product.name || "Sản phẩm",
    brand: product.brand || "",
    categoryId: product.categoryId ?? null,
    categoryName: product.categoryName || "",
    images: Array.isArray(product.images) ? product.images : [],
    variants: Array.isArray(product.variants) ? product.variants : []
  };
};

export const getRecentlyViewedProducts = () => {
  return readStorage();
};

export const trackRecentlyViewedProduct = (product) => {
  const summary = toSummary(product);
  if (!summary) return;

  const current = readStorage().filter((item) => item?.id !== summary.id);
  current.unshift(summary);
  writeStorage(current.slice(0, MAX_RECENTLY_VIEWED));
};

