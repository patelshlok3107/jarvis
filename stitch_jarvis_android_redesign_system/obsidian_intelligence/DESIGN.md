---
name: Obsidian Intelligence
colors:
  surface: '#121315'
  surface-dim: '#121315'
  surface-bright: '#38393b'
  surface-container-lowest: '#0d0e10'
  surface-container-low: '#1a1c1d'
  surface-container: '#1e2021'
  surface-container-high: '#292a2b'
  surface-container-highest: '#343536'
  on-surface: '#e3e2e4'
  on-surface-variant: '#bbc9cd'
  inverse-surface: '#e3e2e4'
  inverse-on-surface: '#2f3032'
  outline: '#859397'
  outline-variant: '#3c494c'
  surface-tint: '#27d9f7'
  primary: '#c2f3ff'
  on-primary: '#00363f'
  primary-container: '#38e1ff'
  on-primary-container: '#00616f'
  inverse-primary: '#006878'
  secondary: '#93ccff'
  on-secondary: '#003351'
  secondary-container: '#3198dc'
  on-secondary-container: '#002c47'
  tertiary: '#d6eeff'
  on-tertiary: '#00354a'
  tertiary-container: '#92d7ff'
  on-tertiary-container: '#005e81'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#a6eeff'
  primary-fixed-dim: '#27d9f7'
  on-primary-fixed: '#001f25'
  on-primary-fixed-variant: '#004e5b'
  secondary-fixed: '#cce5ff'
  secondary-fixed-dim: '#93ccff'
  on-secondary-fixed: '#001d31'
  on-secondary-fixed-variant: '#004b73'
  tertiary-fixed: '#c4e7ff'
  tertiary-fixed-dim: '#7bd0ff'
  on-tertiary-fixed: '#001e2c'
  on-tertiary-fixed-variant: '#004c69'
  background: '#121315'
  on-background: '#e3e2e4'
  surface-variant: '#343536'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 36px
    fontWeight: '600'
    lineHeight: 44px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
    letterSpacing: -0.015em
  headline-md:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '500'
    lineHeight: 28px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '500'
    lineHeight: 24px
    letterSpacing: -0.005em
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0em
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0.005em
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
    letterSpacing: 0.01em
  label-md:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.08em
  label-sm:
    fontFamily: JetBrains Mono
    fontSize: 10px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.12em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1.25rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style
The design system delivers an ultra-premium, cinematic intelligence interface tailored for high-agency users seeking calm command over complex tasks. The visual tone is atmospheric minimalism infused with executive luxury: deep void blacks, razor-thin structural delineation, and surgically placed photon-luminescent accents. 

It rejects noisy cyber-tropes in favor of an understated, stealth-luxury aesthetic. Interfaces feel suspended in deep space, hyper-responsive, and whisper-quiet. Interactions evoke absolute precision and computational weightlessness, prioritizing zero cognitive friction, tactile fluidity, and Android-native responsiveness.

## Colors
The palette operates on pure luminance discipline over deep obsidian voids:

- **Base Void (`#050607`)**: Canvas background representing the infinite substrate.
- **Surface Level 1 (`#0B0D0F`)**: Secondary cards, floating command surfaces, and persistent navigation containers.
- **Surface Level 2 (`#101316`)**: Elevated modals, nested chips, active card wells, and transient menus.
- **Structural Lines (`rgba(255, 255, 255, 0.08)`)**: Hairline boundary strokes providing geometric grounding without visual density.
- **Text & Glyph Hierarchy**:
  - Primary: `#F5F5F5` for definitive reads and command inputs.
  - Secondary: `#8B9198` for metadata, structural context, and passive states.
  - Tertiary / Muted: `#4B535B` for inactive icons, structural separators, and timestamps.
- **Bioluminescent Accents (`#38E1FF`, `#38BDF8`, `#0284C7`)**: Reserved strictly for live feedback vectors, acoustic waveform nodes, AI synthesis indicators, and active selection anchors. Accent colors must never wash large surfaces; they function solely as photonic signals.

## Typography
The typographic architecture relies on high-legibility sans-serif precision paired with a technical monospaced engine font:

- **Inter**: Handles all conversational text, user prompts, system headers, and natural interactions. Features subtle optical kerning for high readability on deep dark surfaces.
- **JetBrains Mono**: Serves as the technical substrate. Deployed strictly for system state badges, latency metrics, voice processing parameters, uptime counters, and all-caps categorization tags.
- **Tracking Discipline**: All mono tags and status indicators strictly leverage generous positive letter-spacing (`0.08em` to `0.12em`) rendered in all caps to evoke high-end instrumentation.

