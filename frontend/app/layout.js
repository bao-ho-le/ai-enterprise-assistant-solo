import { Inter } from "next/font/google";
import "./globals.css";
import NavigationBar from "@/components/layout/NavigationBar";
import AuthProvider from "@/lib/AuthContext";

const inter = Inter({
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
  variable: "--font-inter",
});

export const metadata = {
  title: "Enterprise AI Assistant",
  description:
    "Secure, enterprise-grade AI workflows for document intelligence, communication, and analytics.",
};

export default function RootLayout({ children }) {
  return (
    <html lang="en" className={inter.variable}>
      <body className="h-screen flex flex-col overflow-hidden">
        <AuthProvider>
          <NavigationBar />
          {/* App-shell pages (sidebar + main + right panel) manage their own inner
              overflow so only the middle column scrolls; this wrapper's overflow-y-auto
              is what lets plain pages (home, file-storage, ai-usage) scroll normally —
              it never engages for the app-shell pages since their row already fills it exactly. */}
          <div className="flex flex-1 flex-col overflow-y-auto">{children}</div>
        </AuthProvider>
      </body>
    </html>
  );
}
