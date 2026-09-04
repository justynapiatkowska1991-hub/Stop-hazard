(()=>{"use strict";
const STORAGE="stopHazardModule";
const DEFAULT={enabled:true,strict:true,blocked:0,attempts:0,allowed:0,lastBlocked:null,custom:[],events:[],version:"4.0.0"};
const BUILTIN=[
"bet365.com","betway.com","bwin.com","unibet.com","betfair.com","stake.com","betano.com","1xbet.com",
"888.com","888casino.com","pokerstars.com","pokerstarscasino.com","casino.com","casumo.com","betsson.com",
"leovegas.com","williamhill.com","ladbrokes.com","coral.co.uk","sportingbet.com","betvictor.com",
"parimatch.com","melbet.com","22bet.com","roobet.com","bc.game","cloudbet.com","rollbit.com","gamdom.com",
"duelbits.com","gg.bet","fortunejack.com","partypoker.com","videoslots.com","jackpotcity.com","royalpanda.com",
"spinpalace.com","betonline.ag","sportsbetting.ag","mybookie.ag","mostbet.com","1win.pro"
];
const normalize=v=>String(v??"").trim().toLowerCase().replace(/^https?:\\/\\//,"").replace(/^www\\./,"").split("/")[0].split(":")[0];
const load=()=>{try{return {...DEFAULT,...JSON.parse(localStorage.getItem(STORAGE)||"{}")}}catch{return {...DEFAULT}}};
const save=s=>{localStorage.setItem(STORAGE,JSON.stringify(s));return s};
const valid=h=>h.length>=3&&h.length<=253&&h.includes(".")&&!/[\\s<>]/.test(h);
const domains=()=>{const s=load();return [...new Set([...BUILTIN,...(s.custom||[]).map(normalize)])]};
const match=(h,d)=>h===d||h.endsWith("."+d);
const isBlocked=h=>{const s=load();if(!s.enabled)return false;const n=normalize(h);return domains().some(d=>match(n,d))};
const addEvent=(s,host,blocked,reason)=>{s.events=[{host,time:new Date().toISOString(),blocked,reason},...(s.events||[])].slice(0,1000)};
const inspect=host=>{const s=load(),h=normalize(host);if(!valid(h))return{blocked:false,host:h,valid:false};s.attempts++;const hit=s.enabled&&domains().some(d=>match(h,d));if(hit){s.blocked++;s.lastBlocked={host:h,time:Date.now()}}else s.allowed++;addEvent(s,h,hit,hit?"blocked-domain":"allowed");save(s);return{blocked:hit,host:h,valid:true,category:hit?"hazard":null}};
const add=host=>{const s=load(),h=normalize(host);if(!valid(h))return false;s.custom=[...new Set([...(s.custom||[]),h])].slice(0,5000);save(s);return true};
const remove=host=>{const s=load(),h=normalize(host);s.custom=(s.custom||[]).filter(x=>x!==h);save(s);return true};
const setEnabled=v=>save({...load(),enabled:!!v});
const setStrict=v=>save({...load(),strict:!!v});
const resetStats=()=>save({...load(),blocked:0,attempts:0,allowed:0,lastBlocked:null,events:[]});
const exportState=()=>JSON.stringify(load());
const importState=json=>{try{const x=JSON.parse(json);save({...DEFAULT,...x});return true}catch{return false}};
const summary=()=>{const s=load();return{enabled:s.enabled,strict:s.strict,blocked:s.blocked,attempts:s.attempts,allowed:s.allowed,domainCount:domains().length,customCount:(s.custom||[]).length,lastBlocked:s.lastBlocked}};
window.StopHazardModule={version:"4.0.0",normalize,domains,inspect,isBlocked,addDomain:add,removeDomain:remove,setEnabled,setStrict,resetStats,summary,getEvents:()=>load().events||[],exportState,importState};
})();