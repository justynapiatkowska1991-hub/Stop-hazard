const state={domains:JSON.parse(localStorage.getItem("stopHazardDomains")||"[]"),enabled:true,blocked:1284};
const $=id=>document.getElementById(id);
function renderDomains(){
  $("domainList").innerHTML="";
  state.domains.forEach((domain,i)=>{
    const row=document.createElement("div");row.className="domain";
    row.innerHTML="<span>"+escapeHtml(domain)+"</span><button data-i='"+i+"'>Usuń</button>";
    row.querySelector("button").onclick=()=>{state.domains.splice(i,1);save();renderDomains()};
    $("domainList").appendChild(row);
  });
  $("domainCount").textContent=state.domains.length;
}
function escapeHtml(v){return v.replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;","\"":"&quot;","'":"&#39;"}[c]))}
function save(){localStorage.setItem("stopHazardDomains",JSON.stringify(state.domains))}
function setProtection(enabled){
  state.enabled=enabled;
  $("toggle").classList.toggle("on",enabled);
  document.querySelector(".hero").classList.toggle("active",enabled);
  document.querySelector(".hero strong").textContent=enabled?"OCHRONA AKTYWNA":"OCHRONA WYŁĄCZONA";
  document.querySelector(".hero p").textContent=enabled?"Urządzenie jest chronione":"Ochrona jest obecnie wyłączona";
  document.querySelector(".status-dot").style.background=enabled?"#55e77d":"#777";
}
$("toggle").onclick=()=>setProtection(!state.enabled);
$("addDomain").onclick=()=>{
  const input=$("domainInput"),domain=input.value.trim().toLowerCase().replace(/^https?:\/\//,"").replace(/^www\./,"").split("/")[0];
  if(!/^(?=.{1,253}$)([a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)(\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+$/.test(domain)){alert("Wpisz poprawną domenę.");return}
  if(!state.domains.includes(domain))state.domains.push(domain);
  input.value="";save();renderDomains();
};
$("domainInput").addEventListener("keydown",e=>{if(e.key==="Enter")$("addDomain").click()});
$("clear").onclick=()=>{$("activity").innerHTML="<div class='activity-row'><span class='ok'>✓</span><div><b>Brak nowej aktywności</b><small>Historia została wyczyszczona</small></div></div>"};
$("plans").onclick=()=>alert("Premium: 29,99 zł/miesiąc lub 159 zł/rok. Płatności zostaną podłączone do Google Play w wersji produkcyjnej.");
$("navActivity").onclick=()=>document.querySelector(".card").scrollIntoView({behavior:"smooth"});
$("navSettings").onclick=()=>alert("Ustawienia STOP HAZARD — wersja demonstracyjna.");
renderDomains();setProtection(true);
