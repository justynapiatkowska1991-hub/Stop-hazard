(()=>{"use strict";
const KEY="stopHazardSettings";
const DEFAULT={strictMode:true,autoUpdate:true,notifications:true,enabled:true};
const read=()=>{try{return {...DEFAULT,...JSON.parse(localStorage.getItem(KEY)||"{}")}}catch(_){return {...DEFAULT}}};
const save=s=>{localStorage.setItem(KEY,JSON.stringify(s));return s};
window.StopHazardSettings={
 get:read,
 set:(key,value)=>save({...read(),[key]:value}),
 reset:()=>save({...DEFAULT}),
 toggle:key=>save({...read(),[key]:!read()[key])
};
})();