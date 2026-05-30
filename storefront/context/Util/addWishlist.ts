import { itemType } from "../wishlist/wishlist-type";

const addWishlist = (wishlistItems: itemType[], item: itemType) => {
  const targetSize = (item.selectedSize || "").trim().toUpperCase();
  const targetColor = (item.selectedColor || "").trim().toUpperCase();
  const targetVariantId = Number(item.selectedVariantId || 0);
  const isSameWishlistLine = (wishlistItem: itemType) =>
    wishlistItem.id === item.id &&
    (targetVariantId > 0
      ? Number(wishlistItem.selectedVariantId || 0) === targetVariantId
      : ((wishlistItem.selectedSize || "").trim().toUpperCase() === targetSize) &&
        ((wishlistItem.selectedColor || "").trim().toUpperCase() === targetColor));
  const duplicate = wishlistItems.some(isSameWishlistLine);

  if (!duplicate) {
    return [...wishlistItems, { ...item }];
  } else {
    return [...wishlistItems];
  }
};

export default addWishlist;
