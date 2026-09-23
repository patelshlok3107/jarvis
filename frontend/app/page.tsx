"use client";
import { useEffect, useState } from "react";
import { API_URL, healthCheck, jarvisChat } from "../lib/api";
import { CURRENT_APK_URL, CURRENT_APK_SIZE, CURRENT_APK_VERSION } from "../lib/apkConfig";

const statuses: Record<string, { label: string; sub: string; ack: string; color: string }> = {
  AVAILABLE: { label: "AVAILABLE", sub: "Calls ring normally", ack: "You're available, Shlok. I'll let calls through.", color: "#38E1FF" },
  BUSY: { label: "BUSY", sub: "I'm handling your calls", ack: "Understood, Shlok. I'll handle your incoming calls.", color: "#38E1FF" },
  DND: { label: "DND", sub: "Screening all calls", ack: "Do not disturb enabled.", color: "#38E1FF" },
  DRIVING: { label: "DRIVING", sub: "Driving - hands-free", ack: "Driving mode on.", color: "#38E1FF" },
  SLEEPING: { label: "SLEEPING", sub: "Sleeping - quiet", ack: "Sleep mode on.", color: "#38E1FF" },
  MEETING: { label: "MEETING", sub: "In a meeting", ack: "Meeting mode enabled.", color: "#38E1FF" },
  EXAM: { label: "EXAM", sub: "Exam protection", ack: "Exam mode on.", color: "#38E1FF" },
};

