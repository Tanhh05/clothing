import { Fragment, useState } from "react";
import { Menu as HMenu } from "@headlessui/react";
import Link from "next/link";
import Image from "next/image";
import { Dialog, Transition } from "@headlessui/react";
import { useTranslations } from "next-intl";
import { useRouter } from "next/router";

import MenuIcon from "../../public/icons/MenuIcon";
import AuthForm from "../Auth/AuthForm";
import WhistlistIcon from "../../public/icons/WhistlistIcon";
import UserIcon from "../../public/icons/UserIcon";
import SearchIcon from "../../public/icons/SearchIcon";
import DownArrow from "../../public/icons/DownArrow";
import InstagramLogo from "../../public/icons/InstagramLogo";
import FacebookLogo from "../../public/icons/FacebookLogo";
import { useWishlist } from "../../context/wishlist/WishlistProvider";
import { useAuth } from "../../context/AuthContext";
import { useCurrency } from "../../context/CurrencyContext";
import { pushWithLang } from "../../lib/router-utils";

type MenuCategory = {
  id: number;
  name: string;
  slug?: string;
  parentId?: number | null;
};

type Props = {
  categories: MenuCategory[];
};

const buildLocaleHref = (asPath: string, nextLocale: "en" | "my" | "vi") => {
  const pathWithoutLocale = asPath.replace(/^\/(en|my|vi)(?=\/|$)/, "");
  const normalizedPath = pathWithoutLocale.startsWith("/")
    ? pathWithoutLocale
    : `/${pathWithoutLocale}`;
  return `/${nextLocale}${normalizedPath}`;
};

const toCategoryPath = (category: MenuCategory) =>
  category.slug ||
  category.name
    .trim()
    .toLowerCase()
    .replace(/\s+/g, "-");

