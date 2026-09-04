const state={domains:JSON.parse(localStorage.getItem("stopHazardDomains")||"[]"),enabled:localStorage.getItem("stopHazardEnabled")!=="0",blocked:Number(localStorage.getItem("stopHazardBlocked")||1284)};const $=id=>document.getElementById(id);
function escapeHtml(v){return String(v).replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]))}
function save(){localStorage.setItem("stopHazardDomains",JSON.stringify(state.domains));localStorage.setItem("stopHazardEnabled",state.enabled?"1":"0");localStorage.setItem("stopHazardBlocked",String(state.blocked))}
function renderDomains(){const list=$("domainList");if(!list)return;list.innerHTML="";state.domains.forEach((domain,i)=>{const row=document.createElement("div");row.className="domain";row.innerHTML="<span>"+escapeHtml(domain)+"</span><button type='button'>Usuń</button>";row.querySelector("button").onclick=()=>{state.domains.splice(i,1);save();renderDomains()};list.appendChild(row)});const count=$("domainCount");if(count)count.textContent=state.domains.length;const domains=$("domains");if(domains)domains.textContent=8+state.domains.length}
function setProtection(enabled){state.enabled=enabled;save();const t=$("toggle");if(t)t.classList.toggle("on",enabled);const strong=document.querySelector(".hero strong"),p=document.querySelector(".hero p"),dot=document.querySelector(".status-dot");if(strong)strong.textContent=enabled?"OCHRONA AKTYWNA":"OCHRONA WYŁĄCZONA";if(p)p.textContent=enabled?"Urządzenie jest chronione":"Ochrona jest obecnie wyłączona";if(dot)dot.style.background=enabled?"#55e77d":"#777"}
function normalizeDomain(v){return String(v).trim().toLowerCase().replace(/^https?:\\/\\//,"").replace(/^www\\./,"").split("/")[0].split("?")[0].split("#")[0]}
function validDomain(v){return /^(?=.{1,253}$)([a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)(\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+$/.test(v)}
function addDomain(){const input=$("domainInput");if(!input)return;const domain=normalizeDomain(input.value);if(!validDomain(domain)){alert("Wpisz poprawną domenę, np. example.com.");input.focus();return}if(!state.domains.includes(domain))state.domains.push(domain);input.value="";save();renderDomains()}
if($("toggle"))$("toggle").onclick=()=>setProtection(!state.enabled);
if($("addDomain"))$("addDomain").onclick=addDomain;
if($("domainInput"))$("domainInput").addEventListener("keydown",e=>{if(e.key==="Enter"){e.preventDefault();addDomain()}});
if($("clear"))$("clear").onclick=()=>{const activity=$("activity");if(activity)activity.innerHTML="<div class='activity-row'><span class='ok'>✓</span><div><b>Brak nowej aktywności</b><small>Historia została wyczyszczona</small></div></div>"};
if($("plans"))$("plans").onclick=()=>alert("Panel Premium jest przygotowany jako moduł demonstracyjny. Płatności podłączymy przed publikacją produkcyjną.");
if($("navActivity"))$("navActivity").onclick=()=>location.href="activity.html";
if($("navSettings"))$("navSettings").onclick=()=>location.href="settings.html";
if($("blocked"))$("blocked").textContent=state.blocked.toLocaleString("pl-PL");
renderDomains();setProtection(state.enabled);