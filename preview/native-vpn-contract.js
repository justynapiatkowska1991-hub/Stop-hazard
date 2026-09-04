(()=>{"use strict";
/*
 STOP HAZARD — native VPN integration contract 10.0
 This is the boundary between the web/UI core and a real Android VpnService.
 It intentionally does not fake packet forwarding in JavaScript.
*/
const VERSION="10.0.0";
const DEFAULT={
 supported:false,
 permissionGranted:false,
 running:false,
 dnsReady:false,
 ipv4Ready:false,
 ipv6Ready:false,
 upstreamReady:false,
 blocklistReady:false,
 lastHeartbeat:null,
 reason:"native-vpn-not-connected"
};
const KEY="stopHazardNativeVpn";
const read=()=>{try{return {...DEFAULT,...JSON.parse(localStorage.getItem(KEY)||"{}")}}catch{return {...DEFAULT}}};
const save=s=>(localStorage.setItem(KEY,JSON.stringify(s)),s);
function receive(message){
 if(!message||typeof message!=="object")return {ok:false,reason:"invalid-message"};
 const allowed=["supported","permissionGranted","running","dnsReady","ipv4Ready","ipv6Ready","upstreamReady","blocklistReady","reason"];
 const next=read();
 for(const key of allowed)if(Object.prototype.hasOwnProperty.call(message,key))next[key]=key==="reason"?String(message[key]):!!message[key];
 next.lastHeartbeat=new Date().toISOString();
 return save(next);
}
function ready(){
 const s=read();
 return s.supported&&s.permissionGranted&&s.running&&s.dnsReady&&
        s.ipv4Ready&&s.ipv6Ready&&s.upstreamReady&&s.blocklistReady;
}
function requestStart(){
 if(window.StopHazardNativeBridge?.start)return window.StopHazardNativeBridge.start();
 return {ok:false,reason:"android-bridge-unavailable"};
}
function requestStop(){
 if(window.StopHazardNativeBridge?.stop)return window.StopHazardBridge.stop();
 return {ok:false,reason:"android-bridge-unavailable"};
}
function requestPermission(){
 if(window.StopHazardNativeBridge?.requestVpnPermission)return window.StopHazardNativeBridge.requestVpnPermission();
 return {ok:false,reason:"android-bridge-unavailable"};
}
function heartbeat(){
 if(window.StopHazardNativeBridge?.status)return window.StopHazardNativeBridge.status();
 return {ok:false,reason:"android-bridge-unavailable"};
}
window.StopHazardNativeVpn={
 version:VERSION,
 get:read,
 receive,
 ready,
 requestStart,
 requestStop,
 requestPermission,
 heartbeat
};
})();