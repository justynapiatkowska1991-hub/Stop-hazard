(()=>{"use strict";
/* STOP HAZARD — single boot sequence 14.0 */
const required=["StopHazardSystem","StopHazardIntegrity","StopHazardSettings","StopHazardHistory"];
const result={version:"14.0.0",startedAt:new Date().toISOString(),steps:[],ok:false};
const step=(name,ok,detail)=>result.steps.push({name,ok,detail});
function boot(){
 for(const name of required)step("module:"+name,!!window[name],window[name]?"loaded":"missing");
 if(!window.StopHazardSystem){result.ok=false;return result}
 try{
  const gate=window.StopHazardIntegrity?.gate?.()||{allowed:false,errors:["integrity-unavailable"]};
  step("integrity-gate",gate.allowed,gate.errors);
  if(!gate.allowed){result.ok=false;return result}
  const settings=window.StopHazardSettings?.get?.()||{};
  step("settings",true,{strictMode:!!settings.strictMode,autoUpdate:!!settings.autoUpdate});
  const state=window.StopHazardSystem.status?.()||{};
  step("system-status",true,state);
  result.ok=true;
  window.StopHazardHistory?.add?.({type:"boot-complete",ok:true});
 }catch(e){
  step("boot-exception",false,String(e?.message||e));
 }
 return result;
}
window.StopHazardBoot={version:"14.0.0",run:boot};
if(document.readyState==="loading")document.addEventListener("DOMContentLoaded",boot);
else boot();
})();