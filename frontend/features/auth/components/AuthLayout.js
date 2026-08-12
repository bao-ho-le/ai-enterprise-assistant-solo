import { Sparkles } from "lucide-react";

// Facebook-style split screen shared by /login and /register: branding image
// on the left, form on the right. Collapses to form-only below lg.
// ponytail: no hero photo supplied yet — swap this div for
// `<Image src="/auth-hero.png" alt="" fill priority className="object-cover" />`
// (next/image, already a project dependency) once the artwork lands in /public.
export default function AuthLayout({ title, subtitle, children }) {
  return (
    <div className="flex min-h-screen w-full flex-col lg:flex-row">
      <div className="relative hidden w-1/2 flex-col items-center justify-center gap-6 bg-gradient-to-br from-accent to-bg-primary px-12 lg:flex">
        <span className="flex h-16 w-16 items-center justify-center rounded-2xl border border-white/20 bg-white/10 backdrop-blur">
          <Sparkles className="h-8 w-8 text-white" />
        </span>
        <div className="text-center">
          <h2 className="text-3xl font-semibold tracking-tight text-white">Enterprise AI Assistant</h2>
          <p className="mt-3 max-w-sm text-sm text-white/70">
            Secure, enterprise-grade AI workflows for document intelligence, communication, and analytics.
          </p>
        </div>
      </div>

      <div className="flex w-full flex-1 items-center justify-center bg-bg-primary px-6 py-12 lg:w-1/2">
        <div className="w-full max-w-sm">
          <div className="mb-8 flex flex-col items-center gap-2 text-center">
            <span className="flex h-10 w-10 items-center justify-center rounded-lg border border-border-subtle bg-bg-elevated">
              <Sparkles className="h-5 w-5 text-accent" />
            </span>
            <span className="text-sm font-semibold text-text-primary">Enterprise AI Assistant</span>
          </div>
          <h1 className="text-xl font-semibold text-text-primary">{title}</h1>
          {subtitle && <p className="mt-1.5 text-sm text-text-secondary">{subtitle}</p>}
          <div className="mt-6">{children}</div>
        </div>
      </div>
    </div>
  );
}
