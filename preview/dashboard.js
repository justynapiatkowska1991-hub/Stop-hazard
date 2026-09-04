(() => {
  const KEY="stopHazardDashboard";
  const defaults={blocked:1284,allowed:3921,streak:7,lastUpdated:Date.now()};
  const read=()=>{try{return {...defaults,...JSON.parse(localStorage.getItem(KEY)||"{}")}}catch{return {...defaults}}};
  const save=s=>localStorage.setItem(KEY,JSON.stringify(s));
  const state=read();
  window.StopHazardDashboard={
    get:()=>({...state}),
    block(){state.blocked++;state.lastUpdated=Date.now();save(state);return state.blocked},
    allow(){state.allowed++;state.lastUpdated=Date.now();save(state);return state.allowed},
    reset(){Object.assign(state,defaults);state.lastUpdated=Date.now();save(state)}
  };
})();
