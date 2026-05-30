import { useContext, useEffect, useReducer, useRef } from "react";
import axios from "axios";
import { getCookie, setCookies } from "cookies-next";

import wishlistReducer from "./wishlistReducer";
import WishlistContext from "./WishlistContext";
import { useAuth } from "../AuthContext";
import { mapApiProductToItem } from "../Util/productMapper";
import {
  ADD_TO_WISHLIST,
  DELETE_WISHLIST_ITEM,
  CLEAR_WISHLIST,
  itemType,
  wishlistType,
  SET_WISHLIST,
} from "./wishlist-type";

export const ProvideWishlist = ({
  children,
}: {
  children: React.ReactNode;
}) => {
  const value = useProvideWishlist();
  return (
    <WishlistContext.Provider value={value}>
      {children}
    </WishlistContext.Provider>
  );
};

export const useWishlist = () => useContext(WishlistContext);

const useProvideWishlist = () => {
  const initPersistState: wishlistType = { wishlist: [] };
  const [state, dispatch] = useReducer(wishlistReducer, initPersistState);
  const auth = useAuth();
  const syncedTokenRef = useRef<string | null>(null);

  const toRecord = (payload: any) => {
    if (
      payload &&
      typeof payload === "object" &&
      Object.prototype.hasOwnProperty.call(payload, "data")
    ) {
      return payload.data;
    }
    return payload;
  };

  const fetchWishlistItems = async (productIds: number[]) => {
    if (!Array.isArray(productIds) || productIds.length === 0) {
      dispatch({ type: SET_WISHLIST, payload: [] });
      return;
    }
    const products = await Promise.all(
      productIds.map(async (id) => {
        const productRes = await axios.get(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/products/${id}`
        );
        return mapApiProductToItem(productRes.data);
      })
    );
    dispatch({ type: SET_WISHLIST, payload: products });
  };

  useEffect(() => {
    const initialWishlist = getCookie("wishlist");
    if (initialWishlist) {
      const wishlistItems = JSON.parse(initialWishlist as string);
      dispatch({ type: SET_WISHLIST, payload: wishlistItems });
    }
  }, []);

  useEffect(() => {
    setCookies("wishlist", state.wishlist);
  }, [state.wishlist]);

  useEffect(() => {
    const token = auth.user?.token || null;
    if (!token) {
      syncedTokenRef.current = null;
      return;
    }
    if (syncedTokenRef.current === token) return;

    const syncServerWishlist = async () => {
      try {
        const localIds = state.wishlist.map((item) => item.id).filter(Boolean);
        for (const productId of localIds) {
          await axios.post(
            `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/wishlist/items`,
            { productId },
            { headers: { Authorization: `Bearer ${token}` } }
          );
        }

        if (localIds.length === 0) {
          const wishlistRes = await axios.get(
            `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/wishlist`,
            { headers: { Authorization: `Bearer ${token}` } }
          );
          const wishlistData = toRecord(wishlistRes.data);
          const productIds: number[] = Array.isArray(wishlistData?.productIds)
            ? wishlistData.productIds
            : [];
          await fetchWishlistItems(productIds);
        }

        syncedTokenRef.current = token;
      } catch (err) {
        console.error("Failed to sync wishlist with backend:", err);
      }
    };

    syncServerWishlist();
  }, [auth.user?.token]);

  const addToWishlist = async (item: itemType) => {
    dispatch({ type: ADD_TO_WISHLIST, payload: item });
    const token = auth.user?.token;
    if (token) {
      try {
        await axios.post(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/wishlist/items`,
          { productId: item.id },
          { headers: { Authorization: `Bearer ${token}` } }
        );
        return;
      } catch (err) {
        console.error("Failed to add wishlist item on backend:", err);
      }
    }
  };

  const deleteWishlistItem = async (item: itemType) => {
    dispatch({ type: DELETE_WISHLIST_ITEM, payload: item });
    const token = auth.user?.token;
    if (token) {
      try {
        await axios.delete(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/wishlist/items/${item.id}`,
          { headers: { Authorization: `Bearer ${token}` } }
        );
        return;
      } catch (err) {
        console.error("Failed to delete wishlist item on backend:", err);
      }
    }
  };

  const clearWishlist = async () => {
    const token = auth.user?.token;
    if (token) {
      try {
        await Promise.all(
          state.wishlist.map((item) =>
            axios.delete(
              `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/wishlist/items/${item.id}`,
              { headers: { Authorization: `Bearer ${token}` } }
            )
          )
        );
      } catch (err) {
        console.error("Failed to clear wishlist on backend:", err);
      }
    }
    dispatch({ type: CLEAR_WISHLIST });
  };

  const value: wishlistType = {
    wishlist: state.wishlist,
    addToWishlist,
    deleteWishlistItem,
    clearWishlist,
  };

  return value;
};
