import React, { createContext, useContext, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/router";

export type CurrencyCode = "USD" | "MYN" | "VND";

type CurrencyContextType = {
  currency: CurrencyCode;
  setCurrency: (currency: CurrencyCode) => void;
  formatPrice: (baseAmount: number | string) => string;
};

const CurrencyContext = createContext<CurrencyContextType>({
  currency: "VND",
  setCurrency: () => undefined,
  formatPrice: (baseAmount: number | string) =>
    `${Math.round(Number(baseAmount) || 0).toLocaleString("en-US")} VND`,
});

// Prices from backend are in VND.
const VND_TO_CURRENCY: Record<CurrencyCode, number> = {
  VND: 1,
  USD: 1 / 25500,
  MYN: 2100 / 25500,
};

export const ProvideCurrency = ({ children }: { children: React.ReactNode }) => {
  const router = useRouter();
  const [currency, setCurrencyState] = useState<CurrencyCode>("VND");

  useEffect(() => {
    const saved = typeof window !== "undefined" ? localStorage.getItem("currency_v2") : null;
    if (saved === "MMK") {
      setCurrencyState("MYN");
      return;
    }
    if (saved === "USD" || saved === "MYN" || saved === "VND") {
      setCurrencyState(saved as CurrencyCode);
    }
  }, []);

  const setCurrency = (nextCurrency: CurrencyCode) => {
    setCurrencyState(nextCurrency);
    if (typeof window !== "undefined") {
      localStorage.setItem("currency_v2", nextCurrency);
    }
  };

  useEffect(() => {
    const locale = router.locale || "vi";
    const localeCurrency: CurrencyCode =
      locale === "en" ? "USD" : locale === "my" ? "MYN" : "VND";
    setCurrencyState(localeCurrency);
    if (typeof window !== "undefined") {
      localStorage.setItem("currency_v2", localeCurrency);
    }
  }, [router.locale]);

  const formatPrice = (baseAmount: number | string) => {
    const numericAmount =
      typeof baseAmount === "string" ? Number(baseAmount) : baseAmount;
    const safeAmount = Number.isFinite(numericAmount) ? numericAmount : 0;
    const converted = safeAmount * VND_TO_CURRENCY[currency];
    if (currency === "USD") return `$ ${converted.toFixed(2)}`;
    if (currency === "MYN")
      return `${Math.round(converted).toLocaleString("en-US")} MYN`;
    return `${Math.round(converted).toLocaleString("en-US")} VND`;
  };

  const value = useMemo(
    () => ({
      currency,
      setCurrency,
      formatPrice,
    }),
    [currency]
  );

  return <CurrencyContext.Provider value={value}>{children}</CurrencyContext.Provider>;
};

export const useCurrency = () => useContext(CurrencyContext);
