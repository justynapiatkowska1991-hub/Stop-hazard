(() => {
  const STORAGE_KEY = "stopHazardSettings";
  const defaults = {
    notifications: true,
    sound: false,
    strictMode: true,
    safeSearch: true,
    startupProtection: true,
    hideStats: false
  };

  function read() {
    try { return { ...defaults, ...JSON.parse(localStorage.getItem(STORAGE_KEY) || "{}") }; }
    catch (_) { return { ...defaults }; }
  }

  function write(settings) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
    return settings;
  }

  window.StopHazardSettings = {
    get: read,
    update(patch) {
      return write({ ...read(), ...patch });
    },
    reset() {
      return write({ ...defaults });
    }
  };
})();
