---
name: Kinetic Courier
colors:
  surface: '#f9f9ff'
  surface-dim: '#d3daef'
  surface-bright: '#f9f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f1f3ff'
  surface-container: '#e9edff'
  surface-container-high: '#e1e8fd'
  surface-container-highest: '#dce2f7'
  on-surface: '#141b2b'
  on-surface-variant: '#5b4137'
  inverse-surface: '#293040'
  inverse-on-surface: '#edf0ff'
  outline: '#907065'
  outline-variant: '#e4beb1'
  surface-tint: '#a83900'
  primary: '#a83900'
  on-primary: '#ffffff'
  primary-container: '#ff5a00'
  on-primary-container: '#511700'
  inverse-primary: '#ffb59a'
  secondary: '#006b5c'
  on-secondary: '#ffffff'
  secondary-container: '#65fade'
  on-secondary-container: '#007262'
  tertiary: '#0061a6'
  on-tertiary: '#ffffff'
  tertiary-container: '#0095fc'
  on-tertiary-container: '#002c50'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdbcf'
  primary-fixed-dim: '#ffb59a'
  on-primary-fixed: '#380d00'
  on-primary-fixed-variant: '#802900'
  secondary-fixed: '#65fade'
  secondary-fixed-dim: '#41ddc2'
  on-secondary-fixed: '#00201b'
  on-secondary-fixed-variant: '#005045'
  tertiary-fixed: '#d2e4ff'
  tertiary-fixed-dim: '#a0c9ff'
  on-tertiary-fixed: '#001c37'
  on-tertiary-fixed-variant: '#00497f'
  background: '#f9f9ff'
  on-background: '#141b2b'
  surface-variant: '#dce2f7'
typography:
  display:
    fontFamily: Space Grotesk
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.03em
  headline-lg:
    fontFamily: Space Grotesk
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Space Grotesk
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Space Grotesk
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: 0em
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 24px
    letterSpacing: -0.01em
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0em
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
    letterSpacing: 0em
  label-lg:
    fontFamily: Space Grotesk
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 18px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Space Grotesk
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Space Grotesk
    fontSize: 10px
    fontWeight: '700'
    lineHeight: 14px
    letterSpacing: 0.04em
  data-timer:
    fontFamily: Space Grotesk
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 28px
    letterSpacing: -0.02em
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  gutter: 0.75rem
  gutter-mobile: 0.75rem
  margin: 1rem
  margin-mobile: 1rem
  margin-tablet: 1.5rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 0.75rem
  space-lg: 1rem
  space-xl: 1.5rem
  space-2xl: 2rem
---

## Brand & Style

The design system is engineered for lightning-paced on-demand mobile commerce, where every microsecond matters. It balances electric kinetic energy with clinical order clarity. 

### Target Audience & Psychology
Targeted at urban digital natives and fast-paced consumers needing real-time essentials delivered within minutes. The interface evokes unyielding velocity, precision reliability, and an uplifting sense of immediacy without inducing cognitive fatigue or visual anxiety.

### Aesthetic Execution
A high-energy hybrid of **Modern Kinetic Flat** and **Controlled Frosted Glassmorphism**:
- **Hyper-Visual Accelerators**: High-contrast electric accents paired against cool, luminous neutral slates.
- **Glass Floating Surfaces**: Critical floating controls (docked order trackers, bottom sheets, sticky cart bars) utilize translucent layered surfaces to maintain map and route context.
- **Micro-Structured Legibility**: Tight, information-dense utility widgets (timers, dynamic ETA counters, status badges) balanced by generous, breathing component paddings.

## Colors

The palette leverages contrast to communicate urgency, state transitions, and immediate actionability across dense mobile views.

### Roles & Hierarchy
- **Primary (`#FF5A00` / Electric Tangerine)**: Reserved for high-urgency call-to-actions, checkout drivers, active order paths, and primary price accents. It signifies instant speed.
- **Secondary (`#00C2A8` / Vivid Teal)**: Reserved for positive system signals—speed tags (e.g., "15 min"), driver arrival confirmation, high vendor ratings, active courier pings, and delivery discounts.
- **Neutral Charcoal (`#111827` / Dark Slate)**: The foundation for dominant typography, high-priority status indicators, and crisp contrast surfaces against light backdrops.
- **Canvas & Neutral Surfaces**: 
  - Canvas base: `#F8FAFC` (Slate 50)
  - Card & Container surface: `#FFFFFF`
  - Subtle interactive & card tier 2: `#F1F5F9` (Slate 100)
  - Ghost borders & separators: `#E2E8F0` (Slate 200)

### Color Application Guidelines
Never wash entire screen backgrounds in primary electric tangerine; use it as focused kinetic spotlights. Secondary teal must be prioritized for dynamic countdown timers and "On Time Guarantee" badges to preserve the contrast between speed (orange) and trust/accuracy (teal).

## Typography

The typographic hierarchy pairs **Space Grotesk** for numerical metrics, headings, and data-dense status anchors with **Plus Jakarta Sans** for friction-free reading across contextual UI.

### Typographic Pairing Logic
- **Space Grotesk**: Brings technical precision, contemporary streetwise energy, and distinct geometric numerals. Applied across headlines, prices, delivery timestamps, ETA counters, and micro-labels.
- **Plus Jakarta Sans**: Supplies friendly, highly legible humanist sans-serif shapes for descriptions, vendor addresses, modifiers, item inventories, and transaction small print.

### Specialized Roles
- **`data-timer`**: Engineered exclusively for live delivery tracking counters and flash deal expirations to eliminate horizontal jitter when values count down.
- **`label-sm`**: All-caps micro token utilized inside badges, speed tags, and courier vehicle indicators.

