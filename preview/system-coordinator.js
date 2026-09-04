(()=>{"use strict";
/* STOP HAZARD — unified system coordinator 13.0 */
const A=()=>window.StopHazardApp;
const I=()=>window.StopHazardIntegrity;
const S=()=>window.StopHazardSettings;
const H=()=>window.StopHazardHistory;
const C=()=>window.StopHazardController;
const B=()=>window.StopHazardBlocklist;
const N=()=>window.StopHazardNativeVpn;

function status(){
 const app=A?.()?.snapshot?.()||{};
 const integrity=I?.()?.get?.()||{};
 const controller=C?.()?.status?.()||{};
 return {
  version:"13.0.0",
  app,
  controller,
  integrity,
  blocklistCount:B?.()?.getAll?.()?.length||0,
  settings:S?.()?.get?.()||{},
  native:N?.()?.get?.()||{}
 };
}
async function startup(){
 const gate=I?.()?.gate?.()||{allowed:false,errors:["integrity-module-unavailable"],warnings:[]};
 H?.()?.add?.({type:"startup",allowed:gate.allowed});
 if(A?.()?.initialize)await A().initialize();
 return {...status(),gate};
}
async function updateDatabase(){
 const result=await A?.()?.refresh?.();
 H?.()?.add?.({type:"database-update",ok:!!result?.update?.ok,count:result?.update?.count||0});
 return result;
}
function enableProtection(){
 const gate=I?.()?.gate?.();
 if(gate&&!gate.allowed)return {ok:false,reason:"integrity-gate",errors:gate.errors};
 const result=A?.()?.enable?.()||{action:{ok:false}};
 H?.()?.add?.({type:"enable",result});
 return result;
}
function disableProtection(){
 const result=A?.()?.disable?.()||{action:{ok:false}};
 H?.()?.add?.({type:"disable",result});
 return result;
}
function inspect(host){
 const result=A?.()?.checkHost?.(host)||{available:false,blocked:false,host};
 if(result.blocked)H?.()?.add?.({type:"blocked-domain",host});
 return result;
}
window.StopHazardSystem={
 version:"13.0.0",
 status,
 startup,
 updateDatabase,
 enableProtection,
 disableProtection,
 inspect,
 reset:()=>{H?.()?.clear?.();return status()}
};
})();