export default function Page() {
  const [current, setCurrent] = useState("AVAILABLE");
  const [listening, setListening] = useState(false);
  const [voiceOut, setVoiceOut] = useState("");
  const [health, setHealth] = useState<string>("checking...");
  const [history, setHistory] = useState<any[]>([]);
  const [tab, setTab] = useState<"home"|"assistant"|"calls"|"history"|"settings">("home");
  const [lang, setLang] = useState("en");
  const [speed, setSpeed] = useState("1");
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState("all");
  const [isAndroid, setIsAndroid] = useState(false);
  const [dlStatus, setDlStatus] = useState<"idle"|"downloading"|"done"|"error">("idle");

  useEffect(() => {
    try { const h = JSON.parse(localStorage.getItem("jarvis_hist")||"[]"); setHistory(h); } catch {}
    const s = localStorage.getItem("jarvis_status"); if (s && statuses[s]) setCurrent(s);
    if (API_URL) healthCheck().then(r=>setHealth(r.status)).catch(()=>setHealth("offline"));
    else setHealth("offline");
  }, []);
  useEffect(()=>{ try{ setIsAndroid(/Android/i.test(navigator.userAgent)); }catch{} },[]);

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
    const entry = { id: Date.now().toString(), time: Date.now(), status: k, disposition: "MODE_SET", callerName: "System", isSimulated: false, jarvisResponse: statuses[k].ack };
    const h = JSON.parse(localStorage.getItem("jarvis_hist")||"[]");
    h.unshift(entry);
    localStorage.setItem("jarvis_hist", JSON.stringify(h.slice(0,20)));
    setHistory(h.slice(0,20));
    speak(statuses[k].ack);
  }
  async function handleVoice(t: string) {
    const lower = t.toLowerCase();
    let cmd: string|null = null;
    if (lower.includes("busy")) cmd="BUSY";
    else if (lower.includes("available")) cmd="AVAILABLE";
    else if (lower.includes("don't disturb")||lower.includes("do not disturb")||lower.includes("dnd")) cmd="DND";
    else if (lower.includes("driving")||lower.includes("drive")) cmd="DRIVING";
    else if (lower.includes("sleep")) cmd="SLEEPING";
    else if (lower.includes("meeting")) cmd="MEETING";
    else if (lower.includes("exam")) cmd="EXAM";
    else if (lower.includes("who called")) { setTab("history"); speak("Checking your recent calls, Shlok."); return; }
    if (cmd) { setStatus(cmd); setVoiceOut(`"${t}" -> ${statuses[cmd].ack}`); return; }
    try {
      const r = await jarvisChat(t, current);
      speak(r.reply);
      setVoiceOut(`"${t}" -> ${r.reply}`);
    } catch { speak("Sorry Shlok, I didn't catch that. Try: I'm busy or I'm available."); }
  }

  const s = statuses[current] || statuses.AVAILABLE;
  const greeting = (()=>{ const h=new Date().getHours(); return h<12?"Good morning":h<18?"Good afternoon":"Good evening"; })();

  return (
    <div style={{maxWidth:420,margin:"0 auto",minHeight:"100vh",background:"#121315",display:"flex",flexDirection:"column",fontFamily:"Inter, system-ui, sans-serif"}}>
      <style>{`@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&family=JetBrains+Mono:wght@400;500&display=swap');`}</style>
      <div style={{position:"sticky",top:0,zIndex:10,background:"rgba(18,19,21,0.85)",backdropFilter:"blur(12px)",borderBottom:"1px solid rgba(255,255,255,0.06)",padding:"12px 20px 0"}} />

      {tab==="home" && (
        <div style={{padding:"12px 20px 0",display:"flex",flexDirection:"column",gap:16,paddingBottom:88}}>
          <div style={{display:"flex",justifyContent:"space-between",alignItems:"center"}}>
            <div><div style={{fontSize:12,color:"#BBC9CD",letterSpacing:1,textTransform:"uppercase"}}>{greeting}</div><div style={{fontSize:22,color:"#E3E2E4",fontWeight:300,marginTop:2}}>Shlok</div></div>
            <div style={{display:"flex",alignItems:"center",gap:8,background:"#1A1C1D",padding:"6px 10px",borderRadius:999,border:"1px solid rgba(255,255,255,0.06)"}}><span style={{width:6,height:6,borderRadius:"50%",background:"#38E1FF",boxShadow:"0 0 6px #38E1FF"}}></span><span style={{fontSize:10,color:"#BBC9CD",letterSpacing:1.5,fontFamily:"JetBrains Mono"}}>99.8% SYNC</span></div>
          </div>

          <div style={{background:"#1A1C1D",border:"1px solid rgba(56,225,255,0.15)",borderRadius:20,padding:12,display:"flex",flexDirection:"column",gap:8,boxShadow:"0 0 20px rgba(56,225,255,0.08)"}}>
            <div style={{display:"flex",justifyContent:"space-between",alignItems:"center"}}>
              <div><div style={{fontSize:13,color:"#E3E2E4",fontWeight:600,letterSpacing:0.5}}>{isAndroid ? "Install JARVIS" : "Get JARVIS for Android"}</div><div style={{fontSize:10,color:"#BBC9CD",fontFamily:"JetBrains Mono"}}>Native APK • {CURRENT_APK_SIZE} • v{CURRENT_APK_VERSION} • One tap</div></div>
              <a href={CURRENT_APK_URL} target="_blank" rel="noopener" onClick={()=>setDlStatus("downloading")} style={{display:"flex",alignItems:"center",gap:6,background:"#38E1FF",color:"#00363F",padding:"10px 14px",borderRadius:999,fontSize:11,fontWeight:700,letterSpacing:1,fontFamily:"JetBrains Mono",textDecoration:"none",whiteSpace:"nowrap",boxShadow:"0 0 12px rgba(56,225,255,0.4)"}}>↓ INSTALL</a>
            </div>
            {dlStatus==="downloading" && <div style={{fontSize:10,color:"#38E1FF",fontFamily:"JetBrains Mono",textAlign:"center"}}>Downloading JARVIS…</div>}
            {dlStatus==="done" && <div style={{fontSize:10,color:"#38E1FF",fontFamily:"JetBrains Mono",textAlign:"center"}}>Download complete — open APK to install.</div>}
            {dlStatus==="error" && <div style={{fontSize:10,color:"#FFB4AB",textAlign:"center"}}>Unable to download. <button onClick={()=>setDlStatus("idle")} style={{background:"transparent",border:"1px solid #FFB4AB",color:"#FFB4AB",padding:"4px 8px",borderRadius:999,fontSize:10,marginLeft:8}}>TRY AGAIN</button></div>}
            <div style={{fontSize:9,color:"#3C494C",fontFamily:"JetBrains Mono",textAlign:"center"}}>Direct GitHub release • Verified SHA {CURRENT_APK_SIZE.includes("17771638") ? "a46192…" : ""} • <a href="/install" style={{color:"#38E1FF",textDecoration:"none"}}>Details →</a></div>
          </div>

          <div style={{display:"flex",flexDirection:"column",alignItems:"center",padding:"8px 0"}}>
            <div style={{position:"relative",width:192,height:192,display:"grid",placeItems:"center",cursor:"pointer"}} onClick={()=>setTab("assistant")}>
              <div style={{position:"absolute",width:208,height:208,borderRadius:"50%",background:"rgba(56,225,255,0.08)",filter:"blur(20px)"}}/>
              <div style={{position:"absolute",inset:0,borderRadius:"50%",background:"#0D0E10",boxShadow:"inset 0 0 24px rgba(39,217,247,0.06)",display:"grid",placeItems:"center"}}>
                <div style={{width:144,height:144,borderRadius:"50%",background:"#1A1C1D",display:"grid",placeItems:"center",boxShadow:"0 4px 12px rgba(0,0,0,0.4)"}}>
                  <div style={{width:96,height:96,borderRadius:"50%",background:"radial-gradient(ellipse at center, #0D0E10 0%, #00616F 60%, #38E1FF 100%)",display:"grid",placeItems:"center",boxShadow:"0 0 28px rgba(56,225,255,0.22)"}}>
                    <div style={{width:40,height:40,borderRadius:"50%",background:"#0D0E10",display:"grid",placeItems:"center"}}><div style={{width:16,height:16,borderRadius:"50%",background:"#38E1FF",boxShadow:"0 0 12px #38E1FF"}}/></div>
                  </div>
                </div>
              </div>
            </div>
            <div style={{fontSize:10,color:"#BBC9CD",letterSpacing:3,fontFamily:"JetBrains Mono",marginTop:12}}>J.A.R.V.I.S</div>
            <div style={{display:"flex",alignItems:"center",gap:6,background:"#292A2B",padding:"6px 12px",borderRadius:999,marginTop:6}}><span style={{width:6,height:6,borderRadius:"50%",background:"#38E1FF"}}></span><span style={{fontSize:10,color:"#38E1FF",letterSpacing:1.5,fontFamily:"JetBrains Mono"}}>{s.label}</span></div>
            <div style={{fontSize:18,color:"#E3E2E4",fontStyle:"italic",fontWeight:300,marginTop:8,textAlign:"center"}}>“How can I help you, Shlok?”</div>
          </div>

          <div>
            <div style={{display:"flex",justifyContent:"space-between",padding:"0 4px"}}><span style={{fontSize:10,color:"#BBC9CD",letterSpacing:1.5,fontFamily:"JetBrains Mono"}}>Quick Modes</span><span style={{fontSize:10,color:"#3C494C",letterSpacing:1}}>TACTILE OVERRIDE</span></div>
            <div style={{display:"flex",gap:8,overflowX:"auto",padding:"8px 0",margin:"0 -20px",paddingLeft:20,paddingRight:20}}>
              {Object.keys(statuses).map(k=>{
                const sel=k===current;
                return <button key={k} onClick={()=>setStatus(k)} style={{flexShrink:0,display:"flex",alignItems:"center",gap:6,height:36,padding:"0 16px",borderRadius:999,background:sel?"#38E1FF":"#1A1C1D",color:sel?"#00616F":"#BBC9CD",border:sel?"1px solid rgba(56,225,255,0.4)":"1px solid rgba(255,255,255,0.06)",fontSize:10,letterSpacing:1,fontFamily:"JetBrains Mono",cursor:"pointer",fontWeight:sel?600:400}}><span style={{width:6,height:6,borderRadius:"50%",background:sel?"#00616F":"#3C494C"}}/>{k}</button>
              })}
            </div>
          </div>

          <div style={{background:"#1A1C1D",border:"1px solid rgba(255,255,255,0.08)",borderRadius:20,padding:16}}>
            <div style={{display:"flex",justifyContent:"space-between",alignItems:"center"}}><span style={{display:"flex",alignItems:"center",gap:6,fontSize:10,color:"#BBC9CD",letterSpacing:1,fontFamily:"JetBrains Mono"}}>● JARVIS STATUS</span><span style={{background:"#0D0E10",padding:"4px 8px",borderRadius:999,fontSize:10,color:"#38E1FF",fontFamily:"JetBrains Mono"}}>OPERATIONAL</span></div>
            <div style={{display:"flex",flexDirection:"column",gap:8,marginTop:12}}>
              {([
                ["Voice Assistant","Neural NLP engine active","ON",true],
                ["Call Assistant","Screening & Busy intercept","ON",true],
                ["Wake Word","“Hey JARVIS” primed","READY",true],
              ] as const).map(([title,sub,badge,active])=>(
                <div key={title} style={{display:"flex",justifyContent:"space-between",alignItems:"center",background:"#1E2021",padding:"10px 12px",borderRadius:8}}>
                  <div style={{display:"flex",alignItems:"center",gap:12}}><div style={{width:32,height:32,borderRadius:"50%",background:"#292A2B",display:"grid",placeItems:"center",color:"#38E1FF"}}>●</div><div><div style={{fontSize:14,color:"#E3E2E4",fontWeight:500}}>{title}</div><div style={{fontSize:10,color:"#BBC9CD",fontFamily:"JetBrains Mono"}}>{sub}</div></div></div>
                  <span style={{background:active?"rgba(56,225,255,0.15)":"#343536",color:active?"#38E1FF":"#BBC9CD",padding:"4px 10px",borderRadius:999,fontSize:10,fontFamily:"JetBrains Mono"}}>{badge}</span>
                </div>
              ))}
            </div>
            <div style={{marginTop:12,background:"rgba(13,14,16,0.6)",margin:"12px -16px -16px",padding:"10px 16px",display:"flex",justifyContent:"space-between",alignItems:"center"}}><span style={{fontSize:12,color:"#BBC9CD"}}>Everything is ready and synchronized</span><span style={{fontSize:10,color:"#3C494C",fontFamily:"JetBrains Mono"}}>42ms LATENCY</span></div>
          </div>

          <div style={{background:"#1A1C1D",border:"1px solid rgba(255,255,255,0.08)",borderRadius:20,padding:16}}>
            <div style={{display:"flex",justifyContent:"space-between"}}><span style={{fontSize:10,color:"#BBC9CD",letterSpacing:1,fontFamily:"JetBrains Mono"}}>Upcoming Timeline</span><span style={{fontSize:10,color:"#38E1FF",fontFamily:"JetBrains Mono"}}>● AUTOMATION READY</span></div>
            <div style={{display:"flex",gap:12,marginTop:12}}><div style={{width:32,height:32,borderRadius:8,background:"#292A2B",display:"grid",placeItems:"center",color:"#38E1FF"}}>▣</div><div><div style={{fontSize:14,color:"#E3E2E4",fontWeight:500}}>Board Meeting <span style={{fontSize:10,color:"#BBC9CD",fontFamily:"JetBrains Mono"}}>10:30 PM</span></div><div style={{fontSize:12,color:"#BBC9CD",marginTop:4}}>Auto-Busy will engage • Call guardian intercepts active</div></div></div>
          </div>

        </div>
      )}

      {tab==="assistant" && (
        <div style={{padding:"12px 20px 0",display:"flex",flexDirection:"column",gap:12,paddingBottom:88}}>
          <div style={{background:"#0D0E10",border:"1px solid rgba(255,255,255,0.06)",borderRadius:20,padding:16,display:"flex",justifyContent:"space-between",alignItems:"center"}}>
            <div style={{display:"flex",alignItems:"center",gap:10}}><span style={{color:"#38E1FF"}}>◉</span><div><div style={{fontSize:12,color:"#E3E2E4",letterSpacing:1,fontFamily:"JetBrains Mono"}}>JARVIS NEURAL CORE</div><div style={{fontSize:10,color:"#BBC9CD",letterSpacing:1}}>SUB-HERTZ v4.9 ACTIVE</div></div></div>
            <span style={{background:"#292A2B",color:"#38E1FF",padding:"6px 10px",borderRadius:999,fontSize:10,fontFamily:"JetBrains Mono"}}>● {listening?"LISTENING":"READY"}</span>
          </div>
          <div style={{background:"#0D0E10",borderRadius:20,padding:20,display:"flex",flexDirection:"column",alignItems:"center",border:"1px solid rgba(255,255,255,0.06)"}}>
            <div style={{width:64,height:64,borderRadius:"50%",background:"#292A2B",display:"grid",placeItems:"center",boxShadow:"0 0 24px rgba(56,225,255,0.35)"}}>◉</div>
            <div style={{display:"flex",gap:6,marginTop:12,height:32,alignItems:"center"}}>{[2,4,6,8,5,7,3,1].map((h,i)=><div key={i} style={{width:4,borderRadius:2,background: i%2?"#38E1FF":"rgba(56,225,255,0.4)",height: listening? `${6+h*2}px`:`${4+i%3}px`,transition:"height 0.3s"}}/>)}</div>
            <div style={{background:"#292A2B",padding:"6px 12px",borderRadius:999,marginTop:12,fontSize:12,color:"#BBC9CD"}}>“Hey JARVIS, what's on my schedule?”</div>
          </div>
          <div style={{display:"flex",flexDirection:"column",gap:12}}>
            <div style={{alignSelf:"flex-end",background:"#1A1C1D",padding:12,borderRadius:16,maxWidth:"80%"}}><div style={{fontSize:14,color:"#E3E2E4"}}>I'm busy for the next hour with product review.</div></div>
            <div style={{alignSelf:"flex-start",background:"#0D0E10",border:"1px solid rgba(255,255,255,0.06)",padding:12,borderRadius:16,maxWidth:"80%",borderLeft:"2px solid #38E1FF"}}><div style={{fontSize:10,color:"#38E1FF",letterSpacing:1}}>JARVIS</div><div style={{fontSize:14,color:"#E3E2E4",marginTop:4}}>{voiceOut || "Understood. Busy Mode has been activated until 10:30 PM. Incoming calls will be gracefully screened."}</div></div>
          </div>
          <div style={{background:"#0D0E10",border:"1px solid rgba(255,255,255,0.06)",borderRadius:20,padding:16,display:"flex",justifyContent:"space-between",alignItems:"center",marginTop:8}}>
            <button onClick={()=>{ const v=prompt("Type:","I'm busy"); if(v) handleVoice(v);}} style={{width:48,height:48,borderRadius:"50%",background:"#292A2B",border:"none",color:"#BBC9CD",cursor:"pointer"}}>⌨</button>
            <button onClick={()=>{
              const SR=(window as any).SpeechRecognition||(window as any).webkitSpeechRecognition;
              if(!SR){ const inp=prompt("Type:","I'm busy"); if(inp) handleVoice(inp); return; }
              const rec=new SR(); rec.lang=lang==="hi"?"hi-IN":lang==="gu"?"gu-IN":"en-IN"; setListening(true); rec.start();
              rec.onresult=(e:any)=>{ setListening(false); handleVoice(e.results[0][0].transcript); };
              rec.onerror=()=>setListening(false); rec.onend=()=>setListening(false);
            }} style={{width:64,height:64,borderRadius:"50%",background:"#38E1FF",border:"none",boxShadow:"0 0 24px rgba(56,225,255,0.6)",cursor:"pointer",color:"#00363F",fontSize:24}}>{listening?"◼":"●"}</button>
            <button style={{width:48,height:48,borderRadius:"50%",background:"#292A2B",border:"none",color:"#BBC9CD"}}>⚙</button>
          </div>
          <div style={{textAlign:"center",fontSize:10,color:"#BBC9CD"}}>Tap to speak • Hold for continuous dialogue</div>
        </div>
      )}

      {tab==="calls" && (
        <div style={{padding:"12px 20px 0",display:"flex",flexDirection:"column",gap:12,paddingBottom:88}}>
          <div style={{display:"flex",justifyContent:"space-between"}}><span style={{fontSize:10,color:"#BBC9CD",letterSpacing:1}}>Call Assistant</span><span style={{fontSize:10,color:"#38E1FF"}}>● AGENT LIVE</span></div>
          <div style={{background:"#1A1C1D",border:"1px solid rgba(255,255,255,0.08)",borderRadius:20,padding:16,display:"flex",justifyContent:"space-between",alignItems:"center"}}>
            <div><div style={{fontSize:16,color:"#E3E2E4",fontWeight:600}}>Autonomous Call Screening</div><div style={{fontSize:12,color:"#BBC9CD"}}>Active • Filtering unknown & scheduled calls</div></div>
            <div style={{width:48,height:28,borderRadius:999,background:"#343536",padding:2,display:"flex",justifyContent:"flex-end"}}><div style={{width:24,height:24,borderRadius:"50%",background:"#38E1FF"}}/></div>
          </div>
          <div style={{background:"#1E2021",border:"1px solid rgba(255,255,255,0.08)",borderRadius:20,padding:16}}>
            <div style={{display:"flex",gap:12}}><div style={{width:36,height:36,borderRadius:8,background:"#292A2B",display:"grid",placeItems:"center",color:"#38E1FF"}}>◉</div><div><div style={{fontSize:16,color:"#E3E2E4",fontWeight:600}}>Busy Mode Engaged</div><div style={{fontSize:12,color:"#BBC9CD"}}>Calendar sync: Executive Sync Call</div></div></div>
            <div style={{background:"#0D0E10",borderRadius:8,padding:12,marginTop:12}}><div style={{fontSize:10,color:"#38E1FF",letterSpacing:1}}>JARVIS SPOKEN RESPONSE</div><div style={{fontSize:14,color:"#E3E2E4",fontStyle:"italic",marginTop:6}}>“Shlok is currently busy in a meeting. Please state your urgent message and JARVIS will summarize it immediately.”</div></div>
          </div>
          <div style={{display:"flex",gap:8,overflowX:"auto"}}>{["All (12)","Screened (8)","Transcribed (4)"].map((t,i)=><span key={t} style={{padding:"8px 16px",borderRadius:999,background:i===0?"#292A2B":"#1A1C1D",color:i===0?"#38E1FF":"#BBC9CD",fontSize:10,whiteSpace:"nowrap",border:"1px solid rgba(255,255,255,0.06)"}}>{t}</span>)}</div>
          {history.slice(0,3).map((e:any)=>(
            <div key={e.id} style={{background:"#1A1C1D",border:"1px solid rgba(255,255,255,0.08)",borderRadius:20,padding:16}}>
              <div style={{display:"flex",justifyContent:"space-between"}}><div style={{display:"flex",gap:12}}><div style={{width:40,height:40,borderRadius:"50%",background:"#3198DC",display:"grid",placeItems:"center",color:"#002C47",fontWeight:700}}>{(e.callerName||"U").slice(0,2).toUpperCase()}</div><div><div style={{fontSize:14,color:"#E3E2E4",fontWeight:600}}>{e.callerName||e.callerNumber}</div><div style={{fontSize:12,color:"#BBC9CD"}}>Today • {new Date(e.time).toLocaleTimeString()}</div></div></div><span style={{background:"#343536",color:"#38E1FF",padding:"4px 8px",borderRadius:999,fontSize:10}}>HANDLED BY JARVIS</span></div>
              <div style={{background:"#1E2021",borderRadius:8,padding:12,marginTop:8}}><div style={{fontSize:10,color:"#38E1FF",letterSpacing:1}}>AI Summary</div><div style={{fontSize:12,color:"#E3E2E4",marginTop:4}}>{e.jarvisResponse||"No summary"}</div></div>
            </div>
          ))}
          {history.length===0 && <div style={{background:"#1A1C1D",borderRadius:20,padding:16,textAlign:"center",color:"#BBC9CD",fontSize:12}}>No handled calls yet. Enable BUSY and call this phone.</div>}
        </div>
      )}

      {tab==="history" && (
        <div style={{padding:"12px 20px 0",display:"flex",flexDirection:"column",gap:12,paddingBottom:88}}>
          <div><div style={{fontSize:10,color:"#38E1FF",letterSpacing:1.5}}>AUDIT ARCHIVE</div><div style={{fontSize:18,color:"#E3E2E4",fontWeight:600}}>Activity Timeline</div><div style={{fontSize:12,color:"#BBC9CD",marginTop:4}}>Audit log of all autonomous decisions.</div></div>
          <input value={query} onChange={e=>setQuery(e.target.value)} placeholder="Filter by keyword..." style={{width:"100%",background:"#1A1C1D",border:"1px solid rgba(255,255,255,0.08)",color:"#E3E2E4",padding:"10px 12px",borderRadius:8,fontSize:12}}/>
          <div style={{display:"flex",gap:8,overflowX:"auto"}}>{["all","voice","calls","modes","security"].map(k=>(
            <button key={k} onClick={()=>setFilter(k)} style={{padding:"6px 12px",borderRadius:999,background:filter===k?"#292A2B":"#1A1C1D",color:filter===k?"#38E1FF":"#BBC9CD",border:"1px solid rgba(255,255,255,0.06)",fontSize:10,whiteSpace:"nowrap"}}>{k}</button>
          ))}</div>
          <div style={{position:"relative",paddingLeft:24}}>
            <div style={{position:"absolute",left:8,top:0,bottom:0,width:2,background:"#292A2B"}}/>
            <div style={{display:"flex",flexDirection:"column",gap:16}}>
              {history.filter((e:any)=>filter==="all"||filter==="calls").concat(history).slice(0,5).map((e:any)=>(
                <div key={e.id} style={{display:"flex",gap:12}}>
                  <div style={{width:32,height:32,borderRadius:"50%",background:"#0D0E10",border:"1px solid rgba(255,255,255,0.08)",display:"grid",placeItems:"center",color:"#38E1FF",flexShrink:0}}>●</div>
                  <div style={{flex:1,background:"#1A1C1D",border:"1px solid rgba(255,255,255,0.08)",borderRadius:20,padding:16}}>
                    <div style={{display:"flex",justifyContent:"space-between"}}><span style={{fontSize:10,color:"#38E1FF",letterSpacing:1}}>{new Date(e.time).toLocaleTimeString()}</span><span style={{background:"rgba(56,225,255,0.1)",color:"#38E1FF",padding:"4px 8px",borderRadius:999,fontSize:10}}>VOICE AGENT</span></div>
                    <div style={{fontSize:16,color:"#E3E2E4",fontWeight:600,marginTop:4}}>{e.status || "MODE"}</div>
                    <div style={{fontSize:12,color:"#BBC9CD",marginTop:4}}>{e.jarvisResponse || e.disposition}</div>
                  </div>
                </div>
              ))}
              {history.length===0 && <div style={{background:"#1A1C1D",borderRadius:20,padding:16,textAlign:"center",color:"#BBC9CD"}}>No logs yet. Try voice command.</div>}
            </div>
          </div>
        </div>
      )}

      {tab==="settings" && (
        <div style={{padding:"12px 20px 0",display:"flex",flexDirection:"column",gap:12,paddingBottom:88}}>
          <div style={{display:"flex",justifyContent:"space-between"}}><span style={{fontSize:10,color:"#3C494C",letterSpacing:1.5}}>Settings & System</span><span style={{fontSize:10,color:"#38E1FF"}}>● NODE: 0x9F4A</span></div>
          <div style={{background:"#1A1C1D",border:"1px solid rgba(255,255,255,0.08)",borderRadius:20,padding:16,display:"flex",justifyContent:"space-between",alignItems:"center"}}>
            <div style={{display:"flex",gap:12}}><div style={{width:48,height:48,borderRadius:8,background:"#292A2B",display:"grid",placeItems:"center",color:"#38E1FF"}}>◈</div><div><div style={{fontSize:16,color:"#E3E2E4",fontWeight:600}}>Shlok</div><div style={{fontSize:12,color:"#BBC9CD"}}>JARVIS Neural Engine v4.8</div></div></div>
            <span style={{background:"#343536",color:"#38E1FF",padding:"6px 10px",borderRadius:999,fontSize:10}}>SYNCHRONIZED</span>
          </div>
          <div style={{background:"#1A1C1D",border:"1px solid rgba(255,255,255,0.08)",borderRadius:20,padding:16}}>
            <div style={{display:"flex",justifyContent:"space-between"}}><span style={{fontSize:10,color:"#BBC9CD",letterSpacing:1}}>System Diagnostics</span><span style={{fontSize:10,color:"#38E1FF"}}>● ALL SYSTEMS HEALTHY</span></div>
            <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:8,marginTop:12,background:"#0D0E10",padding:8,borderRadius:8}}>
              {[
                ["Application","READY"],
                ["Microphone",typeof navigator!=="undefined" && (navigator as any).mediaDevices?"READY":"OFF"],
                ["Voice Engine","READY"],
                ["Wake Word","READY"],
                ["Call Assistant","READY"],
                ["TTS Synthesis","READY"],
                ["On-Device Neural","READY"],
                ["Network","ONLINE"],
              ].map(([k,v])=>(
                <div key={k} style={{display:"flex",justifyContent:"space-between",background:"#1A1C1D",padding:"8px 12px",borderRadius:6}}><span style={{fontSize:10,color:"#BBC9CD"}}>{k}</span><span style={{fontSize:10,color:v==="READY"||v==="ONLINE"?"#38E1FF":"#BBC9CD"}}>● {v}</span></div>
              ))}
            </div>
          </div>
          <div style={{background:"#1A1C1D",border:"1px solid rgba(255,255,255,0.08)",borderRadius:20,padding:4}}>
            {[
              ["Wake Word","Hey JARVIS (Always Ready)","VOICE_TRIGGER"],
              ["Voice Persona","JARVIS Voice 01 (Neural British)","EN-GB"],
              ["Speech Speed & Cadence","1.05x Dynamic",""],
              ["Response Style","Concise & Precision",""],
              ["Language","English, Hindi, Gujarati","TRI-LINGUAL"],
            ].map(([t,s,b])=>(
              <div key={t} style={{display:"flex",justifyContent:"space-between",alignItems:"center",padding:"12px 16px",borderBottom:"1px solid rgba(255,255,255,0.04)"}}><div><div style={{fontSize:14,color:"#E3E2E4"}}>{t}</div><div style={{fontSize:12,color:"#BBC9CD"}}>{s}</div></div><span style={{fontSize:10,color:"#3C494C"}}>{b}</span></div>
            ))}
          </div>
          <div style={{textAlign:"center",padding:16,color:"#3C494C",fontSize:10,letterSpacing:1}}>JARVIS Core OS Build 2025.4.1 • Designed for Shlok • v2.1.0 Obsidian</div>
        </div>
      )}

      <div style={{position:"sticky",bottom:0,background:"rgba(18,19,21,0.85)",backdropFilter:"blur(12px)",borderTop:"1px solid rgba(255,255,255,0.06)",display:"flex",justifyContent:"space-around",padding:"8px 0  max(8px, env(safe-area-inset-bottom))"}}>
        {([
          ["home","HOME","auto_awesome"],
          ["assistant","AI CORE","graphic_eq"],
          ["calls","SECURE","shield"],
          ["history","LOGS","history"],
          ["settings","SYSTEM","tune"],
        ] as const).map(([k,label])=>(
          <button key={k} onClick={()=>setTab(k as any)} style={{background:tab===k?"rgba(56,225,255,0.08)":"none",border:"none",color:tab===k?"#38E1FF":"#BBC9CD",fontSize:10,letterSpacing:1,cursor:"pointer",padding:"6px 10px",borderRadius:10,display:"flex",flexDirection:"column",alignItems:"center",gap:2}}>
            <span>{label==="HOME"?"◈":label==="AI CORE"?"◉":label==="SECURE"?"⬢":label==="LOGS"?"≡":"⚙"}</span>{label}
          </button>
        ))}
      </div>
    </div>
  );
}
