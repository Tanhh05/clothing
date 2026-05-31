import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import Image from "next/image";
import { useRouter } from "next/router";
import { useTranslations } from "next-intl";
import axios from "axios";

import TopNav from "./TopNav";
import WhistlistIcon from "../../public/icons/WhistlistIcon";
import UserIcon from "../../public/icons/UserIcon";
import AuthForm from "../Auth/AuthForm";
import SearchForm from "../SearchForm/SearchForm";
import CartItem from "../CartItem/CartItem";
import Menu from "../Menu/Menu";
import AppHeader from "./AppHeader";
import { useWishlist } from "../../context/wishlist/WishlistProvider";

import styles from "./Header.module.css";

type Props = {
  title?: string;
};

type MenuCategory = {
  id: number;
  name: string;
  slug?: string;
  parentId?: number | null;
  showInMenu?: boolean;
  status?: string;
};

const toCategoryPath = (category: MenuCategory) =>
  category.slug ||
  category.name
    .trim()
    .toLowerCase()
    .replace(/\s+/g, "-");

const API_BASE_URL =
  process.env.NEXT_PUBLIC_BACKEND_URL ||
  process.env.NEXT_PUBLIC_PROD_BACKEND_URL ||
  "";

const Header: React.FC<Props> = ({ title }) => {
  const router = useRouter();
  const t = useTranslations("Navigation");
  const { wishlist } = useWishlist();
  const [animate, setAnimate] = useState("");
  const [scrolled, setScrolled] = useState<boolean>(false);
  const [didMount, setDidMount] = useState<boolean>(false); // to disable Can't perform a React state Warning
  const [menuCategories, setMenuCategories] = useState<MenuCategory[]>([]);
  const [activeParentId, setActiveParentId] = useState<number | null>(null);

  // Calculate Number of Wishlist
  let noOfWishlist = wishlist.length;

  // Animate Wishlist Number
  const handleAnimate = useCallback(() => {
    if (noOfWishlist === 0) return;
    setAnimate("animate__animated animate__headShake");
  }, [noOfWishlist, setAnimate]);

  // Set animate when no of wishlist changes
  useEffect(() => {
    handleAnimate();
    setTimeout(() => {
      setAnimate("");
    }, 1000);
  }, [handleAnimate]);

  const handleScroll = useCallback(() => {
    const offset = window.scrollY;
    if (offset > 30) {
      setScrolled(true);
    } else {
      setScrolled(false);
    }
  }, [setScrolled]);

  useEffect(() => {
    setDidMount(true);
    window.addEventListener("scroll", handleScroll);
    return () => setDidMount(false);
  }, [handleScroll]);

  useEffect(() => {
    const fetchMenuCategories = async () => {
      if (!API_BASE_URL) {
        console.error(
          "Missing NEXT_PUBLIC_BACKEND_URL (or NEXT_PUBLIC_PROD_BACKEND_URL)."
        );
        return;
      }
      try {
        const res = await axios.get(
          `${API_BASE_URL}/api/categories?page=0&size=50&sortBy=displayOrder&direction=asc`,
          {
            headers: {
              "Accept-Language": router.locale || "vi",
            },
          }
        );
        const payload =
          res.data &&
          typeof res.data === "object" &&
          Object.prototype.hasOwnProperty.call(res.data, "data")
            ? res.data.data
            : res.data;
        const categories: MenuCategory[] = Array.isArray(payload?.content)
          ? payload.content
          : [];
        const visibleCategories = categories.filter(
          (category) =>
            category?.showInMenu !== false &&
            (category?.status ? category.status.toUpperCase() === "ACTIVE" : true)
        );
        setMenuCategories(visibleCategories);
      } catch (error) {
        console.error("Failed to load menu categories:", error);
      }
    };

    fetchMenuCategories();
  }, [router.locale]);

  if (!didMount) {
    return null;
  }

  const parentCategories = menuCategories.filter((category) => !category.parentId);
  const childrenByParent = menuCategories.reduce(
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
      {/* ===== <head> section ===== */}
      <AppHeader title={title} />

      {/* ===== Skip to main content button ===== */}
      <a
        href="#main-content"
        className="whitespace-nowrap absolute z-50 left-4 opacity-90 rounded-md bg-white px-4 py-3 transform -translate-y-40 focus:translate-y-0 transition-all duration-300"
      >
        {t("skip_to_main_content")}
      </a>

      {/* ===== Top Navigation ===== */}
      <TopNav />

      {/* ===== Main Navigation ===== */}
      <nav
        className={`${
          scrolled ? "bg-white sticky top-0 shadow-md z-50" : "bg-transparent"
        } w-full z-50 h-20 relative`}
      >
        <div className="app-max-width w-full">
          <div
            className={`flex items-center justify-between app-x-padding ${styles.mainMenu}`}
          >
            {/* Hamburger Menu and Mobile Nav */}
            <div className="flex-1 lg:flex-0 lg:hidden">
              <Menu categories={menuCategories} />
            </div>

            {/* Left Nav */}
            <ul className={`hidden lg:flex lg:flex-1 items-center ${styles.leftMenu}`}>
              {parentCategories.map((category) => {
                const children = childrenByParent[category.id] || [];
                return (
                  <li
                    key={category.id}
                    className="relative"
                    onMouseEnter={() => setActiveParentId(category.id)}
                    onMouseLeave={() => setActiveParentId(null)}
                  >
                    <Link href={`/product-category/${toCategoryPath(category)}`}>
                      <a>{category.name}</a>
                    </Link>
                    {children.length > 0 && activeParentId === category.id && (
                      <div className="absolute left-0 top-full pt-3 z-50">
                        <ul className="bg-white min-w-44 shadow-md border border-gray100 py-2">
                          {children.map((child) => (
                            <li key={child.id} className="px-4 py-2 mr-0 block">
                              <Link
                                href={`/product-category/${toCategoryPath(child)}`}
                              >
                                <a className="whitespace-nowrap hover:text-gray400">
                                  {child.name}
                                </a>
                              </Link>
                            </li>
                          ))}
                        </ul>
                      </div>
                    )}
                  </li>
                );
              })}
            </ul>

            {/* TWENTY Logo */}
            <div className="flex-1 lg:flex-none flex justify-center items-center cursor-pointer">
              <div className="h-auto">
                <Link href="/">
                  <a className="flex items-center gap-2">
                    <Image
                      className="justify-center"
                      src="/admin-logo.png"
                      alt="TWENTY"
                      width={32}
                      height={32}
                    />
                    <span className="text-base sm:text-lg font-semibold whitespace-nowrap">
                      TWENTY
                    </span>
                  </a>
                </Link>
              </div>
            </div>

            {/* Right Nav */}
            <ul className={`flex flex-1 items-center justify-end ${styles.rightMenu}`}>
              <li>
                <SearchForm />
              </li>
              <li>
                <AuthForm>
                  <UserIcon />
                </AuthForm>
              </li>
              <li>
                <Link href="/wishlist" passHref>
                  {/* <a className="relative" aria-label="Wishlist"> */}
                  <button
                    type="button"
                    className="relative"
                    aria-label={t("wishlist")}
                  >
                    <WhistlistIcon />
                    {noOfWishlist > 0 && (
                      <span
                        className={`${animate} absolute text-xs -top-3 bg-gray500 text-gray100 py-1 px-2 rounded-full`}
                      >
                        {noOfWishlist}
                      </span>
                    )}
                  </button>
                  {/* </a> */}
                </Link>
              </li>
              <li>
                <CartItem />
              </li>
            </ul>
          </div>
        </div>
      </nav>
    </>
  );
};

export default Header;
