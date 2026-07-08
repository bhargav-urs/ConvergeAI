// Mirrors of the backend DTOs (com.convergeai.dto).

export type AgentName = "ANALYST" | "ENGINEER" | "REVIEWER";

export type DocumentStatus = "PROCESSING" | "READY" | "FAILED";

export type QuestionStatus =
  | "PENDING"
  | "RETRIEVING"
  | "DEBATING_ROUND_1"
  | "DEBATING_ROUND_2"
  | "DEBATING_ROUND_3"
  | "CONSENSUS"
  | "COMPLETED"
  | "FAILED";

export type AgentResponseStatus = "OK" | "FAILED";

export type DebateMode = "NORMAL" | "FAST";

export interface DocumentDto {
  id: string;
  filename: string;
  contentType: string | null;
  sizeBytes: number;
  status: DocumentStatus;
  chunkCount: number;
  charCount: number;
  errorMessage: string | null;
  uploadedAt: string;
}

export interface ChunkDto {
  id: string;
  chunkIndex: number;
  content: string;
  charCount: number;
}

export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ContextSnippet {
  chunkId: string;
  chunkIndex: number;
  rank: number;
  content: string;
  distance: number;
}

export interface AgentMessage {
  agent: AgentName;
  displayName: string;
  model: string;
  round: number;
  content: string | null;
  critiqueReceived: string | null;
  status: AgentResponseStatus;
  errorMessage: string | null;
  latencyMs: number | null;
}

export interface Consensus {
  finalAnswer: string;
  agreementPoints: string[] | null;
  disagreementPoints: string[] | null;
  confidenceScore: number;
  model: string;
}

export interface AgentDebate {
  agent: AgentName;
  displayName: string;
  roleDescription: string;
  model: string;
  initialResponse: AgentMessage | null;
  critique: AgentMessage | null;
  revision: AgentMessage | null;
}

export interface QuestionSummary {
  id: string;
  documentId: string;
  documentFilename: string;
  questionText: string;
  status: QuestionStatus;
  mode: DebateMode;
  processingTimeMs: number | null;
  confidenceScore: number | null;
  finalAnswerPreview: string | null;
  createdAt: string;
}

export interface QuestionDetail {
  id: string;
  documentId: string;
  documentFilename: string;
  questionText: string;
  status: QuestionStatus;
  mode: DebateMode;
  errorMessage: string | null;
  processingTimeMs: number | null;
  createdAt: string;
  completedAt: string | null;
  context: ContextSnippet[];
  agents: AgentDebate[];
  consensus: Consensus | null;
}

export interface AgentStats {
  agent: AgentName;
  displayName: string;
  model: string;
  debatesParticipated: number;
  avgStabilityScore: number | null;
  avgInitialLatencyMs: number | null;
}

export interface AnalyticsSummary {
  totalDocuments: number;
  totalQuestions: number;
  completedQuestions: number;
  failedQuestions: number;
  avgProcessingTimeMs: number | null;
  avgConfidenceScore: number | null;
  agentStats: AgentStats[];
}

export interface SubmitQuestionRequest {
  documentId: string;
  questionText: string;
  mode: DebateMode;
}

export interface ConfigStatus {
  anyProviderConfigured: boolean;
  openRouterConfigured: boolean;
  directProviders: string[];
  models: {
    analyst: string;
    engineer: string;
    reviewer: string;
    consensus: string;
  };
}

// ---- WebSocket events -------------------------------------------------------

export type DebateEventType =
  | "document.indexed"
  | "document.failed"
  | "stage.changed"
  | "context.retrieved"
  | "agent.response"
  | "agent.critique"
  | "agent.revision"
  | "consensus.generated"
  | "debate.completed"
  | "debate.error"
  | "question.accepted";

export interface DebateEvent<T = unknown> {
  type: DebateEventType;
  questionId: string | null;
  timestamp: string;
  payload: T;
}

export interface StageChangedPayload {
  stage: QuestionStatus;
}

export interface DebateCompletedPayload {
  processingTimeMs: number;
  status: QuestionStatus;
}

export interface DebateErrorPayload {
  error: string;
  processingTimeMs?: number;
}

export interface DocumentIndexedPayload {
  documentId: string;
  chunkCount: number;
  elapsedMs: number;
}

export interface DocumentFailedPayload {
  documentId: string;
  error: string;
}
