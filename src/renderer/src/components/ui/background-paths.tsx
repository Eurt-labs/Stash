import React from "react";
import { motion } from "framer-motion";
import { Button } from "./button";

// 12 elegantly spaced, bounded flowing curves within standard viewBox [0 0 1000 600]
const BASE_CURVES = [
  "M-50,120 C180,30 350,280 620,140 C800,40 920,220 1050,150",
  "M-50,180 C200,80 380,340 650,190 C830,90 940,270 1050,200",
  "M-50,240 C220,130 410,400 680,240 C860,140 960,320 1050,250",
  "M-50,300 C240,180 440,460 710,290 C890,190 980,370 1050,300",
  "M-50,360 C260,230 470,520 740,340 C920,240 1000,420 1050,350",
  "M-50,420 C280,280 500,580 770,390 C950,290 1020,470 1050,400",
  "M-50,80 C150,220 400,60 600,260 C780,420 900,120 1050,220",
  "M-50,140 C170,270 420,110 630,310 C810,470 920,170 1050,270",
  "M-50,200 C190,320 440,160 660,360 C840,520 940,220 1050,320",
  "M-50,260 C210,370 460,210 690,410 C870,570 960,270 1050,370",
  "M-50,320 C230,420 480,260 720,460 C900,620 980,320 1050,420",
  "M-50,380 C250,470 500,310 750,510 C930,670 1000,370 1050,470"
];

export function FloatingPaths({ position = 1 }: { position?: number }) {
  return (
    <div
      style={{
        position: 'absolute',
        inset: 0,
        pointerEvents: 'none',
        overflow: 'hidden',
        contain: 'strict',
        willChange: 'transform'
      }}
    >
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
        {BASE_CURVES.map((d, i) => {
          const opacity = 0.02 + (i / BASE_CURVES.length) * 0.05;
          const strokeWidth = 0.8 + (i % 3) * 0.4;
          const duration = 16 + (i % 6) * 3;

          return (
            <motion.path
              key={i}
              d={d}
              stroke="currentColor"
              strokeWidth={strokeWidth}
              strokeOpacity={opacity}
              initial={{ pathLength: 0.4, opacity: 0.3 }}
              animate={{
                pathLength: [0.3, 0.95, 0.3],
                opacity: [0.2, 0.5, 0.2]
              }}
              transition={{
                duration,
                repeat: Number.POSITIVE_INFINITY,
                ease: "easeInOut",
                delay: (i * 0.4)
              }}
            />
          );
        })}
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
        <FloatingPaths position={1} />
      </div>

      <div style={{ position: 'relative', zIndex: 10, textAlign: 'center', padding: '0 16px' }}>
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 1.5 }}
        >
          <h1 style={{ fontSize: '4rem', fontWeight: 800, marginBottom: '2rem', letterSpacing: '-0.05em' }}>
            {words.map((word, wordIndex) => (
              <span
                key={wordIndex}
                style={{ display: 'inline-block', marginRight: '1rem' }}
              >
                {word.split("").map((letter, letterIndex) => (
                  <motion.span
                    key={`${wordIndex}-${letterIndex}`}
                    initial={{ y: 80, opacity: 0 }}
                    animate={{ y: 0, opacity: 1 }}
                    transition={{
                      delay: wordIndex * 0.1 + letterIndex * 0.03,
                      type: "spring",
                      stiffness: 150,
                      damping: 25,
                    }}
                    style={{ display: 'inline-block' }}
                  >
                    {letter}
                  </motion.span>
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
        </motion.div>
      </div>
    </div>
  );
}
