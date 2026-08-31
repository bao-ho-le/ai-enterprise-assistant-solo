import Link from "next/link";
import {
  ArrowRight,
  PlayCircle,
  FolderSearch,
  Mail,
  FileBarChart,
  ListTree,
  MessagesSquare,
  Activity,
  Upload,
  Database,
  BrainCircuit,
} from "lucide-react";
import SiteFooter from "@/components/layout/SiteFooter";

export const metadata = {
  title: "Enterprise AI Assistant — AI Workspace for Enterprise Teams",
};

const FEATURES = [
  { Icon: FolderSearch, title: "Semantic File Storage", href: "/file-storage", desc: "Search documents by meaning, not just keywords. Find relevant files across your entire organization instantly." },
  { Icon: Mail, title: "AI Email Composer", href: "/write-email", desc: "Draft professional emails with the right tone, length, and language for any audience — customer, employee, or executive." },
  { Icon: FileBarChart, title: "Report Generation", href: "/write-report", desc: "Generate progress and financial reports from your documents with consistent formatting and enterprise writing standards." },
  { Icon: ListTree, title: "Smart Summaries", href: "/summary", desc: "Executive summaries, bullet points, timelines, and action items — tailored to how your stakeholders consume information." },
  { Icon: MessagesSquare, title: "Document Q&A", href: "/document-qa", desc: "Ask questions across multiple documents with source citations. NotebookLM-style grounded answers for your knowledge base." },
  { Icon: Activity, title: "Usage Analytics", href: "/ai-usage", desc: "Monitor token consumption, costs, and model performance across teams. Full visibility into your AI investment." },
];

const STEPS = [
  { Icon: Upload, title: "Upload Documents", desc: "Upload enterprise documents including reports, meeting minutes, email templates, and other business files." },
  { Icon: Database, title: "Vector Index", desc: "Documents are converted into semantic embeddings to enable intelligent retrieval and contextual understanding." },
  { Icon: BrainCircuit, title: "AI Assistant", desc: "Use enterprise AI to summarize documents, answer questions, generate reports, and draft professional emails." },
];

export default function LandingPage() {
  return (
    <>
      <main className="flex-1">
        {/* Intro */}
        <section className="relative overflow-hidden">
          <div className="absolute inset-0 bg-[radial-gradient(ellipse_80%_50%_at_50%_-20%,rgba(59,130,246,0.12),transparent)]" />
          <div className="relative mx-auto max-w-[1440px] px-4 py-24 sm:px-6 sm:py-32 lg:px-8 lg:py-40">
            <div className="mx-auto max-w-3xl text-center">
              <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-border-subtle bg-bg-card px-4 py-1.5">
                <span className="flex h-2 w-2 rounded-full bg-accent" />
                <span className="text-sm text-text-secondary">
                  Enterprise AI Platform · Now in Production
                </span>
              </div>
              <h1 className="text-4xl font-bold tracking-tight text-text-primary sm:text-5xl lg:text-6xl leading-[1.1]">
                The AI workspace built for enterprise teams
              </h1>
              <p className="mt-6 text-lg sm:text-xl text-text-secondary leading-relaxed max-w-2xl mx-auto">
                Search, summarize, draft, and analyze documents with secure AI
                workflows. Designed for compliance, scale, and the way your
                organization actually works.
              </p>
              <div className="mt-10 flex flex-col sm:flex-row items-center justify-center gap-4">
                <Link href="/file-storage" className="btn-primary px-6 py-3 text-base w-full sm:w-auto">
                  <ArrowRight className="h-4 w-4" />
                  Get Started
                </Link>
                <Link href="/document-qa" className="btn-secondary px-6 py-3 text-base w-full sm:w-auto">
                  <PlayCircle className="h-4 w-4" />
                  Try Document QA
                </Link>
              </div>
            </div>
          </div>
        </section>

        {/* Feature overview */}
        <section className="mx-auto max-w-[1440px] px-4 py-20 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="text-3xl sm:text-4xl font-bold tracking-tight text-text-primary">
              Everything your team needs
            </h2>
            <p className="mt-4 text-lg text-text-secondary max-w-2xl mx-auto">
              From semantic document search to AI-powered communication — one
              unified platform for enterprise productivity.
            </p>
          </div>

          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {FEATURES.map(({ Icon, title, href, desc }) => (
              <article
                key={title}
                className="card group p-6 sm:p-8 transition-all duration-300 hover:border-border-default"
              >
                <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-bg-elevated border border-border-subtle mb-5 group-hover:border-accent/30 transition-colors">
                  <Icon className="h-5 w-5 text-accent" />
                </div>
                <h3 className="text-lg font-semibold text-text-primary mb-2">{title}</h3>
                <p className="text-sm text-text-secondary leading-relaxed mb-4">{desc}</p>
                <Link
                  href={href}
                  className="inline-flex items-center gap-1.5 text-sm font-medium text-accent hover:text-accent-hover transition-colors"
                >
                  Learn more <ArrowRight className="h-3.5 w-3.5" />
                </Link>
              </article>
            ))}
          </div>
        </section>

        {/* Architecture illustration */}
        <section className="mx-auto max-w-[1440px] px-4 py-20 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <h2 className="text-3xl sm:text-4xl font-bold tracking-tight text-text-primary">
              Built for enterprise architecture
            </h2>
            <p className="mt-4 text-lg text-text-secondary max-w-2xl mx-auto">
              Secure document ingestion, vector indexing, and AI orchestration —
              designed for compliance and scale.
            </p>
          </div>

          <div className="card p-8 sm:p-12">
            <div className="flex flex-col items-center justify-center gap-6 md:flex-row md:gap-4">
              {STEPS.map(({ Icon, title, desc }, i) => (
                <div key={title} className="contents">
                  <div className="flex-1 rounded-xl border border-border-subtle bg-bg-elevated p-6 text-center">
                    <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-accent/10">
                      <Icon className="h-6 w-6 text-accent" />
                    </div>
                    <h3 className="mb-2 text-sm font-semibold text-text-primary">{title}</h3>
                    <p className="text-xs leading-relaxed text-text-muted">{desc}</p>
                  </div>
                  {i < STEPS.length - 1 && (
                    <div className="hidden md:flex items-center justify-center">
                      <ArrowRight className="h-6 w-6 text-text-muted" />
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* CTA */}
        <section className="mx-auto max-w-[1440px] px-4 py-20 sm:px-6 lg:px-8">
          <div className="card relative overflow-hidden p-10 sm:p-16 text-center">
            <div className="absolute inset-0 bg-[radial-gradient(ellipse_60%_50%_at_50%_100%,rgba(59,130,246,0.08),transparent)]" />
            <div className="relative">
              <h2 className="text-2xl sm:text-3xl font-bold tracking-tight text-text-primary">
                Ready to transform your document workflows?
              </h2>
              <p className="mt-4 text-text-secondary max-w-xl mx-auto">
                Start with semantic search, then expand to AI-powered
                communication and analytics across your organization.
              </p>
              <div className="mt-8 flex flex-col sm:flex-row items-center justify-center gap-4">
                <Link href="/file-storage" className="btn-primary px-6 py-3 text-base w-full sm:w-auto">
                  Explore File Storage
                </Link>
                <Link href="/ai-usage" className="btn-secondary px-6 py-3 text-base w-full sm:w-auto">
                  View Usage Dashboard
                </Link>
              </div>
            </div>
          </div>
        </section>
      </main>

      <SiteFooter />
    </>
  );
}
