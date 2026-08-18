import React, { useEffect, useRef } from "react";
import { Button } from "./button";

export function FloatingPaths() {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext("2d", { alpha: true });
    if (!ctx) return;

    let animationFrameId: number;
    let width = 0;
    let height = 0;
    let step = 0;

    const resize = () => {
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      width = window.innerWidth;
      height = window.innerHeight;
      canvas.width = width * dpr;
      canvas.height = height * dpr;
      canvas.style.width = `${width}px`;
      canvas.style.height = `${height}px`;
      ctx.setTransform(1, 0, 0, 1, 0, 0);
      ctx.scale(dpr, dpr);
    };

    window.addEventListener("resize", resize);
    resize();

    // 6 smooth ambient flowing bezier waves
    const render = () => {
      step += 0.003;
      ctx.clearRect(0, 0, width, height);

      // Get current theme color from computed style
      const primaryColor =
        getComputedStyle(document.documentElement)
          .getPropertyValue("--primary")
          .trim() || "#6366f1";

      ctx.lineWidth = 1.2;

      for (let i = 0; i < 6; i++) {
        const offset = i * 0.5;
        const y1 = height * 0.25 + Math.sin(step + offset) * 50 + i * 35;
        const y2 = height * 0.65 + Math.cos(step * 0.8 + offset) * 70;
        const y3 = height * 0.35 + Math.sin(step * 1.1 + offset) * 60;

        ctx.beginPath();
        ctx.moveTo(-50, y1);
        ctx.bezierCurveTo(
          width * 0.32,
          y1 + Math.cos(step + i) * 45,
          width * 0.68,
          y2 + Math.sin(step + i) * 55,
          width + 50,
          y3
        );

        ctx.strokeStyle = primaryColor;
        ctx.globalAlpha = 0.035 + (i / 6) * 0.045;
        ctx.stroke();
      }

      animationFrameId = requestAnimationFrame(render);
    };

    render();

    return () => {
      window.removeEventListener("resize", resize);
      cancelAnimationFrame(animationFrameId);
    };
  }, []);

  return (
    <canvas
      ref={canvasRef}
      style={{
        position: "fixed",
        inset: 0,
        width: "100%",
        height: "100%",
        pointerEvents: "none",
        zIndex: 0,
      }}
    />
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
      <FloatingPaths />

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
