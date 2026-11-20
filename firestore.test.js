const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

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

test("Unauthenticated user cannot read or write user profiles or videoStates", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("videoStates").doc("vid_1").set({
      userId: ALICE_UID,
      videoId: "vid_1",
      title: "Test",
      category: "Music",
      liked: true,
      saved: false,
      watchCount: 1,
      lastPositionMs: 0,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Authenticated owner can create and partially update UserProfile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Raunak",
      email: "raunak@example.com",
      preferredTopics: ["Tech", "Music"],
      themeMode: "DARK",
      backgroundPlay: true,
      backgroundDataPlay: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredTopics: ["Tech", "Music", "4K Cinema"],
      themeMode: "LIGHT",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field / shadow update
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      themeMode: "SYSTEM",
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Authenticated owner can manage videoStates, subscriptions, and customVideos", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("videoStates").doc("vid_cosmos_4k").set({
      userId: ALICE_UID,
      videoId: "vid_cosmos_4k",
      title: "Aurora Bioluminescence 4K",
      category: "Nature",
      liked: true,
      saved: true,
      watchCount: 2,
      lastPositionMs: 42000,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("videoStates").doc("vid_cosmos_4k").update({
      watchCount: 3,
      lastPositionMs: 90000,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("videoStates").get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("subscriptions").doc("ch_lofi").set({
      userId: ALICE_UID,
      channelId: "ch_lofi",
      channelName: "Glass Synth Lab",
      category: "Music",
      subscribed: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("subscriptions").get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("customVideos").doc("custom_1").set({
      userId: ALICE_UID,
      title: "My Glass Studio Clip",
      channelName: "Raunak",
      category: "Music",
      streamUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
      durationSec: 15,
      shortVideo: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject @DocumentId `id` field inside payload
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("customVideos").doc("custom_2").set({
      id: "custom_2",
      userId: ALICE_UID,
      title: "Bad ID Field",
      channelName: "Raunak",
      category: "Music",
      streamUrl: "https://example.com/v.mp4",
      durationSec: 15,
      shortVideo: false,
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Cross-user access is strictly denied", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const bobRef = context.firestore().collection("users").doc(BOB_UID);
    const created = new Date(Date.now() - 10000);
    await bobRef.set({
      userId: BOB_UID,
      displayName: "Bob",
      preferredTopics: ["Gaming"],
      themeMode: "DARK",
      backgroundPlay: true,
      backgroundDataPlay: false,
      createdAt: created,
      updatedAt: created,
    });
    await bobRef.collection("videoStates").doc("vid_1").set({
      userId: BOB_UID,
      videoId: "vid_1",
      title: "Bob Video",
      category: "Gaming",
      liked: true,
      saved: false,
      watchCount: 1,
      lastPositionMs: 0,
      createdAt: created,
      updatedAt: created,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("videoStates").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("videoStates").doc("vid_1").get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("videoStates").doc("vid_1").update({
      liked: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
