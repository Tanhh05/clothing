import { itemType } from "../cart/cart-types";

const addItemToCart = (
  cartItems: itemType[],
  item: itemType,
  add_one = false
) => {
  const targetSize = (item.selectedSize || "").trim().toUpperCase();
  const targetColor = (item.selectedColor || "").trim().toUpperCase();
  const targetVariantId = Number(item.selectedVariantId || 0);
  const isSameCartLine = (cartItem: itemType) =>
    cartItem.id === item.id &&
    (targetVariantId > 0
      ? Number(cartItem.selectedVariantId || 0) === targetVariantId
      : ((cartItem.selectedSize || "").trim().toUpperCase() === targetSize) &&
        ((cartItem.selectedColor || "").trim().toUpperCase() === targetColor));
  const duplicate = cartItems.some(isSameCartLine);

  if (duplicate) {
    return cartItems.map((cartItem) => {
      const addQty = add_one ? 1 : item.qty || 1;
      const itemQty = (cartItem.qty || 0) + addQty;

      return isSameCartLine(cartItem) ? { ...cartItem, qty: itemQty } : cartItem;
    });
  }
  const itemQty = item.qty || 1;
  return [
    ...cartItems,
    {
      id: item.id,
      name: item.name,
      price: item.price,
      img1: item.img1,
      img2: item.img2,
      slug: item.slug,
      qty: itemQty,
      selectedSize: item.selectedSize,
      selectedColor: item.selectedColor,
      selectedVariantId: item.selectedVariantId,
    },
  ];
};

export default addItemToCart;
