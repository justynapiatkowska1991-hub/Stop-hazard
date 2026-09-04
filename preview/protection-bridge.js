(()=>{"use strict";
const API=()=>window.StopHazardAPI;
const DASH=()=>window.StopHazardDashboard;
const bridge={
  version:"3.1.0",
  status(){
    const a=API?.();
    const d=DASH?.();
    return {
      protection:!!a?.getSettings?.().enabled,
      strict:!!a?.getSettings?.().strictMode,
      domains:a?.domains?.().length||0,
      stats:d?.summary?.()||null
    };
  },
  check(host){
    if(!API?.().inspect)return {blocked:false,host,available:false};
    return {...API().inspect(host),available:true};
  },
  enable(){return API?.().setEnabled?.(true)},
  disable(){return API?.().setEnabled?.(false)},
  strict(value=true){return API?.().setStrict?.(value)},
  addDomain(host){return API?.().addDomain?.(host)},
  removeDomain(host){return API?.().removeDomain?.(host)},
  events(){return API?.().getEvents?.()||[]},
  clearEvents(){return API?.().clearEvents?.()},
  refresh(){
    const s=this.status();
    document.dispatchEvent(new CustomEvent("stop-hazard:state",{detail:s}));
    return s;
  }
};
window.StopHazardBridge=bridge;
if(document.readyState==="loading")document.addEventListener("DOMContentLoaded",()=>bridge.refresh());
else bridge.refresh();
})();