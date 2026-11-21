/** @type {import('dependency-cruiser').IConfiguration} */
module.exports = {
  forbidden: [
    // R1: Cross-module imports MUST go through modules/<name>/index.ts only
    {
      name: "R1-no-cross-module-deep-import",
      severity: "error",
      comment: "Module chỉ được import Public API (modules/<x>/index.ts) của module khác.",
      from: { path: "^src/modules/([^/]+)/" },
      to: {
        path: "^src/modules/([^/]+)/.+",
        pathNot: ["^src/modules/$1/", "^src/modules/[^/]+/index\\.ts$"],
      },
    },
    // R3: domain/ must stay pure TypeScript — no Firebase SDK imports
    {
      name: "R3-domain-no-firebase",
      severity: "error",
      comment: "Lớp domain/ thuần logic nghiệp vụ, không phụ thuộc Firebase SDK.",
      from: { path: "^src/modules/[^/]+/domain/" },
      to: { path: "(firebase-admin|firebase-functions|src/shared/firebase\\.ts)" },
    },
    // R3b: domain/ cannot depend on adapters/ or handlers/ or application/
    {
      name: "R3-domain-inward-only",
      severity: "error",
      comment: "Hướng phụ thuộc Hexagonal: handlers -> application -> domain.",
      from: { path: "^src/modules/[^/]+/domain/" },
      to: { path: "^src/modules/[^/]+/(adapters|handlers|application)/" },
    },
    // R4: No circular dependencies anywhere
    {
      name: "R4-no-circular",
      severity: "error",
      comment: "Cấm phụ thuộc vòng giữa các module.",
      from: {},
      to: { circular: true },
    },
    // R4b: delivery & payment must not import orders (communicate via EventBus)
    {
      name: "R4-delivery-payment-no-orders",
      severity: "error",
      comment: "delivery và payment không gọi ngược lại orders mà phát Domain Event.",
      from: { path: "^src/modules/(delivery|payment)/" },
      to: { path: "^src/modules/orders/" },
    },
  ],
  options: {
    tsConfig: { fileName: "tsconfig.json" },
    doNotFollow: { path: "node_modules" },
  },
};
