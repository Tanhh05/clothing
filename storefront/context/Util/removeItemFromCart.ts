import { itemType } from "../cart/cart-types";

const removeItemFromCart = (cartItems: itemType[], item: itemType) => {
  const targetSize = (item.selectedSize || "").trim().toUpperCase();
  const isSameCartLine = (cartItem: itemType) =>
    cartItem.id === item.id &&
    ((cartItem.selectedSize || "").trim().toUpperCase() === targetSize);
  if (item.qty === 1) {
    return cartItems.filter((cartItem) => !isSameCartLine(cartItem));
  }
  return cartItems.map((cartItem) =>
    isSameCartLine(cartItem) ? { ...cartItem, qty: cartItem.qty! - 1 } : cartItem
  );
};

export default removeItemFromCart;
