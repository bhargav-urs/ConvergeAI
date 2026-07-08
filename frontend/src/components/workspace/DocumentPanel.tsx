import { useCallback, useState } from "react";
import { useDropzone } from "react-dropzone";
import { formatDistanceToNow } from "date-fns";
import {
  FileText,
  Grid2x2,
  History as HistoryIcon,
  Loader2,
  Trash2,
  UploadCloud,
} from "lucide-react";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useDeleteDocument, useDocuments, useUploadDocument } from "@/hooks/useDocuments";
import { useQuestions } from "@/hooks/useQuestions";
import type { DocumentDto, DocumentStatus } from "@/lib/types";
import { cn, formatBytes, truncate } from "@/lib/utils";
import { ChunkViewerDialog } from "./ChunkViewerDialog";

const MAX_UPLOAD_BYTES = 20 * 1024 * 1024;

interface DocumentPanelProps {
  selectedDocumentId: string | null;
  onSelectDocument: (id: string | null) => void;
  onReplayQuestion: (questionId: string) => void;
}

function statusBadge(status: DocumentStatus) {
  switch (status) {
    case "READY":
      return <Badge variant="success">ready</Badge>;
    case "PROCESSING":
      return (
        <Badge variant="info">
          <Loader2 className="h-2.5 w-2.5 animate-spin" />
          indexing
        </Badge>
      );
    case "FAILED":
      return <Badge variant="destructive">failed</Badge>;
  }
}

