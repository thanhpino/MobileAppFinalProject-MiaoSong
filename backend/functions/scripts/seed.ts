/**
 * Script seed dữ liệu mẫu cho Firebase Emulator Suite (Auth + Firestore):
 *  - 2 tài khoản demo: customer@miaosong.vn (CUSTOMER) & merchant@miaosong.vn (MERCHANT)
 *  - 2 quán ăn D2C: Bún Riêu Cô Ba & Bún Chả Cá Nha Trang
 *  - Nhóm món, Món ăn kèm Option Groups (SINGLE bắt buộc + MULTI tùy chọn) & Voucher
 *
 * Cách chạy (sau khi chạy `firebase emulators:start`):
 *   npm run seed:emulator
 */
import * as admin from "firebase-admin";

process.env.FIRESTORE_EMULATOR_HOST = process.env.FIRESTORE_EMULATOR_HOST ?? "127.0.0.1:8080";
process.env.FIREBASE_AUTH_EMULATOR_HOST = process.env.FIREBASE_AUTH_EMULATOR_HOST ?? "127.0.0.1:9099";

admin.initializeApp({ projectId: "miaosong-pro" });
const db = admin.firestore();
const auth = admin.auth();

const FULL_WEEK_SCHEDULE = {
  mon: [{ open: "06:30", close: "21:30" }],
  tue: [{ open: "06:30", close: "21:30" }],
  wed: [{ open: "06:30", close: "21:30" }],
  thu: [{ open: "06:30", close: "21:30" }],
  fri: [{ open: "06:30", close: "21:30" }],
  sat: [{ open: "06:30", close: "22:00" }],
  sun: [{ open: "06:30", close: "22:00" }],
};

export const STORE_AVATAR_BUN_RIEU_CO_BA =
  "https://lh3.googleusercontent.com/aida-public/AB6AXuDLEGTUDj1U_Yg11MfTv01vY_3AF_TF1ilrOTa0QYJDXkEjB2s9pLTEReU_XCfyVoTyXJo0-kGTiFUK-NolEEMFXJUUkBnNP63g97dPZEVofOtqWwywcKNYST6_QkTJ9nZoAxUuCD7YrBZLfgU4NZVVeXQnRpx6EdyIbVSI4sxXHPTmWpJlM_5zc3rC4k10r3U8w22Gw-jC7ahY7tLQ_DZMT3xl8an6mM7CBAleZ5Q";

export const STORE_AVATAR_BUN_CHA_CA_NT =
  "https://lh3.googleusercontent.com/aida-public/AB6AXuCwcq0m3nINlmuB2KG0iqONcr9xO1cdiAGHK9vo-O-DQWz1pnjK0Nu1_RIQwaVrBh9N7xhlx-o16KMQ8IozPwPuN1tkWXvtlbDfpGAv3bN_sKGDLCzEG_TUd7lWBGFli6mMqC0ETSO8HPa_A2NHYPdjq_bQy7kT7x38PQ-g6z1OWiJFIg-jOuYMnR76klNR4TsQEmpDN5U8D2UmfWcqbB_gh9OgivjXWr7YoJeu7eM";

export const STORE_AVATAR_COM_TAM_BA_GHIEN =
  "https://lh3.googleusercontent.com/aida-public/AB6AXuCEBiL4AIGbHXLkxb8FuNt105Uiyo-yCjJyl_aYulwSvznF-WMFeATEfdS1BewJT_79vsXkamcjtjFm0k2Ol7x6fXhfkgnIMzflDM0dKyATESjoncv2YYyc6QC450rFQNkm9JKJzwazUcVsWTEHq1RVcnD1rtqGRfjBs0UXwuYbWp3dXtgtmUXqeiq0UyytMCp0FtcG-zAIFAi7WD6KIQWPjosxlrPeicLA4CUY3wU";

export const STORE_AVATAR_TRA_SUA_MIAOTEA =
  "https://lh3.googleusercontent.com/aida-public/AB6AXuCSgFyvurAxNBhD3FUkp4d0APxADrWGjNc9X_5_AOXQjP4TwbH_wdpsFm6aVPUBhWoLH_fdK8ceBr-87CWNz5uBcTTq38rA02dXH962HqoGod8HHChH1Q4DycVg82qLHCpeKc1Y_jq7X-OJFCTzK7rJrQtImY9ZbDijQ5HhQIosKRSFocPs4hKiUka1dvlVvz35YVbwUFx9EN3FYhhySWx9iXe-GbmLhRnRzd-P1iE";

