import { create } from "zustand";
import type {
  AgentMessage,
  AgentName,
  Consensus,
  ContextSnippet,
  DebateCompletedPayload,
  DebateErrorPayload,
  DebateEvent,
  DebateMode,
  QuestionDetail,
  QuestionStatus,
  StageChangedPayload,
} from "@/lib/types";

export interface AgentSlot {
  response: AgentMessage | null;
  critique: AgentMessage | null;
  revision: AgentMessage | null;
}

export interface ChatEntry {
  id: string;
  role: "user" | "consensus" | "error";
  questionId: string;
  text: string;
  consensus?: Consensus;
  timestamp: number;
}

interface DebateState {
  /** Question currently shown in the debate panel (live or replayed). */
  activeQuestionId: string | null;
  questionText: string | null;
  stage: QuestionStatus | "IDLE";
  /** Mode of the active/replayed debate (drives the stage timeline shape). */
  activeMode: DebateMode;
  /** User's mode preference for the next question. */
  preferredMode: DebateMode;
  /** True when the active question is being replayed from history, not streamed. */
  isReplay: boolean;
  context: ContextSnippet[];
  agents: Record<AgentName, AgentSlot>;
  consensus: Consensus | null;
  error: string | null;
  /** Pipeline stage the debate was in when it failed (for the timeline marker). */
  failedAtStage: QuestionStatus | null;
  processingTimeMs: number | null;
  chat: ChatEntry[];

  startDebate: (questionId: string, questionText: string, mode: DebateMode) => void;
  setPreferredMode: (mode: DebateMode) => void;
  handleEvent: (event: DebateEvent) => void;
  hydrateFromDetail: (detail: QuestionDetail, opts?: { replay?: boolean }) => void;
  reset: () => void;
}

const emptyAgents = (): Record<AgentName, AgentSlot> => ({
  ANALYST: { response: null, critique: null, revision: null },
  ENGINEER: { response: null, critique: null, revision: null },
  REVIEWER: { response: null, critique: null, revision: null },
});

let chatEntrySeq = 0;
const nextChatId = () => `chat-${Date.now()}-${chatEntrySeq++}`;

