import { ReactNode } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import AccountSidebar from "./AccountSidebar";

type Props = {
  section: string;
  children: ReactNode;
};

const AccountPageLayout = ({ section, children }: Props) => {
  const t = useTranslations("Account");
  return (
    <div className="grid grid-cols-1 md:grid-cols-12 gap-6 lg:gap-8 items-start">
      <div className="md:col-span-4 lg:col-span-3">
        <AccountSidebar />
      </div>
      <section className="md:col-span-8 lg:col-span-9 max-w-5xl min-w-0">
        <div className="text-xs sm:text-sm text-gray500 mb-4 bg-gray100 px-3 sm:px-4 py-3 break-words">
          <Link href="/">
            <a className="hover:text-black">{t("home")}</a>
          </Link>{" "}
          /{" "}
          <Link href="/profile">
            <a className="hover:text-black">{t("account")}</a>
          </Link>{" "}
          / <span>{section}</span>
        </div>
        <div className="border border-gray200 bg-white p-5 md:p-7">{children}</div>
      </section>
    </div>
  );
};

export default AccountPageLayout;
