(() => {
  const KEY="stopHazardSecurity";
  const defaults={score:94,daysProtected:7,blockedToday:17,checks:[
    {label:"Ochrona domen",value:true},
    {label:"Własne reguły",value:true},
    {label:"Historia zdarzeń",value:true},
    {label:"Tryb ścisły",value:true}
  ]};
  function get(){try{return {...defaults,...JSON.parse(localStorage.getItem(KEY)||"{}")}}catch{return {...defaults}}}
  function save(v){localStorage.setItem(KEY,JSON.stringify(v));return v}
  window.StopHazardSecurity={
    get, toggle(name){
      const s=get();s.checks=s.checks.map(c=>c.label===name?{...c,value:!c.value}:c);
      s.score=Math.round(s.checks.filter(c=>c.value).length/s.checks.length*100);return save(s);
    }
  };
})();
