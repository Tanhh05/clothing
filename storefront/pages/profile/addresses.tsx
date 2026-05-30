import { useCallback, useEffect, useState } from "react";
import { GetStaticProps } from "next";
import { useRouter } from "next/router";
import { useTranslations } from "next-intl";
import axios from "axios";

import Header from "../../components/Header/Header";
import Footer from "../../components/Footer/Footer";
import AccountPageLayout from "../../components/Account/AccountPageLayout";
import Input from "../../components/Input/Input";
import Button from "../../components/Buttons/Button";
import { useAuth } from "../../context/AuthContext";
import { pushWithLang } from "../../lib/router-utils";

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

const ProfileAddressesPage = () => {
  const t = useTranslations("LoginRegister");
  const indexT = useTranslations("Index");
  const accountT = useTranslations("Account");
  const auth = useAuth();
  const router = useRouter();

  const [isSavingAddress, setIsSavingAddress] = useState(false);
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
  const [showAddressForm, setShowAddressForm] = useState(false);

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
    setShowAddressForm(true);
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
    setShowAddressForm(true);
  };

  useEffect(() => {
    if (!auth.isAuthReady) return;
    if (!auth.user) {
      pushWithLang(router, "/");
    }
  }, [auth.isAuthReady, auth.user, router]);

  useEffect(() => {
    let active = true;
    const loadProvinces = async () => {
      try {
        const res = await axios.get(
          `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/address/provinces`
        );
        if (!active) return;
        const provinceData = unwrapApiData<AddressUnit[]>(res.data);
        setProvinces(Array.isArray(provinceData) ? provinceData : []);
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
        const addressData = unwrapApiData<UserAddress[]>(res.data);
        const loadedAddresses: UserAddress[] = Array.isArray(addressData)
          ? addressData
          : [];
        setAddresses(loadedAddresses);
        if (loadedAddresses.length === 0) {
          resetAddressForm();
          return;
        }
        const defaultAddress =
          loadedAddresses.find((address) => address.isDefault) ||
          loadedAddresses[0];
        setAddressId(defaultAddress.id);
        setRecipientName(defaultAddress.recipientName || "");
        setAddressPhone(defaultAddress.phone || "");
        setProvince(defaultAddress.province || "");
        setDistrict(defaultAddress.district || "");
        setWard(defaultAddress.ward || "");
        setAddressLine(defaultAddress.addressLine || "");
        setSetAsDefault(Boolean(defaultAddress.isDefault));
        setShowAddressForm(false);
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
        const districtData = unwrapApiData<AddressUnit[]>(res.data);
        const data: AddressUnit[] = Array.isArray(districtData) ? districtData : [];
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
        const wardData = unwrapApiData<AddressUnit[]>(res.data);
        const data: AddressUnit[] = Array.isArray(wardData) ? wardData : [];
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
        setAddressId(selected.id);
        setRecipientName(selected.recipientName || "");
        setAddressPhone(selected.phone || "");
        setProvince(selected.province || "");
        setDistrict(selected.district || "");
        setWard(selected.ward || "");
        setAddressLine(selected.addressLine || "");
        setSetAsDefault(Boolean(selected.isDefault));
      }
      setAddressMessage(t("address_updated_success"));
      setShowAddressForm(false);
    } catch (err) {
      console.error("Save address failed:", err);
      setAddressError(t("address_update_failed"));
    } finally {
      setIsSavingAddress(false);
    }
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
      <Header title={`${accountT("shipping_addresses")} - TWENTY`} />
      <main id="main-content" className="app-max-width app-x-padding py-8 md:py-10">
        <AccountPageLayout section={accountT("shipping_addresses")}>
            <div className="mb-6">
              <div className="flex items-center justify-between mb-3">
                <h2 className="text-2xl font-semibold">{t("default_address")}</h2>
                <button
                  type="button"
                  onClick={resetAddressForm}
                  className="px-5 py-2 border border-gray300 bg-white hover:bg-gray100"
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
                    className={`py-4 px-3 ${
                      addressId === item.id ? "bg-gray100" : ""
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
                          <span className="inline-block mt-1 text-xs bg-gray200 px-2 py-0.5">
                            {t("default")}
                          </span>
                        )}
                      </div>
                      <div className="flex flex-wrap gap-2">
                        <button
                          type="button"
                          onClick={() => fillAddressForm(item)}
                          className="text-sm text-gray400 hover:text-black"
                        >
                          {t("edit")}
                        </button>
                        {!item.isDefault && (
                          <button
                            type="button"
                            onClick={() => handleSetDefaultAddress(item)}
                            className="text-sm text-gray400 hover:text-black"
                          >
                            {t("set_default")}
                          </button>
                        )}
                        <button
                          type="button"
                          onClick={() => handleDeleteAddress(item.id)}
                          className="text-sm text-gray400 hover:text-black"
                        >
                          {t("delete")}
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {showAddressForm && (
            <form onSubmit={handleSaveAddress} className="mt-8 border-t border-gray200 pt-6">
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
                  onChange={(e) => setAddressLine((e.target as HTMLInputElement).value)}
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
                <button
                  type="button"
                  onClick={() => setShowAddressForm(false)}
                  className="ml-3 px-5 py-2 border border-gray300 bg-white hover:bg-gray100"
                >
                  {accountT("hide_form")}
                </button>
              </div>
            </form>
            )}
        </AccountPageLayout>
      </main>
      <Footer />
    </div>
  );
};

export const getStaticProps: GetStaticProps = async ({ locale }) => {
  return {
    props: {
      messages: (await import(`../../messages/common/${locale}.json`)).default,
    },
  };
};

export default ProfileAddressesPage;
