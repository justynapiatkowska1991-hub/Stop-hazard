(()=>{"use strict";
const KEY="stopHazardActions";
const defaults={enabled:true,strictMode:true,blocked:0,attempts:0,lastBlocked:null};
function get(){try{return {...defaults,...JSON.parse(localStorage.getItem(KEY)||"{}")}}catch{return {...defaults}}}
function save(s){localStorage.setItem(KEY,JSON.stringify(s));return s}
function normalize(host){return String(host||"").trim().toLowerCase().replace(/^https?:\\/\\//,"").replace(/^www\\./,"").split("/")[0]}
function matches(host,domain){return host===domain||host.endsWith("."+domain)}
const domains=new Set(["bet365.com","betway.com","bwin.com","unibet.com","betfair.com","stake.com","betano.com","1xbet.com","888.com","888casino.com","pokerstars.com","pokerstarscasino.com","casino.com","casumo.com","betsson.com","leovegas.com","williamhill.com","ladbrokes.com","sportingbet.com","betvictor.com","parimatch.com","melbet.com","22bet.com","roobet.com","bc.game","cloudbet.com","rollbit.com","gamdom.com","partypoker.com","videoslots.com"]);
function isBlocked(host){const s=get();if(!s.enabled)return false;const h=normalize(host);return [...domains].some(d=>matches(h,d))}
function inspect(host){const s=get();s.attempts++;if(isBlocked(host)){s.blocked++;s.lastBlocked={host:normalize(host),time:new Date().toISOString()};save(s);return {blocked:true,host:normalize(host)}}save(s);return {blocked:false,host:normalize(host)}}
window.StopHazardProtection={get,isBlocked,inspect,setEnabled(v){return save({...get(),enabled:!!v})},setStrictMode(v){return save({...get(),strictMode:!!v})},reset(){return save({...defaults})},domains:()=>[...domains]};
})();