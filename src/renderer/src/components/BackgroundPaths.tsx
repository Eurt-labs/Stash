import React, { useEffect, useRef } from "react";

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

    // Pre-generate static star field for Taylor Swift & particles for Billie/Weeknd
    const stars = Array.from({ length: 45 }, () => ({
      x: Math.random(),
      y: Math.random(),
      size: 0.8 + Math.random() * 2,
      phase: Math.random() * Math.PI * 2,
      speed: 0.5 + Math.random() * 1.5
    }));

    const particles = Array.from({ length: 30 }, () => ({
      x: Math.random(),
      y: Math.random(),
      size: 1.5 + Math.random() * 3,
      speedY: 0.2 + Math.random() * 0.5,
      speedX: (Math.random() - 0.5) * 0.3,
      opacity: 0.2 + Math.random() * 0.6
    }));

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

    // ── Multi-Artist Art Style Canvas Renderer ──
    const render = () => {
      step += 0.0035;
      ctx.clearRect(0, 0, width, height);

      const isLight = document.documentElement.getAttribute("data-mode") === "light";
      const theme = document.documentElement.getAttribute("data-theme") || "indigo";
      const primaryColor =
        getComputedStyle(document.documentElement)
          .getPropertyValue("--primary")
          .trim() || "#6366f1";

      // ══════════════════════════════════════════════════════════════
      // 1. BILLIE EILISH: Deep Ocean Caustics & Underwater Particles
      // ══════════════════════════════════════════════════════════════
      if (theme === "billie") {
        // Deep aquatic blue backdrop gradient
        const oceanGrad = ctx.createLinearGradient(0, 0, 0, height);
        oceanGrad.addColorStop(0, isLight ? "#e0f2fe" : "#040d1a");
        oceanGrad.addColorStop(0.5, isLight ? "#bae6fd" : "#061528");
        oceanGrad.addColorStop(1, isLight ? "#f0f9ff" : "#020710");
        ctx.fillStyle = oceanGrad;
        ctx.fillRect(0, 0, width, height);

        // Sunken caustic light rays from the top
        ctx.save();
        for (let r = 0; r < 4; r++) {
          const rayX = width * (0.2 + r * 0.22) + Math.sin(step * 0.8 + r) * 40;
          const rayGrad = ctx.createLinearGradient(rayX, 0, rayX + (r - 1.5) * 80, height * 0.85);
          rayGrad.addColorStop(0, "rgba(6, 182, 212, 0.22)");
          rayGrad.addColorStop(0.6, "rgba(34, 211, 238, 0.06)");
          rayGrad.addColorStop(1, "transparent");

          ctx.beginPath();
          ctx.moveTo(rayX - 30, 0);
          ctx.lineTo(rayX + 30, 0);
          ctx.lineTo(rayX + (r - 1.5) * 120 + 80, height * 0.9);
          ctx.lineTo(rayX + (r - 1.5) * 120 - 80, height * 0.9);
          ctx.closePath();
          ctx.fillStyle = rayGrad;
          ctx.fill();
        }
        ctx.restore();

        // Floating aquatic glowing bubbles
        particles.forEach((p, idx) => {
          const py = (p.y - (step * p.speedY * 0.08) % 1) * height;
          const px = (p.x + Math.sin(step + idx) * 0.05) * width;
          ctx.beginPath();
          ctx.arc(px, py, p.size, 0, Math.PI * 2);
          ctx.fillStyle = isLight ? "rgba(6, 182, 212, 0.4)" : "rgba(34, 211, 238, 0.6)";
          ctx.shadowColor = "#06b6d4";
          ctx.shadowBlur = 8;
          ctx.fill();
          ctx.shadowBlur = 0;
        });

        // Flowing deep water ribbons
        for (let i = 0; i < 10; i++) {
          const y1 = height * 0.35 + Math.sin(step * 0.9 + i * 0.4) * 60 + i * 28;
          const y2 = height * 0.75 + Math.cos(step * 0.7 + i * 0.4) * 70 + i * 14;
          ctx.beginPath();
          ctx.moveTo(-50, y1);
          ctx.bezierCurveTo(width * 0.35, y1 + 50, width * 0.65, y2 - 40, width + 50, y2);
          ctx.strokeStyle = "#06b6d4";
          ctx.lineWidth = 1.6;
          ctx.globalAlpha = 0.12 + (i / 10) * 0.22;
          ctx.stroke();
        }
      }

      // ══════════════════════════════════════════════════════════════
      // 2. THE WEEKND: After Hours Crimson Synthwave City Horizon
      // ══════════════════════════════════════════════════════════════
      else if (theme === "weeknd") {
        // Dark crimson noir gradient
        const weekndGrad = ctx.createLinearGradient(0, 0, 0, height);
        weekndGrad.addColorStop(0, isLight ? "#fff1f2" : "#0d0204");
        weekndGrad.addColorStop(0.55, isLight ? "#ffe4e6" : "#24040a");
        weekndGrad.addColorStop(1, isLight ? "#fecdd3" : "#090103");
        ctx.fillStyle = weekndGrad;
        ctx.fillRect(0, 0, width, height);

        // Pulsing After Hours neon red horizon glow
        const horizonGrad = ctx.createRadialGradient(
          width * 0.5,
          height * 0.65,
          10,
          width * 0.5,
          height * 0.65,
          width * 0.65
        );
        horizonGrad.addColorStop(0, "rgba(255, 30, 66, 0.32)");
        horizonGrad.addColorStop(0.6, "rgba(225, 29, 72, 0.12)");
        horizonGrad.addColorStop(1, "transparent");
        ctx.fillStyle = horizonGrad;
        ctx.fillRect(0, 0, width, height);

        // Synthwave Perspective Laser Grid
        const horizonY = height * 0.65;
        ctx.strokeStyle = "rgba(255, 30, 66, 0.3)";
        ctx.lineWidth = 1.2;

        // Horizontal grid lines moving toward viewer
        for (let g = 0; g < 7; g++) {
          const ratio = Math.pow(g / 7, 2);
          const lineY = horizonY + ratio * (height - horizonY);
          ctx.beginPath();
          ctx.moveTo(0, lineY);
          ctx.lineTo(width, lineY);
          ctx.stroke();
        }

        // Perspective fan lines
        for (let f = -6; f <= 6; f++) {
          const startX = width * 0.5 + f * (width * 0.09);
          ctx.beginPath();
          ctx.moveTo(width * 0.5, horizonY);
          ctx.lineTo(startX, height);
          ctx.stroke();
        }

        // Flowing crimson waves across top
        for (let i = 0; i < 8; i++) {
          const y = height * 0.2 + Math.sin(step * 1.2 + i * 0.5) * 45 + i * 22;
          ctx.beginPath();
          ctx.moveTo(-50, y);
          ctx.bezierCurveTo(width * 0.3, y - 40, width * 0.7, y + 40, width + 50, y);
          ctx.strokeStyle = "#ff1e42";
          ctx.lineWidth = 1.8;
          ctx.globalAlpha = 0.15 + (i / 8) * 0.3;
          ctx.stroke();
        }
      }

      // ══════════════════════════════════════════════════════════════
      // 3. TAYLOR SWIFT: Midnights Starry Constellations & Lavender Mist
      // ══════════════════════════════════════════════════════════════
      else if (theme === "taylor") {
        // Deep twilight lavender nebula
        const twilightGrad = ctx.createLinearGradient(0, 0, width, height);
        twilightGrad.addColorStop(0, isLight ? "#faf5ff" : "#090514");
        twilightGrad.addColorStop(0.5, isLight ? "#f3e8ff" : "#170c2e");
        twilightGrad.addColorStop(1, isLight ? "#ede9fe" : "#06030d");
        ctx.fillStyle = twilightGrad;
        ctx.fillRect(0, 0, width, height);

        // Luminous lavender ambient cloud
        const nebulaGrad = ctx.createRadialGradient(
          width * 0.6,
          height * 0.35,
          20,
          width * 0.6,
          height * 0.35,
          width * 0.5
        );
        nebulaGrad.addColorStop(0, "rgba(144, 97, 249, 0.28)");
        nebulaGrad.addColorStop(0.7, "rgba(168, 85, 247, 0.08)");
        nebulaGrad.addColorStop(1, "transparent");
        ctx.fillStyle = nebulaGrad;
        ctx.fillRect(0, 0, width, height);

        // Twinkling Star Constellations
        const starCoords: Array<{ x: number; y: number }> = [];
        stars.forEach((s) => {
          const sx = s.x * width;
          const sy = s.y * height;
          starCoords.push({ x: sx, y: sy });
          const twinkle = 0.3 + 0.7 * Math.abs(Math.sin(step * s.speed + s.phase));

          ctx.beginPath();
          ctx.arc(sx, sy, s.size, 0, Math.PI * 2);
          ctx.fillStyle = `rgba(216, 180, 254, ${twinkle})`;
          ctx.shadowColor = "#ac8af8";
          ctx.shadowBlur = 6;
          ctx.fill();
          ctx.shadowBlur = 0;
        });

        // Fine constellation connecting lines
        ctx.strokeStyle = "rgba(168, 85, 247, 0.16)";
        ctx.lineWidth = 0.8;
        for (let c = 0; c < starCoords.length - 1; c += 3) {
          ctx.beginPath();
          ctx.moveTo(starCoords[c].x, starCoords[c].y);
          ctx.lineTo(starCoords[c + 1].x, starCoords[c + 1].y);
          ctx.stroke();
        }

        // Starry lavender wave ribbons
        for (let i = 0; i < 10; i++) {
          const y = height * 0.3 + Math.sin(step * 0.8 + i * 0.4) * 50 + i * 26;
          ctx.beginPath();
          ctx.moveTo(-50, y);
          ctx.bezierCurveTo(width * 0.35, y + 45, width * 0.65, y - 45, width + 50, y);
          ctx.strokeStyle = "#9061f9";
          ctx.lineWidth = 1.4;
          ctx.globalAlpha = 0.1 + (i / 10) * 0.25;
          ctx.stroke();
        }
      }

      // ══════════════════════════════════════════════════════════════
      // 4. DAFT PUNK: RAM Golden Chrome & Laser Matrix
      // ══════════════════════════════════════════════════════════════
      else if (theme === "daftpunk") {
        const goldGrad = ctx.createLinearGradient(0, 0, width, height);
        goldGrad.addColorStop(0, isLight ? "#fefce8" : "#0d0a03");
        goldGrad.addColorStop(0.5, isLight ? "#fef9c3" : "#221906");
        goldGrad.addColorStop(1, isLight ? "#fef08a" : "#080601");
        ctx.fillStyle = goldGrad;
        ctx.fillRect(0, 0, width, height);

        // Golden chrome laser sweeps
        for (let i = 0; i < 12; i++) {
          const angle = step * 0.5 + i * 0.5;
          const y1 = height * 0.2 + Math.sin(angle) * 80 + i * 30;
          const y2 = height * 0.8 - Math.cos(angle) * 80 - i * 15;

          ctx.beginPath();
          ctx.moveTo(-50, y1);
          ctx.bezierCurveTo(width * 0.4, y1 + 60, width * 0.6, y2 - 60, width + 50, y2);
          ctx.strokeStyle = i % 2 === 0 ? "#fbbf24" : "#fde047";
          ctx.lineWidth = 1.6;
          ctx.globalAlpha = 0.12 + (i / 12) * 0.28;
          ctx.stroke();
        }
      }

      // ══════════════════════════════════════════════════════════════
      // 5. TRAVIS SCOTT: Utopia Neon Emerald Sand Dunes
      // ══════════════════════════════════════════════════════════════
      else if (theme === "travis") {
        const desertGrad = ctx.createLinearGradient(0, 0, 0, height);
        desertGrad.addColorStop(0, isLight ? "#f0fdf4" : "#020d07");
        desertGrad.addColorStop(0.6, isLight ? "#dcfce7" : "#051f12");
        desertGrad.addColorStop(1, isLight ? "#bbf7d0" : "#010804");
        ctx.fillStyle = desertGrad;
        ctx.fillRect(0, 0, width, height);

        // Neon desert dune waves
        for (let d = 0; d < 8; d++) {
          const duneY = height * 0.45 + Math.sin(step * 0.7 + d * 0.6) * 45 + d * 35;
          ctx.beginPath();
          ctx.moveTo(-50, duneY);
          ctx.bezierCurveTo(width * 0.3, duneY - 40, width * 0.7, duneY + 50, width + 50, duneY);
          ctx.strokeStyle = "#10b981";
          ctx.lineWidth = 1.8;
          ctx.globalAlpha = 0.14 + (d / 8) * 0.3;
          ctx.stroke();
        }
      }

      // ══════════════════════════════════════════════════════════════
      // 6. LANA DEL REY: Vintage Rose Quartz & Vinyl Record Grooves
      // ══════════════════════════════════════════════════════════════
      else if (theme === "lana") {
        const roseGrad = ctx.createLinearGradient(0, 0, width, height);
        roseGrad.addColorStop(0, isLight ? "#fff1f2" : "#0f0307");
        roseGrad.addColorStop(0.5, isLight ? "#fce7f3" : "#240814");
        roseGrad.addColorStop(1, isLight ? "#fbcfe8" : "#080104");
        ctx.fillStyle = roseGrad;
        ctx.fillRect(0, 0, width, height);

        // Concentric Vinyl Record Grooves in bottom corner
        const centerX = width * 0.85;
        const centerY = height * 0.75;
        for (let v = 0; v < 8; v++) {
          const r = 60 + v * 38;
          ctx.beginPath();
          ctx.arc(centerX, centerY, r, 0, Math.PI * 2);
          ctx.strokeStyle = "rgba(244, 114, 182, 0.18)";
          ctx.lineWidth = 1.2;
          ctx.stroke();
        }

        // Soft romantic vintage flowing curves
        for (let i = 0; i < 9; i++) {
          const y = height * 0.25 + Math.sin(step * 0.8 + i * 0.4) * 40 + i * 28;
          ctx.beginPath();
          ctx.moveTo(-50, y);
          ctx.bezierCurveTo(width * 0.35, y + 50, width * 0.65, y - 30, width + 50, y);
          ctx.strokeStyle = "#f472b6";
          ctx.lineWidth = 1.5;
          ctx.globalAlpha = 0.12 + (i / 9) * 0.24;
          ctx.stroke();
        }
      }

      // ══════════════════════════════════════════════════════════════
      // 7. STANDARD LIQUID GLASS MESH (Indigo, Emerald, Sunset, etc.)
      // ══════════════════════════════════════════════════════════════
      else {
        // Flowing Ambient Orbs
        const orb1_x = width * 0.35 + Math.sin(step * 0.8) * (width * 0.2);
        const orb1_y = height * 0.25 + Math.cos(step * 0.6) * (height * 0.15);
        const orb1_r = Math.min(width, height) * 0.45;

        const orb1_grad = ctx.createRadialGradient(orb1_x, orb1_y, 0, orb1_x, orb1_y, orb1_r);
        orb1_grad.addColorStop(0, primaryColor);
        orb1_grad.addColorStop(1, "transparent");

        ctx.save();
        ctx.globalAlpha = isLight ? 0.12 : 0.16;
        ctx.fillStyle = orb1_grad;
        ctx.fillRect(0, 0, width, height);
        ctx.restore();

        // 16 Flowing Liquid Wave Ribbons
        const lineCount = 16;
        for (let i = 0; i < lineCount; i++) {
          const factor = i / lineCount;
          const offset = i * 0.4;

          const y1 = height * 0.2 + Math.sin(step + offset) * 60 + i * 22;
          const y2 = height * 0.6 + Math.cos(step * 0.8 + offset) * 80 + i * 10;
          const y3 = height * 0.4 + Math.sin(step * 1.1 + offset) * 70 + i * 16;

          ctx.beginPath();
          ctx.moveTo(-60, y1);
          ctx.bezierCurveTo(
            width * 0.32,
            y1 + Math.cos(step + i * 0.25) * 50,
            width * 0.68,
            y2 + Math.sin(step + i * 0.25) * 60,
            width + 60,
            y3
          );

          ctx.strokeStyle = primaryColor;
          ctx.lineWidth = 1.3 + (i % 3) * 0.4;
          ctx.globalAlpha = isLight ? 0.08 + factor * 0.18 : 0.12 + factor * 0.28;
          ctx.stroke();
        }
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
