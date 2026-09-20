require("dotenv").config();
const express = require("express");
const cors = require("cors");
const app = express();
const PORT = process.env.PORT || 10000;

// CORS - only allow Vercel frontend, not *
const allowed = (process.env.ALLOWED_ORIGIN || "").split(",").map(s=>s.trim()).filter(Boolean);
const corsOpts = {
  origin: (origin, cb) => {
    if (!origin) return cb(null, true); // health checks / curl
    if (allowed.length===0) return cb(null, true); // dev: allow all if not configured
    if (allowed.some(a=> origin===a || origin.endsWith(a.replace(/^https?:\/\//,'')) || origin.includes(a))) return cb(null, true);
    // Also allow any vercel.app subdomain if ALLOWED_ORIGIN contains vercel
    if (allowed.some(a=>a.includes("vercel.app")) && origin.includes("vercel.app")) return cb(null, true);
    return cb(new Error("CORS blocked: "+origin));
  }
};
app.use(cors(corsOpts));
app.use(express.json({ limit: "100kb" }));

app.get("/health", (req,res)=>{
  res.json({ status: "ok", service: "jarvis-backend", time: new Date().toISOString(), allowedOrigin: allowed });
});

// Jarvis AI proxy - template fallback if no API key
app.post("/api/jarvis/chat", async (req,res)=>{
  try {
    const { message="", status="BUSY" } = req.body||{};
    if (!message) return res.status(400).json({ error: "message required" });

    const apiKey = process.env.AI_API_KEY;
    // If AI_API_KEY not set, use template (offline) - never expose key status
    if (!apiKey) {
      const lower = message.toLowerCase();
      let reply;
      if (lower.includes("speak")||lower.includes("talk")) reply = `Shlok is currently ${status.toLowerCase()}. Would you like to leave a message?`;
      else if (lower.includes("urgent")) reply = "Understood — I'll notify Shlok immediately.";
      else reply = `Hello. I am JARVIS, Shlok's personal AI assistant. Shlok is currently ${status.toLowerCase()} and cannot take your call. Would you like to leave a message?`;
      return res.json({ reply, source: "template" });
    }

    // Example: proxy to OpenAI-compatible endpoint if AI_API_KEY is set
    // Keep provider modular - user can set AI_PROVIDER=openai/gemini and AI_ENDPOINT
    const provider = process.env.AI_PROVIDER || "openai";
    const endpoint = process.env.AI_ENDPOINT || "https://api.openai.com/v1/chat/completions";
    // Minimal forwarding - do not log secrets
    const ctrl = new AbortController();
    const t = setTimeout(()=>ctrl.abort(), 8000);
    const r = await fetch(endpoint, {
      method: "POST",
      headers: { "Content-Type":"application/json", "Authorization": `Bearer ${apiKey}` },
      body: JSON.stringify({
        model: process.env.AI_MODEL || "gpt-4o-mini",
        messages: [
          { role: "system", content: `You are JARVIS, Shlok's concise AI call assistant. Status: ${status}. Be polite, brief, ask to leave a message.` },
          { role: "user", content: message }
        ],
        max_tokens: 120
      }),
      signal: ctrl.signal
    });
    clearTimeout(t);
    if (!r.ok) throw new Error(`AI ${r.status}`);
    const data = await r.json();
    const reply = data.choices?.[0]?.message?.content || "Shlok is busy. Would you like to leave a message?";
    res.json({ reply: reply.trim(), source: provider });
  } catch (e) {
    // User-friendly, no stack trace, no secrets
    res.status(502).json({ error: "JARVIS is temporarily unable to connect to the core system.", code: "AI_UNAVAILABLE" });
  }
});

// 404
app.use((req,res)=>res.status(404).json({ error: "not found" }));
// Error handler - hide internals
app.use((err,req,res,next)=>{
  if (err.message && err.message.startsWith("CORS")) return res.status(403).json({ error: "CORS: origin not allowed" });
  res.status(500).json({ error: "JARVIS is temporarily unable to connect to the core system." });
});

app.listen(PORT, ()=>console.log(`JARVIS backend listening on ${PORT} allowed=${allowed.join(",")||"(dev open)"}`));
