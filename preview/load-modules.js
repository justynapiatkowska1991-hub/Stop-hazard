(()=>{"use strict";
/* STOP HAZARD — ordered web-module loader 15.0 */
const modules=[
 "preview/protection-settings.js",
 "preview/protection-history.js",
 "preview/transport-status.js",
 "preview/system-integrity.js",
 "preview/application-orchestrator.js",
 "preview/system-coordinator.js",
 "preview/protection-dashboard.js",
 "preview/boot-sequence.js"
];
const state={loaded:[],failed:[],startedAt:new Date().toISOString()};
function load(src){
 return new Promise((resolve,reject)=>{
  if(document.querySelector('script[data-stop-hazard="'+src+'"]'))return resolve();
  const s=document.createElement("script");
  s.src=src;s.async=false;s.dataset.stopHazard=src;
  s.onload=()=>{state.loaded.push(src);resolve()};
  s.onerror=()=>{state.failed.push(src);reject(new Error("module-load-failed:"+src))};
  document.head.appendChild(s);
 });
}
async function start(){
 for(const src of modules){
  try{await load(src)}catch(_){/* continue so diagnostics can report all failures */}
 }
 window.StopHazardLoader={version:"15.0.0",state:()=>({...state})};
 document.dispatchEvent(new CustomEvent("stop-hazard:modules-loaded",{detail:{...state}}));
 return {...state};
}
if(document.readyState==="loading")document.addEventListener("DOMContentLoaded",start);
else start();
})();