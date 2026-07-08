import { useCallback, useState } from "react";
import { toast } from "sonner";
import { FileText, KeyRound, Radio, X } from "lucide-react";
import { api } from "@/lib/api";
import { useConfig } from "@/hooks/useConfig";
import { useDebateSocket } from "@/hooks/useDebateSocket";
import { useDocumentEvents } from "@/hooks/useDocumentEvents";
import { useDocuments } from "@/hooks/useDocuments";
import { useDebateStore } from "@/store/debateStore";
import { Button } from "@/components/ui/button";
import { ChatPanel } from "@/components/workspace/ChatPanel";
import { DebatePanel } from "@/components/workspace/DebatePanel";
import { DocumentPanel } from "@/components/workspace/DocumentPanel";
import { cn } from "@/lib/utils";

type DrawerPanel = "documents" | "debate" | null;

/**
 * The three-panel RAG workspace:
 * left — document library & upload, center — chat, right — live debate.
 * On narrower viewports the side panels open as slide-over drawers instead.
 */
export default function WorkspacePage() {
  const [selectedDocumentId, setSelectedDocumentId] = useState<string | null>(null);
  const [drawer, setDrawer] = useState<DrawerPanel>(null);
  const { data: documents } = useDocuments();
  const { data: config } = useConfig();

  const activeQuestionId = useDebateStore((s) => s.activeQuestionId);
  const isReplay = useDebateStore((s) => s.isReplay);
  const hydrateFromDetail = useDebateStore((s) => s.hydrateFromDetail);

  // Live event streams.
  useDocumentEvents();
  useDebateSocket(activeQuestionId, isReplay);

  const selectedDocument =
    documents?.find((doc) => doc.id === selectedDocumentId) ?? null;

  const handleReplayQuestion = useCallback(
    (questionId: string) => {
      api
        .getQuestion(questionId)
        .then((detail) => hydrateFromDetail(detail, { replay: true }))
        .catch(() => toast.error("Could not load that debate"));
    },
    [hydrateFromDetail],
  );

  const documentPanel = (
    <DocumentPanel
      selectedDocumentId={selectedDocumentId}
      onSelectDocument={(id) => {
        setSelectedDocumentId(id);
        setDrawer(null);
      }}
      onReplayQuestion={(id) => {
        handleReplayQuestion(id);
        setDrawer(null);
      }}
    />
  );

  return (
    <div className="flex h-[calc(100vh-3.5rem)] flex-col">
      {config && !config.anyProviderConfigured && (
        <div className="flex items-start gap-2.5 border-b border-amber-300 bg-amber-50 px-4 py-2.5">
          <KeyRound className="mt-0.5 h-4 w-4 shrink-0 text-amber-700" />
          <p className="text-xs leading-relaxed text-amber-900">
            <span className="font-semibold">No LLM provider configured on the server.</span>{" "}
            Document upload and indexing work, but debates will be rejected. Get a free key at{" "}
            <a
              href="https://openrouter.ai/keys"
              target="_blank"
              rel="noreferrer"
              className="font-medium underline"
            >
              openrouter.ai/keys
            </a>
            , then restart the backend with <code className="rounded bg-amber-100 px-1">OPENROUTER_API_KEY</code> set.
          </p>
        </div>
      )}

      {/* Narrow-viewport toolbar: opens the side panels as drawers. */}
      <div className="flex items-center gap-2 border-b px-3 py-2 xl:hidden">
        <Button
          variant="outline"
          size="sm"
          className="lg:hidden"
          onClick={() => setDrawer("documents")}
        >
          <FileText className="h-3.5 w-3.5" />
          Documents
        </Button>
        <Button variant="outline" size="sm" onClick={() => setDrawer("debate")}>
          <Radio className="h-3.5 w-3.5" />
          Live debate
        </Button>
      </div>

      <div className="mx-auto grid min-h-0 w-full max-w-[1500px] flex-1 grid-cols-1 lg:grid-cols-[300px_minmax(0,1fr)] xl:grid-cols-[300px_minmax(0,1fr)_400px]">
        <aside className="hidden border-r lg:block">{documentPanel}</aside>

        <section className="min-w-0">
          <ChatPanel selectedDocument={selectedDocument} />
        </section>

        <aside className="hidden border-l xl:block">
          <DebatePanel />
        </aside>
      </div>

      {/* Slide-over drawer for narrow viewports */}
      {drawer && (
        <div className="fixed inset-0 z-50 xl:hidden">
          <button
            type="button"
            aria-label="Close panel"
            className="absolute inset-0 bg-black/50 backdrop-blur-sm"
            onClick={() => setDrawer(null)}
          />
          <div
            className={cn(
              "absolute top-0 flex h-full w-[min(400px,90vw)] flex-col bg-background shadow-xl animate-slide-up",
              drawer === "documents" ? "left-0 border-r" : "right-0 border-l",
            )}
          >
            <div className="flex justify-end border-b px-2 py-1.5">
              <Button variant="ghost" size="icon" className="h-7 w-7" onClick={() => setDrawer(null)}>
                <X className="h-4 w-4" />
              </Button>
            </div>
            <div className="min-h-0 flex-1">
              {drawer === "documents" ? documentPanel : <DebatePanel />}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
