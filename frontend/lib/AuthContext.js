"use client";

import { createContext, useCallback, useContext, useEffect, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import { Loader2 } from "lucide-react";
import { getAccessToken, getRefreshToken, setTokens, clearTokens } from "@/lib/auth";
import * as authService from "@/services/authService";
import { getMe } from "@/services/userService";

const PUBLIC_PATHS = ["/login", "/register"];

const AuthContext = createContext(null);

export function useAuth() {
  return useContext(AuthContext);
}

export default function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    if (!getAccessToken()) {
      setLoading(false);
      return;
    }
    getMe()
      .then(setUser)
      .catch(() => clearTokens())
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (loading) return;
    const isPublic = PUBLIC_PATHS.includes(pathname);
    if (!user && !isPublic) router.replace("/login");
    if (user && isPublic) router.replace("/");
  }, [loading, user, pathname, router]);

  const login = useCallback(async (userName, password) => {
    const tokens = await authService.login(userName, password);
    setTokens(tokens);
    const me = await getMe();
    setUser(me);
    return me;
  }, []);

  const register = useCallback(async (data) => {
    const tokens = await authService.register(data);
    setTokens(tokens);
    const me = await getMe();
    setUser(me);
    return me;
  }, []);

  const logout = useCallback(async () => {
    const refreshToken = getRefreshToken();
    clearTokens();
    setUser(null);
    router.replace("/login");
    if (refreshToken) authService.logout(refreshToken).catch(() => {});
  }, [router]);

  const value = { user, loading, login, register, logout, setUser };

  const isPublic = PUBLIC_PATHS.includes(pathname);
  if (loading || (!user && !isPublic) || (user && isPublic)) {
    return (
      <div className="flex flex-1 items-center justify-center">
        <Loader2 className="h-6 w-6 animate-spin text-text-muted" />
      </div>
    );
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
