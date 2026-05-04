import { Menu } from "@headlessui/react";
import { useTranslations } from "next-intl";
import { useRouter } from "next/router";
import Link from "next/link";

import InstagramLogo from "../../public/icons/InstagramLogo";
import FacebookLogo from "../../public/icons/FacebookLogo";
import DownArrow from "../../public/icons/DownArrow";
import styles from "./Header.module.css";
import { useCurrency } from "../../context/CurrencyContext";

type LinkProps = {
  href: string;
  locale: "en" | "my" | "vi";
  active: boolean;
};

const MyLink: React.FC<LinkProps> = ({
  href,
  locale,
  children,
  active,
  ...rest
}) => {
  return (
    <Link href={href} locale={locale}>
      <a
        className={`py-2 px-4 text-center ${
          active ? "bg-gray200 text-gray500" : "bg-white text-gray500"
        }`}
        {...rest}
      >
        {children}
      </a>
    </Link>
  );
};

const TopNav = () => {
  const router = useRouter();
  const { asPath, locale } = router;
  const t = useTranslations("Navigation");
  const { currency, setCurrency } = useCurrency();

  return (
    <div className="bg-gray500 text-gray100 hidden lg:block">
      <div className="flex justify-between app-max-width">
        <ul className={`flex ${styles.topLeftMenu}`}>
          <li>
            <a href="#" aria-label="Twenty Facebook Page">
              <FacebookLogo />
            </a>
          </li>
          <li>
            <a href="#" aria-label="Twenty Instagram Account">
              <InstagramLogo />
            </a>
          </li>
          <li>
            <a href="#">{t("about_us")}</a>
          </li>
          <li>
            <a href="#">{t("our_policy")}</a>
          </li>
        </ul>
        <ul className={`flex ${styles.topRightMenu}`}>
          <li>
            <Menu as="div" className="relative">
              <Menu.Button as="a" href="#" className="flex">
                {locale === "en"
                  ? t("eng")
                  : locale === "my"
                  ? t("myn")
                  : t("vie")}{" "}
                <DownArrow />
              </Menu.Button>
                <Menu.Items
                  className="flex flex-col w-20 right-0 absolute p-1 border border-gray200 bg-white mt-2 outline-none"
                  style={{ zIndex: 9999 }}
                >
                <Menu.Item>
                  {({ active }) => (
                    <MyLink active={active} href={asPath} locale="en">
                      {t("eng")}
                    </MyLink>
                  )}
                </Menu.Item>
                <Menu.Item>
                  {({ active }) => (
                    <MyLink active={active} href={asPath} locale="my">
                      {t("myn")}
                    </MyLink>
                  )}
                </Menu.Item>
                <Menu.Item>
                  {({ active }) => (
                    <MyLink active={active} href={asPath} locale="vi">
                      {t("vie")}
                    </MyLink>
                  )}
                </Menu.Item>
              </Menu.Items>
            </Menu>
          </li>
          <li>
            <Menu as="div" className="relative">
              <Menu.Button as="a" href="#" className="flex">
                {currency} <DownArrow />
              </Menu.Button>
              <Menu.Items
                className="flex flex-col w-20 right-0 absolute p-1 border border-gray200 bg-white mt-2 outline-none"
                style={{ zIndex: 9999 }}
              >
                <Menu.Item>
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
                </Menu.Item>
                <Menu.Item>
                  {({ active }) => (
                    <button
                      type="button"
                      onClick={() => setCurrency("MMK")}
                      className={`${
                        active || currency === "MMK"
                          ? "bg-gray100 text-gray500"
                          : "bg-white text-gray500"
                      } py-2 px-4 text-center focus:outline-none`}
                    >
                      {t("mmk")}
                    </button>
                  )}
                </Menu.Item>
                <Menu.Item>
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
                </Menu.Item>
              </Menu.Items>
            </Menu>
          </li>
        </ul>
      </div>
    </div>
  );
};

export default TopNav;
