// Single source of truth — same-origin resumable download.
// /api/download streams frontend/public/*.apk with correct
// Content-Type + Content-Length + Accept-Ranges (206 resume),
// so Android DownloadManager doesn't stall like it does on
// GitHub's double-redirect + chunked responses.
export const CURRENT_APK_VERSION = "2.1.0";
export const CURRENT_APK_CODE = 10;
export const CURRENT_APK_FILE = `JARVIS-v${CURRENT_APK_VERSION}.apk`;
export const CURRENT_APK_URL = `/JARVIS-v${CURRENT_APK_VERSION}.apk`;
export const CURRENT_APK_DOWNLOAD_ROUTE = `/api/download`;
export const CURRENT_APK_FALLBACK = `https://github.com/patelshlok3107/jarvis/releases/download/v${CURRENT_APK_VERSION}/JARVIS-v${CURRENT_APK_VERSION}.apk`;
export const CURRENT_APK_SIZE = "11.75 MB (11749373 bytes)";
export const CURRENT_APK_SHA256 = "4E9966B11795B5FFBAEFE53C614B8BE22BEAB4958169031E9D7EBC87D9EE997D";
export const RELEASE_URL = "https://github.com/patelshlok3107/jarvis/releases";
