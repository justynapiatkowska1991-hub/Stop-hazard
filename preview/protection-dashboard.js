(()=>{"use strict";
const q=s=>document.querySelector(s);
function render(root=document.body){
 if(q("#stopHazardDashboard"))return;
 const el=document.createElement("section");
 el.id="stopHazardDashboard";
 el.innerHTML=`
 <div class="sh-card">
  <div class="sh-title"><span>STOP HAZARD</span><b id="sh-state">—</b></div>
  <div class="sh-main"><strong id="sh-blocked">0</strong><small>ZABLOKOWANE</small></div>
  <div class="sh-stats">
   <span>Sprawdzone <b id="sh-inspected">0</b></span>
   <span>Domeny <b id="sh-domains">0</b></span>
   <span>Historia <b id="sh-history">0</b></span>
  </div>
  <div class="sh-buttons">
   <button id="sh-check">Diagnostyka</button>
   <button id="sh-update">Aktualizuj bazę</button>
   <button id="sh-toggle">Ochrona</button>
  </div>
  <pre id="sh-output"></pre>
 </div>`;
 root.appendChild(el);
 const refresh=()=>{
  const api=window.StopHazardCore;
  const c=window.StopHazardController;
  const m=window.StopHazardModule||window.StopHazardAPI;
  const s=c?.status?.()||{};
  const stats=m?.summary?.()||m?.stats?.()||{};
  q("#sh-state").textContent=s.enabled?"AKTYWNA":"NIEAKTYWNA";
  q("#sh-blocked").textContent=stats.blocked||0;
  q("#sh-inspected").textContent=stats.attempts||stats.inspected||0;
  q("#sh-domains").textContent=window.StopHazardBlocklist?.status?.().domains||window.StopHazardBlocklist?.getAll?.().length||0;
  q("#sh-history").textContent=window.StopHazardHistory?.count?.()||0;
 };
 q("#sh-check").onclick=async()=>{
  const r=window.StopHazardController?.selfCheck?.();
  q("#sh-output").textContent=JSON.stringify(await r,null,2);refresh();
 };
 q("#sh-update").onclick=async()=>{
  try{
   const r=await window.StopHazardController?.refreshBlocklist?.();
   window.StopHazardHistory?.add?.({type:"blocklist-update",result:r});
   q("#sh-output").textContent=JSON.stringify(r,null,2);
  }catch(e){q("#sh-output").textContent="Błąd aktualizacji bazy";}
  refresh();
 };
 q("#sh-toggle").onclick=()=>{
  const s=window.StopHazardController?.status?.()||{};
  const r=s.enabled?window.StopHazardController.disable():window.StopHazardController.enable();
  window.StopHazardHistory?.add?.({type:s.enabled?"protection-disabled":"protection-enabled",result:r});
  q("#sh-output").textContent=JSON.stringify(r,null,2);refresh();
 };
 refresh();
}
window.StopHazardDashboard={render,refresh:render};
if(document.readyState==="loading")document.addEventListener("DOMContentLoaded",()=>render());
else render();
})();