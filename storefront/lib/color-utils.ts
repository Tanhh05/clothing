const COLOR_LABEL_MAP_VI: Record<string, string> = {
  black: "Đen",
  white: "Trắng",
  gray: "Xám",
  grey: "Xám",
  red: "Đỏ",
  blue: "Xanh dương",
  green: "Xanh lá",
  yellow: "Vàng",
  pink: "Hồng",
  purple: "Tím",
  brown: "Nâu",
  orange: "Cam",
  beige: "Be",
  navy: "Xanh navy",
  cream: "Kem",
  silver: "Bạc",
  gold: "Vàng ánh kim",
};

const COLOR_LABEL_MAP_EN: Record<string, string> = {
  black: "Black",
  white: "White",
  gray: "Gray",
  grey: "Grey",
  red: "Red",
  blue: "Blue",
  green: "Green",
  yellow: "Yellow",
  pink: "Pink",
  purple: "Purple",
  brown: "Brown",
  orange: "Orange",
  beige: "Beige",
  navy: "Navy",
  cream: "Cream",
  silver: "Silver",
  gold: "Gold",
};

const normalizeColorKey = (value: string): string =>
  value
    .trim()
    .toLowerCase()
    .replace(/[_-]/g, " ")
    .replace(/\s+/g, " ");

export const toLocalizedColorLabel = (value: string, locale?: string): string => {
  const normalized = normalizeColorKey(value);
  if (locale === "en") return COLOR_LABEL_MAP_EN[normalized] || value;
  if (locale === "vi" || !locale) return COLOR_LABEL_MAP_VI[normalized] || value;
  return value;
};
