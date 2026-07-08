import { History, Radio, Timer, Zap } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { AGENT_ORDER } from "@/lib/agents";
import { useDebateStore } from "@/store/debateStore";
import { formatDuration } from "@/lib/utils";
import { AgentCard } from "./AgentCard";
import { ConsensusCard } from "./ConsensusCard";
import { ContextSnippets } from "./ContextSnippets";
import { StageTimeline } from "./StageTimeline";

/**
 * Right panel: live visualization of the debate pipeline — stage timeline,
 * retrieved context, per-agent activity, and the consensus once it lands.
 */
export function DebatePanel() {
  const stage = useDebateStore((s) => s.stage);
  const activeMode = useDebateStore((s) => s.activeMode);
  const failedAtStage = useDebateStore((s) => s.failedAtStage);
  const isReplay = useDebateStore((s) => s.isReplay);
  const context = useDebateStore((s) => s.context);
  const agents = useDebateStore((s) => s.agents);
  const consensus = useDebateStore((s) => s.consensus);
  const error = useDebateStore((s) => s.error);
  const processingTimeMs = useDebateStore((s) => s.processingTimeMs);
  const questionText = useDebateStore((s) => s.questionText);

  const isIdle = stage === "IDLE";
  const isLive = !isReplay && !isIdle && stage !== "COMPLETED" && stage !== "FAILED";

  return (
    <div className="flex h-full flex-col">
      <div className="flex items-center justify-between border-b px-4 py-3">
        <h2 className="text-sm font-semibold">Live debate</h2>
        {isLive && (
          <Badge variant="info" className="animate-pulse-soft">
            <Radio className="h-3 w-3" />
            streaming
          </Badge>
        )}
        {isReplay && (
          <Badge variant="secondary">
            <History className="h-3 w-3" />
            replay
          </Badge>
        )}
        {!isIdle && activeMode === "FAST" && (
          <Badge variant="warning">
            <Zap className="h-3 w-3" />
            fast
          </Badge>
        )}
        {processingTimeMs != null && !isLive && (
          <Badge variant="secondary">
            <Timer className="h-3 w-3" />
            {formatDuration(processingTimeMs)}
          </Badge>
        )}
      </div>

      <div className="scrollbar-thin flex-1 space-y-5 overflow-y-auto p-4">
        {isIdle ? (
          <div className="flex h-full flex-col items-center justify-center text-center">
            <Radio className="mb-3 h-8 w-8 text-muted-foreground/50" />
            <p className="text-sm font-medium text-muted-foreground">No debate running</p>
            <p className="mt-1 max-w-[240px] text-xs text-muted-foreground">
              Ask a question about your document and watch the three agents debate it here in
              real time.
            </p>
          </div>
        ) : (
          <>
            {questionText && (
              <p className="rounded-md bg-muted px-3 py-2 text-xs font-medium text-foreground/80">
                {questionText}
              </p>
            )}

            <section>
              <h3 className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                Pipeline
              </h3>
              <StageTimeline
                stage={stage === "FAILED" && failedAtStage ? failedAtStage : stage}
                failed={stage === "FAILED"}
                mode={activeMode}
              />
              {error && (
                <p className="mt-2 rounded-md border border-destructive/30 bg-destructive/5 px-3 py-2 text-xs text-destructive">
                  {error}
                </p>
              )}
            </section>

            {context.length > 0 && (
              <>
                <Separator />
                <section>
                  <h3 className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                    Retrieved context · {context.length} chunks
                  </h3>
                  <ContextSnippets snippets={context} />
                </section>
              </>
            )}

            <Separator />
            <section>
              <h3 className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                Agents
              </h3>
              <div className="space-y-2.5">
                {AGENT_ORDER.map((agent) => (
                  <AgentCard key={agent} agent={agent} slot={agents[agent]} stage={stage} />
                ))}
              </div>
            </section>

            {consensus && (
              <>
                <Separator />
                <section>
                  <ConsensusCard consensus={consensus} />
                </section>
              </>
            )}
          </>
        )}
      </div>
    </div>
  );
}
