// All API URLs come from env - never hardcode localhost in production
export const API_URL = process.env.NEXT_PUBLIC_API_URL || "";

export async function healthCheck(): Promise<{status: string}> {
  if (!API_URL) return { status: "no-backend-configured" };
  const res = await fetch(`${API_URL}/health`, { cache: "no-store" });
  if (!res.ok) throw new Error(`Health failed: ${res.status}`);
  return res.json();
}

export async function jarvisChat(message: string, status: string) {
  if (!API_URL) {
    // Offline fallback - template response (no backend required)
    return templateReply(message, status);
  }
  try {
    const res = await fetch(`${API_URL}/api/jarvis/chat`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ message, status }),
    });
    if (!res.ok) throw new Error(`API ${res.status}`);
    return res.json();
  } catch {
    return templateReply(message, status);
  }
}

function templateReply(message: string, status: string) {
  const lower = message.toLowerCase();
  if (lower.includes("speak") || lower.includes("talk")) return { reply: `Shlok is currently ${status.toLowerCase()}. Would you like to leave a message?`, source: "template" };
  if (lower.includes("urgent")) return { reply: "Understood — I'll notify Shlok immediately.", source: "template" };
  return { reply: `Hello. I am JARVIS, Shlok's personal AI assistant. Shlok is currently ${status.toLowerCase()}. Would you like to leave a message?`, source: "template" };
}
