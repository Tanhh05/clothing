import { useEffect, useState } from "react";
import { GetStaticProps } from "next";
import { useRouter } from "next/router";
import { useTranslations } from "next-intl";

import Header from "../components/Header/Header";
import Footer from "../components/Footer/Footer";
import AccountPageLayout from "../components/Account/AccountPageLayout";
import Input from "../components/Input/Input";
import Button from "../components/Buttons/Button";
import { useAuth } from "../context/AuthContext";
import { pushWithLang } from "../lib/router-utils";

const ProfilePage = () => {
  const t = useTranslations("LoginRegister");
  const indexT = useTranslations("Index");
  const accountT = useTranslations("Account");
  const auth = useAuth();
  const router = useRouter();

  const [username, setUsername] = useState("");
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    if (!auth.isAuthReady) return;
    if (!auth.user) {
      pushWithLang(router, "/");
      return;
    }
    setUsername(auth.user.username || "");
    setFullName(auth.user.fullname || "");
    setEmail(auth.user.email || "");
    setPhone(auth.user.phone || "");
  }, [auth.isAuthReady, auth.user, router]);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setIsSaving(true);
    setError("");
    setMessage("");
    const payload: {
      username?: string;
      fullName?: string;
      email?: string;
      phone?: string;
    } = {};
    if (username.trim().length >= 3) payload.username = username.trim();
    if (fullName.trim() !== "") payload.fullName = fullName.trim();
    if (email.trim() !== "") payload.email = email.trim();
    if (phone.trim() !== "") payload.phone = phone.trim();
    const response = await auth.updateProfile?.(payload);
    if (response?.success) {
      setMessage(t("profile_updated_success"));
    } else {
      setError(t(response?.message || "error_occurs"));
    }
    setIsSaving(false);
  };

  if (!auth.isAuthReady || !auth.user) {
    return null;
  }

  return (
    <div>
      <Header title={`${t("profile")} - TWENTY`} />
      <main id="main-content" className="app-max-width app-x-padding py-8 md:py-10">
        <AccountPageLayout section={accountT("my_profile")}>
          <section className="max-w-3xl">
            <h2 className="text-2xl font-semibold mb-6">{t("profile")}</h2>
            <form onSubmit={handleSubmit}>
                <div className="mb-5">
                  <label htmlFor="username" className="text-sm">
                    {t("username")}
                  </label>
                  <Input
                    name="username"
                    type="text"
                    extraClass="w-full mt-1"
                    border="border-2 border-gray300"
                    value={username}
                    onChange={(e) => setUsername((e.target as HTMLInputElement).value)}
                  />
                </div>

                <div className="mb-5">
                  <label htmlFor="fullName" className="text-sm">
                    {t("name")}
                  </label>
                  <Input
                    name="fullName"
                    type="text"
                    extraClass="w-full mt-1"
                    border="border-2 border-gray300"
                    value={fullName}
                    onChange={(e) => setFullName((e.target as HTMLInputElement).value)}
                  />
                </div>

                <div className="mb-5">
                  <label htmlFor="email" className="text-sm">
                    {t("email_address")}
                  </label>
                  <Input
                    name="email"
                    type="email"
                    extraClass="w-full mt-1"
                    border="border-2 border-gray300"
                    value={email}
                    onChange={(e) => setEmail((e.target as HTMLInputElement).value)}
                  />
                </div>

                <div className="mb-5">
                  <label htmlFor="phone" className="text-sm">
                    {t("phone")}
                  </label>
                  <Input
                    name="phone"
                    type="text"
                    extraClass="w-full mt-1"
                    border="border-2 border-gray300"
                    value={phone}
                    onChange={(e) => setPhone((e.target as HTMLInputElement).value)}
                  />
                </div>

                {message && <p className="text-green-700 mb-4 text-sm">{message}</p>}
                {error && <p className="text-red mb-4 text-sm">{error}</p>}

                <div className="flex flex-wrap gap-3 pt-1">
                  <Button
                    type="submit"
                    value={isSaving ? indexT("loading") : t("save_changes")}
                    extraClass="text-center"
                  />
                </div>
              </form>
            </section>
        </AccountPageLayout>
      </main>
      <Footer />
    </div>
  );
};

export const getStaticProps: GetStaticProps = async ({ locale }) => {
  return {
    props: {
      messages: (await import(`../messages/common/${locale}.json`)).default,
    },
  };
};

export default ProfilePage;
