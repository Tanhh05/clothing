import React, { createContext, useContext, useMemo } from "react";

type Dict = Record<string, any>;
type TranslateValues = Record<string, string | number>;
type Translator = (key: string, values?: TranslateValues) => string;

type I18nContextType = {
  messages: Dict;
};

const I18nContext = createContext<I18nContextType>({ messages: {} });

const getByPath = (obj: Dict, path: string): any => {
  if (!obj || !path) return undefined;
  return path.split(".").reduce((acc: any, part: string) => {
    if (acc == null) return undefined;
    return acc[part];
  }, obj);
};

const interpolate = (template: string, values?: TranslateValues): string => {
  if (!values) return template;
  let result = template;
  Object.entries(values).forEach(([k, v]) => {
    result = result.replace(new RegExp(`\\{${k}\\}`, "g"), String(v));
  });
  return result;
};

export const NextIntlProvider = ({
  children,
  messages,
}: {
  children: React.ReactNode;
  messages?: Dict;
}) => {
  const value = useMemo(() => ({ messages: messages || {} }), [messages]);

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
};

export const useTranslations = (namespace?: string): Translator => {
  const { messages } = useContext(I18nContext);

  return (key: string, values?: TranslateValues) => {
    const scopedKey = namespace ? `${namespace}.${key}` : key;
    const resolved = getByPath(messages, scopedKey);
    if (typeof resolved === "string") {
      return interpolate(resolved, values);
    }
    return key;
  };
};