export const useDebateStore = create<DebateState>((set, get) => ({
  activeQuestionId: null,
  questionText: null,
  stage: "IDLE",
  activeMode: "NORMAL",
  preferredMode: "NORMAL",
  isReplay: false,
  context: [],
  agents: emptyAgents(),
  consensus: null,
  error: null,
  failedAtStage: null,
  processingTimeMs: null,
  chat: [],

  setPreferredMode: (mode) => set({ preferredMode: mode }),

  startDebate: (questionId, questionText, mode) =>
    set((state) => ({
      activeQuestionId: questionId,
      questionText,
      stage: "PENDING",
      activeMode: mode,
      isReplay: false,
      context: [],
      agents: emptyAgents(),
      consensus: null,
      error: null,
      failedAtStage: null,
      processingTimeMs: null,
      chat: [
        ...state.chat,
        {
          id: nextChatId(),
          role: "user",
          questionId,
          text: questionText,
          timestamp: Date.now(),
        },
      ],
    })),

  handleEvent: (event) => {
    const { activeQuestionId } = get();
    // Ignore events for a question that is no longer active (e.g. user replayed
    // a historical question while an old debate is still streaming).
    if (!event.questionId || event.questionId !== activeQuestionId) {
      return;
    }
    switch (event.type) {
      case "stage.changed": {
        const payload = event.payload as StageChangedPayload;
        set({ stage: payload.stage });
        break;
      }
      case "context.retrieved": {
        set({ context: event.payload as ContextSnippet[] });
        break;
      }
      case "agent.response":
      case "agent.critique":
      case "agent.revision": {
        const message = event.payload as AgentMessage;
        set((state) => {
          const slot = { ...state.agents[message.agent] };
          if (event.type === "agent.response") slot.response = message;
          if (event.type === "agent.critique") slot.critique = message;
          if (event.type === "agent.revision") slot.revision = message;
          return { agents: { ...state.agents, [message.agent]: slot } };
        });
        break;
      }
      case "consensus.generated": {
        set({ consensus: event.payload as Consensus });
        break;
      }
      case "debate.completed": {
        const payload = event.payload as DebateCompletedPayload;
        const consensus = get().consensus;
        set((state) => ({
          stage: "COMPLETED",
          processingTimeMs: payload.processingTimeMs,
          chat: consensus
            ? [
                ...state.chat,
                {
                  id: nextChatId(),
                  role: "consensus",
                  questionId: event.questionId as string,
                  text: consensus.finalAnswer,
                  consensus,
                  timestamp: Date.now(),
                },
              ]
            : state.chat,
        }));
        break;
      }
      case "debate.error": {
        const payload = event.payload as DebateErrorPayload;
        const stageAtFailure = get().stage;
        set((state) => ({
          stage: "FAILED",
          failedAtStage:
            stageAtFailure !== "IDLE" && stageAtFailure !== "FAILED" ? stageAtFailure : null,
          error: payload.error,
          processingTimeMs: payload.processingTimeMs ?? null,
          chat: [
            ...state.chat,
            {
              id: nextChatId(),
              role: "error",
              questionId: event.questionId as string,
              text: payload.error,
              timestamp: Date.now(),
            },
          ],
        }));
        break;
      }
      default:
        break;
    }
  },

  /**
   * Fills the debate panel from a REST detail fetch. Used both to recover
   * events that fired before the STOMP subscription landed and to replay
   * historical debates from the dashboard/history list.
   */
  hydrateFromDetail: (detail, opts) => {
    const agents = emptyAgents();
    for (const agentDebate of detail.agents) {
      agents[agentDebate.agent] = {
        response: agentDebate.initialResponse,
        critique: agentDebate.critique,
        revision: agentDebate.revision,
      };
    }
    // For failed debates, infer which pipeline stage broke from what data exists,
    // so the timeline marks the failure point instead of showing all stages done.
    let failedAtStage: QuestionStatus | null = null;
    if (detail.status === "FAILED") {
      if (detail.context.length === 0) {
        failedAtStage = "RETRIEVING";
      } else if (
        !detail.agents.some((a) => a.initialResponse && a.initialResponse.status === "OK")
      ) {
        failedAtStage = "DEBATING_ROUND_1";
      } else {
        failedAtStage = "CONSENSUS";
      }
    }
    const isReplay = opts?.replay ?? false;
    set((state) => {
      // Live-mode hydration of an already-finished debate (e.g. page refresh or
      // events missed before the subscription landed): rebuild the chat entries
      // the WebSocket handler would have produced, without duplicating any.
      let chat = state.chat;
      if (!isReplay && (detail.status === "COMPLETED" || detail.status === "FAILED")) {
        const has = (role: ChatEntry["role"]) =>
          chat.some((e) => e.questionId === detail.id && e.role === role);
        const additions: ChatEntry[] = [];
        if (!has("user")) {
          additions.push({
            id: nextChatId(),
            role: "user",
            questionId: detail.id,
            text: detail.questionText,
            timestamp: Date.now(),
          });
        }
        if (detail.status === "COMPLETED" && detail.consensus && !has("consensus")) {
          additions.push({
            id: nextChatId(),
            role: "consensus",
            questionId: detail.id,
            text: detail.consensus.finalAnswer,
            consensus: detail.consensus,
            timestamp: Date.now(),
          });
        }
        if (detail.status === "FAILED" && detail.errorMessage && !has("error")) {
          additions.push({
            id: nextChatId(),
            role: "error",
            questionId: detail.id,
            text: detail.errorMessage,
            timestamp: Date.now(),
          });
        }
        if (additions.length > 0) {
          chat = [...chat, ...additions];
        }
      }
      return {
        activeQuestionId: detail.id,
        questionText: detail.questionText,
        stage: detail.status,
        activeMode: detail.mode ?? "NORMAL",
        isReplay,
        context: detail.context,
        agents,
        consensus: detail.consensus,
        error: detail.errorMessage,
        failedAtStage,
        processingTimeMs: detail.processingTimeMs,
        chat,
      };
    });
  },

  reset: () =>
    set({
      activeQuestionId: null,
      questionText: null,
      stage: "IDLE",
      activeMode: "NORMAL",
      isReplay: false,
      context: [],
      agents: emptyAgents(),
      consensus: null,
      error: null,
      failedAtStage: null,
      processingTimeMs: null,
    }),
}));
