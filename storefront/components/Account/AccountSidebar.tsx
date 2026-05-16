import Link from "next/link";
import { useRouter } from "next/router";
import { useAuth } from "../../context/AuthContext";
import { useNotify } from "../../context/NotificationContext";
import { pushWithLang } from "../../lib/router-utils";

type Item = {
  label: string;
  href: string;
};

const items: Item[] = [
  { label: "Thông tin của tôi", href: "/profile" },
  { label: "Địa chỉ giao hàng", href: "/profile/addresses" },
  { label: "Theo dõi đơn hàng", href: "/orders" },
  { label: "Wishlist", href: "/wishlist" },
  { label: "Thông báo", href: "/notifications" },
  { label: "Yêu cầu đổi trả", href: "/returns" },
];

const AccountSidebar = () => {
  const router = useRouter();
  const auth = useAuth();
  const { notify } = useNotify();
  const currentPath = router.asPath || "";

  const handleLogout = async () => {
    await auth.logout?.();
    notify("Đăng xuất thành công", "success");
    pushWithLang(router, "/");
  };

  return (
    <aside className="w-full xl:w-72 h-fit">
      <h2 className="text-5xl font-semibold mb-8">Tài khoản</h2>
      <ul className="space-y-3">
        {items.map((item) => {
          const active =
            currentPath === item.href ||
            (item.href !== "/" && currentPath.startsWith(item.href));
          return (
            <li key={item.href}>
              <Link href={item.href}>
                <a className={active ? "font-semibold text-black" : "text-black"}>
                  {item.label}
                </a>
              </Link>
            </li>
          );
        })}
      </ul>
      <button
        type="button"
        onClick={handleLogout}
        className="mt-6 text-left font-semibold text-black"
      >
        Đăng xuất
      </button>
    </aside>
  );
};

export default AccountSidebar;
