export interface PushPayload {
  title: string;
  body: string;
  channelId: "miaosong_new_order" | "miaosong_order_status" | "miaosong_late_alert";
  sound?: string;
  data: Record<string, string>;
}

export interface NotificationPort {
  sendToUsers(uids: string[], payload: PushPayload): Promise<void>;
}