## Layout & Spacing
The layout model follows an Android-first fluid grid optimized for edge-to-edge OLED viewports:

- **Safe Zones & Margins**: Default horizontal canvas padding is set to `margin` (20px) to balance thumb reachability with breathing room on modern devices.
- **Rhythm & Stacking**: Vertical relationships obey an 8-point spatial cadence (`space-xs` = 4px, `space-sm` = 8px, `space-md` = 16px, `space-lg` = 24px, `space-xl` = 32px).
- **Edge-to-Edge Fluidity**: Dynamic elements (voice transcripts, floating pill switches, live metric rows) adapt horizontally while maintaining fixed gutters of `1rem` (16px).

## Elevation & Depth
Elevation is constructed purely through tonal stratification and physical border definition rather than aggressive drop shadows:

- **Layer 0 (Canvas)**: `#050607` — The absolute background plane.
- **Layer 1 (Cards & Groups)**: `#0B0D0F` enclosed in a precise `1px solid rgba(255, 255, 255, 0.08)` hairline border.
- **Layer 2 (Floating Modals & Active Bottom Sheets)**: `#101316` reinforced by a soft ambient shadow (`box-shadow: 0 16px 40px -8px rgba(0, 0, 0, 0.8)`), bounded by the same hairline border.
- **Luminescent Highlights**: AI active states emit a subtle, restrained cyan glow (`box-shadow: 0 0 24px -4px rgba(56, 225, 255, 0.15)`), never bleeding excessively or lowering text contrast.

## Shapes
Geometry balances organic handheld comfort with industrial precision:

- **Cards & Primary Modules**: Formed with standard 20px to 24px border radii, mirroring hardware chassis curves.
- **Pills & Navigation Elements**: Full pill geometry (`border-radius: 9999px`) for quick switches, floating actions, voice interaction nodes, and category tabs.
- **Micro Elements**: Checkboxes, nested status flags, and badge backgrounds apply 6px to 8px radii to retain tactical sharpness.

## Components

### Buttons & Action Controls
- **Primary Interactive**: Fully pill-shaped or 16px rounded surface. In active intelligence states, uses solid `#38E1FF` with deep `#050607` high-contrast typography. For passive primary triggers, dark `#101316` surface with `rgba(255, 255, 255, 0.08)` hairline border and `#F5F5F5` text.
- **Ghost/Tertiary Actions**: Transparent background, `#8B9198` text, transitioning to `#F5F5F5` on touch with a 4% white surface highlight.
- **Touch Targets**: Minimum 48px height across all mobile interactive surfaces.

### Cards & Grouping Containers
- **Visual Spec**: Solid `#0B0D0F` or `#101316` fill, 20px to 24px border radius, enclosed in a crisp `1px` border of `rgba(255, 255, 255, 0.08)`.
- **Content Padding**: `1.25rem` (20px) internal padding, isolating content against the dark frame.

### Chips & Pill Tabs
- **Structure**: Pill-shaped (`border-radius: 9999px`), 36px standard height.
- **Active State**: Surface shifts to `#101316`, border brightens to `rgba(56, 225, 255, 0.4)`, text snaps to `#38E1FF` accompanied by a leading 6px glowing status dot.
- **Inactive State**: Background transparent or `#0B0D0F`, border `rgba(255, 255, 255, 0.06)`, text `#8B9198`.

### Input Fields & Command Prompts
- **Surface**: `#0B0D0F` with seamless vertical alignment, 16px radius, and interior padding of 14px 18px.
- **Focus State**: Hairline border transitions from `rgba(255, 255, 255, 0.08)` to `#38BDF8` with a micro ambient cyan aura (`0 0 12px rgba(56, 189, 248, 0.2)`). Placeholder rendered in `#4B535B`.

### Lists & Activity Rows
- **Dividers**: Replaced by 12px vertical spacing or ultra-faint borders (`rgba(255, 255, 255, 0.04)`).
- **Row Anatomy**: Left-aligned primary label (`#F5F5F5`, Inter 14px), subtext (`#8B9198`, Inter 12px), right-aligned timestamp or telemetry data (`#8B9198`, JetBrains Mono 10px uppercase).

### AI Voice Waveform & Orb Engine
- **Waveform Lines**: Variable-height audio nodes utilizing `#38E1FF` and `#0284C7` with linear interpolation. Hairline stroke width (1.5px to 2px) to match system aesthetics.
- **Pulse Indicators**: 6px status dots anchored alongside system diagnostics. Steady state `#8B9198`; active processing `#38E1FF` with a breathing opacity transition.