export const DISH_IMAGE_BUN_BO_HUE =
  "https://lh3.googleusercontent.com/aida-public/AB6AXuBBVh_q7NEC4Dm6gzcSsG3DmZX0eOm-fgSIC4fA1_nj6SMhyv621X-N2CTGYkfYoLFKxTe8yr0L6BJT8Xmkd40lu00vCD-bOz7Qjk5s7jH5TVOikXs9Eu6PhGVnsFY_xBY1r8n2_cz7hIaPUXnFSir28sX50aWOlizhfdSiY_T9YYKO-xBcKKNqOR05OJMAPPrQevEJaLXqsuG5wc4v7VWjS_mEhROqPQqfR_q211A";

async function upsertUser(params: {
  uid: string;
  email: string;
  password: string;
  displayName: string;
  phone: string;
  role: "CUSTOMER" | "MERCHANT";
  storeId?: string;
}) {
  try {
    await auth.getUser(params.uid);
  } catch {
    await auth.createUser({
      uid: params.uid,
      email: params.email,
      password: params.password,
      displayName: params.displayName,
    });
  }
  await auth.setCustomUserClaims(params.uid, {
    role: params.role,
    ...(params.storeId ? { storeId: params.storeId } : {}),
  });
  await db.collection("users").doc(params.uid).set({
    name: params.displayName,
    email: params.email,
    phone: params.phone,
    role: params.role,
    storeId: params.storeId ?? null,
    fcmTokens: [],
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });
}

