(()=>{"use strict";
const KEY="stopHazardHistory";
const MAX=500;
const read=()=>{try{return JSON.parse(localStorage.getItem(KEY)||"[]")}catch(_){return[]}};
const write=a=>{localStorage.setItem(KEY,JSON.stringify(a.slice(-MAX)));return a};
window.StopHazardHistory={
 get:read,
 add:event=>write([...read(),{time:new Date().toISOString(),...event}]),
 clear:()=>write([]),
 count:()=>read().length,
 last:()=>read().at(-1)||null
};
})();