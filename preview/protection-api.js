(()=>{"use strict";
const VERSION="3.0.0";
const SETTINGS="stopHazardSettings";
const EVENTS="stopHazardEvents";
const DEFAULT={enabled:true,strictMode:true,pausedUntil:0,version:VERSION};
const BUILTIN=new Set([
"bet365.com","betway.com","bwin.com","unibet.com","betfair.com","stake.com","betano.com",
"1xbet.com","888.com","888casino.com","pokerstars.com","pokerstarscasino.com","casino.com",
"casumo.com","betsson.com","leovegas.com","williamhill.com","ladbrokes.com","coral.co.uk",
"sportingbet.com","betvictor.com","parimatch.com","melbet.com","22bet.com","roobet.com",
"bc.game","cloudbet.com","rollbit.com","gamdom.com","duelbits.com","gg.bet","fortunejack.com",
"partypoker.com","videoslots.com","jackpotcity.com","royalpanda.com","spinpalace.com",
"betonline.ag","sportsbetting.ag","mybookie.ag","mostbet.com","1win.pro"
]);
const normalize=v=>String(v??"").trim().toLowerCase().replace(/^https?:\\/\\//,"").replace(/^www\\./,"").split("/")[0].split(":")[0];
const read=(key,fallback)=>{try{return JSON.parse(localStorage.getItem(key))??fallback}catch{return fallback}};
const write=(key,value)=>localStorage.setItem(key,JSON.stringify(value));
const settings=()=>({...DEFAULT,...read(SETTINGS,{})});
const custom=()=>read("stopHazardCustomDomains",[]).map(normalize).filter(Boolean);
const domains=()=>[...new Set([...BUILTIN,...custom()])];
const matches=(host,rule)=>host===rule||host.endsWith("."+rule);
const paused=()=>settings().pausedUntil>Date.now();
const isBlocked=host=>{const s=settings();if(!s.enabled||paused())return false;const h=normalize(host);return domains().some(d=>matches(h,d))};
function event(host,blocked,reason){
 const a=read(EVENTS,[]);a.unshift({id:crypto?.randomUUID?.()||String(Date.now()),host:normalize(host),blocked,reason,time:new Date().toISOString()});
 write(EVENTS,a.slice(0,1000));
}
function inspect(host){
 const h=normalize(host);
 if(!h)return {blocked:false,reason:"empty-host"};
 const blocked=isBlocked(h);
 event(h,blocked,blocked?"blocked-domain":"allowed");
 return {blocked,host:h,reason:blocked?"Domena objęta ochroną":"Domena dozwolona",version:VERSION};
}
function setEnabled(value){const s=settings();s.enabled=!!value;write(SETTINGS,s);return s}
function setStrict(value){const s=settings();s.strictMode=!!value;write(SETTINGS,s);return s}
function pause(ms){const s=settings();s.pausedUntil=Date.now()+Math.max(0,Number(ms)||0);write(SETTINGS,s);return s}
function addDomain(host){const h=normalize(host);if(!h.includes(".")||h.length>253)return false;const a=custom();if(!a.includes(h))a.push(h);write("stopHazardCustomDomains",a.slice(0,2000));return true}
function removeDomain(host){const h=normalize(host);write("stopHazardCustomDomains",custom().filter(x=>x!==h))}
window.StopHazardAPI={VERSION,normalize,domains,inspect,isBlocked,setEnabled,setStrict,pause,addDomain,removeDomain,getSettings:settings,getEvents:()=>read(EVENTS,[]),clearEvents:()=>write(EVENTS,[])};
})();