/**
 * Shweep monetization backend.
 *
 * Optional, future-facing backend that:
 *  - authenticates users (Google on Android, Apple on iOS via Firebase Auth),
 *  - owns the authoritative daily sheep quota in Firestore,
 *  - mirrors the RevenueCat `unlimited_sheep` entitlement via webhook.
 *
 * RevenueCat is the purchase source of truth. Firestore is the quota source of
 * truth. The client never writes authoritative state.
 */

import { onCall, HttpsError, CallableRequest } from "firebase-functions/v2/https";
import { onRequest } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import { logger } from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();

const REVENUECAT_SECRET_API_KEY = defineSecret("REVENUECAT_SECRET_API_KEY");
const REVENUECAT_WEBHOOK_AUTH = defineSecret("REVENUECAT_WEBHOOK_AUTH");

const ENTITLEMENT_ID = "unlimited_sheep";
const DAILY_LIMIT = 50;
const RESET_HOUR_UTC = 12;

interface QuotaStatus {
  unlimited: boolean;
  periodStart: number;
  periodEnd: number;
  limit: number;
  used: number;
  remaining: number;
  exhaustedAt: number | null;
}

function requireUid(request: CallableRequest): string {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "Sign-in required.");
  }
  return uid;
}

function periodBounds(now: Date): { start: Date; end: Date } {
  const start = new Date(Date.UTC(
    now.getUTCFullYear(),
    now.getUTCMonth(),
    now.getUTCDate(),
    RESET_HOUR_UTC,
    0,
    0,
    0,
  ));
  if (now.getTime() < start.getTime()) {
    start.setUTCDate(start.getUTCDate() - 1);
  }
  const end = new Date(start.getTime() + 24 * 60 * 60 * 1000);
  return { start, end };
}

function periodId(uid: string, start: Date): string {
  return `${uid}_${start.getTime()}`;
}

function emptyStatus(unlimited: boolean, now: Date): QuotaStatus {
  const { start, end } = periodBounds(now);
  return {
    unlimited,
    periodStart: start.getTime(),
    periodEnd: end.getTime(),
    limit: DAILY_LIMIT,
    used: 0,
    remaining: unlimited ? Number.MAX_SAFE_INTEGER : DAILY_LIMIT,
    exhaustedAt: null,
  };
}

async function isUnlimited(uid: string): Promise<boolean> {
  const snap = await db.collection("entitlements").doc(uid).get();
  return snap.data()?.unlimitedSheepActive === true;
}

/** Registers a random per-installation id for diagnostics and idempotency. */
export const registerInstallation = onCall(async (request) => {
  const uid = requireUid(request);
  const installationId = String(request.data?.installationId ?? "");
  const platform = String(request.data?.platform ?? "unknown");
  const appVersion = String(request.data?.appVersion ?? "unknown");

  if (installationId.length < 8 || installationId.length > 64) {
    throw new HttpsError("invalid-argument", "Invalid installationId.");
  }

  const now = admin.firestore.FieldValue.serverTimestamp();
  await db.collection("installations").doc(installationId).set(
    { uid, platform, appVersion, lastSeenAt: now, createdAt: now },
    { merge: true },
  );
  await db.collection("users").doc(uid).set(
    { platform, lastSeenAt: now },
    { merge: true },
  );

  return { ok: true };
});

/** Returns the current quota status for the signed-in user. */
export const getQuotaStatus = onCall(async (request) => {
  const uid = requireUid(request);
  const now = new Date();
  const unlimited = await isUnlimited(uid);
  if (unlimited) {
    return emptyStatus(true, now);
  }

  const { start } = periodBounds(now);
  const snap = await db
    .collection("quotaPeriods")
    .doc(periodId(uid, start))
    .get();
  const data = snap.data();

  if (!data) {
    return emptyStatus(false, now);
  }

  const used = Number(data.usedCount ?? 0);
  const exhaustedAt = data.exhaustedAt ? data.exhaustedAt.toMillis() : null;
  return {
    unlimited: false,
    periodStart: start.getTime(),
    periodEnd: start.getTime() + 24 * 60 * 60 * 1000,
    limit: DAILY_LIMIT,
    used,
    remaining: Math.max(0, DAILY_LIMIT - used),
    exhaustedAt,
  } satisfies QuotaStatus;
});

/**
 * Atomically consumes one sheep for the signed-in user.
 *
 * Idempotent per `requestId`: retries with the same id return the prior result
 * instead of consuming twice.
 */
