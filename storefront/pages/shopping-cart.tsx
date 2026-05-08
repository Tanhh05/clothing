import { useEffect, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { useTranslations } from "next-intl";
import axios from "axios";

import Header from "../components/Header/Header";
import Footer from "../components/Footer/Footer";
import LeftArrow from "../public/icons/LeftArrow";
import Button from "../components/Buttons/Button";
import GhostButton from "../components/Buttons/GhostButton";
import { GetStaticProps } from "next";
import { roundDecimal } from "../components/Util/utilFunc";
import { useCart } from "../context/cart/CartProvider";
import { useCurrency } from "../context/CurrencyContext";
import { useRouter } from "next/router";
import { useAuth } from "../context/AuthContext";

type UserAddress = {
  id: number;
  province: string;
  district: string;
  ward: string;
  isDefault: boolean;
};

type AddressUnit = {
  id: string;
  name: string;
};

// let w = window.innerWidth;

const ShoppingCart = () => {
  const t = useTranslations("CartWishlist");
  const { formatPrice } = useCurrency();
  const router = useRouter();
  const auth = useAuth();
  const { cart, addOne, removeItem, deleteItem, clearCart } = useCart();
  const [selectedDistrictId, setSelectedDistrictId] = useState("");
  const [selectedWardCode, setSelectedWardCode] = useState("");
  const [deliFee, setDeliFee] = useState<number | null>(null);

  let subtotal = 0;

  useEffect(() => {
    const loadDefaultAddressAndFee = async () => {
      if (!auth.user?.token) return;
      try {
        const addressesRes = await axios.get(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/user/addresses`,
          {
            headers: {
              Authorization: `Bearer ${auth.user.token}`,
            },
          }
        );
        const addresses: UserAddress[] = Array.isArray(addressesRes.data)
          ? addressesRes.data
          : [];
        if (addresses.length === 0) return;

        const defaultAddress =
          addresses.find((item) => item.isDefault) || addresses[0];

        const provincesRes = await axios.get(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/address/provinces`
        );
        const provinceOptions: AddressUnit[] = Array.isArray(provincesRes.data)
          ? provincesRes.data
          : [];
        const matchedProvince = provinceOptions.find(
          (item) => item.name === defaultAddress.province
        );
        if (!matchedProvince) return;

        const districtsRes = await axios.get(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/address/districts`,
          { params: { provinceId: matchedProvince.id } }
        );
        const districtOptions: AddressUnit[] = Array.isArray(districtsRes.data)
          ? districtsRes.data
          : [];
        const matchedDistrict = districtOptions.find(
          (item) => item.name === defaultAddress.district
        );
        if (!matchedDistrict) return;
        setSelectedDistrictId(matchedDistrict.id);

        const wardsRes = await axios.get(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/address/wards`,
          { params: { districtId: matchedDistrict.id } }
        );
        const wardOptions: AddressUnit[] = Array.isArray(wardsRes.data)
          ? wardsRes.data
          : [];
        const matchedWard = wardOptions.find(
          (item) => item.name === defaultAddress.ward
        );
        if (!matchedWard) return;
        setSelectedWardCode(matchedWard.id);
      } catch (err) {
        console.error("Load cart shipping address failed:", err);
      }
    };
    loadDefaultAddressAndFee();
  }, [auth.user]);

  useEffect(() => {
    const loadShippingFee = async () => {
      if (!selectedDistrictId || !selectedWardCode) {
        setDeliFee(null);
        return;
      }
      try {
        const res = await axios.get(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/address/shipping-fee`,
          {
            params: {
              districtId: selectedDistrictId,
              wardCode: selectedWardCode,
            },
          }
        );
        setDeliFee(Number(res.data?.fee || 0));
      } catch (err) {
        console.error("Load cart shipping fee failed:", err);
        setDeliFee(null);
      }
    };
    loadShippingFee();
  }, [selectedDistrictId, selectedWardCode]);

  return (
    <div>
      {/* ===== Head Section ===== */}
      <Header title={`Shopping Cart - Twenty`} />

      <main id="main-content">
        {/* ===== Heading & Continue Shopping */}
        <div className="app-max-width px-4 sm:px-8 md:px-20 w-full border-t-2 border-gray100">
          <h1 className="text-2xl sm:text-4xl text-center sm:text-left mt-6 mb-2 animatee__animated animate__bounce">
            {t("shopping_cart")}
          </h1>
          <div className="mt-6 mb-3">
            <Link href="/">
              <a className="inline-block">
                <LeftArrow size="sm" extraClass="inline-block" />{" "}
                {t("continue_shopping")}
              </a>
            </Link>
          </div>
        </div>

        {/* ===== Cart Table Section ===== */}
        <div className="app-max-width px-4 sm:px-8 md:px-20 mb-14 flex flex-col lg:flex-row">
          <div className="h-full w-full lg:w-4/6 mr-4">
            <table className="w-full mb-6">
              <thead>
                <tr className="border-t-2 border-b-2 border-gray200">
                  <th className="font-normal text-left sm:text-center py-2 xl:w-72">
                    {t("product_details")}
                  </th>
                  <th
                    className={`font-normal py-2 hidden sm:block ${
                      cart.length === 0 ? "text-center" : "text-right"
                    }`}
                  >
                    {t("unit_price")}
                  </th>
                  <th className="font-normal py-2">{t("quantity")}</th>
                  <th className="font-normal py-2 text-right">{t("amount")}</th>
                  <th
                    className="font-normal py-2 text-right"
                    style={{ minWidth: "3rem" }}
                  ></th>
                </tr>
              </thead>
              <tbody>
                {cart.length === 0 ? (
                  <tr className="w-full text-center h-60 border-b-2 border-gray200">
                    <td colSpan={5}>{t("cart_is_empty")}</td>
                  </tr>
                ) : (
                  cart.map((item) => {
                    subtotal += item.price * item.qty!;
                    const productLink = item.slug
                      ? `/products/${encodeURIComponent(item.slug)}`
                      : "/";
                    return (
                      <tr
                        className="border-b-2 border-gray200"
                        key={`${item.id}-${item.selectedSize || "na"}`}
                      >
                        <td className="my-3 flex flex-col xl:flex-row items-start sm:items-center xl:space-x-2 text-center xl:text-left">
                          <Link href={productLink}>
                            <a>
                              <Image
                                src={item.img1 as string}
                                alt={item.name}
                                width={95}
                                height={128}
                                className="h-32 xl:mr-4"
                              />
                            </a>
                          </Link>
                          <span>
                            {item.name}
                            {item.selectedSize ? (
                              <span className="block text-sm text-gray400">
                                Size: {item.selectedSize}
                              </span>
                            ) : null}
                          </span>
                        </td>
                        <td className="text-right text-gray400 hidden sm:table-cell">
                          {formatPrice(roundDecimal(item.price))}
                        </td>
                        <td>
                          <div className="w-12 h-32 sm:h-auto sm:w-3/4 md:w-2/6 mx-auto flex flex-col-reverse sm:flex-row border border-gray300 sm:divide-x-2 divide-gray300">
                            <div
                              onClick={() => removeItem!(item)}
                              className="h-full w-12 flex justify-center items-center cursor-pointer hover:bg-gray500 hover:text-gray100"
                            >
                              -
                            </div>
                            <div className="h-full w-12 flex justify-center items-center pointer-events-none">
                              {item.qty}
                            </div>
                            <div
                              onClick={() => addOne!(item)}
                              className="h-full w-12 flex justify-center items-center cursor-pointer hover:bg-gray500 hover:text-gray100"
                            >
                              +
                            </div>
                          </div>
                        </td>
                        <td className="text-right text-gray400">
                          {formatPrice(roundDecimal(item.price * item.qty!))}
                          <br />
                          <span className="text-xs">
                            ({formatPrice(roundDecimal(item.price))})
                          </span>
                        </td>
                        <td className="text-right" style={{ minWidth: "3rem" }}>
                          <button
                            onClick={() => deleteItem!(item)}
                            type="button"
                            className="outline-none text-gray300 hover:text-gray500 focus:outline-none text-4xl sm:text-2xl"
                          >
                            &#10005;
                          </button>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
            <div>
              <GhostButton
                onClick={clearCart}
                extraClass="hidden sm:inline-block"
              >
                {t("clear_cart")}
              </GhostButton>
            </div>
          </div>
          <div className="h-full w-full lg:w-4/12 mt-10 lg:mt-0">
            {/* Cart Totals */}
            <div className="border border-gray500 divide-y-2 divide-gray200 p-6">
              <h2 className="text-xl mb-3">{t("cart_totals")}</h2>
              <div className="flex justify-between py-2">
                <span className="uppercase">{t("subtotal")}</span>
                <span>{formatPrice(roundDecimal(subtotal))}</span>
              </div>
              <div className="py-3 flex justify-between">
                <span className="uppercase">{t("shipping_fee")}</span>
                <span>{deliFee === null ? "_" : formatPrice(deliFee)}</span>
              </div>
              <div className="flex justify-between py-3">
                <span>{t("order_total")}</span>
                <span>{formatPrice(roundDecimal(subtotal + (deliFee ?? 0)))}</span>
              </div>
              <Button
                value={t("proceed_to_checkout")}
                size="xl"
                extraClass="w-full"
                onClick={() => router.push(`/checkout`)}
                disabled={cart.length < 1 ? true : false}
              />
            </div>
          </div>
        </div>
      </main>

      {/* ===== Footer Section ===== */}
      <Footer />
    </div>
  );
};

export const getStaticProps: GetStaticProps = async ({ locale }) => {
  return {
    props: {
      messages: (await import(`../messages/common/${locale}.json`)).default,
    },
  };
};

export default ShoppingCart;
