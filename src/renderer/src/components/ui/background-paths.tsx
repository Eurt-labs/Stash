import React from "react";
import { Button } from "./button";

const BASE_CURVES = [
  { d: "M-50,120 C180,30 350,280 620,140 C800,40 920,220 1050,150", width: 1.2, opacity: 0.06, animClass: "path-flow-1" },
  { d: "M-50,200 C200,80 380,340 650,190 C830,90 940,270 1050,200", width: 0.9, opacity: 0.05, animClass: "path-flow-2" },
  { d: "M-50,280 C220,130 410,400 680,240 C860,140 960,320 1050,250", width: 1.1, opacity: 0.07, animClass: "path-flow-3" },
  { d: "M-50,360 C240,180 440,460 710,290 C890,190 980,370 1050,300", width: 0.8, opacity: 0.04, animClass: "path-flow-1" },
  { d: "M-50,140 C170,270 420,110 630,310 C810,470 920,170 1050,270", width: 1.0, opacity: 0.06, animClass: "path-flow-2" },
  { d: "M-50,240 C190,320 440,160 660,360 C840,520 940,220 1050,320", width: 1.2, opacity: 0.05, animClass: "path-flow-3" },
  { d: "M-50,340 C210,370 460,210 690,410 C870,570 960,270 1050,370", width: 0.9, opacity: 0.06, animClass: "path-flow-1" },
  { d: "M-50,440 C230,420 480,260 720,460 C900,620 980,320 1050,420", width: 1.1, opacity: 0.04, animClass: "path-flow-2" }
];

export function FloatingPaths() {
  return (
    <div
      style={{
        position: 'absolute',
        inset: 0,
        pointerEvents: 'none',
        overflow: 'hidden',
        contain: 'strict',
        transform: 'translateZ(0)'
      }}
    >
      <style>{`
        @keyframes floatFlow1 {
          0%, 100% { transform: translate3d(0, 0, 0); opacity: 0.55; }
          50% { transform: translate3d(12px, -8px, 0); opacity: 0.9; }
        }
        @keyframes floatFlow2 {
          0%, 100% { transform: translate3d(0, 0, 0); opacity: 0.45; }
          50% { transform: translate3d(-14px, 10px, 0); opacity: 0.8; }
        }
        @keyframes floatFlow3 {
          0%, 100% { transform: translate3d(0, 0, 0); opacity: 0.35; }
          50% { transform: translate3d(10px, 12px, 0); opacity: 0.75; }
        }
        .path-flow-1 { animation: floatFlow1 16s ease-in-out infinite; will-change: transform, opacity; }
        .path-flow-2 { animation: floatFlow2 22s ease-in-out infinite; will-change: transform, opacity; }
        .path-flow-3 { animation: floatFlow3 26s ease-in-out infinite; will-change: transform, opacity; }
      `}</style>
      <svg
        style={{
          width: '100%',
          height: '100%',
          color: 'var(--primary, #6366f1)',
          display: 'block'
        }}
        viewBox="0 0 1000 600"
        fill="none"
        preserveAspectRatio="none"
      >
        <title>Background Paths</title>
        {BASE_CURVES.map((item, i) => (
          <path
            key={i}
            d={item.d}
            className={item.animClass}
            stroke="currentColor"
            strokeWidth={item.width}
            strokeOpacity={item.opacity}
            strokeDasharray="14 8"
          />
        ))}
      </svg>
    </div>
  );
}

export function BackgroundPaths({
  title = "Background Paths",
}: {
  title?: string;
}) {
  const words = title.split(" ");

  return (
    <div style={{ position: 'relative', minHeight: '100vh', width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden' }}>
      <div style={{ position: 'absolute', inset: 0 }}>
        <FloatingPaths />
      </div>

      <div style={{ position: 'relative', zIndex: 10, textAlign: 'center', padding: '0 16px' }}>
        <div>
          <h1 style={{ fontSize: '4rem', fontWeight: 800, marginBottom: '2rem', letterSpacing: '-0.05em' }}>
            {words.map((word, wordIndex) => (
              <span
                key={wordIndex}
                style={{ display: 'inline-block', marginRight: '1rem' }}
              >
                {word.split("").map((letter, letterIndex) => (
                  <span
                    key={`${wordIndex}-${letterIndex}`}
                    style={{ display: 'inline-block' }}
                  >
                    {letter}
                  </span>
                ))}
              </span>
            ))}
          </h1>

          <Button
            variant="ghost"
            style={{ fontSize: '1rem', fontWeight: 600, padding: '1.5rem 2rem', borderRadius: '1rem' }}
          >
            <span>Discover Excellence</span>
            <span style={{ marginLeft: '0.75rem' }}>→</span>
          </Button>
        </div>
      </div>
    </div>
  );
}