export default function Menu({ categories }: Props) {
  const t = useTranslations("Navigation");
  const router = useRouter();
  const { locale } = router;
  const { wishlist } = useWishlist();
  const { currency, setCurrency } = useCurrency();
  const auth = useAuth();
  const [open, setOpen] = useState(false);
  const [searchValue, setSearchValue] = useState("");
  const [expandedParentId, setExpandedParentId] = useState<number | null>(null);

  // Calculate Number of Wishlist
  let noOfWishlist = wishlist.length;

  function closeModal() {
    setOpen(false);
    setExpandedParentId(null);
  }

  function openModal() {
    setOpen(true);
  }

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setOpen(false);
    pushWithLang(router, `/search?q=${searchValue}`);
  };

  const handleChange = (e: React.FormEvent<HTMLInputElement>) => {
    setSearchValue((e.target as HTMLInputElement).value);
  };

  const parentCategories = categories.filter((category) => !category.parentId);
  const childrenByParent = categories.reduce(
    (acc: Record<number, MenuCategory[]>, category) => {
      if (!category.parentId) return acc;
      if (!acc[category.parentId]) acc[category.parentId] = [];
      acc[category.parentId].push(category);
      return acc;
    },
    {}
  );

  return (
    <>
      <div className="relative">
        <button
          type="button"
          aria-label="Hamburger Menu"
          onClick={openModal}
          className="focus:outline-none"
        >
          <MenuIcon />
        </button>
      </div>
      <Transition show={open} as={Fragment}>
        <Dialog
          as="div"
          className="fixed inset-0 z-10 overflow-y-auto"
          style={{ zIndex: 99999 }}
          static
          open={open}
          onClose={closeModal}
        >
          <div className="min-h-screen">
            <Transition.Child as={Fragment}>
              <Dialog.Overlay className="fixed inset-0 bg-gray500 opacity-50" />
            </Transition.Child>
            <Transition.Child
              as={Fragment}
              enter="ease-linear duration-600"
              enterFrom="opacity-0"
              enterTo="opacity-100"
              leave="ease-linear duration-300"
              leaveFrom="translate-x-0"
              leaveTo="-translate-x-full"
            >
              <div
                style={{ height: "100vh" }}
                className="relative opacity-95 overflow-y-auto inline-block dur h-screen w-full max-w-md overflow-hidden text-left align-middle transition-all transform bg-white shadow-xl"
              >
                <div className="flex justify-between items-center p-6 pb-0">
                  <Link href="/">
                    <a className="flex items-center gap-2">
                      <Image
                        className="justify-center"
                        src="/admin-logo.png"
                        alt="CLOTHING"
                        width={28}
                        height={28}
                      />
                      <span className="text-base font-semibold">CLOTHING</span>
                    </a>
                  </Link>
                  <button
                    type="button"
                    className="outline-none focus:outline-none text-3xl sm:text-2xl"
                    onClick={closeModal}
                  >
                    &#10005;
                  </button>
                </div>

                <div className="mb-10">
                  <div className="itemContainer px-6 w-full flex flex-col justify-around items-center">
                    <form
                      className="flex w-full justify-between items-center mt-5 mb-5 border-gray300 border-b-2"
                      onSubmit={handleSubmit}
                    >
                      <SearchIcon extraClass="text-gray300 w-6 h-6" />
                      <input
                        type="search"
                        placeholder={t("search_anything")}
                        className="px-4 py-2 w-full focus:outline-none text-xl"
                        onChange={handleChange}
                      />
                    </form>
                    {parentCategories.map((category) => (
                      <div key={category.id} className="w-full">
                        <div className="w-full flex items-center justify-between py-2">
                          <Link href={`/product-category/${toCategoryPath(category)}`}>
                            <a
                              className="flex-1 text-xl hover:bg-gray100 text-left block"
                              onClick={closeModal}
                            >
                              {category.name}
                            </a>
                          </Link>
                          {(childrenByParent[category.id] || []).length > 0 && (
                            <button
                              type="button"
                              aria-label={`toggle-${category.id}`}
                              className="ml-2 p-1 text-gray400 hover:text-black"
                              onClick={() =>
                                setExpandedParentId((prev) =>
                                  prev === category.id ? null : category.id
                                )
                              }
                            >
                              <DownArrow
                                extraClass={`w-4 h-4 transition-transform ${
                                  expandedParentId === category.id
                                    ? "rotate-0"
                                    : "rotate-180"
                                }`}
                              />
                            </button>
                          )}
                        </div>
                        {expandedParentId === category.id &&
                          (childrenByParent[category.id] || []).map((child) => (
                            <Link
                              key={child.id}
                              href={`/product-category/${toCategoryPath(child)}`}
                            >
                              <a
                                className="w-full text-lg text-gray400 hover:bg-gray100 text-left py-1 pl-5 block"
                                onClick={closeModal}
                              >
                                {child.name}
                              </a>
                            </Link>
                          ))}
                      </div>
                    ))}
                    <Link href="/blogs">
                      <a
                        className="w-full text-xl hover:bg-gray100 text-left py-2"
                        onClick={closeModal}
                      >
                        {t("blogs")}
                      </a>
                    </Link>
                    <Link href="/about">
                      <a
                        className="w-full text-xl hover:bg-gray100 text-left py-2"
                        onClick={closeModal}
                      >
                        {t("about_us")}
                      </a>
                    </Link>
                    <Link href="/contact">
                      <a
                        className="w-full text-xl hover:bg-gray100 text-left py-2"
                        onClick={closeModal}
                      >
                        {t("contact_us")}
                      </a>
                    </Link>
                    <hr className="border border-gray300 w-full mt-2" />
                    <div className="w-full text-xl py-2 my-3 flex justify-between">
                      <AuthForm extraClass="flex justify-between w-full">
                        <span>{auth.user ? t("profile") : t("login")}</span>
                        <UserIcon />
                      </AuthForm>
                    </div>
                    <hr className="border border-gray300 w-full" />
                    <Link href="/wishlist">
                      <a className="text-xl py-2 my-3 w-full flex justify-between">
                        <span>{t("wishlist")}</span>
                        <div className="relative">
                          <WhistlistIcon />
                          {noOfWishlist > 0 && (
                            <span
                              className={`absolute text-xs -top-0 -left-7 bg-gray500 text-gray100 py-1 px-2 rounded-full`}
                            >
                              {noOfWishlist}
                            </span>
                          )}
                        </div>
                      </a>
                    </Link>
                    <hr className="border border-gray300 w-full" />

                    {/* Locale Dropdown */}
                    <HMenu
                      as="div"
                      className="relative bg-gray100 mt-4 mb-2 w-full"
                    >
                      <HMenu.Button
                        as="a"
                        href="#"
                        className="flex justify-center items-center py-2 px-4 text-center"
                      >
                        {locale === "en"
                          ? t("english")
                          : locale === "my"
                          ? t("myanmar")
                          : t("vietnamese")}{" "}
                        <DownArrow />
                      </HMenu.Button>
                      <HMenu.Items
                        className="flex flex-col w-full right-0 absolute p-1 border border-gray200 bg-white mt-2 outline-none"
                        style={{ zIndex: 9999 }}
                      >
                        <HMenu.Item>
                          <button
                              type="button"
                            onClick={() => window.location.assign(buildLocaleHref(router.asPath, "en"))}
                            className={`${
                              locale === "en"
                                ? "bg-gray200 text-gray500"
                                : "bg-white text-gray500"
                            } py-2 px-4 text-center focus:outline-none w-full`}
                          >
                            {t("english")}
                          </button>
                        </HMenu.Item>
                        <HMenu.Item>
                          <button
                            type="button"
                            onClick={() => window.location.assign(buildLocaleHref(router.asPath, "my"))}
                            className={`${
                              locale === "my"
                                ? "bg-gray200 text-gray500"
                                : "bg-white text-gray500"
                            } py-2 px-4 text-center focus:outline-none w-full`}
                          >
                            {t("myanmar")}
                          </button>
                        </HMenu.Item>
                        <HMenu.Item>
                          <button
                            type="button"
                            onClick={() => window.location.assign(buildLocaleHref(router.asPath, "vi"))}
                            className={`${
                              locale === "vi"
                                ? "bg-gray200 text-gray500"
                                : "bg-white text-gray500"
                            } py-2 px-4 text-center focus:outline-none w-full`}
                          >
                            {t("vietnamese")}
                          </button>
                        </HMenu.Item>
                      </HMenu.Items>
                    </HMenu>

                    {/* Currency Dropdown */}
                    <HMenu as="div" className="relative bg-gray100 my-2 w-full">
                      <HMenu.Button
                        as="a"
                        href="#"
                        className="flex justify-center items-center py-2 px-4 text-center"
                      >
                        {currency} <DownArrow />
                      </HMenu.Button>
                      <HMenu.Items
                        className="flex flex-col w-full right-0 absolute p-1 border border-gray200 bg-white mt-2 outline-none"
                        style={{ zIndex: 9999 }}
                      >
                        <HMenu.Item>
                          {({ active }) => (
                            <button
                              type="button"
                              onClick={() => setCurrency("USD")}
                              className={`${
                                active || currency === "USD"
                                  ? "bg-gray100 text-gray500"
                                  : "bg-white text-gray500"
                              } py-2 px-4 text-center focus:outline-none`}
                            >
                              {t("usd")}
                            </button>
                          )}
                        </HMenu.Item>
                        <HMenu.Item>
                          {({ active }) => (
                            <button
                              type="button"
                              onClick={() => setCurrency("MYN")}
                              className={`${
                                active || currency === "MYN"
                                  ? "bg-gray100 text-gray500"
                                  : "bg-white text-gray500"
                              } py-2 px-4 text-center focus:outline-none`}
                            >
                              MYN
                            </button>
                          )}
                        </HMenu.Item>
                        <HMenu.Item>
                          {({ active }) => (
                            <button
                              type="button"
                              onClick={() => setCurrency("VND")}
                              className={`${
                                active || currency === "VND"
                                  ? "bg-gray100 text-gray500"
                                  : "bg-white text-gray500"
                              } py-2 px-4 text-center focus:outline-none`}
                            >
                              VND
                            </button>
                          )}
                        </HMenu.Item>
                      </HMenu.Items>
                    </HMenu>

                    <div className="flex my-10 w-2/5 space-x-6 justify-center">
                      <a
                        href="#"
                        className="text-gray400 w-10 h-10 py-1 px-auto flex justify-center rounded-md active:bg-gray300"
                        aria-label="CLOTHING Facebook Page"
                      >
                        <FacebookLogo extraClass="h-8" />
                      </a>
                      <a
                        href="#"
                        className="text-gray400 w-10 h-10 py-1 px-auto flex justify-center rounded-md active:bg-gray300"
                        aria-label="CLOTHING Facebook Page"
                      >
                        <InstagramLogo extraClass="h-8" />
                      </a>
                    </div>
                  </div>
                </div>
              </div>
            </Transition.Child>
          </div>
        </Dialog>
      </Transition>
    </>
  );
}
