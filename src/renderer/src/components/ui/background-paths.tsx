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

    // Layered flowing geometric ribbon paths
    const render = () => {
      step += 0.004;
      ctx.clearRect(0, 0, width, height);

      // Get current theme color from computed style
      const primaryColor =
        getComputedStyle(document.documentElement)
          .getPropertyValue("--primary")
          .trim() || "#6366f1";

      // 1. Subtle radial ambient glow at the top center
      const gradient = ctx.createRadialGradient(
        width * 0.5,
        height * 0.15,
        10,
        width * 0.5,
        height * 0.15,
        Math.max(width, height) * 0.65
      );
      gradient.addColorStop(0, primaryColor);
      gradient.addColorStop(1, "transparent");

      ctx.save();
      ctx.globalAlpha = 0.08;
      ctx.fillStyle = gradient;
      ctx.fillRect(0, 0, width, height);
      ctx.restore();

      // 2. 18 Vibrant Layered Flowing Path Ribbons (Two opposing waves)
      const lineCount = 18;
      for (let i = 0; i < lineCount; i++) {
        const factor = i / lineCount;
        const offset = i * 0.35;

        // Wave 1: Flowing top-left to bottom-right
        const y1_a = height * 0.15 + Math.sin(step + offset) * 55 + i * 24;
        const y2_a = height * 0.55 + Math.cos(step * 0.85 + offset) * 75 + i * 12;
        const y3_a = height * 0.35 + Math.sin(step * 1.15 + offset) * 65 + i * 18;

        ctx.beginPath();
        ctx.moveTo(-60, y1_a);
        ctx.bezierCurveTo(
          width * 0.3,
          y1_a + Math.cos(step + i * 0.2) * 50,
          width * 0.65,
          y2_a + Math.sin(step + i * 0.2) * 60,
          width + 60,
          y3_a
        );

        ctx.strokeStyle = primaryColor;
        ctx.lineWidth = 1.2 + (i % 3) * 0.4;
        ctx.globalAlpha = 0.12 + factor * 0.28; // clearly visible opacity (12% to 40%)
        ctx.stroke();

        // Wave 2: Opposing counter-wave flowing bottom-left to top-right
        const y1_b = height * 0.85 - Math.cos(step * 0.9 + offset) * 50 - i * 22;
        const y2_b = height * 0.45 - Math.sin(step * 0.75 + offset) * 70 - i * 14;
        const y3_b = height * 0.65 - Math.cos(step * 1.05 + offset) * 60 - i * 16;

        ctx.beginPath();
        ctx.moveTo(-60, y1_b);
        ctx.bezierCurveTo(
          width * 0.35,
          y1_b - Math.sin(step + i * 0.3) * 45,
          width * 0.7,
          y2_b - Math.cos(step + i * 0.3) * 55,
          width + 60,
          y3_b
        );

        ctx.lineWidth = 1.0 + (i % 2) * 0.5;
        ctx.globalAlpha = 0.08 + factor * 0.22;
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
