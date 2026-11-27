import { db, messaging } from "../../../shared/firebase";
import { NotificationPort, PushPayload } from "../ports/notificationPort";

export class FcmNotificationAdapter implements NotificationPort {
  async sendToUsers(uids: string[], payload: PushPayload): Promise<void> {
    const uniqueUids = [...new Set(uids.filter((u) => typeof u === "string" && u.length > 0))];
    if (uniqueUids.length === 0) return;

    // Gửi theo từng uid kèm targetUid + data-only để MiaoMessagingService.onMessageReceived()
    // luôn lọc đúng vai trò (targetRole) & tài khoản (targetUid) ở cả foreground lẫn background.
    for (const uid of uniqueUids) {
      const snap = await db.collection("users").doc(uid).get();
      const userTokens = snap.data()?.fcmTokens;
      if (!Array.isArray(userTokens)) continue;
      const tokens = [...new Set(userTokens.filter((t): t is string => typeof t === "string" && t.length > 0))];
      if (tokens.length === 0) continue;

      await messaging.sendEachForMulticast({
        tokens,
        android: {
          priority: "high",
        },
        data: {
          ...payload.data,
          title: payload.title,
          body: payload.body,
          channelId: payload.channelId,
          sound: payload.sound ?? "default",
          targetUid: uid,
        },
      });
    }
  }
}
