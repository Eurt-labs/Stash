import React, { useEffect, useRef } from "react";
import { ColorTheme, ThemeMode } from "../../../shared/types";

interface FloatingPathsProps {
  theme?: ColorTheme;
  mode?: ThemeMode;
}

// ── Artist Palette Configurations (Harmonious Dual-Tone Wave Sets) ──
const THEME_PALETTES: Record<
  string,
  { primary: string; secondary: string; ambient: string; bgDark: string; bgLight: string }
> = {
  weeknd: {
    primary: "#ff1e42",
    secondary: "#fb7185",
    ambient: "#e11d48",
    bgDark: "#0d0205",
    bgLight: "#fff1f2",
  },
  taylor: {
    primary: "#9061f9",
    secondary: "#38bdf8",
    ambient: "#7e3af2",
    bgDark: "#090514",
    bgLight: "#faf5ff",
  },
  billie: {
    primary: "#06b6d4",
    secondary: "#10b981",
    ambient: "#0284c7",
    bgDark: "#020914",
    bgLight: "#f0f9ff",
  },
  daftpunk: {
    primary: "#fbbf24",
    secondary: "#f59e0b",
    ambient: "#d97706",
    bgDark: "#0d0a02",
    bgLight: "#fefce8",
  },
  travis: {
    primary: "#10b981",
    secondary: "#34d399",
    ambient: "#059669",
    bgDark: "#020d07",
    bgLight: "#f0fdf4",
  },
  lana: {
    primary: "#f472b6",
    secondary: "#fb7185",
    ambient: "#ec4899",
    bgDark: "#0f0307",
    bgLight: "#fff1f2",
  },
  emerald: {
    primary: "#10b981",
    secondary: "#34d399",
    ambient: "#059669",
    bgDark: "#020d07",
    bgLight: "#f0fdf4",
  },
  sunset: {
    primary: "#f43f5e",
    secondary: "#fb7185",
    ambient: "#e11d48",
    bgDark: "#0e0306",
    bgLight: "#fff1f2",
  },
  sapphire: {
    primary: "#3b82f6",
    secondary: "#60a5fa",
    ambient: "#2563eb",
    bgDark: "#030a16",
    bgLight: "#eff6ff",
  },
  amber: {
    primary: "#f59e0b",
    secondary: "#fbbf24",
    ambient: "#d97706",
    bgDark: "#0e0902",
    bgLight: "#fffbeb",
  },
  crimson: {
    primary: "#ef4444",
    secondary: "#f87171",
    ambient: "#dc2626",
    bgDark: "#0e0202",
    bgLight: "#fef2f2",
  },
  oled: {
    primary: "#ffffff",
    secondary: "#94a3b8",
    ambient: "#475569",
    bgDark: "#000000",
    bgLight: "#f8fafc",
  },
  indigo: {
    primary: "#6366f1",
    secondary: "#818cf8",
    ambient: "#4f46e5",
    bgDark: "#07090e",
    bgLight: "#f8fafc",
  },
};

