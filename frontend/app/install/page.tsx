"use client";
import { CURRENT_APK_URL, CURRENT_APK_FALLBACK, RELEASE_URL, CURRENT_APK_VERSION as VERSION, CURRENT_APK_SHA256 as APK_SHA256, CURRENT_APK_SIZE as APK_SIZE } from "../../lib/apkConfig";
const APK_URL = process.env.NEXT_PUBLIC_ANDROID_APK_URL || CURRENT_APK_URL;
const APK_FALLBACK = CURRENT_APK_FALLBACK;

export default function InstallPage() {
  const isAndroid = typeof navigator !== "undefined" && /Android/i.test(navigator.userAgent);
  return (
    <div style={{maxWidth:420,margin:"0 auto",minHeight:"100vh",background:"#05070A",color:"#E8F1FF",display:"flex",flexDirection:"column",padding:16}}>
      <div style={{textAlign:"center",padding:"24px 0 12px"}}>
        <div style={{width:96,height:96,margin:"0 auto",borderRadius:24,background:"rgba(0,229,255,.08)",border:"1px solid rgba(0,229,255,.3)",display:"grid",placeItems:"center",overflow:"hidden"}}>
          <img src="/icon-192.png" alt="JARVIS" style={{width:72,height:72,objectFit:"contain"}} />
        </div>
        <div style={{letterSpacing:8,fontWeight:300,color:"#00E5FF",marginTop:12,fontSize:18}}>JARVIS</div>
        <div style={{fontSize:10,letterSpacing:3,color:"rgba(255,255,255,.5)"}}>SHLOK'S AI ASSISTANT</div>
        <div style={{fontSize:11,color:"rgba(255,255,255,.6)",marginTop:8}}>Your personal AI assistant</div>
        <div style={{fontSize:11,color:"rgba(255,255,255,.5)"}}>Version {VERSION} • APK • Android 8.0+</div>
      </div>

      <div style={{background:"#0E1218",border:"1px solid #1E2A3A",borderRadius:16,padding:16,marginTop:8}}>
        <a href={APK_URL} download={`JARVIS-v${VERSION}.apk`} target="_blank" rel="noopener" style={{display:"block",padding:16,background:"#00E5FF",color:"#000",borderRadius:999,fontWeight:800,textAlign:"center",textDecoration:"none",fontSize:15}}>⬇ DOWNLOAD JARVIS</a>
        <div style={{display:"flex",gap:8,marginTop:10}}>
          <button onClick={()=>{ window.location.href = APK_URL; }} style={{flex:1,padding:12,background:"transparent",border:"1px solid #00E5FF",color:"#00E5FF",borderRadius:999,fontWeight:700,cursor:"pointer",fontSize:11}}>Retry download</button>
          <a href={APK_FALLBACK} target="_blank" rel="noopener" style={{flex:1,padding:12,background:"transparent",border:"1px solid #1E2A3A",color:"rgba(255,255,255,.6)",borderRadius:999,fontWeight:700,textAlign:"center",textDecoration:"none",fontSize:11}}>GitHub direct</a>
        </div>
        <div style={{fontSize:10,color:"rgba(255,255,255,.35)",textAlign:"center",marginTop:8}}>JARVIS-v{VERSION}.apk • {APK_SIZE} • SHA-256: {APK_SHA256.slice(0,16)}… • {isAndroid ? "Tap to download on this phone" : "Open this page on your Android phone"}</div>
        <div style={{fontSize:10,color:"rgba(255,255,255,.4)",textAlign:"center",marginTop:6,wordBreak:"break-all"}}>Verified: {APK_SIZE} • SHA-256: {APK_SHA256} • If size differs, retry download.</div>
        <a href={RELEASE_URL} target="_blank" rel="noopener" style={{display:"block",textAlign:"center",fontSize:11,color:"#00E5FF",marginTop:10,textDecoration:"none"}}>View all releases on GitHub →</a>
      </div>

      <div style={{background:"#1A1A0A",border:"1px solid rgba(255,193,7,.3)",borderRadius:16,padding:16,marginTop:12}}>
        <div style={{fontSize:11,letterSpacing:1,color:"#FFC107",fontWeight:700}}>DOWNLOAD COMPLETE? NEXT STEPS</div>
        <div style={{fontSize:11,lineHeight:1.6,marginTop:8,color:"rgba(255,255,255,.7)"}}>
          After download reaches 100%:<br/>
          1. Swipe down notification → tap <b>JARVIS-v2.1.0.apk</b> → <b>Install</b><br/>
          2. If not in notifications: open <b>Files → Downloads</b> → tap APK<br/>
          3. If blocked: <b>Settings → Allow from this source</b> (Chrome) → then Install<br/>
          4. If update fails: <b>Uninstall old JARVIS first</b> (different signature) → then install
        </div>
      </div>

      <div style={{background:"#0E1218",border:"1px solid #1E2A3A",borderRadius:16,padding:16,marginTop:12}}>
        <div style={{fontSize:11,letterSpacing:1,color:"#FFC107",fontWeight:700}}>INSTALLATION</div>
        <div style={{fontSize:11,lineHeight:1.6,marginTop:8,color:"rgba(255,255,255,.7)"}}>
          1. Tap <b>DOWNLOAD JARVIS</b> above<br/>
          2. Android may show <b>"Install unknown apps"</b> → tap <b>Allow</b> for this source (Chrome / browser)<br/>
          3. Open the downloaded APK → tap <b>Install</b><br/>
          4. Open <b>JARVIS</b> from app drawer → grant Phone/Microphone/Notifications → set Call Screening role<br/>
          5. Enable <b>BUSY MODE</b> → close app → lock phone → call this phone from another phone to test
        </div>
        <div style={{fontSize:10,color:"rgba(255,255,255,.35)",marginTop:8}}>We never bypass Android security. The APK is signed and verified via GitHub Releases.</div>
      </div>

      <div style={{background:"#111827",border:"1px solid #1E2A3A",borderRadius:16,padding:12,marginTop:12}}>
        <div style={{fontSize:10,letterSpacing:1,color:"rgba(255,255,255,.6)"}}>WHAT'S INSIDE</div>
        <div style={{fontSize:11,marginTop:6,color:"rgba(255,255,255,.7)"}}>Native Kotlin + Compose • Telecom (CallScreeningService / ConnectionService) • ForegroundService • TTS/STT • Encrypted storage • No localhost</div>
      </div>

      <div style={{flex:1}}/>
      <a href="/" style={{textAlign:"center",fontSize:11,color:"rgba(255,255,255,.4)",marginTop:16,textDecoration:"none"}}>← Back to JARVIS web</a>
    </div>
  );
}
