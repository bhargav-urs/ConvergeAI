import ReactMarkdown from "react-markdown";
import { cn } from "@/lib/utils";

interface MarkdownContentProps {
  content: string;
  className?: string;
}

/** Renders agent/consensus markdown with chat-appropriate typography. */
export function MarkdownContent({ content, className }: MarkdownContentProps) {
  return (
    <div className={cn("prose-chat", className)}>
      <ReactMarkdown>{content}</ReactMarkdown>
    </div>
  );
}
