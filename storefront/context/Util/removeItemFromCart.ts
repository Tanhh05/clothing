import { itemType } from "../cart/cart-types";

const removeItemFromCart = (cartItems: itemType[], item: itemType) => {
  const targetSize = (item.selectedSize || "").trim().toUpperCase();
  const targetColor = (item.selectedColor || "").trim().toUpperCase();
  const targetVariantId = Number(item.selectedVariantId || 0);
  const isSameCartLine = (cartItem: itemType) =>
    cartItem.id === item.id &&
    (targetVariantId > 0
      ? Number(cartItem.selectedVariantId || 0) === targetVariantId
      : ((cartItem.selectedSize || "").trim().toUpperCase() === targetSize) &&
        ((cartItem.selectedColor || "").trim().toUpperCase() === targetColor));
  if (item.qty === 1) {
    return cartItems.filter((cartItem) => !isSameCartLine(cartItem));
  }
  return cartItems.map((cartItem) =>
    isSameCartLine(cartItem) ? { ...cartItem, qty: cartItem.qty! - 1 } : cartItem
  );
};

export default removeItemFromCart;
