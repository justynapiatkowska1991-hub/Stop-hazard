(() => {
  const KEY="stopHazardPremium";
  const DEFAULT={enabled:false,plan:"FREE",activatedAt:null};
  const load=()=>{try{return {...DEFAULT,...JSON.parse(localStorage.getItem(KEY)||"{}")}}catch{return {...DEFAULT}}};
  const save=v=>localStorage.setItem(KEY,JSON.stringify(v));
  window.StopHazardPremium={
    get:load,
    isActive:()=>load().enabled===true,
    activateDemo(){
      const s={...load(),enabled:true,plan:"PREMIUM",activatedAt:new Date().toISOString()};
      save(s);return s;
    },
    deactivate(){const s={...load(),enabled:false,plan:"FREE"};save(s);return s}
  };
})();
