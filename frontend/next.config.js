/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  // No localhost hardcoding - API URL comes from env
  env: {
    NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL || "",
  },
};
module.exports = nextConfig;
