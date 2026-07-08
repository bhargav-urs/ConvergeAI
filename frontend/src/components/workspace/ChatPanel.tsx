import { useEffect, useRef, useState } from "react";
import { GitMerge, Loader2, MessageSquareText, Scale, SendHorizonal, XCircle, Zap } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { useSubmitQuestion } from "@/hooks/useQuestions";
import { useDebateStore } from "@/store/debateStore";
import type { DocumentDto } from "@/lib/types";
import { cn } from "@/lib/utils";
import { MarkdownContent } from "./MarkdownContent";

interface ChatPanelProps {
  selectedDocument: DocumentDto | null;
}

const STAGE_HINTS: Record<string, string> = {
  PENDING: "Queued…",
  RETRIEVING: "Retrieving relevant context from the document…",
  DEBATING_ROUND_1: "Round 1 — agents are forming independent answers…",
  DEBATING_ROUND_2: "Round 2 — agents are critiquing each other…",
  DEBATING_ROUND_3: "Round 3 — agents are revising their answers…",
  CONSENSUS: "Synthesizing the consensus answer…",
};

export function ChatPanel({ selectedDocument }: ChatPanelProps) {
  const [draft, setDraft] = useState("");
  const chat = useDebateStore((s) => s.chat);
  const stage = useDebateStore((s) => s.stage);
  const isReplay = useDebateStore((s) => s.isReplay);
  const preferredMode = useDebateStore((s) => s.preferredMode);
  const setPreferredMode = useDebateStore((s) => s.setPreferredMode);
  const submitQuestion = useSubmitQuestion();
  const scrollRef = useRef<HTMLDivElement | null>(null);

  const debateRunning =
    !isReplay && stage !== "IDLE" && stage !== "COMPLETED" && stage !== "FAILED";

  const canAsk =
    selectedDocument?.status === "READY" && !debateRunning && !submitQuestion.isPending;

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: "smooth" });
  }, [chat.length, stage]);

  const handleSubmit = () => {
    const text = draft.trim();
    if (!text || !canAsk || !selectedDocument) return;
    submitQuestion.mutate({
      documentId: selectedDocument.id,
      questionText: text,
      mode: preferredMode,
    });
    setDraft("");
  };

  return (
    <div className="flex h-full flex-col">
      <div className="flex items-center justify-between border-b px-4 py-3">
        <h2 className="text-sm font-semibold">
          {selectedDocument ? (
            <>
              Asking about{" "}
              <span className="text-primary">{selectedDocument.filename}</span>
            </>
          ) : (
            "Chat"
          )}
        </h2>
        {debateRunning && (
          <Badge variant="info" className="animate-pulse-soft">
            <Loader2 className="h-3 w-3 animate-spin" />
            {STAGE_HINTS[stage] ? "debate in progress" : stage}
          </Badge>
        )}
      </div>

      <div ref={scrollRef} className="scrollbar-thin flex-1 space-y-4 overflow-y-auto p-4">
        {chat.length === 0 && !debateRunning && (
          <div className="flex h-full flex-col items-center justify-center text-center">
            <MessageSquareText className="mb-3 h-8 w-8 text-muted-foreground/50" />
            <p className="text-sm font-medium text-muted-foreground">
              {selectedDocument
                ? "Ask your first question about this document"
                : "Upload and select a document to begin"}
            </p>
            <p className="mt-1 max-w-sm text-xs text-muted-foreground">
              Three AI agents will debate the answer across structured rounds — grounded strictly
              in the document, never outside knowledge.
            </p>
          </div>
        )}

        {chat.map((entry) => {
          if (entry.role === "user") {
            return (
              <div key={entry.id} className="flex justify-end">
                <div className="max-w-[85%] rounded-2xl rounded-br-sm bg-primary px-4 py-2.5 text-sm text-primary-foreground">
                  {entry.text}
                </div>
              </div>
            );
          }
          if (entry.role === "error") {
            return (
              <div key={entry.id} className="flex">
                <div className="flex max-w-[85%] items-start gap-2 rounded-2xl rounded-bl-sm border border-destructive/30 bg-destructive/5 px-4 py-2.5">
                  <XCircle className="mt-0.5 h-4 w-4 shrink-0 text-destructive" />
                  <div>
                    <p className="text-xs font-semibold text-destructive">Debate failed</p>
                    <p className="text-xs text-destructive/90">{entry.text}</p>
                  </div>
                </div>
              </div>
            );
          }
          return (
            <div key={entry.id} className="flex">
              <div className="max-w-[85%] rounded-2xl rounded-bl-sm border bg-card px-4 py-3 shadow-sm">
                <div className="mb-1.5 flex items-center gap-2">
                  <GitMerge className="h-3.5 w-3.5 text-primary" />
                  <span className="text-xs font-semibold">Consensus</span>
                  {entry.consensus && (
                    <Badge variant="secondary" className="text-[10px]">
                      {entry.consensus.confidenceScore}% confidence
                    </Badge>
                  )}
                </div>
                <MarkdownContent content={entry.text} />
              </div>
            </div>
          );
        })}

        {debateRunning && (
          <div className="flex">
            <div className="flex items-center gap-2 rounded-2xl rounded-bl-sm border bg-card px-4 py-2.5 text-xs text-muted-foreground">
              <Loader2 className="h-3.5 w-3.5 animate-spin text-primary" />
              {STAGE_HINTS[stage] ?? "Working…"}
            </div>
          </div>
        )}
      </div>

      <div className="border-t p-3">
        <div className="mb-2 flex items-center gap-2">
          <div className="inline-flex rounded-lg bg-muted p-0.5" role="radiogroup" aria-label="Debate mode">
            <button
              type="button"
              role="radio"
              aria-checked={preferredMode === "NORMAL"}
              onClick={() => setPreferredMode("NORMAL")}
              title="Full 3-round debate: answers → cross-critique → revision → consensus. Most trustworthy."
              className={cn(
                "flex items-center gap-1 rounded-md px-2.5 py-1 text-xs font-medium transition-colors",
                preferredMode === "NORMAL"
                  ? "bg-card text-foreground shadow"
                  : "text-muted-foreground hover:text-foreground",
              )}
            >
              <Scale className="h-3 w-3" />
              Normal
            </button>
            <button
              type="button"
              role="radio"
              aria-checked={preferredMode === "FAST"}
              onClick={() => setPreferredMode("FAST")}
              title="Single round with concise answers, straight to consensus. ~3× faster, skips cross-critique."
              className={cn(
                "flex items-center gap-1 rounded-md px-2.5 py-1 text-xs font-medium transition-colors",
                preferredMode === "FAST"
                  ? "bg-card text-foreground shadow"
                  : "text-muted-foreground hover:text-foreground",
              )}
            >
              <Zap className="h-3 w-3" />
              Fast
            </button>
          </div>
          <span className="text-[11px] text-muted-foreground">
            {preferredMode === "FAST"
              ? "1 round, concise answers — quicker, lighter scrutiny"
              : "3 rounds with cross-critique — slower, most trustworthy"}
          </span>
        </div>
        <div className="flex items-end gap-2">
          <Textarea
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter" && !e.shiftKey) {
                e.preventDefault();
                handleSubmit();
              }
            }}
            placeholder={
              !selectedDocument
                ? "Select a ready document first…"
                : selectedDocument.status !== "READY"
                  ? "Document is still indexing…"
                  : debateRunning
                    ? "Wait for the current debate to finish…"
                    : "Ask a question about the document… (Enter to send)"
            }
            disabled={!canAsk}
            rows={2}
            maxLength={4000}
            className="resize-none"
          />
          <Button
            onClick={handleSubmit}
            disabled={!canAsk || draft.trim().length === 0}
            size="icon"
            className="h-[60px] w-11 shrink-0"
            title="Send"
          >
            {submitQuestion.isPending ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <SendHorizonal className="h-4 w-4" />
            )}
          </Button>
        </div>
      </div>
    </div>
  );
}
