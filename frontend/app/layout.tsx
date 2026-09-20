import "./globals.css";
export const metadata = {
  title: "JARVIS — Shlok's AI Assistant",
  description: "Mobile-only JARVIS call assistant. Background-first, real cellular handling.",
};
export const viewport = {
  width: "device-width",
  initialScale: 1,
  maximumScale: 1,
};
export default function RootLayout({ children }: { children: React.ReactNode }) {
  return <html lang="en"><body>{children}</body></html>;
}
