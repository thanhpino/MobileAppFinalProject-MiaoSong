export interface DomainEvent {
  readonly type: string;
  readonly occurredAt: Date;
  readonly payload: Record<string, unknown>;
}

export type EventHandler<T extends DomainEvent = DomainEvent> = (event: T) => Promise<void>;

/**
 * In-process EventBus cho Modular Monolith.
 * Khi cần tách microservice: thay lớp này bằng Cloud Pub/Sub adapter, giữ nguyên logic module.
 */
export class InProcessEventBus {
  private readonly handlers = new Map<string, EventHandler[]>();

  subscribe<T extends DomainEvent>(eventType: T["type"], handler: EventHandler<T>): void {
    const list = this.handlers.get(eventType) ?? [];
    list.push(handler as EventHandler);
    this.handlers.set(eventType, list);
  }

  async publish(event: DomainEvent): Promise<void> {
    const list = this.handlers.get(event.type) ?? [];
    for (const handler of list) {
      await handler(event);
    }
  }
}

export const eventBus = new InProcessEventBus();
