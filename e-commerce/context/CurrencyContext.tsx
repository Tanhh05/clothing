import React, { createContext, useContext, useEffect, useMemo, useState } from "react";

export type CurrencyCode = "USD" | "MMK" | "VND";

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
  MMK: 2100 / 25500,
};

export const ProvideCurrency = ({ children }: { children: React.ReactNode }) => {
  const [currency, setCurrencyState] = useState<CurrencyCode>("VND");

  useEffect(() => {
    const saved = typeof window !== "undefined" ? localStorage.getItem("currency_v2") : null;
    if (saved === "USD" || saved === "MMK" || saved === "VND") {
      setCurrencyState(saved);
    }
  }, []);

  const setCurrency = (nextCurrency: CurrencyCode) => {
    setCurrencyState(nextCurrency);
    if (typeof window !== "undefined") {
      localStorage.setItem("currency_v2", nextCurrency);
    }
  };

  const formatPrice = (baseAmount: number | string) => {
    const numericAmount =
      typeof baseAmount === "string" ? Number(baseAmount) : baseAmount;
    const safeAmount = Number.isFinite(numericAmount) ? numericAmount : 0;
    const converted = safeAmount * VND_TO_CURRENCY[currency];
    if (currency === "USD") return `$ ${converted.toFixed(2)}`;
    if (currency === "MMK")
      return `${Math.round(converted).toLocaleString("en-US")} MMK`;
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
