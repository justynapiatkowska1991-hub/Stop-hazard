(()=>{"use strict";
/*
 STOP HAZARD — centralized blocklist configuration
 The full HaGeZi Gambling list is maintained upstream and currently contains
 roughly 472k entries. The app should fetch/update it rather than hard-code
 hundreds of thousands of strings into the APK/web bundle.
*/
window.StopHazardBlocklistConfig={
 version:"5.0.0",
 sources:[
  {
   id:"hagezi-gambling-full",
   category:"gambling",
   url:"https://raw.githubusercontent.com/hagezi/dns-blocklists/main/domains/gambling.txt",
   expectedScale:"400k+",
   enabled:true
  },
  {
   id:"hagezi-gambling-medium",
   category:"gambling",
   url:"https://raw.githubusercontent.com/hagezi/dns-blocklists/main/domains/gambling_medium.txt",
   expectedScale:"100k+",
   enabled:false
  },
  {
   id:"hagezi-gambling-mini",
   category:"gambling",
   url:"https://raw.githubusercontent.com/hagezi/dns-blocklists/main/domains/gambling_mini.txt",
   expectedScale:"90k+",
   enabled:false
  }
 ],
 policy:{
  matchSubdomains:true,
  normalizeHostnames:true,
  deduplicate:true,
  maxEntries:600000,
  rejectMalformed:true
 },
 recommendation:"Use the full list as the primary source. Keep medium/mini as fallback profiles for devices with limited memory."
};
})();