import type {
  AnalyticsSummary,
  ChunkDto,
  ConfigStatus,
  DocumentDto,
  QuestionDetail,
  QuestionSummary,
  SpringPage,
  SubmitQuestionRequest,
} from "./types";

export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export const WS_URL = `${API_BASE_URL}/ws`;

export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, init);
  if (!response.ok) {
    let detail = `Request failed with status ${response.status}`;
    try {
      // Backend errors use RFC 7807 problem+json with a human-readable `detail`.
      const body = (await response.json()) as { detail?: string; title?: string };
      detail = body.detail ?? body.title ?? detail;
    } catch {
      // non-JSON error body; keep the generic message
    }
    throw new ApiError(response.status, detail);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export const api = {
  uploadDocument(file: File): Promise<DocumentDto> {
    const form = new FormData();
    form.append("file", file);
    return request<DocumentDto>("/api/documents", { method: "POST", body: form });
  },

  listDocuments(): Promise<DocumentDto[]> {
    return request<DocumentDto[]>("/api/documents");
  },

  getDocument(id: string): Promise<DocumentDto> {
    return request<DocumentDto>(`/api/documents/${id}`);
  },

  getChunks(documentId: string, page: number, size: number): Promise<SpringPage<ChunkDto>> {
    return request<SpringPage<ChunkDto>>(
      `/api/documents/${documentId}/chunks?page=${page}&size=${size}`,
    );
  },

  deleteDocument(id: string): Promise<void> {
    return request<void>(`/api/documents/${id}`, { method: "DELETE" });
  },

  submitQuestion(body: SubmitQuestionRequest): Promise<QuestionSummary> {
    return request<QuestionSummary>("/api/questions", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
  },

  listQuestions(documentId?: string): Promise<QuestionSummary[]> {
    const query = documentId ? `?documentId=${documentId}` : "";
    return request<QuestionSummary[]>(`/api/questions${query}`);
  },

  getQuestion(id: string): Promise<QuestionDetail> {
    return request<QuestionDetail>(`/api/questions/${id}`);
  },

  analyticsSummary(): Promise<AnalyticsSummary> {
    return request<AnalyticsSummary>("/api/analytics/summary");
  },

  getConfig(): Promise<ConfigStatus> {
    return request<ConfigStatus>("/api/config");
  },
};
