import { useCallback, useEffect, useState } from "react";
import { GetStaticProps } from "next";
import { useRouter } from "next/router";
import { useTranslations } from "next-intl";
import axios from "axios";

import Header from "../components/Header/Header";
import Footer from "../components/Footer/Footer";
import Input from "../components/Input/Input";
import Button from "../components/Buttons/Button";
import { useAuth } from "../context/AuthContext";
import { useNotify } from "../context/NotificationContext";

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

const ProfilePage = () => {
  const t = useTranslations("LoginRegister");
  const indexT = useTranslations("Index");
  const auth = useAuth();
  const { notify } = useNotify();
  const router = useRouter();

  const [username, setUsername] = useState("");
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [isSavingAddress, setIsSavingAddress] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [addressMessage, setAddressMessage] = useState("");
  const [addressError, setAddressError] = useState("");
  const [addressId, setAddressId] = useState<number | null>(null);
  const [addresses, setAddresses] = useState<UserAddress[]>([]);
  const [recipientName, setRecipientName] = useState("");
  const [addressPhone, setAddressPhone] = useState("");
  const [province, setProvince] = useState("");
  const [district, setDistrict] = useState("");
  const [ward, setWard] = useState("");
  const [addressLine, setAddressLine] = useState("");
  const [provinces, setProvinces] = useState<AddressUnit[]>([]);
  const [districts, setDistricts] = useState<AddressUnit[]>([]);
  const [wards, setWards] = useState<AddressUnit[]>([]);
  const [selectedProvinceId, setSelectedProvinceId] = useState("");
  const [selectedDistrictId, setSelectedDistrictId] = useState("");
  const [selectedWardId, setSelectedWardId] = useState("");
  const [isAddressOptionsLoading, setIsAddressOptionsLoading] = useState(false);
  const [setAsDefault, setSetAsDefault] = useState(true);

  const resetAddressForm = useCallback(() => {
    setAddressId(null);
    setRecipientName(auth.user?.fullname || "");
    setAddressPhone(auth.user?.phone || "");
    setProvince("");
    setDistrict("");
    setWard("");
    setAddressLine("");
    setSelectedProvinceId("");
    setSelectedDistrictId("");
    setSelectedWardId("");
    setSetAsDefault(addresses.length === 0);
  }, [addresses.length, auth.user?.fullname, auth.user?.phone]);

  const fillAddressForm = (item: UserAddress) => {
    setAddressId(item.id);
    setRecipientName(item.recipientName || "");
    setAddressPhone(item.phone || "");
    setProvince(item.province || "");
    setDistrict(item.district || "");
    setWard(item.ward || "");
    setAddressLine(item.addressLine || "");
    setSetAsDefault(Boolean(item.isDefault));
  };

  useEffect(() => {
    if (!auth.isAuthReady) return;
    if (!auth.user) {
      router.push("/");
      return;
    }
    setUsername(auth.user.username || "");
    setFullName(auth.user.fullname || "");
    setEmail(auth.user.email || "");
    setPhone(auth.user.phone || "");
  }, [auth.isAuthReady, auth.user, router]);

  useEffect(() => {
    let active = true;
    const loadProvinces = async () => {
      try {
        const res = await axios.get(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/address/provinces`
        );
        if (!active) return;
        setProvinces(Array.isArray(res.data) ? res.data : []);
      } catch (err) {
        console.error("Load provinces failed:", err);
      }
    };
    loadProvinces();
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    let active = true;
    const loadAddresses = async () => {
      if (!auth.user?.token) return;
      setIsAddressOptionsLoading(true);
      try {
        const res = await axios.get(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/user/addresses`,
          {
            headers: {
              Authorization: `Bearer ${auth.user.token}`,
            },
          }
        );
        if (!active) return;
        const loadedAddresses: UserAddress[] = Array.isArray(res.data)
          ? res.data
          : [];
        setAddresses(loadedAddresses);
        if (loadedAddresses.length === 0) {
          resetAddressForm();
          return;
        }
        const defaultAddress =
          loadedAddresses.find((address) => address.isDefault) ||
          loadedAddresses[0];
        fillAddressForm(defaultAddress);
      } catch (err) {
        console.error("Load addresses failed:", err);
      } finally {
        if (!active) return;
        setIsAddressOptionsLoading(false);
      }
    };
    loadAddresses();
    return () => {
      active = false;
    };
  }, [auth.user, resetAddressForm]);

  useEffect(() => {
    if (!selectedProvinceId) {
      setDistricts([]);
      setSelectedDistrictId("");
      return;
    }
    let active = true;
    const loadDistricts = async () => {
      try {
        const res = await axios.get(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/address/districts`,
          { params: { provinceId: selectedProvinceId } }
        );
        if (!active) return;
        const data: AddressUnit[] = Array.isArray(res.data) ? res.data : [];
        setDistricts(data);
      } catch (err) {
        console.error("Load districts failed:", err);
        if (!active) return;
        setDistricts([]);
      }
    };
    loadDistricts();
    return () => {
      active = false;
    };
  }, [selectedProvinceId]);

  useEffect(() => {
    if (!selectedDistrictId) {
      setWards([]);
      setSelectedWardId("");
      return;
    }
    let active = true;
    const loadWards = async () => {
      try {
        const res = await axios.get(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/address/wards`,
          { params: { districtId: selectedDistrictId } }
        );
        if (!active) return;
        const data: AddressUnit[] = Array.isArray(res.data) ? res.data : [];
        setWards(data);
      } catch (err) {
        console.error("Load wards failed:", err);
        if (!active) return;
        setWards([]);
      }
    };
    loadWards();
    return () => {
      active = false;
    };
  }, [selectedDistrictId]);

  useEffect(() => {
    if (!province || provinces.length === 0) return;
    const found = provinces.find((item) => item.name === province);
    if (found) setSelectedProvinceId(found.id);
  }, [province, provinces]);

  useEffect(() => {
    if (!district || districts.length === 0) return;
    const found = districts.find((item) => item.name === district);
    if (found) setSelectedDistrictId(found.id);
  }, [district, districts]);

  useEffect(() => {
    if (!ward || wards.length === 0) return;
    const found = wards.find((item) => item.name === ward);
    if (found) setSelectedWardId(found.id);
  }, [ward, wards]);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setIsSaving(true);
    setError("");
    setMessage("");
    const payload: {
      username?: string;
      fullName?: string;
      email?: string;
      phone?: string;
    } = {};
    if (username.trim().length >= 3) payload.username = username.trim();
    if (fullName.trim() !== "") payload.fullName = fullName.trim();
    if (email.trim() !== "") payload.email = email.trim();
    if (phone.trim() !== "") payload.phone = phone.trim();
    const response = await auth.updateProfile?.(payload);
    if (response?.success) {
      setMessage(t("profile_updated_success"));
    } else {
      setError(t(response?.message || "error_occurs"));
    }
    setIsSaving(false);
  };

  const handleLogout = () => {
    auth.logout?.();
    notify(t("logout_successful"), "success");
    router.push("/");
  };

  const handleSaveAddress = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!auth.user?.token) return;
    setIsSavingAddress(true);
    setAddressError("");
    setAddressMessage("");
    try {
      const payload = {
        recipientName,
        phone: addressPhone,
        province,
        district,
        ward,
        addressLine,
        isDefault: setAsDefault,
      };
      if (addressId) {
        await axios.put(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/user/addresses/${addressId}`,
          payload,
          {
            headers: {
              Authorization: `Bearer ${auth.user.token}`,
            },
          }
        );
      } else {
        const createRes = await axios.post(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/user/addresses`,
          payload,
          {
            headers: {
              Authorization: `Bearer ${auth.user.token}`,
            },
          }
        );
        setAddressId(createRes.data?.id || null);
      }
      const data = await reloadAddresses();
      if (data.length > 0) {
        const selected =
          data.find((item) => item.id === addressId) ||
          data.find((item) => item.isDefault) ||
          data[0];
        fillAddressForm(selected);
      }
      setAddressMessage(t("address_updated_success"));
    } catch (err) {
      console.error("Save address failed:", err);
      setAddressError(t("address_update_failed"));
    } finally {
      setIsSavingAddress(false);
    }
  };

  const handleProvinceChange = (value: string) => {
    setSelectedProvinceId(value);
    const selected = provinces.find((item) => item.id === value);
    setProvince(selected?.name || "");
    setDistrict("");
    setWard("");
    setSelectedDistrictId("");
    setSelectedWardId("");
  };

  const handleDistrictChange = (value: string) => {
    setSelectedDistrictId(value);
    const selected = districts.find((item) => item.id === value);
    setDistrict(selected?.name || "");
    setWard("");
    setSelectedWardId("");
  };

  const handleWardChange = (value: string) => {
    setSelectedWardId(value);
    const selected = wards.find((item) => item.id === value);
    setWard(selected?.name || "");
  };

  const reloadAddresses = async () => {
    if (!auth.user?.token) return [] as UserAddress[];
    const res = await axios.get(
      `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/user/addresses`,
      {
        headers: {
          Authorization: `Bearer ${auth.user.token}`,
        },
      }
    );
    const data: UserAddress[] = Array.isArray(res.data) ? res.data : [];
    setAddresses(data);
    return data;
  };

  const handleSetDefaultAddress = async (item: UserAddress) => {
    if (!auth.user?.token) return;
    setAddressError("");
    setAddressMessage("");
    try {
      await axios.put(
        `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/user/addresses/${item.id}`,
        {
          recipientName: item.recipientName,
          phone: item.phone,
          province: item.province,
          district: item.district,
          ward: item.ward,
          addressLine: item.addressLine,
          isDefault: true,
        },
        {
          headers: {
            Authorization: `Bearer ${auth.user.token}`,
          },
        }
      );
      const data = await reloadAddresses();
      const defaultAddress = data.find((addr) => addr.isDefault) || data[0];
      if (defaultAddress) fillAddressForm(defaultAddress);
      setAddressMessage(t("address_set_default_success"));
    } catch (err) {
      console.error("Set default address failed:", err);
      setAddressError(t("address_set_default_failed"));
    }
  };

  const handleDeleteAddress = async (id: number) => {
    if (!auth.user?.token) return;
    setAddressError("");
    setAddressMessage("");
    try {
      await axios.delete(
        `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/user/addresses/${id}`,
        {
          headers: {
            Authorization: `Bearer ${auth.user.token}`,
          },
        }
      );
      const data = await reloadAddresses();
      if (data.length === 0) {
        resetAddressForm();
      } else {
        const defaultAddress = data.find((addr) => addr.isDefault) || data[0];
        fillAddressForm(defaultAddress);
      }
      setAddressMessage(t("address_deleted_success"));
    } catch (err) {
      console.error("Delete address failed:", err);
      setAddressError(t("address_delete_failed"));
    }
  };

  if (!auth.isAuthReady || !auth.user) {
    return null;
  }

  return (
    <div>
      <Header title={`Profile - Twenty`} />
      <main id="main-content" className="app-max-width app-x-padding py-8 md:py-10">
        <h1 className="text-3xl mb-8">{t("profile")}</h1>

        <div className="grid grid-cols-1 xl:grid-cols-2 gap-6 items-start">
          <section className="border border-gray200 p-5 md:p-7">
            <h2 className="text-xl mb-5">{t("profile")}</h2>
            <form onSubmit={handleSubmit}>
          <div className="mb-4">
            <label htmlFor="username" className="text-sm">
              {t("username")}
            </label>
            <Input
              name="username"
              type="text"
              extraClass="w-full mt-1"
              border="border-2 border-gray300"
              value={username}
              onChange={(e) => setUsername((e.target as HTMLInputElement).value)}
            />
          </div>

          <div className="mb-4">
            <label htmlFor="fullName" className="text-sm">
              {t("name")}
            </label>
            <Input
              name="fullName"
              type="text"
              extraClass="w-full mt-1"
              border="border-2 border-gray300"
              value={fullName}
              onChange={(e) => setFullName((e.target as HTMLInputElement).value)}
            />
          </div>

          <div className="mb-4">
            <label htmlFor="email" className="text-sm">
              {t("email_address")}
            </label>
            <Input
              name="email"
              type="email"
              extraClass="w-full mt-1"
              border="border-2 border-gray300"
              value={email}
              onChange={(e) => setEmail((e.target as HTMLInputElement).value)}
            />
          </div>

          <div className="mb-4">
            <label htmlFor="phone" className="text-sm">
              {t("phone")}
            </label>
            <Input
              name="phone"
              type="text"
              extraClass="w-full mt-1"
              border="border-2 border-gray300"
              value={phone}
              onChange={(e) => setPhone((e.target as HTMLInputElement).value)}
            />
          </div>

          {message && <p className="text-green-700 mb-4">{message}</p>}
          {error && <p className="text-red mb-4">{error}</p>}

          <div className="flex flex-wrap gap-3 pt-2">
              <Button
                type="submit"
                value={isSaving ? indexT("loading") : t("save_changes")}
                extraClass="text-center"
              />
            <button
              type="button"
              onClick={handleLogout}
              className="text-xl sm:text-base py-2 sm:py-1 px-5 border border-gray400 bg-white text-gray500 hover:bg-gray100"
            >
              {t("logout")}
            </button>
          </div>
            </form>
          </section>

          <section className="border border-gray200 p-5 md:p-7">
            <h2 className="text-2xl mb-4">{t("default_address")}</h2>
            <div className="mb-5">
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-sm font-medium">{t("address_list")}</h3>
                <button
                  type="button"
                  onClick={resetAddressForm}
                  className="text-xs py-1 px-3 border border-gray300 bg-white hover:bg-gray100"
                >
                  {t("add_new_address")}
                </button>
              </div>
              <div className="space-y-2">
                {addresses.length === 0 && (
                  <p className="text-sm text-gray400">{t("no_address")}</p>
                )}
                {addresses.map((item) => (
                  <div
                    key={item.id}
                    className={`border p-3 ${
                      addressId === item.id ? "border-gray500" : "border-gray200"
                    }`}
                  >
                    <div className="flex flex-col gap-2 md:flex-row md:items-start md:justify-between">
                      <div className="text-sm">
                        <p className="font-medium">
                          {item.recipientName} - {item.phone}
                        </p>
                        <p className="text-gray500">
                          {item.addressLine}, {item.ward}, {item.district}, {item.province}
                        </p>
                        {item.isDefault && (
                          <span className="inline-block mt-1 text-xs border border-gray400 px-2 py-0.5">
                            {t("default")}
                          </span>
                        )}
                      </div>
                      <div className="flex flex-wrap gap-2">
                        <button
                          type="button"
                          onClick={() => fillAddressForm(item)}
                          className="text-xs py-1 px-2 border border-gray300 hover:bg-gray100"
                        >
                          {t("edit")}
                        </button>
                        {!item.isDefault && (
                          <button
                            type="button"
                            onClick={() => handleSetDefaultAddress(item)}
                            className="text-xs py-1 px-2 border border-gray300 hover:bg-gray100"
                          >
                            {t("set_default")}
                          </button>
                        )}
                        <button
                          type="button"
                          onClick={() => handleDeleteAddress(item.id)}
                          className="text-xs py-1 px-2 border border-gray300 hover:bg-gray100"
                        >
                          {t("delete")}
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
            <form onSubmit={handleSaveAddress}>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
              <label htmlFor="recipientName" className="text-sm">
                {t("recipient")}
              </label>
              <Input
                name="recipientName"
                type="text"
                extraClass="w-full mt-1 h-11"
                border="border-2 border-gray300"
                value={recipientName}
                onChange={(e) =>
                  setRecipientName((e.target as HTMLInputElement).value)
                }
                required
              />
              </div>

              <div>
              <label htmlFor="addressPhone" className="text-sm">
                {t("recipient_phone")}
              </label>
              <Input
                name="addressPhone"
                type="text"
                extraClass="w-full mt-1 h-11"
                border="border-2 border-gray300"
                value={addressPhone}
                onChange={(e) =>
                  setAddressPhone((e.target as HTMLInputElement).value)
                }
                required
              />
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-4">
              <div>
              <label htmlFor="province" className="text-sm">
                {t("province_city")}
              </label>
              <select
                id="province"
                name="province"
                className="w-full mt-1 border-2 border-gray300 h-11 px-3 bg-white"
                value={selectedProvinceId}
                onChange={(e) => handleProvinceChange(e.target.value)}
                disabled={isAddressOptionsLoading}
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
              <label htmlFor="district" className="text-sm">
                {t("district")}
              </label>
              <select
                id="district"
                name="district"
                className="w-full mt-1 border-2 border-gray300 h-11 px-3 bg-white"
                value={selectedDistrictId}
                onChange={(e) => handleDistrictChange(e.target.value)}
                disabled={!selectedProvinceId || isAddressOptionsLoading}
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
              <label htmlFor="ward" className="text-sm">
                {t("ward")}
              </label>
              <select
                id="ward"
                name="ward"
                className="w-full mt-1 border-2 border-gray300 h-11 px-3 bg-white"
                value={selectedWardId}
                onChange={(e) => handleWardChange(e.target.value)}
                disabled={!selectedDistrictId || isAddressOptionsLoading}
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

            <div className="mt-4">
              <label htmlFor="addressLine" className="text-sm">
                {t("address_detail")}
              </label>
              <Input
                name="addressLine"
                type="text"
                extraClass="w-full mt-1 h-11"
                border="border-2 border-gray300"
                value={addressLine}
                onChange={(e) =>
                  setAddressLine((e.target as HTMLInputElement).value)
                }
                required
              />
            </div>

            <div className="mt-4">
              <label className="inline-flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  checked={setAsDefault}
                  onChange={(e) => setSetAsDefault(e.target.checked)}
                />
                {t("set_as_default_address")}
              </label>
            </div>

            {addressMessage && <p className="text-green-700 mb-4">{addressMessage}</p>}
            {addressError && <p className="text-red mb-4">{addressError}</p>}

            <div className="pt-2">
              <Button
                type="submit"
                value={isSavingAddress ? indexT("loading") : t("save_address")}
                extraClass="text-center"
              />
            </div>
            </form>
          </section>
        </div>
      </main>
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

export default ProfilePage;
