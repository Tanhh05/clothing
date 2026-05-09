import type { NextRouter } from "next/router";

const DEFAULT_LANG = "vi";
const SUPPORTED_LANGS = new Set(["vi", "en", "my"]);

export const resolveLang = (locale?: string) => {
  const normalized = (locale || DEFAULT_LANG).toLowerCase();
  return SUPPORTED_LANGS.has(normalized) ? normalized : DEFAULT_LANG;
};

export const withLang = (href: string, locale?: string) => {
  if (!href || href.startsWith("http://") || href.startsWith("https://")) {
    return href;
  }
  const [path, hashPart] = href.split("#");
  const [pathname, queryString] = path.split("?");
  const params = new URLSearchParams(queryString || "");
  params.set("lang", resolveLang(locale));
  const query = params.toString();
  const hash = hashPart ? `#${hashPart}` : "";
  return query ? `${pathname}?${query}${hash}` : `${pathname}${hash}`;
};

export const pushWithLang = (router: NextRouter, href: string) => {
  return router.push(withLang(href, router.locale));
};
