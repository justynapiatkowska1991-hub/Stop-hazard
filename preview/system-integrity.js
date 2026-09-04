(()=>{"use strict";
/* STOP HAZARD — centralized integrity/safety gate 12.0 */
const KEY="stopHazardIntegrity";
const defaults={lastCheck:null,healthy:false,errors:[],warnings:[]};
const read=()=>{try{return {...defaults,...JSON.parse(localStorage.getItem(KEY)||"{}")}}catch(_){return {...defaults}}};
const save=s=>{localStorage.setItem(KEY,JSON.stringify(s));return s};
const required=[
 ["StopHazardApp","application"],
 ["StopHazardCore","core"],
 ["StopHazardController","controller"],
 ["StopHazardRuntime","runtime"],
 ["StopHazardSettings","settings"],
 ["StopHazardHistory","history"]
];
function check(){
 const errors=[],warnings=[];
 for(const [name,label] of required){
  if(!window[name])errors.push(label+"-missing");
 }
 const c=window.StopHazardController;
 const s=c?.status?.()||{};
 if(s.enabled&&!s.ready)errors.push("protection-claimed-without-readiness");
 if(window.StopHazardNativeVpn&&!window.StopHazardNativeVpn.ready?.())warnings.push("native-vpn-not-ready");
 const b=window.StopHazardBlocklist?.getAll?.()||[];
 if(b.length===0)warnings.push("empty-blocklist");
 const result={lastCheck:new Date().toISOString(),healthy:errors.length===0,errors,warnings};
 return save(result);
}
function gate(){
 const r=check();
 return {allowed:r.healthy,errors:r.errors,warnings:r.warnings};
}
window.StopHazardIntegrity={version:"12.0.0",check,gate,get:read};
})();