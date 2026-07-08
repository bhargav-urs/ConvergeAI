import { useState } from "react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip as ChartTooltip,
  XAxis,
  YAxis,
} from "recharts";
import { format } from "date-fns";
import {
  CheckCircle2,
  FileText,
  Gauge,
  HelpCircle,
  MessagesSquare,
  Timer,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAnalytics } from "@/hooks/useAnalytics";
import { useQuestions } from "@/hooks/useQuestions";
import { AGENTS } from "@/lib/agents";
import { formatDuration, truncate } from "@/lib/utils";
import { QuestionDetailDialog } from "@/components/dashboard/QuestionDetailDialog";

export default function DashboardPage() {
  const { data: analytics, isLoading: analyticsLoading } = useAnalytics();
  const { data: questions, isLoading: questionsLoading } = useQuestions();
  const [openQuestionId, setOpenQuestionId] = useState<string | null>(null);

  const chartData =
    analytics?.agentStats.map((stat) => ({
      name: AGENTS[stat.agent].displayName,
      stability:
        stat.avgStabilityScore == null
          ? 0
          : Math.round(stat.avgStabilityScore * 100),
      hasData: stat.avgStabilityScore != null,
      fill: AGENTS[stat.agent].chartColor,
      debates: stat.debatesParticipated,
    })) ?? [];

  const statCards = [
    {
      label: "Documents indexed",
      value: analytics?.totalDocuments,
      icon: FileText,
    },
    {
      label: "Questions debated",
      value: analytics?.totalQuestions,
      icon: MessagesSquare,
    },
    {
      label: "Completed debates",
      value: analytics?.completedQuestions,
      icon: CheckCircle2,
    },
    {
      label: "Avg processing time",
      value:
        analytics?.avgProcessingTimeMs != null
          ? formatDuration(Math.round(analytics.avgProcessingTimeMs))
          : "—",
      icon: Timer,
    },
    {
      label: "Avg confidence",
      value:
        analytics?.avgConfidenceScore != null
          ? `${Math.round(analytics.avgConfidenceScore)}%`
          : "—",
      icon: Gauge,
    },
  ];

  return (
    <div className="mx-auto max-w-[1200px] space-y-6 px-4 py-8 lg:px-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">History & Analytics</h1>
        <p className="text-sm text-muted-foreground">
          Every debate, its consensus, and how each agent held up under critique.
        </p>
      </div>

      {/* Stat cards */}
      <div className="grid grid-cols-2 gap-4 md:grid-cols-5">
        {statCards.map((stat) => (
          <Card key={stat.label}>
            <CardContent className="pt-5">
              <div className="flex items-center gap-2 text-muted-foreground">
                <stat.icon className="h-4 w-4" />
                <span className="text-xs">{stat.label}</span>
              </div>
              {analyticsLoading ? (
                <Skeleton className="mt-2 h-7 w-16" />
              ) : (
                <p className="mt-1 text-2xl font-bold">{stat.value ?? 0}</p>
              )}
            </CardContent>
          </Card>
        ))}
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_1.4fr]">
        {/* Agent stability chart */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              Answer stability by agent
              <span title="Word-level similarity between an agent's round-1 and round-3 answers. Higher = the initial answer needed fewer revisions after peer critique.">
                <HelpCircle className="h-3.5 w-3.5 text-muted-foreground" />
              </span>
            </CardTitle>
            <CardDescription>
              Higher = initial answer required the fewest revisions after critique
            </CardDescription>
          </CardHeader>
          <CardContent>
            {analyticsLoading ? (
              <Skeleton className="h-56 w-full" />
            ) : chartData.every((d) => !d.hasData) ? (
              <div className="flex h-56 items-center justify-center text-sm text-muted-foreground">
                Complete a debate to see agent metrics.
              </div>
            ) : (
              <ResponsiveContainer width="100%" height={224}>
                <BarChart data={chartData} margin={{ top: 8, right: 8, left: -18, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e5e7eb" />
                  <XAxis dataKey="name" tick={{ fontSize: 11 }} axisLine={false} tickLine={false} />
                  <YAxis
                    domain={[0, 100]}
                    tick={{ fontSize: 11 }}
                    axisLine={false}
                    tickLine={false}
                    unit="%"
                  />
                  <ChartTooltip
                    cursor={{ fill: "rgba(0,0,0,0.04)" }}
                    formatter={(value: number | string) => [`${value}% stability`, ""]}
                    labelFormatter={(label) => String(label)}
                  />
                  <Bar dataKey="stability" radius={[6, 6, 0, 0]} maxBarSize={56}>
                    {chartData.map((entry) => (
                      <Cell key={entry.name} fill={entry.fill} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            )}
            {/* Per-agent quick stats */}
            <div className="mt-4 grid grid-cols-3 gap-2">
              {analytics?.agentStats.map((stat) => (
                <div key={stat.agent} className="rounded-md border p-2 text-center">
                  <p className="truncate text-[11px] font-medium">
                    {AGENTS[stat.agent].displayName}
                  </p>
                  <p className="text-[11px] text-muted-foreground">
                    {stat.debatesParticipated} debates
                  </p>
                  <p className="text-[11px] text-muted-foreground">
                    {stat.avgInitialLatencyMs != null
                      ? `~${formatDuration(Math.round(stat.avgInitialLatencyMs))} first answer`
                      : "no data"}
                  </p>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* History table */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Recent debates</CardTitle>
            <CardDescription>Click a row to inspect the full debate</CardDescription>
          </CardHeader>
          <CardContent>
            {questionsLoading ? (
              <div className="space-y-2">
                {Array.from({ length: 5 }).map((_, i) => (
                  <Skeleton key={i} className="h-12 w-full" />
                ))}
              </div>
            ) : questions?.length === 0 ? (
              <div className="flex h-40 items-center justify-center text-sm text-muted-foreground">
                No debates yet — ask a question in the workspace.
              </div>
            ) : (
              <div className="scrollbar-thin max-h-[420px] space-y-1.5 overflow-y-auto pr-1">
                {questions?.map((question) => (
                  <button
                    key={question.id}
                    type="button"
                    onClick={() => setOpenQuestionId(question.id)}
                    className="w-full rounded-md border p-3 text-left transition-colors hover:border-primary/40 hover:bg-muted/40"
                  >
                    <div className="flex items-start justify-between gap-3">
                      <p className="min-w-0 flex-1 text-sm font-medium leading-5">
                        {truncate(question.questionText, 110)}
                      </p>
                      <span className="flex shrink-0 items-center gap-1">
                        {question.mode === "FAST" && (
                          <Badge variant="warning" className="text-[10px]">
                            fast
                          </Badge>
                        )}
                        <Badge
                          variant={
                            question.status === "COMPLETED"
                              ? "success"
                              : question.status === "FAILED"
                                ? "destructive"
                                : "info"
                          }
                          className="text-[10px]"
                        >
                          {question.status.toLowerCase().replaceAll("_", " ")}
                        </Badge>
                      </span>
                    </div>
                    {question.finalAnswerPreview && (
                      <p className="mt-1 text-xs text-muted-foreground">
                        {truncate(question.finalAnswerPreview, 140)}
                      </p>
                    )}
                    <div className="mt-1.5 flex items-center gap-3 text-[11px] text-muted-foreground">
                      <span>{question.documentFilename}</span>
                      <span>{format(new Date(question.createdAt), "MMM d, HH:mm")}</span>
                      {question.processingTimeMs != null && (
                        <span>{formatDuration(question.processingTimeMs)}</span>
                      )}
                      {question.confidenceScore != null && (
                        <span>{question.confidenceScore}% confidence</span>
                      )}
                    </div>
                  </button>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      <QuestionDetailDialog
        questionId={openQuestionId}
        onClose={() => setOpenQuestionId(null)}
      />
    </div>
  );
}