export const consumeSheep = onCall(async (request) => {
  const uid = requireUid(request);
  const requestId = String(request.data?.requestId ?? "");
  if (requestId.length < 8 || requestId.length > 64) {
    throw new HttpsError("invalid-argument", "Invalid requestId.");
  }

  const now = new Date();
  if (await isUnlimited(uid)) {
    return { result: "unlimited", status: emptyStatus(true, now) };
  }

  const { start } = periodBounds(now);
  const quotaRef = db.collection("quotaPeriods").doc(periodId(uid, start));
  const requestRef = db.collection("quotaRequests").doc(`${uid}_${requestId}`);

  return db.runTransaction(async (tx) => {
    const prior = await tx.get(requestRef);
    if (prior.exists) {
      const cached = prior.data();
      return { result: cached?.result ?? "allowed", status: cached?.status };
    }

    const quotaSnap = await tx.get(quotaRef);
    const used = Number(quotaSnap.data()?.usedCount ?? 0);
    const allowed = used < DAILY_LIMIT;
    const nextUsed = allowed ? used + 1 : used;
    const exhaustedAt = !allowed || nextUsed >= DAILY_LIMIT ? now : null;

    const status: QuotaStatus = {
      unlimited: false,
      periodStart: start.getTime(),
      periodEnd: start.getTime() + 24 * 60 * 60 * 1000,
      limit: DAILY_LIMIT,
      used: nextUsed,
      remaining: Math.max(0, DAILY_LIMIT - nextUsed),
      exhaustedAt: exhaustedAt ? exhaustedAt.getTime() : null,
    };
    const result = allowed ? "allowed" : "exhausted";

    tx.set(
      quotaRef,
      {
        uid,
        periodStart: start.getTime(),
        limit: DAILY_LIMIT,
        usedCount: nextUsed,
        exhaustedAt: exhaustedAt
          ? admin.firestore.Timestamp.fromDate(exhaustedAt)
          : null,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      },
      { merge: true },
    );
    tx.set(requestRef, {
      uid,
      requestId,
      result,
      status,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    return { result, status };
  });
});

/**
 * Mirrors the RevenueCat entitlement.
 *
 * Verifies the configured authorization header, deduplicates by event id, then
 * fetches current customer state from RevenueCat and stores `isActive`.
 */
export const revenueCatWebhook = onRequest(
  { secrets: [REVENUECAT_SECRET_API_KEY, REVENUECAT_WEBHOOK_AUTH] },
  async (req, res) => {
    if (req.get("Authorization") !== REVENUECAT_WEBHOOK_AUTH.value()) {
      res.status(401).send("unauthorized");
      return;
    }

    const event = req.body?.event ?? {};
    const eventId = String(event.id ?? "");
    const appUserId = String(event.app_user_id ?? "");
    if (!eventId || !appUserId) {
      res.status(400).send("missing event id or app_user_id");
      return;
    }

    const eventRef = db.collection("revenueCatEvents").doc(eventId);
    if ((await eventRef.get()).exists) {
      res.status(200).send("duplicate");
      return;
    }

    // Fetch current state rather than reconstructing it from the event type.
    const response = await fetch(
      `https://api.revenuecat.com/v1/subscribers/${encodeURIComponent(appUserId)}`,
      { headers: { Authorization: `Bearer ${REVENUECAT_SECRET_API_KEY.value()}` } },
    );

    if (!response.ok) {
      logger.error("RevenueCat lookup failed", { status: response.status });
      res.status(502).send("revenuecat lookup failed");
      return;
    }

    const payload = (await response.json()) as {
      subscriber?: { entitlements?: Record<string, { expires_date?: string | null }> };
    };
    const entitlement = payload.subscriber?.entitlements?.[ENTITLEMENT_ID];
    const expires = entitlement?.expires_date ?? null;
    const active = Boolean(entitlement) && (!expires || new Date(expires) > new Date());

    await db.collection("entitlements").doc(appUserId).set(
      {
        unlimitedSheepActive: active,
        productId: UNLIMITED_SHEEP_PRODUCT_ID_FALLBACK,
        lastRevenueCatEventId: eventId,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      },
      { merge: true },
    );
    await eventRef.set({
      processedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    res.status(200).send("ok");
  },
);

const UNLIMITED_SHEEP_PRODUCT_ID_FALLBACK = "unlimited_sheep";
