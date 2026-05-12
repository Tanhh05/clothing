import { itemType } from "../cart/cart-types";

type ProductVariant = {
  price?: number | string;
};

type ProductImage = {
  url?: string;
  isMain?: boolean;
};

type ApiProduct = {
  id: number;
  slug?: string;
  name: string;
  description?: string;
  categoryName?: string;
  categorySlug?: string;
  variants?: ProductVariant[];
  images?: ProductImage[];
  price?: number | string;
  minPrice?: number | string;
  colors?: string[];
};

const toNumber = (value: unknown) => {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string") {
    const parsed = Number(value);
    if (Number.isFinite(parsed)) return parsed;
  }
  return null;
};

export const mapApiProductToItem = (product: ApiProduct): itemType => {
  const images = Array.isArray(product.images) ? product.images : [];
  const mainImage = images.find((image) => image?.isMain && image?.url)?.url;
  const fallbackImage = images.find((image) => image?.url)?.url;
  const img1 = mainImage || fallbackImage || "/bg-img/ourshop.png";
  const img2 = images.find((image) => image?.url && image.url !== img1)?.url || img1;

  const variants = Array.isArray(product.variants) ? product.variants : [];
  const variantPrices = variants
    .map((variant) => toNumber(variant?.price))
    .filter((price): price is number => price !== null && price > 0);
  const variantPrice = variantPrices.length > 0 ? Math.min(...variantPrices) : null;
  const basePrice =
    variantPrice ??
    toNumber(product.minPrice) ??
    toNumber(product.price) ??
    0;

  return {
    id: product.id,
    slug: product.slug,
    name: product.name,
    description: product.description || "",
    detail: product.description || "",
    price: basePrice,
    img1,
    img2,
    colors: Array.isArray(product.colors)
      ? product.colors.filter(
          (color): color is string => typeof color === "string" && color.trim().length > 0
        )
      : [],
    categoryName: product.categoryName || undefined,
    categorySlug: product.categorySlug || undefined,
  };
};
