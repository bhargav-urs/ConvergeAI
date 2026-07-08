import { Client, type IMessage, type StompSubscription } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { WS_URL } from "./api";
import type { DebateEvent } from "./types";

type EventHandler = (event: DebateEvent) => void;

/**
 * Singleton STOMP connection manager over SockJS.
 *
 * The client lazily activates on the first subscription, transparently
 * re-subscribes everything after a reconnect, and tears the socket down when
 * the last subscriber leaves.
 */
class StompManager {
  private client: Client | null = null;
  private subscriptions = new Map<
    string,
    { handlers: Set<EventHandler>; stompSub: StompSubscription | null }
  >();

  private ensureClient(): Client {
    if (this.client) {
      return this.client;
    }
    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL) as WebSocket,
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        // (Re)establish every registered subscription on connect/reconnect.
        for (const [destination, entry] of this.subscriptions) {
          entry.stompSub = client.subscribe(destination, (message) =>
            this.dispatch(destination, message),
          );
        }
      },
      onStompError: (frame) => {
        console.error("STOMP broker error:", frame.headers["message"]);
      },
    });
    client.activate();
    this.client = client;
    return client;
  }

  private dispatch(destination: string, message: IMessage): void {
    const entry = this.subscriptions.get(destination);
    if (!entry) return;
    try {
      const event = JSON.parse(message.body) as DebateEvent;
      for (const handler of entry.handlers) {
        handler(event);
      }
    } catch (e) {
      console.error("Failed to parse STOMP message", e);
    }
  }

  /** Subscribes to a destination; returns an unsubscribe function. */
  subscribe(destination: string, handler: EventHandler): () => void {
    const client = this.ensureClient();
    let entry = this.subscriptions.get(destination);
    if (!entry) {
      entry = { handlers: new Set(), stompSub: null };
      this.subscriptions.set(destination, entry);
      if (client.connected) {
        entry.stompSub = client.subscribe(destination, (message) =>
          this.dispatch(destination, message),
        );
      }
    }
    entry.handlers.add(handler);

    return () => {
      const current = this.subscriptions.get(destination);
      if (!current) return;
      current.handlers.delete(handler);
      if (current.handlers.size === 0) {
        current.stompSub?.unsubscribe();
        this.subscriptions.delete(destination);
        if (this.subscriptions.size === 0 && this.client) {
          void this.client.deactivate();
          this.client = null;
        }
      }
    };
  }

  /** Sends a payload to an application destination (client → server). */
  send(destination: string, body: unknown): void {
    const client = this.ensureClient();
    if (client.connected) {
      client.publish({ destination, body: JSON.stringify(body) });
    } else {
      console.warn("STOMP client not connected; message to", destination, "dropped");
    }
  }
}

export const stompManager = new StompManager();

export const topics = {
  debate: (questionId: string) => `/topic/debate/${questionId}`,
  documents: "/topic/documents",
};
