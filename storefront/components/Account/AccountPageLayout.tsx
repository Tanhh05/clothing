import { ReactNode } from "react";
import Link from "next/link";
import AccountSidebar from "./AccountSidebar";

type Props = {
  section: string;
  children: ReactNode;
};

const AccountPageLayout = ({ section, children }: Props) => {
  return (
    <div className="grid grid-cols-1 md:grid-cols-12 gap-6 lg:gap-12 items-start">
      <div className="md:col-span-4 lg:col-span-3">
        <AccountSidebar />
      </div>
      <section className="md:col-span-8 lg:col-span-9 max-w-5xl">
        <div className="text-sm text-gray400 uppercase tracking-wide">
          <Link href="/">
            <a className="hover:text-black">TRANG CHỦ</a>
          </Link>{" "}
          &#8250;{" "}
          <Link href="/profile">
            <a className="hover:text-black">TÀI KHOẢN</a>
          </Link>{" "}
          &#8250; <span>{section}</span>
        </div>
        <div className="border-t-4 border-black mt-4 mb-6" />
        {children}
      </section>
    </div>
  );
};

export default AccountPageLayout;
