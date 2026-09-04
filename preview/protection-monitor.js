(()=>{"use strict";
const KEY="stopHazardMonitor";
const defaults={enabled:true,strict:true,blocked:0,attempts:0,allowed:0,domains:0,lastBlocked:null,startedAt:Date.now()};
const domains=[
"bet365.com","betway.com","bwin.com","unibet.com","betfair.com","stake.com","betano.com",
"1xbet.com","888.com","888casino.com","pokerstars.com","pokerstarscasino.com","casino.com",
"casumo.com","betsson.com","leovegas.com","williamhill.com","ladbrokes.com","coral.co.uk",
"sportingbet.com","betvictor.com","parimatch.com","melbet.com","22bet.com","roobet.com",
"bc.game","cloudbet.com","rollbit.com","gamdom.com","duelbits.com","gg.bet","fortunejack.com",
"partypoker.com","videoslots.com","jackpotcity.com","royalpanda.com","spinpalace.com",
"betonline.ag","sportsbetting.ag","mybookie.ag","mostbet.com","1win.pro"
];
const norm=v=>String(v??"").trim().toLowerCase().replace(/^https?:\\/\\//,"").replace(/^www\\./,"").split("/")[0].split(":")[0];
const read=()=>{try{return {...defaults,...JSON.parse(localStorage.getItem(KEY)||"{}")}}catch{return {...defaults}}};
const save=s=>{localStorage.setItem(KEY,JSON.stringify(s));return s};
const custom=()=>{try{return JSON.parse(localStorage.getItem("stopHazardCustomDomains")||"[]").map(norm).filter(Boolean)}catch{return[]}};
const list=()=>[...new Set([...domains,...custom()])];
const match=(host,rule)=>host===rule||host.endsWith("."+rule);
function inspect(host){
 const s=read(),h=norm(host);s.attempts++;
 const hit=s.enabled&&list().some(d=>match(h,d));
 if(hit){s.blocked++;s.lastBlocked={host:h,time:Date.now()}}else s.allowed++;
 s.domains=list().length;save(s);
 return {blocked:hit,host:h,category:hit?"hazard":null,reason:hit?"Domena objęta ochroną":"Dozwolone"};
}
function add(host){const h=norm(host);if(!h.includes("."))return false;const a=custom();if(!a.includes(h))a.push(h);localStorage.setItem("stopHazardCustomDomains",JSON.stringify(a.slice(0,2000)));return true}
window.StopHazardMonitor={inspect,add,remove(h){const n=norm(h);localStorage.setItem("stopHazardCustomDomains",JSON.stringify(custom().filter(x=>x!==n)))},domains:list,stats:read,enable(){return save({...read(),enabled:true})},disable(){return save({...read(),enabled:false})},setStrict(v){return save({...read(),strict:!!v})},reset(){return save({...defaults,startedAt:Date.now()})}};
})();