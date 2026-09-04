(()=>{"use strict";
/* STOP HAZARD INTEGRATION MANIFEST 9.0
   One manifest describing how the existing core is expected to start.
   This avoids duplicating configuration across modules.
*/
const manifest={
 version:"9.0.0",
 application:"STOP HAZARD",
 core:{
  loader:"core-loader.js",
  index:"core-index.js",
  controller:"protection-controller.js",
  runtime:"production-readiness.js"
 },
 protection:{
  module:"protection-module.js",
  api:"protection-api.js",
  monitor:"protection-monitor.js",
  bridge:"protection-bridge.js"
 },
 blocklists:{
  manager:"blocklist-manager.js",
  updater:"gambling-blocklist-updater.js",
  config:"blocklist-config.js",
  primary:"hagezi-gambling-full"
 },
 ui:{
  shell:"app-shell.js",
  stylesheet:"protection-ui.css"
 },
 startup:[
  "Load core dependencies",
  "Run module health check",
  "Load cached blocklist",
  "Refresh blocklist when requested by the native layer",
  "Report real VPN readiness",
  "Only expose protection as active when the native runtime confirms readiness"
 ],
 safety:{
  failClosedWhenRuntimeUnavailable:true,
  neverClaimSystemWideProtectionWithoutNativeVpn:true,
  neverStoreSecretsInFrontend:true,
  doNotTreatLocalStorageAsSecureCredentialStorage:true
 }
};
window.StopHazardManifest=manifest;
window.StopHazardManifest.get=()=>structuredClone?structuredClone(manifest):JSON.parse(JSON.stringify(manifest));
})();