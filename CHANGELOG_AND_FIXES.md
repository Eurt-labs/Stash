# 🚀 Stash Downloader — Architectural Challenges, Technical Root Causes & Fixes Log

This document provides a comprehensive, detailed record of every technical challenge, rendering issue, performance bottleneck, and architectural design hurdle encountered during the evolution of **Stash Media Downloader v2.0**, along with their exact root causes and solutions.

---

## 📑 Table of Contents
1. [Challenge 1: GPU Buffer Exhaustion & Chromium Tile Seam Glitches](#1-gpu-buffer-exhaustion--chromium-tile-seam-glitches)
2. [Challenge 2: Dark Mode Theme Color Specificity Shadowing](#2-dark-mode-theme-color-specificity-shadowing)
3. [Challenge 3: Logo Geometry & Monochromatic Vector Simplification](#3-logo-geometry--monochromatic-vector-simplification)
4. [Challenge 4: Translucent Liquid Glass Design System & Contrast Passthrough](#4-translucent-liquid-glass-design-system--contrast-passthrough)
5. [Challenge 5: Chromium Scrollbar Corner Bleeding on Frosted Acrylic Cards](#5-chromium-scrollbar-corner-bleeding-on-frosted-acrylic-cards)
6. [Challenge 6: Artist Signature Themes Reactivity & Canvas Synchronization](#6-artist-signature-themes-reactivity--canvas-synchronization)
7. [Challenge 7: Eliminating Harsh Geometric Wireframes in Favor of Organic Liquid Curves](#7-eliminating-harsh-geometric-wireframes-in-favor-of-organic-liquid-curves)
8. [Challenge 8: Production Bundle Optimization & Dead Package Pruning](#8-production-bundle-optimization--dead-package-pruning)

---

## 1. GPU Buffer Exhaustion & Chromium Tile Seam Glitches

### 🛑 The Challenge
- In earlier iterations, animated SVG background paths driven by `framer-motion` caused severe CPU spikes (>35%) and visual horizontal tearing/seams across Electron's Chromium viewport.
- Electron logs reported `tile_manager.cc: tile memory limits exceeded`.

### 🔍 Technical Root Cause
- Animating fullscreen SVG stroked paths with dynamic dash-arrays and transforms forced the Chromium rasterizer to recreate GPU texture tiles across all grid sections on every single animation frame.
- This overflowed Chromium's internal tile memory budget, resulting in tile dropping, visual seam lines, and high memory usage.

### 🛠️ How It Was Fixed
- Completely removed `framer-motion` SVG path interpolation loops.
- Replaced the SVG background with an optimized, GPU-accelerated HTML5 `<canvas>` element (`BackgroundPaths.tsx`).
- Configured canvas resolution scaling with `window.devicePixelRatio` and a lightweight `requestAnimationFrame` loop that calculates continuous cubic Bezier curves in sub-millisecond CPU time (<0.1% CPU utilization, 60fps locked, zero tile manager warnings).

---

## 2. Dark Mode Theme Color Specificity Shadowing

### 🛑 The Challenge
- When selecting standard or artist themes (e.g. Green, Pink, Blue, The Weeknd, Billie Eilish) while in Dark Mode, primary action buttons, search bars, and queue badges remained locked to indigo purple (`#6366f1`).

### 🔍 Technical Root Cause
- In CSS, `body[data-mode='dark']` had `--primary: #6366f1` declared directly.
- Because `<body>` is a direct descendant of `<html>`, CSS variable inheritance prioritized the property set on `body[data-mode='dark']` over `html[data-theme='sunset']` or `[data-theme='emerald']` on the root, shadowing and overriding the active color palette.

### 🛠️ How It Was Fixed
- **Decoupled Mode & Theme Tokens** in `index.css`:
  - `[data-mode='dark']` and `[data-mode='light']` were refactored to **only** control surface backgrounds, text colors, and glass opacity.
  - `[data-theme='...']` exclusively defines `--primary`, `--primary-hover`, `--primary-light`, `--primary-muted`, and `--primary-glow`.
- Synchronized `data-theme` and `data-mode` attributes onto both `document.documentElement` and `document.body` in `App.tsx`, providing instantaneous live color reactivity.

---

## 3. Logo Geometry & Monochromatic Vector Simplification

### 🛑 The Challenge
- Earlier branding iterations contained multicolored gradients, lightning flashes, and distorted musical note geometries that clashed with minimal UI aesthetics.
- The user provided a hand-drawn sketch specifying a single continuous stroke transitioning from a musical note to a download arrow.

### 🔍 Technical Root Cause
- Separate SVG elements without mathematical tangency created disjointed joints and visual clutter when scaled to small application squircle sizes (36px).

### 🛠️ How It Was Fixed
- Re-engineered the vector math in `Logo.tsx` and `stash_logo.svg`:
  - **Bottom-Left Note Head**: Precision circle at `cx: 36, cy: 62, r: 14`.
  - **Continuous Vertical Stem & Arch**: Starts at the exact right tangent `(50, 62)`, rises vertically to `y: 28`, smoothly bridges over `C50 15, 70 15, 70 28`, and drops vertically as an arrow shaft to `y: 56`.
  - **Centered Download Arrowhead**: Terminates at `M59 46 L70 56 L81 46` with rounded caps and joins (`stroke-width: 5.5px`).

---

## 4. Translucent Liquid Glass Design System & Contrast Passthrough

### 🛑 The Challenge
- Card containers, path pickers, and the empty queue box felt opaque and solid (`#12151e`), blocking the animated ambient canvas background from being seen.

### 🔍 Technical Root Cause
- Default card background values used high opacity fills (`rgba(17, 21, 33, 0.72)` in dark mode and `rgba(255, 255, 255, 0.78)` in light mode) which created visual opacity blocking.

### 🛠️ How It Was Fixed
- Replaced opaque surfaces with **ultra-translucent frosted acrylic liquid glass**:
  - **Dark Mode**: `background: rgba(10, 14, 24, 0.32)`, `backdrop-filter: blur(28px) saturate(200%)`.
  - **Light Mode**: `background: rgba(255, 255, 255, 0.28)`, `backdrop-filter: blur(20px) saturate(180%)`.
  - Added specular top inner highlights (`inset 0 1px 1px 0 rgba(255, 255, 255, 0.16)`) and subtle themed glowing focus borders.

---

## 5. Chromium Scrollbar Corner Bleeding on Frosted Acrylic Cards

### 🛑 The Challenge
- On rounded modal cards (`border-radius: 16px`), scrollbar tracks bled past the top-right and bottom-right rounded corners, creating dark gray pill-shaped artifacts.

### 🔍 Technical Root Cause
- Chromium's default scrollbar engine positions track coordinates relative to the bounding box of the scrollable container. When `overflow-y: auto` is placed directly on an element with `border-radius`, the rectangular track ignores the curved boundary.

### 🛠️ How It Was Fixed
- **Architectural Separation**:
  - Set the outer `.modal-card` to `overflow: hidden; padding: 0; display: flex; flex-direction: column;`.
  - Pinned `.modal-header` at the top and wrapped scrollable content in an internal container with `flex: 1; overflow-y: auto; padding: 16px 22px 22px 22px;`.
- **Pill-Rounded Inset Scrollbars** in `index.css`:
  - Added `margin: 8px 0` on `::-webkit-scrollbar-track` to offset track boundaries away from corners.
  - Set `border-radius: 9999px` on `::-webkit-scrollbar-thumb`.

---

## 6. Artist Signature Themes Reactivity & Canvas Synchronization

### 🛑 The Challenge
- Switching between standard colors (e.g. Indigo, Green, Pink) and artist signature styles (The Weeknd, Taylor Swift, Billie Eilish, Daft Punk, Travis Scott, Lana Del Rey) sometimes did not immediately update the canvas background.

### 🔍 Technical Root Cause
- `<FloatingPaths />` was rendered in `App.tsx` without passing `theme` and `mode` as React props. The canvas `requestAnimationFrame` loop relied on DOM queries that could lag behind React's virtual DOM reconciliation cycle.

### 🛠️ How It Was Fixed
- Updated `FloatingPaths.tsx` to accept `theme: ColorTheme` and `mode: ThemeMode` props.
- Implemented `themeRef` and `modeRef` synchronization hooks to provide **0ms instant transition** in the canvas render loop.
- Connected `<FloatingPaths theme={theme} mode={mode} />` in `App.tsx`.

---

## 7. Eliminating Harsh Geometric Wireframes in Favor of Organic Liquid Curves

### 🛑 The Challenge
- Early artist theme mockups introduced sharp perspective grids, jagged lightning lines, and segmented wireframes that looked harsh and clashed with the liquid glass aesthetic.

### 🔍 Technical Root Cause
- Hardcoded discrete linear segments (`ctx.lineTo`) caused sharp visual discontinuities and aliasing artifacts on high-DPI displays.

### 🛠️ How It Was Fixed
- Standardized all 13 themes onto an **organic dual-tone liquid wave architecture**:
  - 14 primary cubic Bezier wave ribbons (`Layer A`) flowing top-left to right.
  - 10 counter Bezier wave ribbons (`Layer B`) flowing bottom-left across the screen.
  - 2 chromatic ambient light orbs breathing softly at 60fps with distinct artist color harmonies.

---

## 8. Production Bundle Optimization & Dead Package Pruning

### 🛑 The Challenge
- Unused dependencies (`@radix-ui/react-slot`, `class-variance-authority`, `clsx`, `framer-motion`, `tailwind-merge`) and obsolete component files bloated bundle size and added maintenance overhead.

### 🔍 Technical Root Cause
- Legacy boilerplate dependencies remained in `package.json` after shifting to native CSS tokens and custom canvas shaders.

### 🛠️ How It Was Fixed
- Uninstalled all 5 unused packages and pruned `node_modules`.
- Deleted unused component files (`button.tsx`, `utils.ts`, and legacy directories).
- **Results**: Production JS bundle reduced from **339 kB** down to **184 kB** (45.7% reduction) with zero warnings.
