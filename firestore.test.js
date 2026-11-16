const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_fin_123";
const BOB_UID = "bob_fin_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validProfilePayload(uid) {
  const now = new Date();
  return {
    userId: uid,
    displayName: "Alice Investor",
    subscriptionTier: "student",
    riskProfile: "moderate",
    alertThresholdPct: 2.5,
    notificationsEnabled: true,
    createdAt: now,
    updatedAt: now,
  };
}

function validHoldingPayload(uid) {
  const now = new Date();
  return {
    userId: uid,
    symbol: "NVDA",
    companyName: "NVIDIA Corp.",
    exchange: "NASDAQ",
    shares: 12.5,
    avgCostPrice: 128.4,
    currency: "USD",
    sector: "Semiconductors",
    createdAt: now,
    updatedAt: now,
  };
}

function validAlertPayload(uid) {
  const now = new Date();
  return {
    userId: uid,
    symbol: "NVDA",
    exchange: "NASDAQ",
    targetChangePct: 3.0,
    direction: "both",
    triggerMessage: "Watching 3% intraday swing",
    triggered: false,
    read: false,
    createdAt: now,
    updatedAt: now,
  };
}

function validReportPayload(uid) {
  const now = new Date();
  return {
    userId: uid,
    tierUsed: "adult",
    headline: "BOJ signals gradual yield normalization as Nikkei exports rally",
    region: "APAC",
    macroImpact: "Stronger USD/JPY lifts export-heavy Tokyo industrial and semiconductor names.",
    recommendedTicker: "7203.T",
    actionSignal: "ACCUMULATE",
    confidenceScore: 84,
    rationale: "FX tailwinds and global hybrid demand sustain margin expansion.",
    createdAt: now,
    updatedAt: now,
  };
}

function validWatchlistPayload(uid) {
  const now = new Date();
  return {
    userId: uid,
    symbol: "NVDA",
    companyName: "NVIDIA Corporation",
    exchange: "NASDAQ",
    divergenceTag: "DIVERGENCE_ALERT",
    divergenceAlertActive: true,
    divergenceConfidence: 87,
    aiCorrelationSummary: "Decoupling from historical NIKKEI & DAX semi correlation spread.",
    targetIndices: "NIKKEI 225, DAX 40, BSE SENSEX",
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user: cannot read or write profiles or holdings", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("holdings").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID)));
});

test("Owner: can create and update their own UserAdvisoryProfile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(profileRef.set(validProfilePayload(ALICE_UID)));
  await assertSucceeds(profileRef.update({
    subscriptionTier: "adult",
    updatedAt: new Date(),
  }));
});

test("Shadow update & identity spoofing: rejects ghost fields and mismatched userId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validProfilePayload(ALICE_UID),
      isAdmin: true,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set(validProfilePayload(ALICE_UID))
  );
});

test("Cross-user isolation: Bob cannot read or mutate Alice holdings, alerts, or reports", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("holdings").doc("h1").set(validHoldingPayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("alerts").doc("a1").set(validAlertPayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("ai_reports").doc("r1").set(validReportPayload(ALICE_UID))
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("holdings").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("holdings").doc("h1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("alerts").doc("a1").delete());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("ai_reports").get());
});

test("Owner: can CRUD holdings, alerts, and create/delete ai_reports", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const hRef = aliceDb.collection("users").doc(ALICE_UID).collection("holdings").doc("h_nvda");
  await assertSucceeds(hRef.set(validHoldingPayload(ALICE_UID)));
  await assertSucceeds(hRef.update({ shares: 20, avgCostPrice: 132.0, updatedAt: new Date() }));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("holdings").get());
  await assertSucceeds(hRef.delete());

  const rRef = aliceDb.collection("users").doc(ALICE_UID).collection("ai_reports").doc("r_1");
  await assertSucceeds(rRef.set(validReportPayload(ALICE_UID)));
  await assertFails(rRef.update({ confidenceScore: 99 }));
  await assertSucceeds(rRef.delete());

  const wRef = aliceDb.collection("users").doc(ALICE_UID).collection("watchlist").doc("NVDA");
  await assertSucceeds(wRef.set(validWatchlistPayload(ALICE_UID)));
  await assertSucceeds(wRef.update({
    divergenceTag: "BULLISH_DECOUPLING",
    divergenceAlertActive: true,
    divergenceConfidence: 91,
    aiCorrelationSummary: "Strong divergence vs DAX 40 and NIKKEI 225 peers.",
    targetIndices: "NIKKEI 225, DAX 40, BSE SENSEX",
    updatedAt: new Date(),
  }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("watchlist").get());
  await assertSucceeds(wRef.delete());
});