## Layout & Spacing

Designed fundamentally around a mobile viewport (375px–430px base canvas), this design system uses a fluid single-to-double column layout with dynamic bottom safe-area offsets.

### Grid & Canvas Structure
- **Mobile Viewport**: 4-column fluid layout with an outer canvas edge margin of `1rem` (16px) and column gutters of `0.75rem` (12px).
- **Tablet / Responsive Sheet Views**: 8-column layout with `1.5rem` (24px) gutters and maximum container bounding widths of 640px for modal checkout sheets to retain thumb-driven ergonomical efficiency.

### Density & Rhythms
- **Tight Micro-Spacings (`space-xs`, `space-sm`)**: Used for inline tag groups, rating stars with count text, item incremental steppers, and pricing units.
- **Structural Groupings (`space-md`, `space-lg`)**: Separates merchant metadata, delivery notes, and stacked card segments.
- **Floating Overlays & Sticky Trays**: All bottom-docked actions must include bottom safe-area insets (`env(safe-area-inset-bottom) + 12px`) to ensure zero overlap with native OS navigation home bars.

## Elevation & Depth

Visual depth is achieved through layered tonal planes, frosted glassmorphism for floating overlays, and high-performance ambient drop shadows that prevent spatial mudriness during rapid mobile scrolling.

### Surface Tiers
- **Tier 0 (Canvas Base)**: `#F8FAFC`. The foundational backdrop upon which all scrollable feeds, categories, and background maps reside.
- **Tier 1 (Surface Cards & Lists)**: `#FFFFFF` bounded by a subtle structural border (`1px solid #E2E8F0`). Elevated by an ambient shadow: `0 2px 8px -2px rgba(17, 24, 39, 0.04), 0 1px 4px -1px rgba(17, 24, 39, 0.02)`.
- **Tier 2 (Interactive Floating Elements)**: Bottom bar checkouts, live order floating pills, and sticky search bars:
  - Surface: `rgba(255, 255, 255, 0.88)`
  - Backdrop Blur: `blur(16px) saturate(180%)`
  - Border: `1px solid rgba(255, 255, 255, 0.6)`
  - Shadow: `0 12px 32px -4px rgba(17, 24, 39, 0.12), 0 4px 12px -2px rgba(17, 24, 39, 0.04)`
- **Tier 3 (Active Tracking Drawer & Sheets)**: Solid `#FFFFFF` sheets anchored with high depth: `0 -8px 24px -4px rgba(17, 24, 39, 0.08)`.

## Shapes

The interface embraces an ultra-modern aerodynamic geometry, utilizing deep organic radii for containment and full capsules for actions and tags.

### Radius System
- **Pill (Fully Rounded / 9999px)**: Applied universally to interactive buttons, category chips, countdown containers, search bars, and driver live badges. Communicates speed, friendliness, and tactile thumb-reachability.
- **Large Container Radius (`20px` / `rounded-xl`)**: Applied to primary content cards, store listing modules, modal sheets, and dispatch tracking cards.
- **Medium Nested Radius (`16px` / `rounded-lg`)**: Applied to product media thumbnails, internal banner promotions, and sub-cards inside parent groups.

## Components

### Buttons
- **Primary Pill Action**: Background `#FF5A00`, text `#FFFFFF`, font `label-lg`. Height: 52px for primary checkout drivers; 40px for secondary actions. Tap feedback: scales down to `0.97` with a brightness bump.
- **Secondary / Ghost Pill**: Surface `rgba(0, 194, 168, 0.12)`, text `#00C2A8`, border `none`. Used for reorder actions, quick vouchers, and add-to-cart helpers.
- **Floating Cart Button**: Docked full-width floating capsule; backdrop-filter glass background with integrated badge count and total price summary.

### Chips & Badges
- **Speed Tag**: Pill shape, background `#00C2A8` with `#FFFFFF` text or light tint `#E6F9F6` with `#00A38D` text. Displays lightning icon alongside precise timeframes (`18 min`).
- **Category Filter Chips**: Pill shape, background `#FFFFFF`, border `1px solid #E2E8F0`. When active: background `#111827`, text `#FFFFFF`, border `#111827`.

### Cards
- **Merchant / Delivery Item Card**: Surface `#FFFFFF`, border-radius `20px`, padding `space-md`, border `1px solid #E2E8F0`. Features a 1:1 image thumbnail (`rounded-lg`), immediate delivery time badge top-left, and quick-add "+" interactive pill button positioned bottom-right.
- **Live Order Tracking Card**: Elevated Tier 2 card. Features a split layout: left segment holds dynamic ETA (`data-timer` typography in `#FF5A00`), right segment holds courier vehicle illustration or real-time status tracker with animated pulsing teal dot.

### Input Fields & Search Bars
- **Global Search Capsule**: Height: 48px. Background `#F1F5F9`, border `1px solid transparent`, border-radius: 9999px. Focused state transitions smoothly to `#FFFFFF` background with `1px solid #FF5A00` ring. Left search icon in `#111827`, right visual voice/camera trigger icon.

### Checkboxes & Steppers
- **Quantity Steppers**: Compact pill capsule with light gray fill `#F1F5F9`. Increment and decrement icon buttons surrounding a Space Grotesk digit (`label-md`). When quantity exceeds 0, container border shifts to `#FF5A00`.
- **Selection Checks**: Circular radio/checkbox elements with active state solid `#FF5A00` containing a sharp white checkmark.

### Live Tracking Progress Indicators
- **Delivery Timeline Bar**: Horizontal segmented step bar. Completed segments in solid `#00C2A8`, active segment in pulsing `#FF5A00`, remaining segments in `#E2E8F0`. Height: 4px with pill endcaps.