async function seed() {
  console.log("🐱 Seeding MiaoSong Emulator data...");

  // 1. Demo accounts
  await upsertUser({
    uid: "demo_customer_01",
    email: "customer@miaosong.vn",
    password: "Password123!",
    displayName: "Huỳnh Trần Thiên Phú",
    phone: "0901234567",
    role: "CUSTOMER",
  });
  await db
    .collection("users")
    .doc("demo_customer_01")
    .collection("addresses")
    .doc("addr_tdtu")
    .set({
      label: "Trường ĐH Tôn Đức Thắng",
      fullText: "19 Nguyễn Hữu Thọ, Tân Phong, Quận 7, TP.HCM",
      lat: 10.7326,
      lng: 106.6998,
      note: "Gọi khi tới cổng chính",
      isDefault: true,
    });

  await upsertUser({
    uid: "demo_merchant_01",
    email: "merchant@miaosong.vn",
    password: "Password123!",
    displayName: "Bún Riêu Cô Ba (Quán)",
    phone: "0909111222",
    role: "MERCHANT",
    storeId: "store_bun_rieu_co_ba",
  });

  // 2. Store 1: Bún Riêu Cô Ba
  const store1 = db.collection("stores").doc("store_bun_rieu_co_ba");
  await store1.set({
    name: "Bún Riêu Cô Ba",
    slug: "bun-rieu-co-ba",
    address: "420 Nguyễn Thị Thập, Tân Quy, Quận 7, TP.HCM",
    avatarUrl: STORE_AVATAR_BUN_RIEU_CO_BA,
    lat: 10.7385,
    lng: 106.7124,
    phone: "+84909111222",
    manualStatus: "OPEN",
    busyUntil: null,
    weeklySchedule: FULL_WEEK_SCHEDULE,
    specialDays: [],
    maxDistanceKm: 10,
    deliveryProvider: "MOCK",
    ratingAvg: 4.9,
    ratingCount: 128,
  });

  await store1.collection("categories").doc("cat_main").set({ name: "Món chính", sortOrder: 1 });
  await store1.collection("categories").doc("cat_extra").set({ name: "Món thêm", sortOrder: 2 });
  await store1.collection("categories").doc("cat_drink").set({ name: "Thức uống", sortOrder: 3 });

  await store1.collection("menuItems").doc("item_bun_rieu_dac_biet").set({
    categoryId: "cat_main",
    name: "Bún Riêu Cua Đặc Biệt",
    description: "Riêu cua đồng nguyên chất, đậu hũ chiên giòn, huyết, chả lụa, nước lèo thanh ngọt.",
    imageUrl: DISH_IMAGE_BUN_BO_HUE,
    basePrice: 45000,
    isAvailable: true,
    optionGroups: [
      {
        id: "grp_size",
        name: "Chọn kích cỡ",
        type: "SINGLE",
        required: true,
        min: 1,
        max: 1,
        options: [
          { id: "opt_size_kid", name: "Tô trẻ em", priceDelta: 0, isAvailable: true },
          { id: "opt_size_regular", name: "Tô thường", priceDelta: 0, isAvailable: true },
          { id: "opt_size_large", name: "Tô lớn", priceDelta: 10000, isAvailable: true },
        ],
      },
      {
        id: "grp_topping",
        name: "Thêm món ăn kèm",
        type: "MULTI",
        required: false,
        min: 0,
        max: 4,
        options: [
          { id: "opt_top_cha_ca", name: "Thêm chả cá", priceDelta: 8000, isAvailable: true },
          { id: "opt_top_gio", name: "Thêm giò heo", priceDelta: 15000, isAvailable: true },
          { id: "opt_top_mam_tom", name: "Mắm tôm riêng", priceDelta: 0, isAvailable: true },
          { id: "opt_top_quay", name: "Quẩy giòn", priceDelta: 5000, isAvailable: true },
        ],
      },
    ],
  });

  await store1.collection("menuItems").doc("item_tra_da").set({
    categoryId: "cat_drink",
    name: "Trà Đá Đường",
    description: "Trà lài ướp lạnh thanh mát.",
    imageUrl: STORE_AVATAR_TRA_SUA_MIAOTEA,
    basePrice: 5000,
    isAvailable: true,
    optionGroups: [],
  });

  const now = new Date();
  const nextMonth = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000);
  await store1.collection("vouchers").doc("v_miao10").set({
    code: "MIAO10",
    type: "PERCENT",
    value: 10,
    maxDiscount: 20000,
    minOrder: 100000,
    startAt: admin.firestore.Timestamp.fromDate(now),
    endAt: admin.firestore.Timestamp.fromDate(nextMonth),
    usageLimit: 100,
    usedCount: 0,
    perUserLimit: 3,
    isActive: true,
  });
  await store1.collection("vouchers").doc("v_giam20k").set({
    code: "GIAM20K",
    type: "FIXED",
    value: 20000,
    maxDiscount: null,
    minOrder: 100000,
    startAt: admin.firestore.Timestamp.fromDate(now),
    endAt: admin.firestore.Timestamp.fromDate(nextMonth),
    usageLimit: 50,
    usedCount: 0,
    perUserLimit: 1,
    isActive: true,
  });

  // 3. Store 2: Bún Chả Cá Nha Trang
  const store2 = db.collection("stores").doc("store_bun_cha_ca_nt");
  await store2.set({
    name: "Bún Chả Cá Nha Trang",
    slug: "bun-cha-ca-nha-trang",
    address: "112 Lê Văn Lương, Tân Kiểng, Quận 7, TP.HCM",
    avatarUrl: STORE_AVATAR_BUN_CHA_CA_NT,
    lat: 10.7421,
    lng: 106.7042,
    phone: "+84909333444",
    manualStatus: "OPEN",
    busyUntil: null,
    weeklySchedule: FULL_WEEK_SCHEDULE,
    specialDays: [],
    maxDistanceKm: 10,
    deliveryProvider: "LALAMOVE",
    ratingAvg: 4.8,
    ratingCount: 86,
  });

  await store2.collection("categories").doc("cat_main").set({ name: "Món chính", sortOrder: 1 });
  await store2.collection("categories").doc("cat_drink").set({ name: "Thức uống", sortOrder: 2 });
  await store2.collection("menuItems").doc("item_bun_cha_ca").set({
    categoryId: "cat_main",
    name: "Bún Chả Cá Sứa Nha Trang",
    description: "Chả cá thu chiên & hấp, sứa giòn, nước dùng ninh từ xương cá biển.",
    imageUrl: STORE_AVATAR_BUN_CHA_CA_NT,
    basePrice: 48000,
    isAvailable: true,
    optionGroups: [
      {
        id: "grp_size",
        name: "Chọn kích cỡ",
        type: "SINGLE",
        required: true,
        min: 1,
        max: 1,
        options: [
          { id: "opt_size_regular", name: "Tô thường", priceDelta: 0, isAvailable: true },
          { id: "opt_size_large", name: "Tô lớn", priceDelta: 12000, isAvailable: true },
        ],
      },
      {
        id: "grp_topping",
        name: "Thêm món ăn kèm",
        type: "MULTI",
        required: false,
        min: 0,
        max: 3,
        options: [
          { id: "opt_top_cha_chien", name: "Thêm chả cá chiên", priceDelta: 10000, isAvailable: true },
          { id: "opt_top_sua", name: "Thêm sứa giòn", priceDelta: 12000, isAvailable: true },
        ],
      },
    ],
  });

  // 4. Store 3: Cơm Tấm Ba Ghiền
  const store3 = db.collection("stores").doc("store_com_tam_ba_ghien");
  await store3.set({
    name: "Cơm Tấm Ba Ghiền — Sườn Bì Chả",
    slug: "com-tam-ba-ghien",
    address: "84 Đặng Văn Ngữ, Phường 10, Phú Nhuận, TP.HCM",
    avatarUrl: STORE_AVATAR_COM_TAM_BA_GHIEN,
    lat: 10.7932,
    lng: 106.6715,
    phone: "+84908555666",
    manualStatus: "OPEN",
    busyUntil: null,
    weeklySchedule: FULL_WEEK_SCHEDULE,
    specialDays: [],
    maxDistanceKm: 12,
    deliveryProvider: "MOCK",
    ratingAvg: 4.9,
    ratingCount: 340,
  });

  await store3.collection("categories").doc("cat_main").set({ name: "Cơm Tấm Đặc Biệt", sortOrder: 1 });
  await store3.collection("menuItems").doc("item_com_tam_suon_bi").set({
    categoryId: "cat_main",
    name: "Cơm Tấm Sườn Cốt Lết Bì Chả",
    description: "Sườn cây nướng than hoa mật ong thơm lừng, bì giòn dai thính gạo, chả trứng hấp béo ngậy.",
    imageUrl: STORE_AVATAR_COM_TAM_BA_GHIEN,
    basePrice: 65000,
    isAvailable: true,
    optionGroups: [
      {
        id: "grp_rice_size",
        name: "Lượng cơm",
        type: "SINGLE",
        required: true,
        min: 1,
        max: 1,
        options: [
          { id: "opt_rice_std", name: "Cơm vừa", priceDelta: 0, isAvailable: true },
          { id: "opt_rice_more", name: "Thêm cơm (+5k)", priceDelta: 5000, isAvailable: true },
        ],
      },
      {
        id: "grp_com_topping",
        name: "Món thêm",
        type: "MULTI",
        required: false,
        min: 0,
        max: 3,
        options: [
          { id: "opt_top_op_la", name: "Trứng ốp la lòng đào", priceDelta: 10000, isAvailable: true },
          { id: "opt_top_canh_kho_qua", name: "Chén canh khổ qua nhồi thịt", priceDelta: 15000, isAvailable: true },
        ],
      },
    ],
  });

  // 5. Store 4: Trà Sữa & Cà Phê MiaoTea
  const store4 = db.collection("stores").doc("store_tra_sua_miaotea");
  await store4.set({
    name: "Trà Sữa & Cà Phê MiaoTea",
    slug: "tra-sua-miaotea",
    address: "65 Đường D1, Tân Hưng, Quận 7, TP.HCM",
    avatarUrl: STORE_AVATAR_TRA_SUA_MIAOTEA,
    lat: 10.7485,
    lng: 106.6980,
    phone: "+84909777888",
    manualStatus: "OPEN",
    busyUntil: null,
    weeklySchedule: FULL_WEEK_SCHEDULE,
    specialDays: [],
    maxDistanceKm: 8,
    deliveryProvider: "MOCK",
    ratingAvg: 4.7,
    ratingCount: 195,
  });

  await store4.collection("categories").doc("cat_main").set({ name: "Trà & Sữa", sortOrder: 1 });
  await store4.collection("menuItems").doc("item_tra_sua_tran_chau").set({
    categoryId: "cat_main",
    name: "Trà Sữa Oolong Trân Châu Hoàng Kim",
    description: "Đậm vị trà oolong cao cấp, sữa tươi thanh trùng béo ngậy kèm trân châu giòn sần sật.",
    imageUrl: STORE_AVATAR_TRA_SUA_MIAOTEA,
    basePrice: 38000,
    isAvailable: true,
    optionGroups: [
      {
        id: "grp_sugar_ice",
        name: "Mức đường đá",
        type: "SINGLE",
        required: true,
        min: 1,
        max: 1,
        options: [
          { id: "opt_std", name: "100% đường, 100% đá", priceDelta: 0, isAvailable: true },
          { id: "opt_less", name: "50% đường, 70% đá", priceDelta: 0, isAvailable: true },
        ],
      },
    ],
  });

  await db.collection("config").doc("payment").set({ pendingPaymentTtlMin: 15 });

  console.log("✅ Seed completed!");
  console.log("   Customer login : customer@miaosong.vn / Password123!");
  console.log("   Merchant login : merchant@miaosong.vn / Password123! (store_bun_rieu_co_ba)");
  process.exit(0);
}

seed().catch((e) => {
  console.error("❌ Seed failed:", e);
  process.exit(1);
});
