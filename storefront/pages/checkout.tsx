import { useEffect, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import axios from "axios";
import Image from "next/image";
import { GetStaticProps } from "next";

import Header from "../components/Header/Header";
import Footer from "../components/Footer/Footer";
import Button from "../components/Buttons/Button";
import { roundDecimal } from "../components/Util/utilFunc";
import { useCart } from "../context/cart/CartProvider";
import Input from "../components/Input/Input";
import { itemType } from "../context/wishlist/wishlist-type";
import { useAuth } from "../context/AuthContext";
import { useCurrency } from "../context/CurrencyContext";

// let w = window.innerWidth;
type PaymentType = "CASH_ON_DELIVERY" | "MOMO" | "VNPAY" | "BANK_TRANSFER";

type Order = {
  id?: number;
  orderNumber?: number;
  customerId?: number;
  shippingAddress: string;
  township?: null | string;
  city?: null | string;
  state?: null | string;
  zipCode?: null | string;
  orderDate: string;
  paymentType?: PaymentType;
  paymentMethod?: string;
  deliveryType?: string;
  totalPrice: number;
  deliveryDate: string;
  paymentUrl?: string | null;
};

type UserAddress = {
  id: number;
  recipientName: string;
  phone: string;
  province: string;
  district: string;
  ward: string;
  addressLine: string;
  isDefault: boolean;
};

type AddressUnit = {
  id: string;
  name: string;
};

type BackendCartItem = {
  id: number;
};

type BackendCartResponse = {
  items?: BackendCartItem[];
};

type ProductVariantApi = {
  id: number;
  stock?: number;
  status?: string;
  size?: string;
  color?: string;
};

type ProductDetailApi = {
  variants?: ProductVariantApi[];
};

const unwrapApiData = <T,>(payload: any): T => {
  if (
    payload &&
    typeof payload === "object" &&
    Object.prototype.hasOwnProperty.call(payload, "data")
  ) {
    return payload.data as T;
  }
  return payload as T;
};

const ShoppingCart = () => {
  const t = useTranslations("CartWishlist");
  const { formatPrice } = useCurrency();
  const { cart, clearCart } = useCart();
  const auth = useAuth();
  const [paymentMethod, setPaymentMethod] =
    useState<PaymentType>("CASH_ON_DELIVERY");

  // Form Fields
  const [name, setName] = useState(auth.user?.fullname || "");
  const [email, setEmail] = useState(auth.user?.email || "");
  const [phone, setPhone] = useState(auth.user?.phone || "");
  const [password, setPassword] = useState("");
  const [address, setAddress] = useState(auth.user?.shippingAddress || "");
  const [addressLine, setAddressLine] = useState("");
  const [isOrdering, setIsOrdering] = useState(false);
  const [errorMsg, setErrorMsg] = useState("");
  const [completedOrder, setCompletedOrder] = useState<Order | null>(null);
  const [orderError, setOrderError] = useState("");
  const [sendEmail, setSendEmail] = useState(false);
  const [savedAddresses, setSavedAddresses] = useState<UserAddress[]>([]);
  const [selectedAddressId, setSelectedAddressId] = useState<number | null>(null);
  const [provinces, setProvinces] = useState<AddressUnit[]>([]);
  const [districts, setDistricts] = useState<AddressUnit[]>([]);
  const [wards, setWards] = useState<AddressUnit[]>([]);
  const [selectedProvinceId, setSelectedProvinceId] = useState("");
  const [selectedDistrictId, setSelectedDistrictId] = useState("");
  const [selectedWardCode, setSelectedWardCode] = useState("");
  const placingOrderRef = useRef(false);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => {
    if (!isOrdering || placingOrderRef.current) return;
    placingOrderRef.current = true;

    setErrorMsg("");

    const makeOrder = async () => {
      try {
        if (!auth.user) {
          const regResponse = await auth.register!(
            email,
            name,
            password,
            address,
            phone
          );
          if (!regResponse.success) {
            setIsOrdering(false);
            if (regResponse.message === "alreadyExists") {
              setErrorMsg("email_already_exists");
            } else {
              setErrorMsg("error_occurs");
            }
            return;
          }
          // Wait for auth.user state update, then effect reruns and creates order once.
          placingOrderRef.current = false;
          return;
        }

        const authToken = auth?.user?.token;
        if (!authToken) {
          setOrderError(t("login_required_reorder"));
          setIsOrdering(false);
          return;
        }

        // Sync FE local cart to BE cart because /api/orders reads from backend cart.
        const backendCartRes = await axios.get<BackendCartResponse>(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/cart`,
          {
            headers: {
              Authorization: `Bearer ${authToken}`,
            },
          }
        );
        const backendCartData = unwrapApiData<BackendCartResponse>(backendCartRes.data);
        const backendItems = Array.isArray(backendCartData?.items)
          ? backendCartData.items
          : [];
        await Promise.all(
          backendItems.map((item) =>
            axios.delete(
              `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/cart/items/${item.id}`,
              {
                headers: {
                  Authorization: `Bearer ${authToken}`,
                },
              }
            )
          )
        );

        for (const cartItem of cart) {
          const productRes = await axios.get<ProductDetailApi>(
            `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/products/${cartItem.id}`
          );
          const variants = Array.isArray(productRes.data?.variants)
            ? productRes.data.variants
            : [];
          const normalizedSelectedSize = (cartItem.selectedSize || "")
            .trim()
            .toUpperCase();
          const normalizedSelectedColor = (cartItem.selectedColor || "")
            .trim()
            .toUpperCase();
          const pickedVariantBySizeAndColor = variants.find(
            (variant) =>
              (variant.size || "").trim().toUpperCase() === normalizedSelectedSize &&
              (variant.color || "").trim().toUpperCase() === normalizedSelectedColor &&
              Number(variant.stock || 0) > 0 &&
              (variant.status || "").toUpperCase() !== "INACTIVE"
          );
          const pickedVariantBySize = variants.find(
            (variant) =>
              (variant.size || "").trim().toUpperCase() === normalizedSelectedSize &&
              Number(variant.stock || 0) > 0 &&
              (variant.status || "").toUpperCase() !== "INACTIVE"
          );
          const pickedVariantFallback = variants.find(
            (variant) =>
              Number(variant.stock || 0) > 0 &&
              (variant.status || "").toUpperCase() !== "INACTIVE"
          );
          const pickedVariant =
            pickedVariantBySizeAndColor || pickedVariantBySize || pickedVariantFallback;
          if (!pickedVariant?.id) {
            throw new Error(`${t("no_valid_variant_for_product")}: ${cartItem.name}`);
          }
          await axios.post(
            `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/cart/items`,
            {
              variantId: pickedVariant.id,
              quantity: cartItem.qty || 1,
            },
            {
              headers: {
                Authorization: `Bearer ${authToken}`,
              },
            }
          );
        }

        const fullAddress = address;
        const selectedProvince =
          provinces.find((item) => item.id === selectedProvinceId)?.name || "";
        const selectedDistrict =
          districts.find((item) => item.id === selectedDistrictId)?.name || "";
        const selectedWard =
          wards.find((item) => item.id === selectedWardCode)?.name || "";
        const res = await axios.post(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/orders`,
          {
            paymentMethod:
              paymentMethod === "CASH_ON_DELIVERY"
                ? "COD"
                : paymentMethod === "MOMO"
                ? "MOMO"
                : "VNPAY",
            address: fullAddress,
            recipientName: name,
            phone,
            province: selectedProvince,
            district: selectedDistrict,
            ward: selectedWard,
            shippingFee: deliFee ?? undefined,
          },
          {
            headers: {
              Authorization: `Bearer ${authToken}`,
            },
          }
        );
        const createdOrder: Order = res.data;
        if (sendEmail && createdOrder?.id) {
          try {
            await axios.post(
              `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/orders/my/${createdOrder.id}/send-confirmation-email`,
              {},
              {
                headers: {
                  Authorization: `Bearer ${authToken}`,
                },
              }
            );
          } catch (emailErr) {
            console.error("Send order email failed:", emailErr);
          }
        }

        if (createdOrder?.paymentUrl) {
          setIsOrdering(false);
          window.location.href = createdOrder.paymentUrl;
          return;
        }

        setCompletedOrder(createdOrder);
        clearCart!();
        setIsOrdering(false);
      } catch (err) {
        console.error("Create order failed:", err);
        const axiosErr = err as any;
        const backendMessage =
          axiosErr?.response?.data?.message ||
          axiosErr?.response?.data?.error ||
          axiosErr?.message ||
          "";
        setOrderError(backendMessage || t("place_order_failed"));
        setIsOrdering(false);
      } finally {
        placingOrderRef.current = false;
      }
    };
    makeOrder();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOrdering, auth.user]);

  useEffect(() => {
    if (auth.user) {
      setName(auth.user.fullname);
      setEmail(auth.user.email);
      setAddress(auth.user.shippingAddress || "");
      setPhone(auth.user.phone || "");
    } else {
      setName("");
      setEmail("");
      setAddress("");
      setPhone("");
    }
  }, [auth.user]);

  useEffect(() => {
    const loadProvinces = async () => {
      try {
        const res = await axios.get(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/address/provinces`
        );
        const provinceData = unwrapApiData<AddressUnit[]>(res.data);
        setProvinces(Array.isArray(provinceData) ? provinceData : []);
      } catch (err) {
        console.error("Load provinces failed:", err);
      }
    };
    loadProvinces();
  }, []);

  useEffect(() => {
    if (!selectedProvinceId) {
      setDistricts([]);
      setSelectedDistrictId("");
      setWards([]);
      setSelectedWardCode("");
      return;
    }
    const loadDistricts = async () => {
      try {
        const res = await axios.get(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/address/districts`,
          { params: { provinceId: selectedProvinceId } }
        );
        const districtData = unwrapApiData<AddressUnit[]>(res.data);
        setDistricts(Array.isArray(districtData) ? districtData : []);
      } catch (err) {
        console.error("Load districts failed:", err);
      }
    };
    loadDistricts();
  }, [selectedProvinceId]);

  useEffect(() => {
    if (!selectedDistrictId) {
      setWards([]);
      setSelectedWardCode("");
      return;
    }
    const loadWards = async () => {
      try {
        const res = await axios.get(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/address/wards`,
          { params: { districtId: selectedDistrictId } }
        );
        const wardData = unwrapApiData<AddressUnit[]>(res.data);
        setWards(Array.isArray(wardData) ? wardData : []);
      } catch (err) {
        console.error("Load wards failed:", err);
      }
    };
    loadWards();
  }, [selectedDistrictId]);

  useEffect(() => {
    if (!address) return;
    const parts = address
      .split(",")
      .map((x) => x.trim())
      .filter(Boolean);
    const p = parts.length >= 4 ? parts[parts.length - 1] : "";
    const d = parts.length >= 3 ? parts[parts.length - 2] : "";
    const w = parts.length >= 2 ? parts[parts.length - 3] : "";
    if (p && provinces.length > 0) {
      const foundProvince = provinces.find((item) => item.name === p);
      if (foundProvince) setSelectedProvinceId(foundProvince.id);
    }
    if (d && districts.length > 0) {
      const foundDistrict = districts.find((item) => item.name === d);
      if (foundDistrict) setSelectedDistrictId(foundDistrict.id);
    }
    if (w && wards.length > 0) {
      const foundWard = wards.find((item) => item.name === w);
      if (foundWard) setSelectedWardCode(foundWard.id);
    }
  }, [address, provinces, districts, wards]);

  useEffect(() => {
    const loadSavedAddresses = async () => {
      if (!auth.user?.token) return;
      try {
        const res = await axios.get(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/user/addresses`,
          {
            headers: {
              Authorization: `Bearer ${auth.user.token}`,
            },
          }
        );
        const addressData = unwrapApiData<UserAddress[]>(res.data);
        const addresses: UserAddress[] = Array.isArray(addressData) ? addressData : [];
        setSavedAddresses(addresses);
        if (addresses.length === 0) return;
        const defaultAddress =
          addresses.find((item) => item.isDefault) || addresses[0];
        setSelectedAddressId(defaultAddress.id);
        setAddressLine(defaultAddress.addressLine || "");
        setPhone(defaultAddress.phone || auth.user.phone || "");
        setName(defaultAddress.recipientName || auth.user.fullname);

        // Resolve saved province/district/ward names to ids so selects auto-fill reliably.
        const provincesRes = await axios.get(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/address/provinces`
        );
        const provinceData = unwrapApiData<AddressUnit[]>(provincesRes.data);
        const provinceOptions: AddressUnit[] = Array.isArray(provinceData)
          ? provinceData
          : [];
        if (provinceOptions.length > 0) setProvinces(provinceOptions);
        const matchedProvince = provinceOptions.find(
          (item) => item.name === defaultAddress.province
        );
        if (!matchedProvince) return;
        setSelectedProvinceId(matchedProvince.id);

        const districtsRes = await axios.get(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/address/districts`,
          { params: { provinceId: matchedProvince.id } }
        );
        const districtData = unwrapApiData<AddressUnit[]>(districtsRes.data);
        const districtOptions: AddressUnit[] = Array.isArray(districtData)
          ? districtData
          : [];
        setDistricts(districtOptions);
        const matchedDistrict = districtOptions.find(
          (item) => item.name === defaultAddress.district
        );
        if (!matchedDistrict) return;
        setSelectedDistrictId(matchedDistrict.id);

        const wardsRes = await axios.get(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/address/wards`,
          { params: { districtId: matchedDistrict.id } }
        );
        const wardData = unwrapApiData<AddressUnit[]>(wardsRes.data);
        const wardOptions: AddressUnit[] = Array.isArray(wardData)
          ? wardData
          : [];
        setWards(wardOptions);
        const matchedWard = wardOptions.find(
          (item) => item.name === defaultAddress.ward
        );
        if (matchedWard) setSelectedWardCode(matchedWard.id);
      } catch (err) {
        console.error("Load saved addresses failed:", err);
      }
    };
    loadSavedAddresses();
  }, [auth.user]);

  const handleSelectSavedAddress = (addressId: number) => {
    setSelectedAddressId(addressId);
    const selected = savedAddresses.find((item) => item.id === addressId);
    if (!selected) return;
    const composedAddress = [
      selected.addressLine,
      selected.ward,
      selected.district,
      selected.province,
    ]
      .filter(Boolean)
      .join(", ");
    setAddress(composedAddress);
    setAddressLine(selected.addressLine || "");
    setPhone(selected.phone || phone);
    setName(selected.recipientName || name);
  };

  const handleProvinceChange = (provinceId: string) => {
    setSelectedProvinceId(provinceId);
    setSelectedDistrictId("");
    setSelectedWardCode("");
  };

  const handleDistrictChange = (districtId: string) => {
    setSelectedDistrictId(districtId);
    setSelectedWardCode("");
  };

  const handleWardChange = (wardCode: string) => {
    setSelectedWardCode(wardCode);
  };

  useEffect(() => {
    const selectedProvince =
      provinces.find((item) => item.id === selectedProvinceId)?.name || "";
    const selectedDistrict =
      districts.find((item) => item.id === selectedDistrictId)?.name || "";
    const selectedWard =
      wards.find((item) => item.id === selectedWardCode)?.name || "";
    const composedAddress = [
      addressLine,
      selectedWard,
      selectedDistrict,
      selectedProvince,
    ]
      .filter(Boolean)
      .join(", ");
    setAddress(composedAddress);
  }, [
    addressLine,
    selectedProvinceId,
    selectedDistrictId,
    selectedWardCode,
    provinces,
    districts,
    wards,
  ]);

  let disableOrder = true;

  if (!auth.user) {
    disableOrder =
      name !== "" &&
      email !== "" &&
      phone !== "" &&
      address !== "" &&
      password !== ""
        ? false
        : true;
  } else {
    disableOrder =
      name !== "" &&
      email !== "" &&
      phone !== "" &&
      addressLine !== "" &&
      selectedProvinceId !== "" &&
      selectedDistrictId !== "" &&
      selectedWardCode !== ""
        ? false
        : true;
  }

  let subtotal: number | string = 0;

  subtotal = roundDecimal(
    cart.reduce(
      (accumulator: number, currentItem: itemType) =>
        accumulator + currentItem.price * currentItem!.qty!,
      0
    )
  );

  const [deliFee, setDeliFee] = useState<number | null>(null);

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
        const feeData = unwrapApiData<{ fee?: number }>(res.data);
        setDeliFee(Number(feeData?.fee || 0));
      } catch (err) {
        console.error("Load shipping fee failed:", err);
        setDeliFee(null);
      }
    };
    loadShippingFee();
  }, [selectedDistrictId, selectedWardCode]);

  return (
    <div>
      {/* ===== Head Section ===== */}
      <Header title={`${t("checkout")} - Twenty`} />

      <main id="main-content">
        {/* ===== Heading & Continue Shopping */}
        <div className="app-max-width px-4 sm:px-8 md:px-20 w-full border-t-2 border-gray100">
          <h1 className="text-2xl sm:text-4xl text-center sm:text-left mt-6 mb-2 animatee__animated animate__bounce">
            {t("checkout")}
          </h1>
        </div>

        {/* ===== Form Section ===== */}
        {!completedOrder ? (
          <div className="app-max-width px-4 sm:px-8 md:px-20 mb-14 flex flex-col lg:flex-row">
            <div className="h-full w-full lg:w-7/12 mr-8">
              {errorMsg !== "" && (
                <span className="text-red text-sm font-semibold">
                  - {t(errorMsg)}
                </span>
              )}
              <div className="my-4">
                <label htmlFor="name" className="text-lg">
                  {t("name")}
                </label>
                <Input
                  name="name"
                  type="text"
                  extraClass="w-full mt-1 mb-2"
                  border="border-2 border-gray400"
                  value={name}
                  onChange={(e) =>
                    setName((e.target as HTMLInputElement).value)
                  }
                  required
                />
              </div>

              <div className="my-4">
                <label htmlFor="email" className="text-lg mb-1">
                  {t("email_address")}
                </label>
                <Input
                  name="email"
                  type="email"
                  readOnly={auth.user ? true : false}
                  extraClass={`w-full mt-1 mb-2 ${
                    auth.user ? "bg-gray100 cursor-not-allowed" : ""
                  }`}
                  border="border-2 border-gray400"
                  value={email}
                  onChange={(e) =>
                    setEmail((e.target as HTMLInputElement).value)
                  }
                  required
                />
              </div>

              {!auth.user && (
                <div className="my-4">
                  <label htmlFor="password" className="text-lg">
                    {t("password")}
                  </label>
                  <Input
                    name="password"
                    type="password"
                    extraClass="w-full mt-1 mb-2"
                    border="border-2 border-gray400"
                    value={password}
                    onChange={(e) =>
                      setPassword((e.target as HTMLInputElement).value)
                    }
                    required
                  />
                </div>
              )}

              <div className="my-4">
                <label htmlFor="phone" className="text-lg">
                  {t("phone")}
                </label>
                <Input
                  name="phone"
                  type="text"
                  extraClass="w-full mt-1 mb-2"
                  border="border-2 border-gray400"
                  value={phone}
                  onChange={(e) =>
                    setPhone((e.target as HTMLInputElement).value)
                  }
                  required
                />
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 my-4">
                <div>
                  <label htmlFor="address" className="text-lg">
                    {t("address")}
                  </label>
                  <Input
                    name="addressLine"
                    type="text"
                    extraClass="w-full mt-1 mb-2"
                    border="border-2 border-gray400"
                    value={addressLine}
                    onChange={(e) =>
                      setAddressLine((e.target as HTMLInputElement).value)
                    }
                    required
                  />
                </div>
                <div>
                  <label htmlFor="province" className="text-lg">
                    {t("province_city")}
                  </label>
                  <select
                    id="province"
                    className="w-full mt-1 mb-2 border-2 border-gray400 p-3 outline-none bg-white"
                    value={selectedProvinceId}
                    onChange={(e) => handleProvinceChange(e.target.value)}
                    required
                  >
                    <option value="">{t("select_province_city")}</option>
                    {provinces.map((item) => (
                      <option key={item.id} value={item.id}>
                        {item.name}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label htmlFor="district" className="text-lg">
                    {t("district")}
                  </label>
                  <select
                    id="district"
                    className="w-full mt-1 mb-2 border-2 border-gray400 p-3 outline-none bg-white"
                    value={selectedDistrictId}
                    onChange={(e) => handleDistrictChange(e.target.value)}
                    disabled={!selectedProvinceId}
                    required
                  >
                    <option value="">{t("select_district")}</option>
                    {districts.map((item) => (
                      <option key={item.id} value={item.id}>
                        {item.name}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label htmlFor="ward" className="text-lg">
                    {t("ward")}
                  </label>
                  <select
                    id="ward"
                    className="w-full mt-1 mb-2 border-2 border-gray400 p-3 outline-none bg-white"
                    value={selectedWardCode}
                    onChange={(e) => handleWardChange(e.target.value)}
                    disabled={!selectedDistrictId}
                    required
                  >
                    <option value="">{t("select_ward")}</option>
                    {wards.map((item) => (
                      <option key={item.id} value={item.id}>
                        {item.name}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {!auth.user && (
                <div className="text-sm text-gray400 mt-8 leading-6">
                  {t("form_note")}
                </div>
              )}
            </div>
            <div className="h-full w-full lg:w-5/12 mt-10 lg:mt-4">
              {/* Cart Totals */}
              <div className="border border-gray500 p-6 divide-y-2 divide-gray200">
                <div className="flex justify-between">
                  <span className="text-base uppercase mb-3">
                    {t("product")}
                  </span>
                  <span className="text-base uppercase mb-3">
                    {t("subtotal")}
                  </span>
                </div>

                <div className="pt-2">
                  {cart.map((item) => (
                    <div
                      className="flex justify-between mb-2"
                      key={`${item.id}-${item.selectedSize || "na"}-${item.selectedColor || "na"}`}
                    >
                      <span className="text-base font-medium">
                        {item.name}
                        {item.selectedSize ? (
                          <span className="text-gray400 ml-1">
                            ({item.selectedSize})
                          </span>
                        ) : null}
                        {item.selectedColor ? (
                          <span className="text-gray400 ml-1">
                            [{item.selectedColor}]
                          </span>
                        ) : null}{" "}
                        <span className="text-gray400">x {item.qty}</span>
                      </span>
                      <span className="text-base">
                        {formatPrice(roundDecimal(item.price * item!.qty!))}
                      </span>
                    </div>
                  ))}
                </div>

                <div className="py-3 flex justify-between">
                  <span className="uppercase">{t("subtotal")}</span>
                  <span>{formatPrice(+subtotal)}</span>
                </div>

                <div className="py-3 flex justify-between">
                  <span className="uppercase">{t("shipping_fee")}</span>
                  <span>{deliFee === null ? "_" : formatPrice(deliFee)}</span>
                </div>

                <div>
                  <div className="flex justify-between py-3">
                    <span>{t("order_total")}</span>
                    <span>{formatPrice(roundDecimal(+subtotal + (deliFee ?? 0)))}</span>
                  </div>

                  <div className="grid gap-3 mt-2 mb-4">
                    <label
                      htmlFor="plan-cash"
                      className="relative flex items-center justify-between bg-white p-4 rounded-lg border border-gray300 cursor-pointer"
                    >
                      <div className="pr-8">
                        <span className="font-semibold text-gray-700 text-base leading-tight capitalize block">
                          {t("cash_on_delivery")}
                        </span>
                        <span className="text-gray400 text-sm mt-1 block">
                          {t("cash_on_delivery_desc")}
                        </span>
                      </div>
                      <input
                        type="radio"
                        name="plan"
                        id="plan-cash"
                        value="CASH_ON_DELIVERY"
                        className="absolute h-0 w-0 appearance-none"
                        onChange={() => setPaymentMethod("CASH_ON_DELIVERY")}
                      />
                      <span
                        aria-hidden="true"
                        className={`${
                          paymentMethod === "CASH_ON_DELIVERY"
                            ? "block"
                            : "hidden"
                        } absolute inset-0 border-2 border-gray500 bg-opacity-10 rounded-lg`}
                      >
                        <span className="absolute top-3 right-3 h-6 w-6 inline-flex items-center justify-center rounded-full bg-gray100">
                          <svg
                            xmlns="http://www.w3.org/2000/svg"
                            viewBox="0 0 20 20"
                            fill="currentColor"
                            className="h-5 w-5 text-green-600"
                          >
                            <path
                              fillRule="evenodd"
                              d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                              clipRule="evenodd"
                            />
                          </svg>
                        </span>
                      </span>
                    </label>
                    <label
                      htmlFor="plan-bank"
                      className="relative flex items-center justify-between bg-white p-4 rounded-lg border border-gray300 cursor-pointer"
                    >
                      <div className="flex items-center gap-3 pr-8">
                        <Image
                          src="/payments/momo.svg"
                          alt="MoMo"
                          width={44}
                          height={44}
                        />
                        <div>
                          <span className="font-semibold text-gray-700 leading-tight capitalize block">
                            MoMo
                          </span>
                          <span className="text-gray400 text-sm mt-1 block">
                            {t("momo_desc")}
                          </span>
                        </div>
                      </div>
                      <input
                        type="radio"
                        name="plan"
                        id="plan-bank"
                        value="MOMO"
                        className="absolute h-0 w-0 appearance-none"
                        onChange={() => setPaymentMethod("MOMO")}
                      />
                      <span
                        aria-hidden="true"
                        className={`${
                          paymentMethod === "MOMO" ? "block" : "hidden"
                        } absolute inset-0 border-2 border-gray500 bg-opacity-10 rounded-lg`}
                      >
                        <span className="absolute top-3 right-3 h-6 w-6 inline-flex items-center justify-center rounded-full bg-gray100">
                          <svg
                            xmlns="http://www.w3.org/2000/svg"
                            viewBox="0 0 20 20"
                            fill="currentColor"
                            className="h-5 w-5 text-green-600"
                          >
                            <path
                              fillRule="evenodd"
                              d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                              clipRule="evenodd"
                            />
                          </svg>
                        </span>
                      </span>
                    </label>
                    <label
                      htmlFor="plan-vnpay"
                      className="relative flex items-center justify-between bg-white p-4 rounded-lg border border-gray300 cursor-pointer"
                    >
                      <div className="flex items-center gap-3 pr-8">
                        <Image
                          src="/payments/zalo.svg"
                          alt="ZaloPay"
                          width={44}
                          height={44}
                        />
                        <div>
                          <span className="font-semibold text-gray-700 leading-tight capitalize block">
                            VNPay
                          </span>
                          <span className="text-gray400 text-sm mt-1 block">
                            {t("vnpay_desc")}
                          </span>
                        </div>
                      </div>
                      <input
                        type="radio"
                        name="plan"
                        id="plan-vnpay"
                        value="VNPAY"
                        className="absolute h-0 w-0 appearance-none"
                        onChange={() => setPaymentMethod("VNPAY")}
                      />
                      <span
                        aria-hidden="true"
                        className={`${
                          paymentMethod === "VNPAY" ? "block" : "hidden"
                        } absolute inset-0 border-2 border-gray500 bg-opacity-10 rounded-lg`}
                      >
                        <span className="absolute top-3 right-3 h-6 w-6 inline-flex items-center justify-center rounded-full bg-gray100">
                          <svg
                            xmlns="http://www.w3.org/2000/svg"
                            viewBox="0 0 20 20"
                            fill="currentColor"
                            className="h-5 w-5 text-green-600"
                          >
                            <path
                              fillRule="evenodd"
                              d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                              clipRule="evenodd"
                            />
                          </svg>
                        </span>
                      </span>
                    </label>
                  </div>

                  <div className="my-8">
                    <div className="relative inline-block w-10 mr-2 align-middle select-none transition duration-200 ease-in">
                      <input
                        type="checkbox"
                        name="send-email-toggle"
                        id="send-email-toggle"
                        checked={sendEmail}
                        onChange={() => setSendEmail(!sendEmail)}
                        className="toggle-checkbox absolute block w-6 h-6 rounded-full bg-white border-4 border-gray300 appearance-none cursor-pointer"
                      />
                      <label
                        htmlFor="send-email-toggle"
                        className="toggle-label block overflow-hidden h-6 rounded-full bg-gray300 cursor-pointer"
                      ></label>
                    </div>
                    <label
                      htmlFor="send-email-toggle"
                      className="text-xs text-gray-700"
                    >
                      {t("send_order_email")}
                    </label>
                  </div>
                </div>

                <Button
                  value={t("place_order")}
                  size="xl"
                  extraClass={`w-full`}
                  onClick={() => setIsOrdering(true)}
                  disabled={disableOrder || isOrdering}
                />
              </div>

              {orderError !== "" && (
                <span className="text-red text-sm font-semibold">
                  - {orderError}
                </span>
              )}
            </div>
          </div>
        ) : (
          <div className="app-max-width px-4 sm:px-8 md:px-20 mb-14 mt-6">
            <div className="text-gray400 text-base">{t("thank_you_note")}</div>

            <div className="flex flex-col md:flex-row">
              <div className="h-full w-full md:w-1/2 mt-2 lg:mt-4">
                <div className="border border-gray500 p-6 divide-y-2 divide-gray200">
                  <div className="flex justify-between">
                    <span className="text-base uppercase mb-3">
                      {t("order_id")}
                    </span>
                    <span className="text-base uppercase mb-3">
                      {completedOrder.orderNumber}
                    </span>
                  </div>

                  <div className="pt-2">
                    <div className="flex justify-between mb-2">
                      <span className="text-base">{t("email_address")}</span>
                      <span className="text-base">{auth.user?.email}</span>
                    </div>
                    <div className="flex justify-between mb-2">
                      <span className="text-base">{t("order_date")}</span>
                      <span className="text-base">
                        {new Date(
                          completedOrder.orderDate
                        ).toLocaleDateString()}
                      </span>
                    </div>
                    <div className="flex justify-between mb-2">
                      <span className="text-base">{t("delivery_date")}</span>
                      <span className="text-base">
                        {new Date(
                          completedOrder.deliveryDate
                        ).toLocaleDateString()}
                      </span>
                    </div>
                  </div>

                  <div className="py-3">
                    <div className="flex justify-between mb-2">
                      <span className="">{t("payment_method")}</span>
                      <span>{completedOrder.paymentType}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="">{t("delivery_method")}</span>
                      <span>{completedOrder.deliveryType}</span>
                    </div>
                  </div>

                  <div className="pt-2 flex justify-between mb-2">
                    <span className="text-base uppercase">{t("total")}</span>
                    <span className="text-base">
                      {formatPrice(completedOrder.totalPrice)}
                    </span>
                  </div>
                </div>
              </div>

              <div className="h-full w-full md:w-1/2 md:ml-8 mt-4 md:mt-2 lg:mt-4">
                <div>
                  {t("your_order_received")}
                  {completedOrder.paymentType === "BANK_TRANSFER" &&
                    t("bank_transfer_note")}
                  {completedOrder.paymentType === "CASH_ON_DELIVERY" &&
                    completedOrder.deliveryType !== "STORE_PICKUP" &&
                    t("cash_delivery_note")}
                  {completedOrder.deliveryType === "STORE_PICKUP" &&
                    t("store_pickup_note")}
                  {t("thank_you_for_purchasing")}
                </div>

                {completedOrder.paymentType === "BANK_TRANSFER" ? (
                  <div className="mt-6">
                    <h2 className="text-xl font-bold">
                      {t("our_banking_details")}
                    </h2>
                    <span className="uppercase block my-1">Sat Naing :</span>

                    <div className="flex justify-between w-full xl:w-1/2">
                      <span className="text-sm font-bold">AYA Bank</span>
                      <span className="text-base">20012345678</span>
                    </div>
                    <div className="flex justify-between w-full xl:w-1/2">
                      <span className="text-sm font-bold">CB Bank</span>
                      <span className="text-base">0010123456780959</span>
                    </div>
                    <div className="flex justify-between w-full xl:w-1/2">
                      <span className="text-sm font-bold">KPay</span>
                      <span className="text-base">095096051</span>
                    </div>
                  </div>
                ) : (
                  <div className="flex justify-center items-center h-56">
                    <div className="w-3/4">
                      <Image
                        className="justify-center"
                        src="/logo.svg"
                        alt="Twenty"
                        width={220}
                        height={50}
                        layout="responsive"
                      />
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
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
