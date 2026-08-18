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

    // Pre-generate stars for Taylor Swift
    const stars = Array.from({ length: 50 }, () => ({
      x: Math.random(),
      y: Math.random() * 0.8,
      size: 1 + Math.random() * 2.5,
      phase: Math.random() * Math.PI * 2,
      speed: 0.8 + Math.random() * 2
    }));

    // Pre-generate particles for Billie & Travis
    const particles = Array.from({ length: 35 }, () => ({
      x: Math.random(),
      y: Math.random(),
      size: 1.5 + Math.random() * 3.5,
      speedY: 0.3 + Math.random() * 0.7,
      phase: Math.random() * Math.PI * 2
    }));

    // City skyscrapers for The Weeknd
    const buildings = Array.from({ length: 28 }, (_, i) => ({
      x: i / 28,
      w: 0.03 + Math.random() * 0.02,
      h: 40 + Math.random() * 90
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

    // ── High-Impact Live Artist Art Styles Renderer ──
    const render = () => {
      step += 0.005;
      ctx.clearRect(0, 0, width, height);

      const isLight = document.documentElement.getAttribute("data-mode") === "light";
      const theme = document.documentElement.getAttribute("data-theme") || "indigo";
      const primaryColor =
        getComputedStyle(document.documentElement)
          .getPropertyValue("--primary")
          .trim() || "#6366f1";

      // ══════════════════════════════════════════════════════════════
      // 1. THE WEEKND: After Hours Synthwave Sun & City Skyline
      // ══════════════════════════════════════════════════════════════
      if (theme === "weeknd") {
        // Dark Crimson / Synthwave Sky
        const skyGrad = ctx.createLinearGradient(0, 0, 0, height);
        skyGrad.addColorStop(0, isLight ? "#ffe4e6" : "#0d0205");
        skyGrad.addColorStop(0.55, isLight ? "#fecdd3" : "#20030a");
        skyGrad.addColorStop(1, isLight ? "#fda4af" : "#060102");
        ctx.fillStyle = skyGrad;
        ctx.fillRect(0, 0, width, height);

        const horizonY = height * 0.58;

        // Radiant Glowing Segmented Synthwave Sun
        const sunX = width * 0.5;
        const sunY = horizonY - 10;
        const sunR = Math.min(width, height) * 0.22;

        const sunGrad = ctx.createLinearGradient(sunX, sunY - sunR, sunX, sunY + sunR);
        sunGrad.addColorStop(0, "#ff1e42");
        sunGrad.addColorStop(0.4, "#fb7185");
        sunGrad.addColorStop(1, "#f43f5e");

        ctx.save();
        ctx.beginPath();
        ctx.arc(sunX, sunY, sunR, Math.PI, 0, false);
        ctx.fillStyle = sunGrad;
        ctx.shadowColor = "#ff1e42";
        ctx.shadowBlur = 40;
        ctx.fill();
        ctx.shadowBlur = 0;

        // Sun horizontal segments
        ctx.fillStyle = isLight ? "#fecdd3" : "#20030a";
        for (let s = 1; s <= 5; s++) {
          const segY = sunY - (s * (sunR / 6));
          ctx.fillRect(sunX - sunR - 10, segY, (sunR + 10) * 2, 3 + s * 1.2);
        }
        ctx.restore();

        // City Skyline Silhouettes across horizon
        ctx.fillStyle = isLight ? "rgba(225, 29, 72, 0.35)" : "rgba(10, 2, 4, 0.95)";
        buildings.forEach((b) => {
          const bx = b.x * width;
          const bw = b.w * width;
          ctx.fillRect(bx, horizonY - b.h, bw, b.h);
        });

        // Bold Synthwave Perspective Grid
        ctx.strokeStyle = isLight ? "rgba(225, 29, 72, 0.45)" : "rgba(255, 30, 66, 0.65)";
        ctx.lineWidth = 1.5;

        // Horizontal grid lines
        for (let g = 1; g <= 9; g++) {
          const ratio = Math.pow(g / 9, 2.2);
          const lineY = horizonY + ratio * (height - horizonY);
          ctx.beginPath();
          ctx.moveTo(0, lineY);
          ctx.lineTo(width, lineY);
          ctx.stroke();
        }

        // Converging perspective fan lines
        for (let f = -9; f <= 9; f++) {
          const startX = width * 0.5 + f * (width * 0.075);
          ctx.beginPath();
          ctx.moveTo(width * 0.5, horizonY);
          ctx.lineTo(startX, height);
          ctx.stroke();
        }
      }

      // ══════════════════════════════════════════════════════════════
      // 2. BILLIE EILISH: Deep Ocean Caustics & Glowing Aquatic Orbs
      // ══════════════════════════════════════════════════════════════
      else if (theme === "billie") {
        // Deep Underwater Ocean Backdrop
        const oceanGrad = ctx.createLinearGradient(0, 0, 0, height);
        oceanGrad.addColorStop(0, isLight ? "#e0f2fe" : "#020914");
        oceanGrad.addColorStop(0.5, isLight ? "#7dd3fc" : "#051830");
        oceanGrad.addColorStop(1, isLight ? "#bae6fd" : "#01040a");
        ctx.fillStyle = oceanGrad;
        ctx.fillRect(0, 0, width, height);

        // Powerful Sunken Caustic Light Beams
        ctx.save();
        for (let r = 0; r < 5; r++) {
          const rayX = width * (0.15 + r * 0.18) + Math.sin(step * 0.6 + r) * 50;
          const rayGrad = ctx.createLinearGradient(rayX, 0, rayX + (r - 2) * 120, height);
          rayGrad.addColorStop(0, "rgba(6, 182, 212, 0.45)");
          rayGrad.addColorStop(0.5, "rgba(34, 211, 238, 0.18)");
          rayGrad.addColorStop(1, "transparent");

          ctx.beginPath();
          ctx.moveTo(rayX - 45, 0);
          ctx.lineTo(rayX + 45, 0);
          ctx.lineTo(rayX + (r - 2) * 160 + 100, height);
          ctx.lineTo(rayX + (r - 2) * 160 - 100, height);
          ctx.closePath();
          ctx.fillStyle = rayGrad;
          ctx.fill();
        }
        ctx.restore();

        // Floating glowing aquatic bubbles & light specks
        particles.forEach((p, idx) => {
          const py = (p.y - (step * p.speedY * 0.1) % 1) * height;
          const px = (p.x + Math.sin(step * 1.5 + idx) * 0.08) * width;
          ctx.beginPath();
          ctx.arc(px, py, p.size * 1.4, 0, Math.PI * 2);
          ctx.fillStyle = isLight ? "rgba(6, 182, 212, 0.6)" : "rgba(34, 211, 238, 0.8)";
          ctx.shadowColor = "#06b6d4";
          ctx.shadowBlur = 12;
          ctx.fill();
          ctx.shadowBlur = 0;
        });

        // Flowing Electric Water Caustic Waves
        for (let i = 0; i < 12; i++) {
          const y1 = height * 0.25 + Math.sin(step + i * 0.5) * 70 + i * 30;
          const y2 = height * 0.65 + Math.cos(step * 0.8 + i * 0.5) * 80 + i * 15;
          ctx.beginPath();
          ctx.moveTo(-50, y1);
          ctx.bezierCurveTo(width * 0.35, y1 + 70, width * 0.65, y2 - 60, width + 50, y2);
          ctx.strokeStyle = "#06b6d4";
          ctx.lineWidth = 2.2;
          ctx.globalAlpha = 0.25 + (i / 12) * 0.35;
          ctx.stroke();
        }
      }

      // ══════════════════════════════════════════════════════════════
      // 3. TAYLOR SWIFT: Midnights Starry Constellations & Glowing Moon
      // ══════════════════════════════════════════════════════════════
      else if (theme === "taylor") {
        // Deep Midnight Sapphire / Lavender Nebula
        const twilightGrad = ctx.createLinearGradient(0, 0, width, height);
        twilightGrad.addColorStop(0, isLight ? "#faf5ff" : "#080414");
        twilightGrad.addColorStop(0.5, isLight ? "#e9d5ff" : "#190a36");
        twilightGrad.addColorStop(1, isLight ? "#f3e8ff" : "#05020c");
        ctx.fillStyle = twilightGrad;
        ctx.fillRect(0, 0, width, height);

        // Luminous Crescent Moon in top right
        const moonX = width * 0.85;
        const moonY = height * 0.22;
        const moonR = 42;

        ctx.save();
        ctx.shadowColor = "#ac8af8";
        ctx.shadowBlur = 30;
        ctx.fillStyle = "#f3e8ff";
        ctx.beginPath();
        ctx.arc(moonX, moonY, moonR, 0, Math.PI * 2);
        ctx.fill();

        // Inner shadow to form crescent
        ctx.globalCompositeOperation = "destination-out";
        ctx.beginPath();
        ctx.arc(moonX - 14, moonY - 8, moonR * 0.92, 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();

        // Swirling Lavender Nebula Aura
        const nebulaGrad = ctx.createRadialGradient(
          width * 0.45,
          height * 0.4,
          20,
          width * 0.45,
          height * 0.4,
          width * 0.55
        );
        nebulaGrad.addColorStop(0, "rgba(168, 85, 247, 0.45)");
        nebulaGrad.addColorStop(0.6, "rgba(144, 97, 249, 0.18)");
        nebulaGrad.addColorStop(1, "transparent");
        ctx.fillStyle = nebulaGrad;
        ctx.fillRect(0, 0, width, height);

        // 50 Twinkling Star Constellations
        const starCoords: Array<{ x: number; y: number }> = [];
        stars.forEach((s) => {
          const sx = s.x * width;
          const sy = s.y * height;
          starCoords.push({ x: sx, y: sy });
          const twinkle = 0.4 + 0.6 * Math.abs(Math.sin(step * s.speed + s.phase));

          ctx.beginPath();
          ctx.arc(sx, sy, s.size * 1.5, 0, Math.PI * 2);
          ctx.fillStyle = `rgba(233, 213, 255, ${twinkle})`;
          ctx.shadowColor = "#c084fc";
          ctx.shadowBlur = 10;
          ctx.fill();
          ctx.shadowBlur = 0;
        });

        // Constellation interconnect lines
        ctx.strokeStyle = "rgba(192, 132, 252, 0.35)";
        ctx.lineWidth = 1;
        for (let c = 0; c < starCoords.length - 1; c += 2) {
          ctx.beginPath();
          ctx.moveTo(starCoords[c].x, starCoords[c].y);
          ctx.lineTo(starCoords[c + 1].x, starCoords[c + 1].y);
          ctx.stroke();
        }

        // Flowing stardust ribbons
        for (let i = 0; i < 10; i++) {
          const y = height * 0.25 + Math.sin(step * 0.8 + i * 0.4) * 60 + i * 30;
          ctx.beginPath();
          ctx.moveTo(-50, y);
          ctx.bezierCurveTo(width * 0.35, y + 60, width * 0.65, y - 50, width + 50, y);
          ctx.strokeStyle = "#9061f9";
          ctx.lineWidth = 1.8;
          ctx.globalAlpha = 0.2 + (i / 10) * 0.35;
          ctx.stroke();
        }
      }

      // ══════════════════════════════════════════════════════════════
      // 4. DAFT PUNK: RAM Golden Chrome Pyramid & Disco Equalizer
      // ══════════════════════════════════════════════════════════════
      else if (theme === "daftpunk") {
        const goldGrad = ctx.createLinearGradient(0, 0, 0, height);
        goldGrad.addColorStop(0, isLight ? "#fefce8" : "#0d0a02");
        goldGrad.addColorStop(0.5, isLight ? "#fef08a" : "#261a04");
        goldGrad.addColorStop(1, isLight ? "#fde047" : "#060401");
        ctx.fillStyle = goldGrad;
        ctx.fillRect(0, 0, width, height);

        // Radiant Pyramid Laser Beams shooting from bottom center
        const pX = width * 0.5;
        const pY = height * 0.85;

        for (let beam = -6; beam <= 6; beam++) {
          ctx.save();
          const targetX = width * 0.5 + beam * (width * 0.12);
          ctx.beginPath();
          ctx.moveTo(pX, pY);
          ctx.lineTo(targetX, 0);
          ctx.strokeStyle = beam % 2 === 0 ? "#fbbf24" : "#fef08a";
          ctx.lineWidth = 2.5;
          ctx.shadowColor = "#fbbf24";
          ctx.shadowBlur = 18;
          ctx.globalAlpha = 0.35 + Math.abs(Math.sin(step * 2 + beam)) * 0.35;
          ctx.stroke();
          ctx.restore();
        }

        // Chrome audio equalizer matrix along the bottom
        const barCount = 32;
        const barW = width / barCount;
        for (let b = 0; b < barCount; b++) {
          const h = 25 + Math.sin(step * 3 + b * 0.4) * 45 + Math.cos(step * 2 + b * 0.2) * 35;
          ctx.fillStyle = b % 2 === 0 ? "rgba(251, 191, 36, 0.4)" : "rgba(254, 240, 138, 0.3)";
          ctx.fillRect(b * barW + 2, height - h, barW - 4, h);
        }

        // Shimmering Golden Chrome Ribbons
        for (let i = 0; i < 10; i++) {
          const y = height * 0.3 + Math.sin(step * 1.2 + i * 0.5) * 60 + i * 25;
          ctx.beginPath();
          ctx.moveTo(-50, y);
          ctx.bezierCurveTo(width * 0.4, y + 50, width * 0.6, y - 50, width + 50, y);
          ctx.strokeStyle = "#fbbf24";
          ctx.lineWidth = 2;
          ctx.globalAlpha = 0.25 + (i / 10) * 0.35;
          ctx.stroke();
        }
      }

      // ══════════════════════════════════════════════════════════════
      // 5. TRAVIS SCOTT: Utopia Emerald Dunes & Neon Lightning Storm
      // ══════════════════════════════════════════════════════════════
      else if (theme === "travis") {
        const desertGrad = ctx.createLinearGradient(0, 0, 0, height);
        desertGrad.addColorStop(0, isLight ? "#f0fdf4" : "#010a05");
        desertGrad.addColorStop(0.55, isLight ? "#bbf7d0" : "#042213");
        desertGrad.addColorStop(1, isLight ? "#86efac" : "#000502");
        ctx.fillStyle = desertGrad;
        ctx.fillRect(0, 0, width, height);

        // Cracking Neon Green Lightning Bolts
        if (Math.sin(step * 4) > 0.75) {
          ctx.save();
          ctx.strokeStyle = "#34d399";
          ctx.lineWidth = 2.5;
          ctx.shadowColor = "#10b981";
          ctx.shadowBlur = 24;
          ctx.beginPath();
          let lx = width * 0.6 + Math.sin(step * 10) * 80;
          let ly = 0;
          ctx.moveTo(lx, ly);
          for (let seg = 0; seg < 6; seg++) {
            lx += (Math.random() - 0.5) * 70;
            ly += height * 0.12;
            ctx.lineTo(lx, ly);
          }
          ctx.stroke();
          ctx.restore();
        }

        // Layered Glowing Emerald Desert Dunes
        for (let d = 0; d < 5; d++) {
          const duneY = height * 0.45 + d * 45;
          ctx.beginPath();
          ctx.moveTo(0, height);
          ctx.lineTo(0, duneY);
          ctx.bezierCurveTo(
            width * 0.3,
            duneY - 50 + Math.sin(step + d) * 20,
            width * 0.7,
            duneY + 60 - Math.sin(step + d) * 20,
            width,
            duneY - 20
          );
          ctx.lineTo(width, height);
          ctx.closePath();
          ctx.fillStyle = `rgba(16, 185, 129, ${0.12 + d * 0.08})`;
          ctx.fill();

          ctx.strokeStyle = "#10b981";
          ctx.lineWidth = 2;
          ctx.stroke();
        }
      }

      // ══════════════════════════════════════════════════════════════
      // 6. LANA DEL REY: Vintage Vinyl Record & Sunset Quartz Vignette
      // ══════════════════════════════════════════════════════════════
      else if (theme === "lana") {
        const roseGrad = ctx.createLinearGradient(0, 0, width, height);
        roseGrad.addColorStop(0, isLight ? "#fff1f2" : "#0e0207");
        roseGrad.addColorStop(0.5, isLight ? "#fbcfe8" : "#280818");
        roseGrad.addColorStop(1, isLight ? "#f472b6" : "#070104");
        ctx.fillStyle = roseGrad;
        ctx.fillRect(0, 0, width, height);

        // Large Spinning Vintage Vinyl Record on the right
        const vX = width * 0.82;
        const vY = height * 0.65;
        const vR = Math.min(width, height) * 0.4;

        ctx.save();
        ctx.translate(vX, vY);
        ctx.rotate(step * 0.8);

        // Vinyl outer disc
        ctx.beginPath();
        ctx.arc(0, 0, vR, 0, Math.PI * 2);
        ctx.fillStyle = isLight ? "rgba(244, 114, 182, 0.3)" : "rgba(18, 5, 12, 0.95)";
        ctx.shadowColor = "#f472b6";
        ctx.shadowBlur = 25;
        ctx.fill();
        ctx.shadowBlur = 0;

        // Concentric LP Grooves
        for (let g = 0; g < 12; g++) {
          const r = vR * 0.35 + g * (vR * 0.05);
          ctx.beginPath();
          ctx.arc(0, 0, r, 0, Math.PI * 2);
          ctx.strokeStyle = "rgba(244, 114, 182, 0.35)";
          ctx.lineWidth = 1.2;
          ctx.stroke();
        }

        // Center Album Label
        ctx.beginPath();
        ctx.arc(0, 0, vR * 0.28, 0, Math.PI * 2);
        ctx.fillStyle = "#ec4899";
        ctx.fill();

        ctx.beginPath();
        ctx.arc(0, 0, 8, 0, Math.PI * 2);
        ctx.fillStyle = "#ffffff";
        ctx.fill();
        ctx.restore();

        // Romantic dusty rose ribbons
        for (let i = 0; i < 10; i++) {
          const y = height * 0.25 + Math.sin(step * 0.9 + i * 0.5) * 50 + i * 30;
          ctx.beginPath();
          ctx.moveTo(-50, y);
          ctx.bezierCurveTo(width * 0.35, y + 55, width * 0.65, y - 40, width + 50, y);
          ctx.strokeStyle = "#f472b6";
          ctx.lineWidth = 2;
          ctx.globalAlpha = 0.22 + (i / 10) * 0.35;
          ctx.stroke();
        }
      }

      // ══════════════════════════════════════════════════════════════
      // 7. STANDARD LIQUID GLASS PALETTES (Indigo, Green, Blue, etc.)
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
        ctx.globalAlpha = isLight ? 0.2 : 0.25;
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
          ctx.lineWidth = 1.6 + (i % 3) * 0.4;
          ctx.globalAlpha = isLight ? 0.18 + factor * 0.25 : 0.2 + factor * 0.35;
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
