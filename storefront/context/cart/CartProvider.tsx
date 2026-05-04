import React, { useContext, useEffect, useReducer } from "react";
import axios from "axios";
import cartReducer from "./cartReducer";
import CartContext from "./CartContext";
import { getCookie, setCookies } from "cookies-next";
import {
  ADD_ITEM,
  ADD_ONE,
  REMOVE_ITEM,
  DELETE_ITEM,
  itemType,
  cartType,
  CLEAR_CART,
  SET_CART,
} from "./cart-types";
import { mapApiProductToItem } from "../Util/productMapper";

export const ProvideCart = ({ children }: { children: React.ReactNode }) => {
  const value = useProvideCart();
  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
};

export const useCart = () => useContext(CartContext);

const useProvideCart = () => {
  const initPersistState: cartType = { cart: [] };
  const [state, dispatch] = useReducer(cartReducer, initPersistState);

  useEffect(() => {
    const initialCart = getCookie("cart");
    if (initialCart) {
      const cartItems = JSON.parse(initialCart as string);
      dispatch({ type: SET_CART, payload: cartItems });
    }
  }, []);

  useEffect(() => {
    const syncInvalidPriceItems = async () => {
      const invalidItems = state.cart.filter(
        (item) => !Number.isFinite(item.price) || item.price <= 0
      );
      if (invalidItems.length === 0) return;

      try {
        const repaired = await Promise.all(
          state.cart.map(async (item) => {
            if (Number.isFinite(item.price) && item.price > 0) return item;
            const res = await axios.get(
              `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/products/${item.id}`
            );
            const mapped = mapApiProductToItem(res.data);
            return {
              ...item,
              price: mapped.price,
              img1: item.img1 || mapped.img1,
              img2: item.img2 || mapped.img2,
            };
          })
        );
        dispatch({ type: SET_CART, payload: repaired });
      } catch (err) {
        console.error("Failed to repair cart prices:", err);
      }
    };
    syncInvalidPriceItems();
  }, [state.cart]);

  useEffect(() => {
    setCookies("cart", state.cart);
  }, [state.cart]);

  const addItem = (item: itemType) => {
    dispatch({
      type: ADD_ITEM,
      payload: item,
    });
  };

  const addOne = (item: itemType) => {
    dispatch({
      type: ADD_ONE,
      payload: item,
    });
  };

  const removeItem = (item: itemType) => {
    dispatch({
      type: REMOVE_ITEM,
      payload: item,
    });
  };

  const deleteItem = (item: itemType) => {
    dispatch({
      type: DELETE_ITEM,
      payload: item,
    });
  };

  const clearCart = () => {
    dispatch({
      type: CLEAR_CART,
    });
  };

  const value: cartType = {
    cart: state.cart,
    addItem,
    addOne,
    removeItem,
    deleteItem,
    clearCart,
  };

  return value;
};
