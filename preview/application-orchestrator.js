(()=>{"use strict";
/*
 STOP HAZARD — single application orchestrator 11.0
 Connects existing runtime, controller, blocklist, history, settings and UI.
 It coordinates state; it does not fake native VPN readiness.
*/
const VERSION="11.0.0";
const get=()=>({
 runtime:window.StopHazardRuntime,
 controller:window.StopHazardController,
 core:window.StopHazardCore,
 blocklist:window.StopHazardBlocklist,
 updater:window.StopHazardBlocklistUpdater,
 settings:window.StopHazardSettings,
 history:window.StopHazardHistory,
 dashboard:window.StopHazardDashboard,
 nativeVpn:window.StopHazardNativeVpn
});
const snapshot=()=>{
 const a=get(), c=a.controller?.status?.()||{}, s=a.settings?.get?.()||{};
 const b=a.blocklist?.getAll?.()||[];
 return {
  version:VERSION,
  protectionEnabled:!!c.enabled,
  systemReady:!!c.ready,
  vpnRunning:!!c.vpn,
  blocklistCount:b.length,
  strictMode:!!s.strictMode,
  autoUpdate:!!s.autoUpdate,
  historyCount:a.history?.count?.()||0,
  missing:c.missing||[]
 };
};
async function initialize(){
 const a=get();
 try{a.dashboard?.render?.()}catch(_){}
 try{a.core?.initialize?.()}catch(_){}
 const state=snapshot();
 document.dispatchEvent(new CustomEvent("stop-hazard:ready",{detail:state}));
 return state;
}
async function refresh(){
 const a=get();
 let result={ok:false};
 try{if(a.controller?.refreshBlocklist)result=await a.controller.refreshBlocklist()}catch(e){result={ok:false,reason:"blocklist-update-failed"}}
 a.history?.add?.({type:"blocklist-refresh",result:{ok:!!result.ok,count:result.count||0}});
 return {...snapshot(),update:result};
}
function enable(){
 const a=get();
 const result=a.controller?.enable?.()||{ok:false,reason:"controller-unavailable"};
 a.history?.add?.({type:"protection-enable",result});
 return {...snapshot(),action:result};
}
function disable(){
 const a=get();
 const result=a.controller?.disable?.()||{ok:false,reason:"controller-unavailable"};
 a.history?.add?.({type:"protection-disable",result});
 return {...snapshot(),action:result};
}
function checkHost(host){
 return get().controller?.checkHost?.(host)||{blocked:false,available:false,host};
}
window.StopHazardApp={
 version:VERSION,
 modules:get,
 snapshot,
 initialize,
 refresh,
 enable,
 disable,
 checkHost
};
if(document.readyState==="loading")document.addEventListener("DOMContentLoaded",()=>initialize());
else initialize();
})();