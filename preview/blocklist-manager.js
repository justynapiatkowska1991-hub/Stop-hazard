(()=>{"use strict";
const KEY="stopHazardBlocklistManager";
const DEFAULT={sources:[],lastUpdate:null,total:0,active:true};
const read=()=>{try{return {...DEFAULT,...JSON.parse(localStorage.getItem(KEY)||"{}")}}catch{return {...DEFAULT}}};
const save=s=>(localStorage.setItem(KEY,JSON.stringify(s)),s);
const normalize=v=>String(v||"").trim().toLowerCase().replace(/^https?:\\/\\//,"").replace(/^www\\./,"").split("/")[0].split(":")[0];
const valid=d=>d.length>2&&d.length<=253&&d.includes(".")&&!/[\\s<>]/.test(d);
const parse=text=>[...new Set(String(text).split(/\\r?\\n/).map(x=>{
 let d=x.trim();
 if(!d||d[0]==="#"||d[0]=="!")return null;
 d=d.replace(/^@@\\|\\|/,"").replace(/^\\|\\|/,"").replace(/\\^.*$/,"");
 d=normalize(d);return valid(d)?d:null;
}).filter(Boolean))];
async function load(url){
 const r=await fetch(url,{cache:"no-store"});if(!r.ok)throw new Error("HTTP "+r.status);
 const list=parse(await r.text());
 if(list.length<100)throw new Error("Invalid blocklist");
 const s=read();s.sources=[...new Set([...s.sources,url])];s.lastUpdate=new Date().toISOString();s.total=list.length;save(s);
 localStorage.setItem("stopHazardBlocklist:"+url,JSON.stringify(list));
 return {ok:true,count:list.length,url};
}
function get(url){try{return JSON.parse(localStorage.getItem("stopHazardBlocklist:"+url)||"[]")}catch{return[]}}
function check(host){
 const h=normalize(host);if(!read().active)return false;
 return getAll().some(d=>h===d||h.endsWith("."+d));
}
function getAll(){
 const s=read();const out=[];
 for(const url of s.sources)out.push(...get(url));
 return [...new Set(out)];
}
function status(){const s=read();return {...s,domains:getAll().length}};
window.StopHazardBlocklist={load,check,getAll,status,activate(){return save({...read(),active:true})},deactivate(){return save({...read(),active:false})},clear(){const s=read();for(const u of s.sources)localStorage.removeItem("stopHazardBlocklist:"+u);return save({...DEFAULT})}};
})();