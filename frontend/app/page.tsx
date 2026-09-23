"use client";
import { useEffect, useState } from "react";
import { API_URL, healthCheck, jarvisChat } from "../lib/api";

const statuses: Record<string, { label: string; sub: string; ack: string; color: string }> = {
  AVAILABLE: { label: "AVAILABLE", sub: "Calls ring normally", ack: "You're available, Shlok. I'll let calls through.", color: "#00E676" },
  BUSY: { label: "BUSY MODE", sub: "I'm handling your calls", ack: "Understood, Shlok. I'll handle your incoming calls.", color: "#00E5FF" },
  DND: { label: "DO NOT DISTURB", sub: "Screening all calls", ack: "Do not disturb enabled.", color: "#FFC107" },
  DRIVING: { label: "DRIVING", sub: "Driving - hands-free", ack: "Driving mode on.", color: "#00E5FF" },
  SLEEPING: { label: "SLEEPING", sub: "Sleeping - quiet", ack: "Sleep mode on.", color: "#B39DDB" },
  MEETING: { label: "MEETING", sub: "In a meeting", ack: "Meeting mode enabled.", color: "#FF8A80" },
};

export default function Page() {
  const [current, setCurrent] = useState("BUSY");
  const [listening, setListening] = useState(false);
  const [voiceOut, setVoiceOut] = useState("");
  const [health, setHealth] = useState<string>("checking...");
  const [history, setHistory] = useState<any[]>([]);
  const [tab, setTab] = useState<"home"|"phone"|"history"|"rules"|"settings">("home");
  const [lang, setLang] = useState("en");
  const [speed, setSpeed] = useState("1");
  const [busyTpl, setBusyTpl] = useState("Hello. I am JARVIS, Shlok's personal AI assistant. Shlok is currently busy and cannot take your call. Please leave a message after the tone.");

  useEffect(() => {
    try { const h = JSON.parse(localStorage.getItem("jarvis_hist")||"[]"); setHistory(h); } catch {}
    const s = localStorage.getItem("jarvis_status"); if (s && statuses[s]) setCurrent(s);
    if (API_URL) healthCheck().then(r=>setHealth(r.status)).catch(()=>setHealth("backend unreachable - offline template mode"));
    else setHealth("no backend - template mode (Android app is the real backend)");
  }, []);

  function speak(t: string) {
    try {
      const u = new SpeechSynthesisUtterance(t);
      u.lang = lang==="hi"?"hi-IN":lang==="gu"?"gu-IN":"en-IN";
      u.rate = parseFloat(speed)||1;
      speechSynthesis.cancel(); speechSynthesis.speak(u);
    } catch {}
    setVoiceOut("JARVIS: "+t);
  }
  function setStatus(k: string) {
    setCurrent(k);
    localStorage.setItem("jarvis_status", k);
    speak(statuses[k].ack);
  }
  async function handleVoice(t: string) {
    const lower = t.toLowerCase();
    let cmd: string|null = null;
    if (lower.includes("busy")) cmd="BUSY";
    else if (lower.includes("available")) cmd="AVAILABLE";
    else if (lower.includes("don't disturb")||lower.includes("do not disturb")||lower.includes("dnd")) cmd="DND";
    else if (lower.includes("driving")) cmd="DRIVING";
    else if (lower.includes("sleep")) cmd="SLEEPING";
    else if (lower.includes("meeting")) cmd="MEETING";
    else if (lower.includes("who called")) { setTab("history"); speak("Checking your recent calls, Shlok."); return; }
    else if (lower.includes("missed")||lower.includes("show")) { setTab("history"); speak("Opening history."); return; }
    if (cmd) { setStatus(cmd); setVoiceOut(`"${t}" -> ${statuses[cmd].ack}`); return; }
    // Otherwise ask cloud API if configured
    try {
      const r = await jarvisChat(t, current);
      speak(r.reply);
      setVoiceOut(`"${t}" -> ${r.reply}`);
    } catch { speak("Sorry Shlok, I didn't catch that. Try: I'm busy or I'm available."); }
  }

  const s = statuses[current];
  return (
    <div style={{maxWidth:420,margin:"0 auto",minHeight:"100vh",background:"#121315",borderLeft:"1px solid #1A1C1D",borderRight:"1px solid #1A1C1D",display:"flex",flexDirection:"column"}}>
      <div style={{display:"flex",justifyContent:"space-between",padding:"12px 16px",fontSize:10,letterSpacing:3,color:"rgba(187,201,205,.8)"}}>
        <span>JARVIS • v2.1.0 Obsidian</span><span>{API_URL ? health : "Offline - Android is backend"}</span>
      </div>

      {tab==="home" && (
        <div>
          <div style={{margin:"12px",background:"linear-gradient(135deg,#0E1218 0%,#0A1A2A 100%)",border:"1px solid rgba(0,229,255,.3)",borderRadius:16,padding:16,textAlign:"center"}}>
            <div style={{width:64,height:64,margin:"0 auto 8px",borderRadius:16,background:"rgba(0,229,255,.1)",border:"1px solid rgba(0,229,255,.4)",display:"grid",placeItems:"center",overflow:"hidden"}}>
              <img src="/icon-192.png" alt="JARVIS" style={{width:48,height:48,objectFit:"contain"}} onError={(e:any)=>e.currentTarget.style.display='none'} />
            </div>
            <div style={{fontSize:11,letterSpacing:3,color:"rgba(255,255,255,.6)"}}>SHLOK'S AI ASSISTANT</div>
            <div style={{fontSize:12,color:"rgba(255,255,255,.7)",marginTop:4}}>Your personal AI mobile assistant — real cellular call handling</div>
            <a href="/install" style={{display:"block",marginTop:12,padding:"14px 20px",background:"#00E5FF",color:"#000",borderRadius:999,fontWeight:800,fontSize:14,letterSpacing:1,textDecoration:"none",textAlign:"center"}}>⬇ INSTALL JARVIS</a>
            <div style={{fontSize:10,color:"rgba(255,255,255,.45)",marginTop:6}}>Native Android APK — not just Add to Home Screen<br/>Vercel = distribution · APK = Telecom & background<br/><span style={{color:"rgba(255,255,255,.35)"}}>APK: {process.env.NEXT_PUBLIC_ANDROID_APK_URL || "GitHub Releases"}</span></div>
          </div>

          <div style={{display:"flex",flexDirection:"column",alignItems:"center",padding:"8px 16px"}}>
            <div style={{width:128,height:128,borderRadius:"50%",border:`1.5px solid ${s.color}`,display:"grid",placeItems:"center",boxShadow:`0 0 22px ${s.color}66`,margin:8}}>
              <div style={{width:46,height:46,borderRadius:"50%",background:s.color,display:"grid",placeItems:"center"}}>O</div>
            </div>
            <div style={{letterSpacing:8,fontWeight:300,color:"#00E5FF"}}>JARVIS</div>
            <div style={{fontSize:12,letterSpacing:2,fontWeight:700,color:s.color}}>{s.label}</div>
            <div style={{fontSize:11,color:"rgba(255,255,255,.45)"}}>{s.sub}</div>
            <div style={{fontSize:10,color:"rgba(255,255,255,.35)",marginTop:6}}>{listening?"Listening...":"Listening: OFF"}</div>
          </div>

          <div style={{display:"flex",gap:6,flexWrap:"wrap",justifyContent:"center",padding:"10px 12px"}}>
            {Object.keys(statuses).map(k=>(
              <button key={k} onClick={()=>setStatus(k)} style={{padding:"6px 10px",borderRadius:999,border:"1px solid #1E2A3A",background:k===current?s.color:"#0A0F18",color:k===current?"#000":"#9AA8C0",fontSize:10,letterSpacing:1,cursor:"pointer",fontWeight:k===current?700:400}}>{k}</button>
            ))}
          </div>

          <div style={{margin:"10px 12px",background:"#0E1218",border:"1px solid #1E2A3A",borderRadius:16,padding:12}}>
            <div style={{fontSize:10,letterSpacing:2,color:"rgba(255,255,255,.6)"}}>VOICE COMMAND</div>
            <button onClick={()=>{
              const SR=(window as any).SpeechRecognition||(window as any).webkitSpeechRecognition;
              if(!SR){ const inp=prompt("Type voice command:","I'm busy"); if(inp) handleVoice(inp); return; }
              const rec=new SR(); rec.lang=lang==="hi"?"hi-IN":lang==="gu"?"gu-IN":"en-IN"; setListening(true); rec.start();
              rec.onresult=(e:any)=>{ setListening(false); handleVoice(e.results[0][0].transcript); };
              rec.onerror=()=>setListening(false); rec.onend=()=>setListening(false);
            }} style={{width:"100%",padding:12,borderRadius:24,border:"none",background:listening?"#FF3B30":"#00E5FF",color:listening?"#fff":"#000",fontWeight:700,cursor:"pointer",marginTop:8}}>
              {listening?"Listening...":"Tap to speak - Hey Jarvis, I'm busy"}
            </button>
            <div style={{fontSize:10,color:"rgba(255,255,255,.6)",marginTop:8}}>{voiceOut}</div>
            <div style={{fontSize:10,color:"rgba(255,255,255,.35)",marginTop:4}}>Try: "I'm busy" | "I'm available" | "who called me?" {API_URL && <span>· Cloud AI via {API_URL}</span>}</div>
          </div>

          <div style={{margin:"10px 12px",background:"#0E1218",border:"1px solid #1E2A3A",borderRadius:16,padding:12}}>
            <div style={{fontSize:10,letterSpacing:2,color:"rgba(255,255,255,.6)"}}>REAL DEVICE TEST — NO SIMULATION</div>
            <div style={{fontSize:11,lineHeight:1.7,marginTop:6,color:"rgba(255,255,255,.8)"}}>
              1. Install APK on your physical Android phone<br/>
              2. Phone tab → grant permissions + Call Screening role<br/>
              3. Enable BUSY MODE above<br/>
              4. Close JARVIS, lock phone<br/>
              5. Call this phone from another phone — JARVIS handles via Telecom (silence + notify; with Default Dialer, caller hears TTS)
            </div>
            <div style={{fontSize:10,color:"rgba(255,255,255,.4)",fontStyle:"italic",marginTop:8}}>Test Status: Waiting for real incoming call... (only cellular trigger)</div>
          </div>

          <div style={{padding:"0 12px",display:"flex",justifyContent:"space-between"}}>
            <div style={{fontSize:10,letterSpacing:2,color:"rgba(255,255,255,.6)",margin:"12px 0 6px"}}>RECENT ACTIVITY</div>
            <button onClick={()=>setTab("history")} style={{background:"transparent",border:"1px solid #1E2A3A",color:"#00E5FF",borderRadius:999,padding:"6px 10px",fontSize:10,cursor:"pointer"}}>View all</button>
          </div>
          <div style={{padding:"0 12px"}}>
            {history.slice(0,3).length? history.slice(0,3).map((e:any)=>(
              <div key={e.id} style={{background:"#0E1218",border:"1px solid #1E2A3A",borderRadius:12,padding:10,margin:"6px 0"}}>
                <div style={{display:"flex",justifyContent:"space-between"}}><b>{e.callerName}</b><span style={{color:"rgba(255,255,255,.45)",fontSize:10}}>{new Date(e.time).toLocaleTimeString()}</span></div>
                <div style={{fontSize:10,color:"#00E5FF"}}>{e.disposition} - {e.status} {e.isSimulated? <span style={{background:"rgba(255,193,7,.15)",color:"#FFC107",padding:"2px 6px",borderRadius:999,marginLeft:6}}>SIMULATED</span>: <span style={{background:"rgba(0,230,118,.12)",color:"#00E676",padding:"2px 6px",borderRadius:999,marginLeft:6}}>REAL</span>}</div>
              </div>
            )) : <div style={{fontSize:10,color:"rgba(255,255,255,.35)",padding:8}}>No real calls yet. Call this phone from another device while BUSY.</div>}
          </div>
          <div style={{height:12}}/>
        </div>
      )}

      {tab==="phone" && (
        <div style={{padding:16}}>
          <div style={{letterSpacing:3,color:"#00E5FF",fontSize:12}}>JARVIS PHONE CONNECTION</div>
          <div style={{marginTop:12,background:health.includes("ok")?"#0A2A1A":"#2A2A0A",border:"1px solid rgba(0,229,255,.2)",borderRadius:12,padding:12}}>
            <div style={{color:health.includes("ok")?"#00E676":"#FFC107",fontWeight:700}}>Backend: {health}</div>
            <div style={{fontSize:11,color:"rgba(255,255,255,.6)"}}>{API_URL||"No backend URL - template mode. Set NEXT_PUBLIC_API_URL in Vercel to connect to Render."}</div>
          </div>
          <div style={{marginTop:12,background:"#0E1218",border:"1px solid #1E2A3A",borderRadius:12,padding:12}}>
            <div style={{fontWeight:700}}>Call Assistant</div><div style={{fontSize:11,color:"rgba(255,255,255,.6)"}}>On Android: CallScreeningService can silence/reject real calls. Log + notify. Needs Default Dialer for TTS injection.</div>
          </div>
          <div style={{marginTop:8,background:"#0E1218",border:"1px solid #1E2A3A",borderRadius:12,padding:12}}>
            <div style={{fontSize:11,letterSpacing:1,color:"#FFC107"}}>WHAT WORKS / WHAT DOES NOT</div>
            <div style={{fontSize:11,marginTop:6,color:"rgba(255,255,255,.7)"}}>WORKS: detect real cellular call, silence per BUSY/rules, save history, notify.</div>
            <div style={{fontSize:11,marginTop:6,color:"rgba(255,255,255,.7)"}}>NEEDS Default Dialer: answer() + TTS so caller hears JARVIS. Without, modem blocks audio - marked SIMULATED.</div>
          </div>
          <div style={{marginTop:12,background:"#111827",border:"1px solid #1E2A3A",borderRadius:12,padding:12}}>
            <div style={{fontSize:11,color:"rgba(255,255,255,.6)"}}>Deploy: Vercel frontend → Render backend → AI. Android app stays native and never needs localhost.</div>
            <div style={{fontSize:10,color:"rgba(255,255,255,.35)",marginTop:6}}>Native call handling (ForegroundService, ConnectionService) stays on device. This web UI is for status, history, and cloud AI.</div>
          </div>
        </div>
      )}

      {tab==="history" && (
        <div style={{padding:16}}>
          <div style={{letterSpacing:3,color:"#00E5FF",fontSize:12}}>JARVIS ACTIVITY</div>
          <div style={{fontSize:10,color:"rgba(255,255,255,.35)"}}>{history.length} entries - encrypted locally on Android</div>
          {history.length? history.map((e:any)=>(
            <div key={e.id} style={{background:"#0E1218",border:"1px solid #1E2A3A",borderRadius:12,padding:10,margin:"8px 0"}}>
              <div style={{display:"flex",justifyContent:"space-between"}}><b>{e.callerName}</b><span style={{fontSize:10,color:"rgba(255,255,255,.45)"}}>{new Date(e.time).toLocaleString()}</span></div>
              <div style={{fontSize:10,color:"#00E5FF"}}>{e.disposition} - {e.status}</div>
              {e.jarvisResponse && <div style={{fontSize:10,color:"rgba(255,255,255,.6)",marginTop:4}}>{e.jarvisResponse}</div>}
            </div>
          )) : <div style={{fontSize:11,color:"rgba(255,255,255,.35)",marginTop:12}}>No real calls yet.</div>}
          <button onClick={()=>{ setHistory([]); localStorage.removeItem("jarvis_hist"); }} style={{width:"100%",marginTop:12,padding:10,background:"transparent",border:"1px solid #1E2A3A",color:"#00E5FF",borderRadius:12,cursor:"pointer"}}>Clear History</button>
        </div>
      )}

      {tab==="rules" && (
        <div style={{padding:16}}>
          <div style={{letterSpacing:3,color:"#00E5FF",fontSize:12}}>CALL RULES</div>
          <div style={{marginTop:12,background:"#0E1218",border:"1px solid #1E2A3A",borderRadius:12,padding:12}}><div style={{fontWeight:700}}>Mom</div><div style={{fontSize:11,color:"rgba(255,255,255,.5)"}}>[ Always Allow ]</div></div>
          <div style={{marginTop:8,background:"#0E1218",border:"1px solid #1E2A3A",borderRadius:12,padding:12}}><div style={{fontWeight:700}}>Rahul</div><div style={{fontSize:11,color:"rgba(255,255,255,.5)"}}>[ Always Allow ]</div></div>
          <div style={{marginTop:8,background:"#0E1218",border:"1px solid #1E2A3A",borderRadius:12,padding:12}}><div style={{fontWeight:700}}>Unknown Numbers</div><div style={{fontSize:11,color:"rgba(255,255,255,.5)"}}>JARVIS Handles when BUSY</div></div>
        </div>
      )}

      {tab==="settings" && (
        <div style={{padding:16}}>
          <div style={{letterSpacing:3,color:"#00E5FF",fontSize:12}}>SETTINGS</div>
          <div style={{marginTop:12,background:"#0E1218",border:"1px solid #1E2A3A",borderRadius:12,padding:12}}>
            <label style={{fontSize:11,color:"rgba(255,255,255,.6)"}}>Busy response</label>
            <textarea value={busyTpl} onChange={e=>setBusyTpl(e.target.value)} rows={3} style={{width:"100%",background:"#0A0F18",border:"1px solid #1E2A3A",color:"#E8F1FF",padding:10,borderRadius:10}}/>
            <div style={{fontSize:10,color:"rgba(255,255,255,.35)",marginTop:6}}>Saved locally. Android app reads this via DataStore.</div>
          </div>
          <div style={{marginTop:8,background:"#0E1218",border:"1px solid #1E2A3A",borderRadius:12,padding:12}}>
            <div style={{fontSize:10,letterSpacing:2,color:"rgba(255,255,255,.6)"}}>Voice</div>
            <div style={{display:"flex",gap:6,marginTop:8}}>{["en","hi","gu"].map(l=>(
              <button key={l} onClick={()=>setLang(l)} style={{padding:"6px 10px",borderRadius:999,border:"1px solid #1E2A3A",background:lang===l?"#00E5FF":"#0A0F18",color:lang===l?"#000":"#9AA8C0",fontSize:11,cursor:"pointer"}}>{l}</button>
            ))}</div>
            <label style={{fontSize:11,color:"rgba(255,255,255,.6)",marginTop:8,display:"block"}}>Speed {speed}</label>
            <input type="range" min="0.5" max="2" step="0.1" value={speed} onChange={e=>setSpeed(e.target.value)} style={{width:"100%"}}/>
            <button onClick={()=>speak("Hello Shlok. I am JARVIS. This is my voice.")} style={{width:"100%",marginTop:8,padding:10,background:"transparent",border:"1px solid #1E2A3A",color:"#00E5FF",borderRadius:12,cursor:"pointer"}}>Preview Voice</button>
          </div>
          <div style={{marginTop:8,background:"#0E1218",border:"1px solid #1E2A3A",borderRadius:12,padding:12}}>
            <div style={{fontSize:11,letterSpacing:1,color:"#FFC107"}}>DEPLOYMENT</div>
            <div style={{fontSize:11,color:"rgba(255,255,255,.6)",marginTop:6}}>Frontend: Vercel (this page)<br/>Backend: {API_URL || "not configured (set NEXT_PUBLIC_API_URL)"} <br/>Health: {API_URL ? `${API_URL}/health` : "—"}</div>
          </div>
        </div>
      )}

      <div style={{flex:1}}/>
      <div style={{position:"sticky",bottom:0,background:"#0E1218",borderTop:"1px solid #1E2A3A",display:"flex",justifyContent:"space-around",padding:"6px 0"}}>
        {(["home","phone","history","rules","settings"] as const).map(t=>(
          <button key={t} onClick={()=>setTab(t)} style={{background:tab===t?"rgba(0,229,255,.08)":"none",border:"none",color:tab===t?"#00E5FF":"rgba(255,255,255,.5)",fontSize:10,letterSpacing:1,cursor:"pointer",padding:"6px 10px",borderRadius:10}}>{t.toUpperCase()}</button>
        ))}
      </div>
    </div>
  );
}
