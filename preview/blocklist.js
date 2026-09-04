(() => {
  const STORAGE="stopHazardBlocklist";
  const EVENT_STORAGE="stopHazardEvents";
  const SETTINGS="stopHazardEnabled";
  const BUILTIN=[
    "bet365.com","betway.com","888.com","888sport.com","williamhill.com","unibet.com",
    "bwin.com","pokerstars.com","betfair.com","ladbrokes.com","coral.co.uk","skybet.com",
    "betsson.com","leovegas.com","mrgreen.com","casumo.com","paddypower.com","10bet.com",
    "betvictor.com","sportingbet.com","22bet.com","1xbet.com","melbet.com","parimatch.com",
    "22bet.com","betano.com","stake.com","roobet.com","bc.game","cloudbet.com","rollbit.com",
    "gamdom.com","duelbits.com","nitrobetting.eu","gg.bet","fortunejack.com","nitrobetting.eu",
    "888casino.com","unibetcasino.com","betwaycasino.com","bwinparty.com","partypoker.com",
    "pokerstarscasino.com","casinocompete.com","casino.com","casumo.com","videoslots.com",
    "mrbitcasino.com","jackpotcity.com","royalpanda.com","spinpalace.com","luckynugget.com",
    "betonline.ag","sportsbetting.ag","mybookie.ag","superbahis.com","1win.pro","mostbet.com"
  ];
  const normalize=v=>String(v||"").trim().toLowerCase().replace(/^https?:\/\//,"").replace(/^www\./,"").split("/")[0];
  const read=(k,f)=>{try{const x=JSON.parse(localStorage.getItem(k));return x??f}catch{return f}};
  const write=(k,v)=>localStorage.setItem(k,JSON.stringify(v));
  function custom(){return read(STORAGE,[]).map(normalize).filter(Boolean)}
  function all(){return [...new Set([...BUILTIN,...custom()])]}
  function matches(host,rule){return host===rule||host.endsWith("."+rule)}
  function enabled(){return localStorage.getItem(SETTINGS)!=="0"}
  function isBlocked(host){
    const h=normalize(host);
    return enabled() && all().some(r=>matches(h,r));
  }
  function record(host,blocked,reason){
    const events=read(EVENT_STORAGE,[]);
    events.unshift({host:normalize(host),blocked,reason:reason||"",ts:Date.now(),age:"teraz"});
    write(EVENT_STORAGE,events.slice(0,500));
  }
  function check(host){
    const h=normalize(host);
    const blocked=isBlocked(h);
    record(h,blocked,blocked?"Domena znajduje się na liście ochrony":"Połączenie dozwolone");
    return {host:h,blocked,reason:blocked?"Domena znajduje się na liście ochrony":"Połączenie dozwolone"};
  }
  window.StopHazardBlocklist={
    builtinCount:BUILTIN.length,
    getAll:all,
    getCustom:custom,
    add(v){const h=normalize(v);if(!h)return false;const d=custom();if(!d.includes(h))d.push(h);write(STORAGE,d.slice(0,1000));return true},
    remove(v){const h=normalize(v);write(STORAGE,custom().filter(x=>x!==h))},
    isBlocked,
    check,
    clearEvents(){write(EVENT_STORAGE,[])},
    version:"2.0"
  };
})();
