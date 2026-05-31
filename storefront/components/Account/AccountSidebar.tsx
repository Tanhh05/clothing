import Link from "next/link";
import { useRouter } from "next/router";
import { useState } from "react";
import { useTranslations } from "next-intl";
import { useAuth } from "../../context/AuthContext";
import { useNotify } from "../../context/NotificationContext";
import { pushWithLang } from "../../lib/router-utils";
import DownArrow from "../../public/icons/DownArrow";

type Item = {
  labelKey: string;
  href: string;
};

const items: Item[] = [
  { labelKey: "my_profile", href: "/profile" },
  { labelKey: "shipping_addresses", href: "/profile/addresses" },
  { labelKey: "order_tracking", href: "/orders" },
  { labelKey: "notifications", href: "/notifications" },
  { labelKey: "returns", href: "/returns" },
];

const AccountSidebar = () => {
  const t = useTranslations("Account");
  const authT = useTranslations("LoginRegister");
  const router = useRouter();
  const auth = useAuth();
  const { notify } = useNotify();
  const [mobileOpen, setMobileOpen] = useState(false);
  const currentPath = router.asPath || "";
  const normalizedPath = currentPath.split("?")[0];
  const activeItem = items.find(
    (item) =>
      normalizedPath === item.href ||
      (item.href !== "/profile" && normalizedPath.startsWith(`${item.href}/`))
  );

  const handleLogout = async () => {
    await auth.logout?.();
    notify(authT("logout_successful"), "success");
    pushWithLang(router, "/");
  };

  const menuItems = (
    <>
      <ul className="space-y-1.5">
        {items.map((item) => {
          const active =
            normalizedPath === item.href ||
            (item.href !== "/profile" && normalizedPath.startsWith(`${item.href}/`));
          return (
            <li key={item.href}>
              <Link href={item.href}>
                <a
                  className={`block px-3 py-2 text-sm transition ${
                    active
                      ? "font-semibold text-black bg-gray100 border-l-2 border-black"
                      : "text-gray500 hover:text-black hover:bg-gray100"
                  }`}
                >
                  {t(item.labelKey)}
                </a>
              </Link>
            </li>
          );
        })}
      </ul>
      <button
        type="button"
        onClick={handleLogout}
        className="mt-5 w-full text-left px-3 py-2 text-sm font-semibold text-black border border-gray300 hover:bg-gray100"
      >
        {authT("logout")}
      </button>
    </>
  );

  return (
    <>
      <aside className="hidden md:block w-full h-fit border border-gray200 p-5 md:p-6 bg-white md:sticky md:top-6">
        <h2 className="text-2xl font-semibold mb-5">{t("account")}</h2>
        {menuItems}
      </aside>

      <aside className="md:hidden w-full border border-gray200 bg-white p-4">
        <button
          type="button"
          onClick={() => setMobileOpen((prev) => !prev)}
          className="w-full flex items-center justify-between"
        >
          <div className="text-left">
            <p className="text-xl font-semibold">{t("account")}</p>
            <p className="text-sm text-gray400 mt-1">
              {activeItem ? t(activeItem.labelKey) : t("my_profile")}
            </p>
          </div>
          <DownArrow
            extraClass={`w-5 h-5 text-gray500 transition-transform ${
              mobileOpen ? "rotate-0" : "rotate-180"
            }`}
          />
        </button>
        {mobileOpen && <div className="mt-4">{menuItems}</div>}
      </aside>
    </>
  );
};

export default AccountSidebar;
