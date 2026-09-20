import "./globals.css";
export const metadata = {
  title: "JARVIS — Shlok's AI Assistant",
  description: "Mobile-only JARVIS call assistant. Real cellular handling via native Android APK — Vercel is distribution only.",
  manifest: "/manifest.json",
  icons: {
    icon: "/icon-192.png",
    apple: "/apple-touch-icon.png",
  },
};
export const viewport = {
  width: "device-width",
  initialScale: 1,
  maximumScale: 1,
};
export default function RootLayout({ children }: { children: React.ReactNode }) {
  return <html lang="en"><body>{children}</body></html>;
}
