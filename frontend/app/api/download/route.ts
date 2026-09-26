import { NextRequest } from "next/server";
import fs from "node:fs";
import path from "node:path";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

const APK_FILE = "JARVIS-v2.1.0.apk";
const APK_VERSION = "2.1.0";

function resolveApkPath(): string {
  const candidates = [
    path.join(process.cwd(), "public", APK_FILE),
    path.join(process.cwd(), "frontend", "public", APK_FILE),
  ];
  for (const p of candidates) {
    try {
      if (fs.existsSync(p)) return p;
    } catch {}
  }
  return candidates[0];
}

function baseHeaders(size: number) {
  return {
    "Content-Type": "application/vnd.android.package-archive",
    "Content-Disposition": `attachment; filename="${APK_FILE}"`,
    "Accept-Ranges": "bytes",
    "Cache-Control": "public, max-age=31536000, immutable",
    "X-Content-Type-Options": "nosniff",
    "Content-Length": String(size),
  } as Record<string, string>;
}

export async function GET(req: NextRequest) {
  const filePath = resolveApkPath();
  if (!fs.existsSync(filePath)) {
    return new Response("APK not found", { status: 404 });
  }

  const stat = fs.statSync(filePath);
  const size = stat.size;

  const range = req.headers.get("range");
  if (!range) {
    // Full download — stream from disk so mobile DownloadManager gets
    // an exact Content-Length and doesn't stall part-way.
    const stream = fs.createReadStream(filePath);
    const webStream = new ReadableStream({
      start(controller) {
        stream.on("data", (chunk: string | Buffer) =>
          controller.enqueue(
            typeof chunk === "string" ? new TextEncoder().encode(chunk) : chunk
          )
        );
        stream.on("end", () => controller.close());
        stream.on("error", (e) => controller.error(e));
      },
      cancel() {
        stream.destroy();
      },
    });
    return new Response(webStream, { status: 200, headers: baseHeaders(size) });
  }

  // Partial content — required for resume on flaky mobile networks.
  // Without 206 support Chrome on Android stalls instead of resuming.
  const m = /^bytes=(\d*)-(\d*)$/.exec(range.trim());
  if (!m) {
    return new Response("Invalid range", {
      status: 416,
      headers: { "Content-Range": `bytes */${size}` },
    });
  }

  let start = m[1] === "" ? NaN : parseInt(m[1], 10);
  let end = m[2] === "" ? NaN : parseInt(m[2], 10);

  if (isNaN(start)) {
    // suffix range: last N bytes
    if (isNaN(end)) {
      return new Response("Invalid range", {
        status: 416,
        headers: { "Content-Range": `bytes */${size}` },
      });
    }
    start = Math.max(0, size - end);
    end = size - 1;
  } else if (isNaN(end)) {
    end = size - 1;
  }

  if (start >= size || end >= size || start > end) {
    return new Response("Range not satisfiable", {
      status: 416,
      headers: { "Content-Range": `bytes */${size}` },
    });
  }

  const chunkSize = end - start + 1;
  const stream = fs.createReadStream(filePath, { start, end });
  const webStream = new ReadableStream({
    start(controller) {
      stream.on("data", (chunk: string | Buffer) =>
        controller.enqueue(
          typeof chunk === "string" ? new TextEncoder().encode(chunk) : chunk
        )
      );
      stream.on("end", () => controller.close());
      stream.on("error", (e) => controller.error(e));
    },
    cancel() {
      stream.destroy();
    },
  });

  return new Response(webStream, {
    status: 206,
    headers: {
      ...baseHeaders(chunkSize),
      "Content-Range": `bytes ${start}-${end}/${size}`,
    },
  });
}

export async function HEAD() {
  const filePath = resolveApkPath();
  if (!fs.existsSync(filePath)) {
    return new Response(null, { status: 404 });
  }
  const size = fs.statSync(filePath).size;
  return new Response(null, { status: 200, headers: baseHeaders(size) });
}