export const FloatingPaths: React.FC<FloatingPathsProps> = ({
  theme = "indigo",
  mode = "dark",
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const themeRef = useRef<ColorTheme>(theme);
  const modeRef = useRef<ThemeMode>(mode);

  useEffect(() => {
    themeRef.current = theme;
  }, [theme]);

  useEffect(() => {
    modeRef.current = mode;
  }, [mode]);

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

    // ── Silky-Smooth Organic Liquid Waves Renderer ──
    const render = () => {
      step += 0.0035;
      ctx.clearRect(0, 0, width, height);

      const isLight = modeRef.current === "light";
      const currentTheme = themeRef.current || "indigo";
      const palette = THEME_PALETTES[currentTheme] || THEME_PALETTES.indigo;

      // 1. Soft Ambient Background Tint
      const bgGrad = ctx.createLinearGradient(0, 0, width, height);
      bgGrad.addColorStop(0, isLight ? palette.bgLight : palette.bgDark);
      bgGrad.addColorStop(1, isLight ? "#ffffff" : "#05070c");
      ctx.fillStyle = bgGrad;
      ctx.fillRect(0, 0, width, height);

      // 2. Primary Chromatic Ambient Glow Orb
      const orb1_x = width * 0.38 + Math.sin(step * 0.75) * (width * 0.18);
      const orb1_y = height * 0.32 + Math.cos(step * 0.55) * (height * 0.14);
      const orb1_r = Math.min(width, height) * 0.45;

      const orb1_grad = ctx.createRadialGradient(orb1_x, orb1_y, 0, orb1_x, orb1_y, orb1_r);
      orb1_grad.addColorStop(0, palette.primary);
      orb1_grad.addColorStop(1, "transparent");

      ctx.save();
      ctx.globalAlpha = isLight ? 0.16 : 0.22;
      ctx.fillStyle = orb1_grad;
      ctx.fillRect(0, 0, width, height);
      ctx.restore();

      // 3. Secondary Chromatic Ambient Glow Orb
      const orb2_x = width * 0.72 + Math.cos(step * 0.65) * (width * 0.16);
      const orb2_y = height * 0.72 + Math.sin(step * 0.45) * (height * 0.18);
      const orb2_r = Math.min(width, height) * 0.48;

      const orb2_grad = ctx.createRadialGradient(orb2_x, orb2_y, 0, orb2_x, orb2_y, orb2_r);
      orb2_grad.addColorStop(0, palette.secondary);
      orb2_grad.addColorStop(1, "transparent");

      ctx.save();
      ctx.globalAlpha = isLight ? 0.12 : 0.16;
      ctx.fillStyle = orb2_grad;
      ctx.fillRect(0, 0, width, height);
      ctx.restore();

      // 4. Silky-Smooth Organic Liquid Waves (Wave Layer A)
      const lineCount = 14;
      for (let i = 0; i < lineCount; i++) {
        const factor = i / lineCount;
        const offset = i * 0.45;

        const y1 = height * 0.18 + Math.sin(step + offset) * 55 + i * 22;
        const y2 = height * 0.58 + Math.cos(step * 0.8 + offset) * 75 + i * 12;
        const y3 = height * 0.38 + Math.sin(step * 1.1 + offset) * 65 + i * 18;

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

        ctx.strokeStyle = i % 2 === 0 ? palette.primary : palette.secondary;
        ctx.lineWidth = 1.4 + (i % 3) * 0.4;
        ctx.globalAlpha = isLight ? 0.14 + factor * 0.24 : 0.18 + factor * 0.32;
        ctx.stroke();
      }

      // 5. Silky-Smooth Counter Liquid Waves (Wave Layer B)
      for (let j = 0; j < 10; j++) {
        const factorB = j / 10;
        const offsetB = j * 0.5;

        const y1_b = height * 0.82 - Math.cos(step * 0.85 + offsetB) * 50 - j * 20;
        const y2_b = height * 0.42 - Math.sin(step * 0.7 + offsetB) * 70 - j * 12;
        const y3_b = height * 0.62 - Math.cos(step * 1.0 + offsetB) * 60 - j * 14;

        ctx.beginPath();
        ctx.moveTo(-60, y1_b);
        ctx.bezierCurveTo(
          width * 0.38,
          y1_b - Math.sin(step + j * 0.3) * 45,
          width * 0.72,
          y2_b - Math.cos(step + j * 0.3) * 55,
          width + 60,
          y3_b
        );

        ctx.strokeStyle = j % 2 === 0 ? palette.secondary : palette.ambient;
        ctx.lineWidth = 1.2 + (j % 2) * 0.5;
        ctx.globalAlpha = isLight ? 0.1 + factorB * 0.2 : 0.14 + factorB * 0.26;
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
};
