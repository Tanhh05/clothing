import { itemType } from "../cart/cart-types";

const addItemToCart = (
  cartItems: itemType[],
  item: itemType,
  add_one = false
) => {
  const targetSize = (item.selectedSize || "").trim().toUpperCase();
  const isSameCartLine = (cartItem: itemType) =>
    cartItem.id === item.id &&
    ((cartItem.selectedSize || "").trim().toUpperCase() === targetSize);
  const duplicate = cartItems.some(isSameCartLine);

  if (duplicate) {
    return cartItems.map((cartItem) => {
      let itemQty = 0;
      !item.qty || add_one
        ? (itemQty = cartItem.qty! + 1)
        : (itemQty = item.qty);

      return isSameCartLine(cartItem) ? { ...cartItem, qty: itemQty } : cartItem;
    });
  }
  // console.log(itemQty);
  let itemQty = 0;
  !item.qty ? itemQty++ : (itemQty = item.qty);
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
    },
  ];
};

export default addItemToCart;
