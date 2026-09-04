(()=>{"use strict";
const KEY="stopHazardTransportStatus";
const defaults={running:false,upstreamReady:false,packetsRead:0,packetsForwarded:0,packetsDropped:0};
function get(){try{return {...defaults,...JSON.parse(localStorage.getItem(KEY)||"{}")}}catch(_){return {...defaults}}}
function set(v){const s={...get(),...v};localStorage.setItem(KEY,JSON.stringify(s));return s}
window.StopHazardTransportStatus={
 get,
 set,
 reset:()=>set(defaults),
 ready:()=>{const s=get();return s.running&&s.upstreamReady}
};
})();