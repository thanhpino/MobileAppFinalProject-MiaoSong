import { db, messaging } from "../../../shared/firebase";
import { NotificationPort, PushPayload } from "../ports/notificationPort";

export class FcmNotificationAdapter implements NotificationPort {
  async sendToUsers(uids: string[], payload: PushPayload): Promise<void> {
    const tokens: string[] = [];
    for (const uid of [...new Set(uids)]) {
      const snap = await db.collection("users").doc(uid).get();
      const userTokens = snap.data()?.fcmTokens;
      if (Array.isArray(userTokens)) {
        tokens.push(...userTokens.filter((t): t is string => typeof t === "string" && t.length > 0));
      }
    }
    if (tokens.length === 0) return;

    await messaging.sendEachForMulticast({
      tokens,
      android: {
        priority: "high",
        notification: {
          channelId: payload.channelId,
          sound: payload.sound ?? "default",
        },
      },
      notification: {
        title: payload.title,
        body: payload.body,
      },
      data: payload.data,
    });
  }
}
