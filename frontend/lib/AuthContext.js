"use client";

import { createContext, useCallback, useContext, useEffect, useState } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { Loader2 } from "lucide-react";
import { getAccessToken, setTokens, clearTokens } from "@/lib/auth";
import * as authService from "@/services/authService";
import { getMe } from "@/services/userService";
import { isAdmin } from "@/lib/permissions";

const PUBLIC_PATHS = ["/", "/login", "/register"];
const AUTH_PAGES = ["/login", "/register"];

function safeReturnUrl(url) {
  if (!url || !url.startsWith("/") || url.startsWith("//") || AUTH_PAGES.includes(url)) return null;
  return url;
}

const AuthContext = createContext(null);

export function useAuth() {
  return useContext(AuthContext);
}

export default function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const pathname = usePathname();
  const router = useRouter();
  const searchParams = useSearchParams();

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
    const isAuthPage = AUTH_PAGES.includes(pathname);
    if (!user && !isPublic) router.replace(`/login?returnUrl=${encodeURIComponent(pathname)}`);
    if (user && isAuthPage) {
      const defaultTarget = isAdmin(user) ? "/admin" : "/file-storage";
      router.replace(safeReturnUrl(searchParams.get("returnUrl")) ?? defaultTarget);
    }
  }, [loading, user, pathname, router, searchParams]);

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
    clearTokens();
    setUser(null);
    router.replace("/login");
    authService.logout().catch(() => {});
  }, [router]);

  const value = { user, loading, login, register, logout, setUser };

  const isPublic = PUBLIC_PATHS.includes(pathname);
  const isAuthPage = AUTH_PAGES.includes(pathname);
  if (loading || (!user && !isPublic) || (user && isAuthPage)) {
    return (
      <div className="flex flex-1 items-center justify-center">
        <Loader2 className="h-6 w-6 animate-spin text-text-muted" />
      </div>
    );
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
