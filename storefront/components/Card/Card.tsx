import { Dialog, Transition } from "@headlessui/react";
import axios from "axios";
import { FC, Fragment, useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { useTranslations } from "next-intl";

import Heart from "../../public/icons/Heart";
import styles from "./Card.module.css";
import HeartSolid from "../../public/icons/HeartSolid";
import Button from "../Buttons/Button";
import GhostButton from "../Buttons/GhostButton";
import { itemType } from "../../context/cart/cart-types";
import { useCart } from "../../context/cart/CartProvider";
import { useWishlist } from "../../context/wishlist/WishlistProvider";
import { useCurrency } from "../../context/CurrencyContext";
import { useNotify } from "../../context/NotificationContext";

type Props = {
  item: itemType;
  enableVariantDialog?: boolean;
};

type ProductVariantApi = {
  size?: string;
};

const normalizeOptionLabel = (value: unknown): string => {
  if (typeof value !== "string") return "";
  return value
    .replace(/[\u200B-\u200D\uFEFF]/g, "")
    .replace(/\s+/g, " ")
    .trim();
};

const normalizeOptionKey = (value: unknown): string =>
  normalizeOptionLabel(value).toUpperCase();

const extractStringOptions = (source: unknown): string[] =>
  Array.isArray(source)
    ? source
        .map((value) => (typeof value === "string" ? normalizeOptionLabel(value) : ""))
        .filter((value) => Boolean(value))
    : [];

const extractImageSources = (payload: any, item: itemType): string[] => {
  const candidates: string[] = [];
  const pushIfValid = (value: unknown) => {
    if (typeof value !== "string") return;
    const normalized = value.trim();
    if (!normalized) return;
    if (!candidates.includes(normalized)) candidates.push(normalized);
  };

  pushIfValid(item.img1);
  pushIfValid(item.img2);
  pushIfValid(payload?.image1);
  pushIfValid(payload?.image2);
  pushIfValid(payload?.img1);
  pushIfValid(payload?.img2);

  if (Array.isArray(payload?.images)) {
    payload.images.forEach((entry: any) => {
      if (typeof entry === "string") {
        pushIfValid(entry);
        return;
      }
      pushIfValid(entry?.url);
      pushIfValid(entry?.imageUrl);
      pushIfValid(entry?.image);
    });
  }

  return candidates;
};

const Card: FC<Props> = ({ item, enableVariantDialog = false }) => {
  const t = useTranslations("CartWishlist");
  const authT = useTranslations("LoginRegister");
  const { wishlist, addToWishlist, deleteWishlistItem } = useWishlist();
  const { addOne, addItem } = useCart();
  const { notify } = useNotify();
  const { formatPrice } = useCurrency();
  const [isHovered, setIsHovered] = useState(false);
  const [isWLHovered, setIsWLHovered] = useState(false);

  const [openVariantDialog, setOpenVariantDialog] = useState(false);
  const [loadingVariants, setLoadingVariants] = useState(false);
  const [dialogSizeOptions, setDialogSizeOptions] = useState<string[]>([]);
  const [dialogColorOptions, setDialogColorOptions] = useState<string[]>([]);
  const [selectedSize, setSelectedSize] = useState("");
  const [selectedColor, setSelectedColor] = useState("");
  const [selectedQty, setSelectedQty] = useState(1);
  const [dialogDescription, setDialogDescription] = useState(item.description || "");
  const [dialogImages, setDialogImages] = useState<string[]>(
    [item.img1, item.img2].filter((value): value is string => Boolean(value))
  );
  const [activeImageIndex, setActiveImageIndex] = useState(0);
  const touchStartXRef = useRef<number | null>(null);

  const { id, slug, name, price, img1, img2 } = item;
  const itemLink = slug ? `/products/${encodeURIComponent(slug)}` : "/";

  const alreadyWishlisted =
    wishlist.filter((wItem) => wItem.id === id).length > 0;

  const sizeOptions = useMemo(
    () =>
      Array.isArray(dialogSizeOptions) && dialogSizeOptions.length
        ? dialogSizeOptions
        : ["M"],
    [dialogSizeOptions]
  );
  const colorOptions = useMemo(
    () => (Array.isArray(dialogColorOptions) ? dialogColorOptions.filter(Boolean) : []),
    [dialogColorOptions]
  );

  useEffect(() => {
    setSelectedSize(sizeOptions[0]);
  }, [sizeOptions]);

  useEffect(() => {
    setSelectedColor(colorOptions[0] || "");
  }, [colorOptions]);

  useEffect(() => {
    if (dialogImages.length === 0) {
      setActiveImageIndex(0);
      return;
    }
    if (activeImageIndex > dialogImages.length - 1) {
      setActiveImageIndex(0);
    }
  }, [dialogImages, activeImageIndex]);

  const currentItem: itemType = {
    ...item,
    qty: selectedQty,
    selectedSize,
    selectedColor,
  };

  const handleWishlist = () => {
    alreadyWishlisted ? deleteWishlistItem!(item) : addToWishlist!(item);
  };

  const handleDialogWishlist = () => {
    alreadyWishlisted
      ? deleteWishlistItem!(currentItem)
      : addToWishlist!(currentItem);
  };

  const openQuickAddDialog = async () => {
    setOpenVariantDialog(true);
    setLoadingVariants(true);
    setSelectedQty(1);
    setActiveImageIndex(0);
    setDialogSizeOptions([]);
    setDialogColorOptions([]);
    setDialogImages([item.img1, item.img2].filter((value): value is string => Boolean(value)));
    try {
      const productKey = slug || String(id);
      const res = await axios.get(
        `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/products/${productKey}`
      );
      const payload = res.data?.data || res.data;
      const productVariants = Array.isArray(payload?.variants)
        ? payload.variants
        : [];
      const sizeList: string[] = Array.from(
        new Set<string>(
          productVariants
            .map((variant: ProductVariantApi) => normalizeOptionLabel(variant?.size))
            .filter((value: string) => Boolean(value))
        )
      );
      const colorList = Array.from(new Set(extractStringOptions(payload?.colors)));
      const fallbackColorList = Array.from(
        new Set(extractStringOptions(item?.colors))
      );
      const mergedColors = colorList.length > 0 ? colorList : fallbackColorList;
      const images = extractImageSources(payload, item);

      setDialogDescription(
        typeof payload?.description === "string" && payload.description.trim()
          ? payload.description.trim()
          : item.description || ""
      );
      setDialogSizeOptions(sizeList);
      setDialogColorOptions(mergedColors);
      setDialogImages(images.length > 0 ? images : ["/bg-img/ourshop.png"]);
    } catch (error) {
      notify("Không tải được thông tin sản phẩm", "error");
      setOpenVariantDialog(false);
    } finally {
      setLoadingVariants(false);
    }
  };

  const handleAddToCart = () => {
    if (!enableVariantDialog) {
      addOne!(item);
      notify(authT("added_to_cart"), "success");
      return;
    }
    openQuickAddDialog();
  };

  const handleConfirmQuickAdd = () => {
    if (!selectedSize || (colorOptions.length > 0 && !selectedColor)) {
      notify("Vui lòng chọn size và màu", "error");
      return;
    }
    addItem!(currentItem);
    notify(authT("added_to_cart"), "success");
    setOpenVariantDialog(false);
  };

  const handleTouchStart = (event: React.TouchEvent<HTMLDivElement>) => {
    touchStartXRef.current = event.touches[0]?.clientX ?? null;
  };

  const handleTouchEnd = (event: React.TouchEvent<HTMLDivElement>) => {
    if (touchStartXRef.current === null || dialogImages.length <= 1) return;
    const touchEndX = event.changedTouches[0]?.clientX ?? touchStartXRef.current;
    const deltaX = touchStartXRef.current - touchEndX;
    touchStartXRef.current = null;
    if (Math.abs(deltaX) < 40) return;
    if (deltaX > 0) {
      setActiveImageIndex((prev) => (prev + 1) % dialogImages.length);
      return;
    }
    setActiveImageIndex((prev) => (prev - 1 + dialogImages.length) % dialogImages.length);
  };

  return (
    <>
      <div className={styles.card}>
        <div className={styles.imageContainer}>
          <Link href={itemLink}>
            <a
              tabIndex={-1}
              onMouseOver={() => setIsHovered(true)}
              onMouseLeave={() => setIsHovered(false)}
            >
              {!isHovered && (
                <Image
                  src={img1 as string}
                  alt={name}
                  width={230}
                  height={300}
                  layout="responsive"
                />
              )}
              {isHovered && (
                <Image
                  className="transition-transform transform hover:scale-110 duration-1000"
                  src={img2 as string}
                  alt={name}
                  width={230}
                  height={300}
                  layout="responsive"
                />
              )}
            </a>
          </Link>
          <button
            type="button"
            className="absolute top-2 right-2 p-1 rounded-full"
            aria-label="Wishlist"
            onClick={handleWishlist}
            onMouseOver={() => setIsWLHovered(true)}
            onMouseLeave={() => setIsWLHovered(false)}
          >
            {isWLHovered || alreadyWishlisted ? <HeartSolid /> : <Heart />}
          </button>
          <button
            type="button"
            onClick={handleAddToCart}
            className={styles.addBtn}
          >
            {t("add_to_cart")}
          </button>
        </div>

        <div className="content">
          <Link href={itemLink}>
            <a className={styles.itemName}>{name}</a>
          </Link>
          <div className="text-gray400">{formatPrice(price)}</div>
          <button
            type="button"
            onClick={handleAddToCart}
            className="uppercase font-bold text-sm sm:hidden"
          >
            {t("add_to_cart")}
          </button>
        </div>
      </div>

      <Transition appear show={openVariantDialog} as={Fragment}>
        <Dialog
          as="div"
          className="fixed inset-0 z-[9999] overflow-y-auto text-black"
          onClose={() => setOpenVariantDialog(false)}
        >
          <div className="min-h-screen px-4 text-center">
            <Transition.Child
              as={Fragment}
              enter="ease-out duration-200"
              enterFrom="opacity-0"
              enterTo="opacity-100"
              leave="ease-in duration-150"
              leaveFrom="opacity-100"
              leaveTo="opacity-0"
            >
              <Dialog.Overlay className="fixed inset-0 bg-black/40" />
            </Transition.Child>
            <span className="inline-block h-screen align-middle" aria-hidden="true">
              &#8203;
            </span>
            <Transition.Child
              as={Fragment}
              enter="ease-out duration-200"
              enterFrom="opacity-0 scale-95"
              enterTo="opacity-100 scale-100"
              leave="ease-in duration-150"
              leaveFrom="opacity-100 scale-100"
              leaveTo="opacity-0 scale-95"
            >
              <div className="inline-block w-full max-w-4xl my-6 text-left align-middle transition-all transform bg-white text-black border border-gray300 shadow-xl">
                <div className="grid grid-cols-1 md:grid-cols-2">
                  <div className="bg-gray100 p-4 md:p-6 flex flex-col justify-between">
                    <div
                      className="w-full flex items-center justify-center"
                      onTouchStart={handleTouchStart}
                      onTouchEnd={handleTouchEnd}
                    >
                      <Image
                        src={dialogImages[activeImageIndex] || "/bg-img/ourshop.png"}
                        alt={`${name}-${activeImageIndex + 1}`}
                        width={700}
                        height={900}
                        className="w-full h-auto object-contain"
                      />
                    </div>
                    <div className="mt-4 flex items-center justify-center gap-2">
                      {dialogImages.map((_, index) => (
                        <button
                          key={index}
                          type="button"
                          aria-label={`image-${index + 1}`}
                          className={`h-3 w-3 rounded-full border ${
                            index === activeImageIndex
                              ? "bg-black border-black"
                              : "bg-transparent border-gray300"
                          }`}
                          onClick={() => setActiveImageIndex(index)}
                        />
                      ))}
                    </div>
                  </div>
                  <div className="p-6 md:p-7 bg-white text-black">
                    <div className="flex items-start justify-between gap-3">
                      <Dialog.Title as="h3" className="text-2xl md:text-3xl font-semibold leading-snug pr-4">
                        {name}
                      </Dialog.Title>
                      <button
                        type="button"
                        className="text-3xl leading-none text-gray400 hover:text-gray500"
                        onClick={() => setOpenVariantDialog(false)}
                      >
                        &#10005;
                      </button>
                    </div>
                    {dialogImages.length > 1 && (
                      <div className="mt-4 flex gap-3 overflow-x-auto pb-1">
                        {dialogImages.map((imageSrc, index) => (
                          <button
                            key={`${imageSrc}-${index}`}
                            type="button"
                            className={`h-20 w-20 shrink-0 border bg-gray100 ${
                              index === activeImageIndex
                                ? "border-gray500"
                                : "border-gray200"
                            }`}
                            onClick={() => setActiveImageIndex(index)}
                          >
                            <Image
                              src={imageSrc}
                              alt={`${name}-thumb-${index + 1}`}
                              width={72}
                              height={72}
                              className="h-full w-full object-cover"
                            />
                          </button>
                        ))}
                      </div>
                    )}

                    {loadingVariants ? (
                      <div className="text-sm text-gray400 mt-4">Đang tải...</div>
                    ) : (
                      <div className="space-y-4 mt-4">
                        <div className="text-2xl text-gray500">{formatPrice(price)}</div>
                        <div className="text-base text-gray500">{dialogDescription}</div>
                        <div className="text-base">Tình trạng: Còn hàng</div>
                        <div className="text-base">Kích cỡ: {selectedSize}</div>
                        <div className="flex flex-wrap gap-3 text-sm">
                          {sizeOptions.map((option, index) => {
                            const normalizedOption = normalizeOptionLabel(option);
                            const selected =
                              normalizeOptionKey(selectedSize) ===
                              normalizeOptionKey(normalizedOption);
                            return (
                              <button
                                key={`${option}-${index}`}
                                type="button"
                                className={`min-w-[2rem] h-8 px-3 flex items-center justify-center border cursor-pointer ${
                                  selected
                                    ? "border-gray500 bg-gray500 text-gray100"
                                    : "border-gray300 text-gray400 hover:bg-gray500 hover:text-gray100"
                                }`}
                                onClick={() => setSelectedSize(option)}
                              >
                                {option}
                              </button>
                            );
                          })}
                        </div>

                        {colorOptions.length > 0 && (
                          <>
                            <div className="text-base">Màu sắc: {selectedColor}</div>
                            <div className="flex flex-wrap gap-3 text-sm">
                              {colorOptions.map((option, index) => {
                                const normalizedOption = normalizeOptionLabel(option);
                                const selected =
                                  normalizeOptionKey(selectedColor) ===
                                  normalizeOptionKey(normalizedOption);
                                return (
                                  <button
                                    key={`${option}-${index}`}
                                    type="button"
                                    className={`min-w-[4.5rem] h-8 px-3 flex items-center justify-center border cursor-pointer ${
                                      selected
                                        ? "border-gray500 bg-gray500 text-gray100"
                                        : "border-gray300 text-gray400 hover:bg-gray500 hover:text-gray100"
                                    }`}
                                    onClick={() => setSelectedColor(option)}
                                  >
                                    {option}
                                  </button>
                                );
                              })}
                            </div>
                          </>
                        )}

                        <div className="flex items-stretch gap-4 pt-1">
                          <div className="h-12 flex border border-gray300 divide-x divide-gray300">
                            <button
                              type="button"
                              className="w-12 text-xl cursor-pointer hover:bg-gray500 hover:text-gray100"
                              onClick={() => setSelectedQty((q) => Math.max(1, q - 1))}
                            >
                              -
                            </button>
                            <div className="w-16 sm:w-12 flex items-center justify-center">
                              {selectedQty}
                            </div>
                            <button
                              type="button"
                              className="w-12 text-xl cursor-pointer hover:bg-gray500 hover:text-gray100"
                              onClick={() => setSelectedQty((q) => q + 1)}
                            >
                              +
                            </button>
                          </div>
                          <Button
                            value={t("add_to_cart")}
                            size="lg"
                            extraClass="h-12 flex-grow text-center whitespace-nowrap"
                            onClick={handleConfirmQuickAdd}
                          />
                          <GhostButton
                            extraClass="h-12 min-w-[3rem] flex items-center justify-center"
                            onClick={handleDialogWishlist}
                          >
                            {alreadyWishlisted ? (
                              <HeartSolid extraClass="inline" />
                            ) : (
                              <Heart extraClass="inline" />
                            )}
                          </GhostButton>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </Transition.Child>
          </div>
        </Dialog>
      </Transition>
    </>
  );
};

export default Card;
