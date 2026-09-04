(()=>{"use strict";
const C=window.StopHazardController;
const M=window.StopHazardModule||window.StopHazardAPI;
const $=s=>document.querySelector(s);
function render(root=document.body){
 if(!root||$("#stopHazardAppShell"))return;
 const box=document.createElement("section");
 box.id="stopHazardAppShell";
 box.className="sh-shell";
 box.innerHTML=[
 '<header class="sh-shell-head"><div><small>STOP HAZARD</small><h1>Centrum ochrony</h1></div><span id="sh-runtime">SPRAWDZANIE…</span></header>',
 '<div class="sh-shell-grid">',
 '<article><strong id="sh-blocked">0</strong><small>ZABLOKOWANE</small></article>',
 '<article><strong id="sh-attempts">0</strong><small>PRÓBY</small></article>',
 '<article><strong id="sh-domains">0</strong><small>DOMENY</small></article>',
 '</div>',
 '<div class="sh-actions"><button id="sh-selfcheck">Sprawdź system</button><button id="sh-refresh">Aktualizuj bazę</button><button id="sh-protect">Włącz ochronę</button></div>',
 '<pre id="sh-report" aria-live="polite"></pre>'
 ].join("");
 root.appendChild(box);
 const refresh=()=>{
   const s=M?.summary?.()||M?.stats?.()||{};
   $("#sh-blocked").textContent=s.blocked||0;
   $("#sh-attempts").textContent=s.attempts||0;
   $("#sh-domains").textContent=s.domainCount||M?.domains?.().length||0;
   const st=C?.status?.()||{};
   $("#sh-runtime").textContent=st.ready?"SYSTEM GOTOWY":"WYMAGA KONFIGURACJI";
   $("#sh-protect").textContent=st.enabled?"Wyłącz ochronę":"Włącz ochronę";
 };
 $("#sh-selfcheck").onclick=async()=>{const r=await C?.selfCheck?.();$("#sh-report").textContent=JSON.stringify(r,null,2);refresh()};
 $("#sh-refresh").onclick=async()=>{const r=await C?.refreshBlocklist?.();$("#sh-report").textContent=JSON.stringify(r,null,2);refresh()};
 $("#sh-protect").onclick=()=>{const st=C?.status?.();const r=st?.enabled?C.disable():C.enable();$("#sh-report").textContent=JSON.stringify(r,null,2);refresh()};
 refresh();
}
window.StopHazardAppShell={render,refresh:()=>window.StopHazardAppShell.render()};
if(document.readyState==="loading")document.addEventListener("DOMContentLoaded",()=>render());
else render();
})();