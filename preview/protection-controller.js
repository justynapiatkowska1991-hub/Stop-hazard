(()=>{"use strict";
const VERSION="6.0.0";
const runtime=()=>window.StopHazardRuntime;
const moduleApi=()=>window.StopHazardModule||window.StopHazardAPI;
const updater=()=>window.StopHazardBlocklistUpdater;
const state=()=>runtime?.().get?.()||{};
function status(){
 const r=runtime?.().readiness?.()||{ready:false,missing:["runtime"]};
 const s=state();
 return {version:VERSION,ready:!!r.ready,enabled:!!s.protectionEnabled,vpn:!!s.vpnRunning,dns:!!s.dnsReady,ipv4:!!s.ipv4Ready,ipv6:!!s.ipv6Ready,upstream:!!s.upstreamReady,blocklist:!!s.blocklistReady,missing:r.missing||[]};
}
async function refreshBlocklist(){
 if(!updater?.().update)return {ok:false,reason:"updater-unavailable"};
 const result=await updater().update();
 runtime?.().update?.({blocklistReady:!!result.ok});
 return result;
}
function checkHost(host){
 const api=moduleApi?.();
 if(!api?.inspect)return {blocked:false,available:false,host};
 return {...api.inspect(host),available:true};
}
function enable(){
 const r=runtime?.().enable?.();
 if(!r?.ok)return r;
 return {ok:true,status:status()};
}
function disable(){
 runtime?.().disable?.();
 return {ok:true,status:status()};
}
window.StopHazardController={
 VERSION,status,diagnostics:()=>runtime?.().diagnostics?.()||{},
 refreshBlocklist,checkHost,enable,disable,
 async selfCheck(){
   const s=status();
   let blocklist=false;
   try{blocklist=!!(await refreshBlocklist()).ok}catch{}
   return {...status(),blocklistUpdated:blocklist};
 }
};
})();