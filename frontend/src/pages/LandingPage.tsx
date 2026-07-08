import { Link } from "react-router-dom";
import {
  ArrowRight,
  Database,
  FileText,
  GitMerge,
  Network,
  ScanSearch,
  Swords,
  Upload,
  Zap,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { AGENT_ORDER, AGENTS } from "@/lib/agents";

const pipeline = [
  { icon: Upload, title: "Ingest", text: "PDF parsed with Apache Tika, split into overlapping chunks." },
  { icon: Database, title: "Index", text: "Chunks embedded in-process (ONNX MiniLM) into pgvector." },
  { icon: ScanSearch, title: "Retrieve", text: "Question embedded and matched by cosine similarity." },
  { icon: Swords, title: "Debate", text: "Three agents answer, cross-critique, and revise over 3 rounds." },
  { icon: GitMerge, title: "Converge", text: "Consensus engine synthesizes one answer with a confidence score." },
];

const problems = [
  {
    title: "Single-model RAG hallucinates",
    text: "One LLM interpreting your document means one point of failure — invented facts slip through unchecked.",
  },
  {
    title: "Adversarial critique catches it",
    text: "Every claim is cross-examined against the retrieved context by a dedicated fact-checking agent.",
  },
  {
    title: "Consensus you can calibrate on",
    text: "Agreements, disagreements, and a 0–100 confidence score — not just an answer, but how much to trust it.",
  },
];

export default function LandingPage() {
  return (
    <div className="mx-auto max-w-[1200px] px-4 lg:px-6">
      {/* Hero */}
      <section className="flex flex-col items-center pb-16 pt-20 text-center">
        <Badge variant="secondary" className="mb-5">
          <Zap className="h-3 w-3" />
          Document-grounded multi-agent RAG
        </Badge>
        <h1 className="max-w-3xl text-4xl font-bold tracking-tight sm:text-6xl">
          Multiple AI minds.
          <br />
          <span className="bg-gradient-to-r from-indigo-600 via-violet-600 to-cyan-600 bg-clip-text text-transparent">
            One trusted answer.
          </span>
        </h1>
        <p className="mt-6 max-w-2xl text-lg text-muted-foreground">
          Upload a document and ask anything. Three independent AI agents answer from the
          retrieved context, critique each other in structured debate rounds, and converge on a
          consensus answer — with every claim cited back to your document.
        </p>
        <div className="mt-8 flex gap-3">
          <Button asChild size="lg">
            <Link to="/workspace">
              Upload a document to start
              <ArrowRight className="h-4 w-4" />
            </Link>
          </Button>
          <Button asChild size="lg" variant="outline">
            <Link to="/dashboard">View analytics</Link>
          </Button>
        </div>
      </section>

      {/* Why */}
      <section className="grid gap-4 pb-16 md:grid-cols-3">
        {problems.map((item) => (
          <Card key={item.title}>
            <CardContent className="pt-5">
              <h3 className="mb-1.5 font-semibold">{item.title}</h3>
              <p className="text-sm text-muted-foreground">{item.text}</p>
            </CardContent>
          </Card>
        ))}
      </section>

      {/* Agents */}
      <section className="pb-16">
        <h2 className="mb-2 text-center text-2xl font-bold tracking-tight">
          Meet the debate panel
        </h2>
        <p className="mx-auto mb-8 max-w-xl text-center text-muted-foreground">
          Three distinct free-tier models with three distinct jobs, orchestrated through
          answer → critique → revision rounds.
        </p>
        <div className="grid gap-4 md:grid-cols-3">
          {AGENT_ORDER.map((name) => {
            const agent = AGENTS[name];
            const Icon = agent.icon;
            return (
              <Card key={name} className={`border ${agent.colors.border}`}>
                <CardContent className="pt-5">
                  <div
                    className={`mb-3 flex h-10 w-10 items-center justify-center rounded-lg ${agent.colors.bg}`}
                  >
                    <Icon className={`h-5 w-5 ${agent.colors.text}`} />
                  </div>
                  <div className="mb-0.5 flex items-center gap-2">
                    <h3 className="font-semibold">{agent.displayName}</h3>
                    <Badge variant="outline" className="text-[10px]">
                      {agent.model}
                    </Badge>
                  </div>
                  <p className={`mb-2 text-xs font-medium ${agent.colors.text}`}>{agent.role}</p>
                  <p className="text-sm text-muted-foreground">{agent.tagline}</p>
                </CardContent>
              </Card>
            );
          })}
        </div>
      </section>

      {/* Architecture pipeline */}
      <section className="pb-20">
        <h2 className="mb-2 text-center text-2xl font-bold tracking-tight">How it works</h2>
        <p className="mx-auto mb-8 max-w-xl text-center text-muted-foreground">
          A structured RAG pipeline with real-time progress streamed over WebSockets at every
          stage.
        </p>
        <div className="grid gap-3 md:grid-cols-5">
          {pipeline.map((step, index) => (
            <div key={step.title} className="relative">
              <Card className="h-full">
                <CardContent className="pt-5">
                  <div className="mb-3 flex items-center gap-2">
                    <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-accent">
                      <step.icon className="h-4 w-4 text-accent-foreground" />
                    </span>
                    <span className="text-xs font-semibold text-muted-foreground">
                      {String(index + 1).padStart(2, "0")}
                    </span>
                  </div>
                  <h3 className="mb-1 text-sm font-semibold">{step.title}</h3>
                  <p className="text-xs text-muted-foreground">{step.text}</p>
                </CardContent>
              </Card>
              {index < pipeline.length - 1 && (
                <ArrowRight className="absolute -right-3 top-1/2 z-10 hidden h-4 w-4 -translate-y-1/2 text-muted-foreground md:block" />
              )}
            </div>
          ))}
        </div>
      </section>

      {/* Tech strip */}
      <section className="border-t pb-14 pt-10">
        <div className="flex flex-wrap items-center justify-center gap-x-8 gap-y-3 text-sm text-muted-foreground">
          <span className="flex items-center gap-1.5">
            <Network className="h-4 w-4" /> Java 21 · Spring Boot 3
          </span>
          <span className="flex items-center gap-1.5">
            <Database className="h-4 w-4" /> PostgreSQL · pgvector
          </span>
          <span className="flex items-center gap-1.5">
            <FileText className="h-4 w-4" /> LangChain4j · Apache Tika
          </span>
          <span className="flex items-center gap-1.5">
            <Zap className="h-4 w-4" /> Local ONNX embeddings · STOMP WebSockets
          </span>
        </div>
      </section>
    </div>
  );
}
