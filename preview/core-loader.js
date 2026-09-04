(()=>{"use strict";
/* STOP HAZARD CORE LOADER 8.0 — single bootstrap for the existing modules. */
const files=[
 "protection-module.js",
 "protection-monitor.js",
 "protection-api.js",
 "protection-bridge.js",
 "protection-controller.js",
 "production-readiness.js",
 "blocklist-manager.js",
 "gambling-blocklist-updater.js",
 "app-shell.js"
];
const loaded=()=>window.StopHazardCore;
function moduleState(){
 return {
  core:!!window.StopHazardCore,
  controller:!!window.StopHazardController,
  protection:!!(window.StopHazardModule||window.StopHazardAPI),
  blocklist:!!window.StopHazardBlocklist,
  updater:!!window.StopHazardBlocklistUpdater,
  runtime:!!window.StopHazardRuntime,
  ui:!!(window.StopHazardAppShell||window.StopHazardUI)
 };
}
function boot(){
 const state=moduleState();
 if(window.StopHazardCore?.initialize)window.StopHazardCore.initialize();
 document.dispatchEvent(new CustomEvent("stop-hazard:boot",{detail:{version:"8.0.0",state}}));
 return {version:"8.0.0",state};
}
window.StopHazardLoader={version:"8.0.0",files,state:moduleState,boot};
if(document.readyState==="loading")document.addEventListener("DOMContentLoaded",boot);
else boot();
})();