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

    // ── Liquid Glass Shader Simulation ──
    const render = () => {
      step += 0.0035;
      ctx.clearRect(0, 0, width, height);

      // Check current mode and theme colors
      const isLightMode = document.documentElement.getAttribute("data-mode") === "light";
      const primaryColor =
        getComputedStyle(document.documentElement)
          .getPropertyValue("--primary")
          .trim() || "#6366f1";

      // 1. Flowing Liquid Ambient Orbs
      const orb1_x = width * 0.35 + Math.sin(step * 0.8) * (width * 0.2);
      const orb1_y = height * 0.25 + Math.cos(step * 0.6) * (height * 0.15);
      const orb1_r = Math.min(width, height) * 0.45;

      const orb1_grad = ctx.createRadialGradient(orb1_x, orb1_y, 0, orb1_x, orb1_y, orb1_r);
      orb1_grad.addColorStop(0, primaryColor);
      orb1_grad.addColorStop(1, "transparent");

      ctx.save();
      ctx.globalAlpha = isLightMode ? 0.12 : 0.14;
      ctx.fillStyle = orb1_grad;
      ctx.fillRect(0, 0, width, height);
      ctx.restore();

      const orb2_x = width * 0.75 + Math.cos(step * 0.7) * (width * 0.18);
      const orb2_y = height * 0.75 + Math.sin(step * 0.5) * (height * 0.2);
      const orb2_r = Math.min(width, height) * 0.5;

      const orb2_grad = ctx.createRadialGradient(orb2_x, orb2_y, 0, orb2_x, orb2_y, orb2_r);
      orb2_grad.addColorStop(0, isLightMode ? "#38bdf8" : "#10b981");
      orb2_grad.addColorStop(1, "transparent");

      ctx.save();
      ctx.globalAlpha = isLightMode ? 0.09 : 0.1;
      ctx.fillStyle = orb2_grad;
      ctx.fillRect(0, 0, width, height);
      ctx.restore();

      // 2. Liquid Glass Refractive Wave Ribbons
      const lineCount = 16;
      for (let i = 0; i < lineCount; i++) {
        const factor = i / lineCount;
        const offset = i * 0.4;

        // Wave Layer A
        const y1_a = height * 0.2 + Math.sin(step + offset) * 60 + i * 22;
        const y2_a = height * 0.6 + Math.cos(step * 0.8 + offset) * 80 + i * 10;
        const y3_a = height * 0.4 + Math.sin(step * 1.1 + offset) * 70 + i * 16;

        ctx.beginPath();
        ctx.moveTo(-60, y1_a);
        ctx.bezierCurveTo(
          width * 0.32,
          y1_a + Math.cos(step + i * 0.25) * 50,
          width * 0.68,
          y2_a + Math.sin(step + i * 0.25) * 60,
          width + 60,
          y3_a
        );

        ctx.strokeStyle = primaryColor;
        ctx.lineWidth = 1.3 + (i % 3) * 0.4;
        ctx.globalAlpha = isLightMode ? 0.08 + factor * 0.18 : 0.12 + factor * 0.26;
        ctx.stroke();

        // Wave Layer B (Counter liquid refraction)
        const y1_b = height * 0.82 - Math.cos(step * 0.85 + offset) * 55 - i * 20;
        const y2_b = height * 0.42 - Math.sin(step * 0.7 + offset) * 75 - i * 12;
        const y3_b = height * 0.62 - Math.cos(step * 1.0 + offset) * 65 - i * 14;

        ctx.beginPath();
        ctx.moveTo(-60, y1_b);
        ctx.bezierCurveTo(
          width * 0.38,
          y1_b - Math.sin(step + i * 0.3) * 45,
          width * 0.72,
          y2_b - Math.cos(step + i * 0.3) * 55,
          width + 60,
          y3_b
        );

        ctx.lineWidth = 1.1 + (i % 2) * 0.5;
        ctx.globalAlpha = isLightMode ? 0.06 + factor * 0.15 : 0.09 + factor * 0.2;
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
