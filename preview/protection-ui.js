(()=>{"use strict";
const q=s=>document.querySelector(s),create=(tag,cls,text)=>{const e=document.createElement(tag);if(cls)e.className=cls;if(text!==undefined)e.textContent=text;return e};
function render(root=document.body){
 if(!root||document.getElementById("stopHazardPanel"))return;
 const panel=create("section");panel.id="stopHazardPanel";panel.className="sh-panel";
 panel.innerHTML='<div class="sh-head"><div><small>STOP HAZARD</small><h2>Ochrona aktywna</h2></div><button id="shToggle">●</button></div><div class="sh-grid"><div><b id="shBlocked">0</b><span>Zablokowane</span></div><div><b id="shAttempts">0</b><span>Próby</span></div><div><b id="shDomains">0</b><span>Domeny</span></div></div><div class="sh-status" id="shStatus">System ochrony gotowy</div>';
 root.appendChild(panel);
 const update=()=>{const s=window.StopHazardCore?.stats?.()||{};q("#shBlocked").textContent=s.blocked||0;q("#shAttempts").textContent=s.attempts||0;q("#shDomains").textContent=window.StopHazardCore?.domains?.().length||0;q("#shStatus").textContent=s.enabled?"Ochrona aktywna":"Ochrona wyłączona";q("#shToggle").textContent=s.enabled?"●":"○"};
 q("#shToggle").onclick=()=>{const s=window.StopHazardCore.stats();s.enabled?window.StopHazardCore.disable():window.StopHazardCore.enable();update()};update();
}
window.StopHazardUI={render};
})();