import addWishlist from "../Util/addWishlist";
import {
  ADD_TO_WISHLIST,
  DELETE_WISHLIST_ITEM,
  CLEAR_WISHLIST,
  wishlistType,
  itemType,
  SET_WISHLIST,
} from "./wishlist-type";

type actionType = {
  type: string;
  payload?: itemType | itemType[];
};

const wishlistReducer = (state: wishlistType, action: actionType) => {
  switch (action.type) {
    case ADD_TO_WISHLIST:
      return {
        ...state,
        wishlist: addWishlist(state.wishlist, action.payload as itemType),
      };
    case DELETE_WISHLIST_ITEM:
      {
        const payload = action.payload as itemType;
        const targetSize = (payload?.selectedSize || "").trim().toUpperCase();
        const targetColor = (payload?.selectedColor || "").trim().toUpperCase();
        const targetVariantId = Number(payload?.selectedVariantId || 0);
        const hasVariantSelector = targetVariantId > 0 || targetSize || targetColor;
        if (!hasVariantSelector) {
          return {
            ...state,
            wishlist: state.wishlist.filter(
              (wishlistItem) => wishlistItem.id !== payload.id
            ),
          };
        }
        return {
          ...state,
          wishlist: state.wishlist.filter(
            (wishlistItem) =>
              !(
                wishlistItem.id === payload.id &&
                (targetVariantId > 0
                  ? Number(wishlistItem.selectedVariantId || 0) === targetVariantId
                  : ((wishlistItem.selectedSize || "").trim().toUpperCase() === targetSize) &&
                    ((wishlistItem.selectedColor || "").trim().toUpperCase() === targetColor))
              )
          ),
        };
      }
    case SET_WISHLIST:
      return {
        ...state,
        wishlist: action.payload as itemType[],
      };
    case CLEAR_WISHLIST:
      return {
        ...state,
        wishlist: [],
      };
    default:
      return state;
  }
};

export default wishlistReducer;
