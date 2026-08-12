import AIUsageView from "@/features/ai-usage/components/AIUsageView";

export default function AdminAIUsagePage() {
  return (
    // Admin layout wraps children in an overflow-hidden flex column, so this page
    // owns its own scroll region — giống hệt trải nghiệm của trang /ai-usage chính.
    <div className="min-h-0 flex-1 overflow-y-auto">
      <AIUsageView />
    </div>
  );
}
