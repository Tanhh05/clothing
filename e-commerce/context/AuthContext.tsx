import axios from "axios";
import { getCookie, removeCookies, setCookies } from "cookies-next";
import React, { useState, useEffect, useContext, createContext } from "react";

type authType = {
  user: null | User;
  isAuthReady: boolean;
  register?: (
    email: string,
    fullname: string,
    password: string,
    shippingAddress: string,
    phone: string
  ) => Promise<{
    success: boolean;
    message: string;
  }>;
  login?: (
    email: string,
    password: string
  ) => Promise<{
    success: boolean;
    message: string;
  }>;
  loginWithGoogle?: (idToken: string) => Promise<{
    success: boolean;
    message: string;
  }>;
  forgotPassword?: (email: string) => Promise<{
    success: boolean;
    message: string;
  }>;
  updateProfile?: (payload: {
    username?: string;
    email?: string;
    fullName?: string;
    phone?: string;
  }) => Promise<{
    success: boolean;
    message: string;
  }>;
  logout?: () => void;
};

const initialAuth: authType = {
  user: null,
  isAuthReady: false,
};

const authContext = createContext<authType>(initialAuth);

type User = {
  id: number;
  username?: string;
  email: string;
  fullname: string;
  shippingAddress?: string;
  phone?: string;
  token: string;
  refreshToken?: string;
  tokenType?: string;
};

// Provider component that wraps your app and makes auth object ...
// ... available to any child component that calls useAuth().
export function ProvideAuth({ children }: { children: React.ReactNode }) {
  const auth = useProvideAuth();
  return <authContext.Provider value={auth}>{children}</authContext.Provider>;
}
// Hook for child components to get the auth object ...
// ... and re-render when it changes.
export const useAuth = () => {
  return useContext(authContext);
};

// Provider hook that creates auth object and handles state
function useProvideAuth() {
  const [user, setUser] = useState<User | null>(null);
  const [isAuthReady, setIsAuthReady] = useState(false);

  useEffect(() => {
    const initialAuth = getCookie("user");
    if (initialAuth) {
      const initUser = JSON.parse(initialAuth as string);
      setUser(initUser);
    }
    setIsAuthReady(true);
  }, []);

  useEffect(() => {
    setCookies("user", user);
  }, [user]);

  const register = async (
    email: string,
    fullname: string,
    password: string,
    shippingAddress: string,
    phone: string
  ) => {
    try {
      const response = await axios.post(
        `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/v1/auth/register`,
        {
          email,
          fullname,
          password,
          shippingAddress,
          phone,
        }
      );
      const registerResponse = response.data;
      const user: User = {
        id: +registerResponse.id,
        email,
        fullname,
        shippingAddress,
        phone,
        token: registerResponse.token,
      };
      setUser(user);
      return {
        success: true,
        message: "register_successful",
      };
    } catch (err) {
      const errResponse = (err as any).response.data;
      let errorMessage: string;
      if (errResponse.error.type === "alreadyExists") {
        errorMessage = errResponse.error.type;
      } else {
        errorMessage = errResponse.error.detail.message;
      }
      return {
        success: false,
        message: errorMessage,
      };
    }
  };

  const login = async (email: string, password: string) => {
    try {
      const response = await axios.post(
        `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/auth/login`,
        {
          usernameOrEmail: email,
          password,
        }
      );
      const loginResponse = response.data;
      const user: User = {
        id: +loginResponse.userId,
        username: loginResponse.username,
        email,
        fullname: loginResponse.username || email,
        token: loginResponse.accessToken,
        refreshToken: loginResponse.refreshToken,
        tokenType: loginResponse.tokenType,
      };
      setUser(user);
      return {
        success: true,
        message: "login_successful",
      };
    } catch (err) {
      console.error("Login failed:", err);
      return {
        success: false,
        message: "incorrect_email_password",
      };
    }
  };

  const loginWithGoogle = async (idToken: string) => {
    try {
      const response = await axios.post(
        `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/auth/google`,
        { idToken }
      );
      const loginResponse = response.data;
      const loginEmail = loginResponse.email || "";
      const user: User = {
        id: +loginResponse.userId,
        username: loginResponse.username,
        email: loginEmail,
        fullname: loginResponse.username || "Google User",
        token: loginResponse.accessToken,
        refreshToken: loginResponse.refreshToken,
        tokenType: loginResponse.tokenType,
      };
      setUser(user);
      return {
        success: true,
        message: "login_successful",
      };
    } catch (err) {
      console.error("Google login failed:", err);
      const axiosErr = err as any;
      const backendMsg =
        axiosErr?.response?.data?.message ||
        axiosErr?.response?.data?.error ||
        "";
      return {
        success: false,
        message: backendMsg || "error_occurs",
      };
    }
  };

  const forgotPassword = async (email: string) => {
    try {
      const response = await axios.post(
        `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/v1/auth/forgot-password`,
        {
          email,
        }
      );
      const forgotPasswordResponse = response.data;
      setUser(user);
      return {
        success: forgotPasswordResponse.success,
        message: "reset_email_sent",
      };
    } catch (err) {
      console.log(err);
      return {
        success: false,
        message: "something_went_wrong",
      };
    }
  };

  const updateProfile = async (payload: {
    username?: string;
    email?: string;
    fullName?: string;
    phone?: string;
  }) => {
    if (!user?.token) {
      return { success: false, message: "error_occurs" };
    }
    try {
      const response = await axios.put(
        `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/user/me`,
        payload,
        {
          headers: {
            Authorization: `Bearer ${user.token}`,
          },
        }
      );
      const nextUser = response.data;
      setUser({
        ...user,
        username: nextUser.username,
        email: nextUser.email,
        fullname: nextUser.fullName || user.fullname,
        phone: nextUser.phone || "",
      });
      return { success: true, message: "profile_updated" };
    } catch (err) {
      console.error("Update profile failed:", err);
      return { success: false, message: "error_occurs" };
    }
  };

  const logout = () => {
    setUser(null);
    removeCookies("user");
  };

  // Return the user object and auth methods
  return {
    user,
    isAuthReady,
    register,
    login,
    loginWithGoogle,
    forgotPassword,
    updateProfile,
    logout,
  };
}
