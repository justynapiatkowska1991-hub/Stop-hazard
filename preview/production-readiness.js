(()=>{"use strict";
/*
 STOP HAZARD — production readiness coordinator
 Centralizes the checks the app needs before enabling system-wide protection.
 It intentionally does not pretend that a partial VPN implementation forwards
 arbitrary traffic. The native transport layer must report its real state.
*/
const KEY="stopHazardRuntime";
const DEFAULT={
  vpnPermission:false,
  vpnRunning:false,
  dnsReady:false,
  ipv4Ready:false,
  ipv6Ready:false,
  upstreamReady:false,
  blocklistReady:false,
  protectionEnabled:false,
  lastCheck:null
};
const read=()=>{try{return {...DEFAULT,...JSON.parse(localStorage.getItem(KEY)||"{}")}}catch{return {...DEFAULT}}};
const save=s=>(localStorage.setItem(KEY,JSON.stringify(s)),s);
const required=["vpnPermission","vpnRunning","dnsReady","ipv4Ready","ipv6Ready","upstreamReady","blocklistReady"];
function readiness(s=read()){
  const missing=required.filter(k=>!s[k]);
  return {
    ready:missing.length===0,
    missing,
    protectionEnabled:!!s.protectionEnabled,
    checkedAt:s.lastCheck
  };
}
function update(values){
  const s={...read(),...values,lastCheck:new Date().toISOString()};
  s.protectionEnabled=readiness({...s,protectionEnabled:false}).ready && !!s.protectionEnabled;
  return save(s);
}
function enable(){
  const s=read();
  const r=readiness(s);
  if(!r.ready)return {ok:false,reason:"runtime-not-ready",missing:r.missing};
  save({...s,protectionEnabled:true,lastCheck:new Date().toISOString()});
  return {ok:true};
}
function disable(){
  return save({...read(),protectionEnabled:false,lastCheck:new Date().toISOString()});
}
function diagnostics(){
  const s=read();
  return {
    ...s,
    readiness:readiness(s),
    timestamp:new Date().toISOString()
  };
}
window.StopHazardRuntime={
  version:"1.0.0",
  get:read,
  update,
  readiness,
  diagnostics,
  enable,
  disable,
  reset:()=>save({...DEFAULT})
};
})();