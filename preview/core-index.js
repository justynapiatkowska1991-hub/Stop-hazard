(()=>{"use strict";
/*
 STOP HAZARD CORE INDEX 7.0
 One entry point for the application's core modules.
 Load this file after the individual preview modules.
*/
const modules={
 runtime:()=>window.StopHazardRuntime,
 controller:()=>window.StopHazardController,
 protection:()=>window.StopHazardModule||window.StopHazardAPI,
 blocklist:()=>window.StopHazardBlocklist,
 updater:()=>window.StopHazardBlocklistUpdater,
 ui:()=>window.StopHazardAppShell||window.StopHazardUI
};
function health(){
 const checks={
  runtime:!!modules.runtime(),
  controller:!!modules.controller(),
  protection:!!modules.protection(),
  blocklist:!!modules.blocklist(),
  updater:!!modules.updater(),
  ui:!!modules.ui()
 };
 return {version:"7.0.0",checks,ok:Object.values(checks).every(Boolean),time:new Date().toISOString()};
}
function status(){
 const controller=modules.controller();
 if(controller?.status)return controller.status();
 return {ready:false,missing:["controller"]};
}
function check(host){
 const controller=modules.controller();
 if(controller?.checkHost)return controller.checkHost(host);
 const protection=modules.protection();
 return protection?.inspect?protection.inspect(host):{blocked:false,available:false};
}
async function initialize(){
 const result={health:health(),status:status()};
 try{
  if(modules.ui()?.render)modules.ui().render();
 }catch(e){result.uiError=String(e)}
 return result;
}
window.StopHazardCore={
 version:"7.0.0",
 modules,
 health,
 status,
 check,
 initialize
};
if(document.readyState==="loading")document.addEventListener("DOMContentLoaded",()=>initialize());
else initialize();
})();