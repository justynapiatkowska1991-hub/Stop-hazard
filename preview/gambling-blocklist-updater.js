(()=>{"use strict";
/*
 STOP HAZARD — large external gambling blocklist integration
 Source: HaGeZi DNS Blocklists, Gambling category.
 The upstream project publishes lists containing hundreds of thousands
 of gambling-related entries and maintains them over time.
 We do not copy the whole upstream list into the application bundle:
 this updater downloads, validates, normalizes, deduplicates and caches it.
*/
const SOURCE_URL="https://raw.githubusercontent.com/hagezi/dns-blocklists/main/domains/gambling.txt";
const CACHE_KEY="stopHazardRemoteBlocklist";
const META_KEY="stopHazardRemoteBlocklistMeta";
const MAX_DOMAIN_LENGTH=253;
const MAX_ENTRIES=600000;
const normalize=value=>String(value||"").trim().toLowerCase()
 .replace(/^https?:\\/\\//,"").replace(/^www\\./,"")
 .split("/")[0].split(":")[0];
const valid=domain=>domain.length>2&&domain.length<=MAX_DOMAIN_LENGTH&&domain.includes(".")&&!/[\\s<>]/.test(domain)&&!/^[0-9.]+$/.test(domain);
const parse=text=>[...new Set(String(text).split(/\\r?\\n/).map(line=>{
 let x=line.trim();
 if(!x||x.startsWith("#")||x.startsWith("!"))return null;
 x=x.replace(/^@@\\|\\|/,"").replace(/^\\|\\|/,"").replace(/\\^.*$/,"");
 x=normalize(x);
 return valid(x)?x:null;
}).filter(Boolean))].slice(0,MAX_ENTRIES);
async function update(options={}){
 const timeout=Number(options.timeout||15000);
 const controller=new AbortController();
 const timer=setTimeout(()=>controller.abort(),timeout);
 try{
   const response=await fetch(SOURCE_URL,{cache:"no-store",signal:controller.signal});
   if(!response.ok)throw new Error("HTTP "+response.status);
   const text=await response.text();
   const list=parse(text);
   if(list.length<1000)throw new Error("Upstream list unexpectedly small");
   localStorage.setItem(CACHE_KEY,JSON.stringify(list));
   localStorage.setItem(META_KEY,JSON.stringify({source:SOURCE_URL,count:list.length,updatedAt:new Date().toISOString()}));
   return {ok:true,count:list.length,source:SOURCE_URL};
 }finally{clearTimeout(timer)}
}
function cached(){try{return JSON.parse(localStorage.getItem(CACHE_KEY)||"[]")}catch{return[]}}
function metadata(){try{return JSON.parse(localStorage.getItem(META_KEY)||"{}")}catch{return{}}}
function contains(host){
 const h=normalize(host);
 return cached().some(d=>h===d||h.endsWith("."+d));
}
window.StopHazardBlocklistUpdater={SOURCE_URL,update,cached,metadata,contains,normalize};
})();