export function DocumentPanel({
  selectedDocumentId,
  onSelectDocument,
  onReplayQuestion,
}: DocumentPanelProps) {
  const { data: documents, isLoading } = useDocuments();
  const upload = useUploadDocument();
  const deleteDocument = useDeleteDocument();
  const { data: questions } = useQuestions(selectedDocumentId ?? undefined);
  const [chunkViewerDoc, setChunkViewerDoc] = useState<DocumentDto | null>(null);

  const onDrop = useCallback(
    (accepted: File[]) => {
      for (const file of accepted) {
        if (file.size > MAX_UPLOAD_BYTES) {
          toast.error(`"${file.name}" exceeds the 20 MB limit`);
          continue;
        }
        upload.mutate(file);
      }
    },
    [upload],
  );

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      "application/pdf": [".pdf"],
      "text/plain": [".txt"],
      "text/markdown": [".md"],
    },
    multiple: true,
  });

  return (
    <div className="flex h-full flex-col">
      <div className="border-b px-4 py-3">
        <h2 className="text-sm font-semibold">Documents</h2>
      </div>

      <div className="p-3">
        <div
          {...getRootProps()}
          className={cn(
            "flex cursor-pointer flex-col items-center justify-center rounded-lg border-2 border-dashed px-4 py-6 text-center transition-colors",
            isDragActive
              ? "border-primary bg-accent"
              : "border-border hover:border-primary/50 hover:bg-muted/50",
          )}
        >
          <input {...getInputProps()} />
          <UploadCloud className="mb-2 h-6 w-6 text-muted-foreground" />
          <p className="text-xs font-medium">
            {isDragActive ? "Drop to upload" : "Drag a PDF here or click to browse"}
          </p>
          <p className="mt-0.5 text-[11px] text-muted-foreground">PDF, TXT, MD · up to 20 MB</p>
          {upload.isPending && (
            <p className="mt-2 flex items-center gap-1 text-[11px] text-primary">
              <Loader2 className="h-3 w-3 animate-spin" /> Uploading…
            </p>
          )}
        </div>
      </div>

      <Tabs defaultValue="documents" className="flex min-h-0 flex-1 flex-col px-3 pb-3">
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="documents" className="text-xs">
            <FileText className="mr-1 h-3.5 w-3.5" />
            Library
          </TabsTrigger>
          <TabsTrigger value="history" className="text-xs">
            <HistoryIcon className="mr-1 h-3.5 w-3.5" />
            History
          </TabsTrigger>
        </TabsList>

        <TabsContent value="documents" className="scrollbar-thin min-h-0 flex-1 overflow-y-auto">
          {isLoading && (
            <div className="space-y-2">
              {Array.from({ length: 3 }).map((_, i) => (
                <Skeleton key={i} className="h-16 w-full" />
              ))}
            </div>
          )}
          {documents?.length === 0 && (
            <p className="px-1 py-6 text-center text-xs text-muted-foreground">
              No documents yet. Upload one to start a debate.
            </p>
          )}
          <div className="space-y-1.5">
            {documents?.map((doc) => {
              const isSelected = doc.id === selectedDocumentId;
              return (
                <div
                  key={doc.id}
                  role="button"
                  tabIndex={0}
                  onClick={() => doc.status === "READY" && onSelectDocument(doc.id)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" && doc.status === "READY") onSelectDocument(doc.id);
                  }}
                  className={cn(
                    "group rounded-md border p-2.5 transition-colors",
                    doc.status === "READY" && "cursor-pointer hover:border-primary/40",
                    isSelected && "border-primary bg-accent/60",
                  )}
                >
                  <div className="flex items-start gap-2">
                    <FileText className="mt-0.5 h-4 w-4 shrink-0 text-muted-foreground" />
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-xs font-medium" title={doc.filename}>
                        {doc.filename}
                      </p>
                      <p className="mt-0.5 text-[11px] text-muted-foreground">
                        {formatBytes(doc.sizeBytes)}
                        {doc.status === "READY" && <> · {doc.chunkCount} chunks</>}
                        {" · "}
                        {formatDistanceToNow(new Date(doc.uploadedAt), { addSuffix: true })}
                      </p>
                      {doc.status === "FAILED" && doc.errorMessage && (
                        <p className="mt-1 text-[11px] text-destructive">
                          {truncate(doc.errorMessage, 120)}
                        </p>
                      )}
                    </div>
                    {statusBadge(doc.status)}
                  </div>
                  <div className="mt-1.5 flex items-center justify-end gap-1 opacity-0 transition-opacity group-hover:opacity-100">
                    {doc.status === "READY" && (
                      <Button
                        variant="ghost"
                        size="sm"
                        className="h-6 px-2 text-[11px]"
                        onClick={(e) => {
                          e.stopPropagation();
                          setChunkViewerDoc(doc);
                        }}
                      >
                        <Grid2x2 className="h-3 w-3" /> Chunks
                      </Button>
                    )}
                    <Button
                      variant="ghost"
                      size="sm"
                      className="h-6 px-2 text-[11px] text-destructive hover:text-destructive"
                      onClick={(e) => {
                        e.stopPropagation();
                        if (window.confirm(`Delete "${doc.filename}" and all its debates?`)) {
                          deleteDocument.mutate(doc.id, {
                            onSuccess: () => {
                              if (isSelected) onSelectDocument(null);
                            },
                          });
                        }
                      }}
                    >
                      <Trash2 className="h-3 w-3" /> Delete
                    </Button>
                  </div>
                </div>
              );
            })}
          </div>
        </TabsContent>

        <TabsContent value="history" className="scrollbar-thin min-h-0 flex-1 overflow-y-auto">
          {!selectedDocumentId && (
            <p className="px-1 py-6 text-center text-xs text-muted-foreground">
              Select a document to see its past debates.
            </p>
          )}
          {selectedDocumentId && questions?.length === 0 && (
            <p className="px-1 py-6 text-center text-xs text-muted-foreground">
              No questions asked about this document yet.
            </p>
          )}
          <div className="space-y-1.5">
            {selectedDocumentId &&
              questions?.map((question) => (
                <button
                  key={question.id}
                  type="button"
                  onClick={() => onReplayQuestion(question.id)}
                  className="w-full rounded-md border p-2.5 text-left transition-colors hover:border-primary/40"
                >
                  <p className="text-xs font-medium leading-4">
                    {truncate(question.questionText, 90)}
                  </p>
                  <div className="mt-1 flex items-center gap-2">
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
                    {question.mode === "FAST" && (
                      <Badge variant="warning" className="text-[10px]">
                        fast
                      </Badge>
                    )}
                    {question.confidenceScore != null && (
                      <span className="text-[11px] text-muted-foreground">
                        {question.confidenceScore}% confidence
                      </span>
                    )}
                  </div>
                </button>
              ))}
          </div>
        </TabsContent>
      </Tabs>

      <ChunkViewerDialog document={chunkViewerDoc} onClose={() => setChunkViewerDoc(null)} />
    </div>
  );
}
