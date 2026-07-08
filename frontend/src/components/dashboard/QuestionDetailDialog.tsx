import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { useQuestionDetail } from "@/hooks/useQuestions";
import { AGENTS } from "@/lib/agents";
import { cn, formatDuration } from "@/lib/utils";
import { MarkdownContent } from "@/components/workspace/MarkdownContent";
import { ConsensusCard } from "@/components/workspace/ConsensusCard";
import { ContextSnippets } from "@/components/workspace/ContextSnippets";
import type { AgentMessage } from "@/lib/types";

interface QuestionDetailDialogProps {
  questionId: string | null;
  onClose: () => void;
}

function Section({ title, message }: { title: string; message: AgentMessage | null }) {
  return (
    <div>
      <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
        {title}
      </p>
      {!message ? (
        <p className="text-sm italic text-muted-foreground">Not produced.</p>
      ) : message.status === "FAILED" ? (
        <p className="text-sm text-amber-700">
          Call failed: {message.errorMessage ?? "unknown error"}
        </p>
      ) : (
        <MarkdownContent content={message.content ?? ""} />
      )}
    </div>
  );
}

/** Full post-hoc inspection of one debate from the history dashboard. */
export function QuestionDetailDialog({ questionId, onClose }: QuestionDetailDialogProps) {
  const { data: detail, isLoading } = useQuestionDetail(questionId);

  return (
    <Dialog open={questionId != null} onOpenChange={(next) => !next && onClose()}>
      <DialogContent className="max-h-[88vh] max-w-4xl overflow-hidden">
        <DialogHeader>
          <DialogTitle className="pr-8 leading-6">
            {detail?.questionText ?? "Debate detail"}
          </DialogTitle>
          <DialogDescription className="flex flex-wrap items-center gap-2">
            {detail && (
              <>
                <span>{detail.documentFilename}</span>
                {detail.processingTimeMs != null && (
                  <Badge variant="secondary" className="text-[10px]">
                    {formatDuration(detail.processingTimeMs)}
                  </Badge>
                )}
                <Badge
                  variant={
                    detail.status === "COMPLETED"
                      ? "success"
                      : detail.status === "FAILED"
                        ? "destructive"
                        : "info"
                  }
                  className="text-[10px]"
                >
                  {detail.status.toLowerCase().replaceAll("_", " ")}
                </Badge>
              </>
            )}
          </DialogDescription>
        </DialogHeader>

        {isLoading && (
          <div className="space-y-3">
            <Skeleton className="h-24 w-full" />
            <Skeleton className="h-48 w-full" />
          </div>
        )}

        {detail && (
          <Tabs defaultValue="consensus" className="min-h-0">
            <TabsList>
              <TabsTrigger value="consensus">Consensus</TabsTrigger>
              <TabsTrigger value="agents">Agent debate</TabsTrigger>
              <TabsTrigger value="context">Retrieved context</TabsTrigger>
            </TabsList>

            <div className="scrollbar-thin max-h-[56vh] overflow-y-auto pr-1">
              <TabsContent value="consensus">
                {detail.consensus ? (
                  <ConsensusCard consensus={detail.consensus} />
                ) : (
                  <p className="py-8 text-center text-sm text-muted-foreground">
                    {detail.errorMessage ?? "No consensus was generated for this debate."}
                  </p>
                )}
              </TabsContent>

              <TabsContent value="agents" className="space-y-4">
                {detail.agents.length === 0 && (
                  <p className="py-8 text-center text-sm text-muted-foreground">
                    No agent activity recorded.
                  </p>
                )}
                {detail.agents.map((agentDebate) => {
                  const meta = AGENTS[agentDebate.agent];
                  const Icon = meta.icon;
                  return (
                    <div
                      key={agentDebate.agent}
                      className={cn("rounded-lg border p-4", meta.colors.border)}
                    >
                      <div className="mb-3 flex items-center gap-2">
                        <span
                          className={cn(
                            "flex h-7 w-7 items-center justify-center rounded-md",
                            meta.colors.bg,
                          )}
                        >
                          <Icon className={cn("h-4 w-4", meta.colors.text)} />
                        </span>
                        <span className="font-semibold">{meta.displayName}</span>
                        <Badge variant="outline" className="text-[10px]">
                          {agentDebate.model}
                        </Badge>
                      </div>
                      <div className="space-y-3">
                        <Section title="Round 1 · Initial answer" message={agentDebate.initialResponse} />
                        <Section title="Round 2 · Critique of peers" message={agentDebate.critique} />
                        <Section title="Round 3 · Revised answer" message={agentDebate.revision} />
                      </div>
                    </div>
                  );
                })}
              </TabsContent>

              <TabsContent value="context">
                {detail.context.length > 0 ? (
                  <ContextSnippets snippets={detail.context} />
                ) : (
                  <p className="py-8 text-center text-sm text-muted-foreground">
                    No context was retrieved.
                  </p>
                )}
              </TabsContent>
            </div>
          </Tabs>
        )}
      </DialogContent>
    </Dialog>
  );
}
