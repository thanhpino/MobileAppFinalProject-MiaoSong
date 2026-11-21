---
name: Warm Savory Delivery
colors:
  surface: '#fff8f5'
  surface-dim: '#f1d4c5'
  surface-bright: '#fff8f5'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#fff1ea'
  surface-container: '#ffeadf'
  surface-container-high: '#ffe3d4'
  surface-container-highest: '#faddcd'
  on-surface: '#27180f'
  on-surface-variant: '#5a4137'
  inverse-surface: '#3e2d23'
  inverse-on-surface: '#ffede5'
  outline: '#8e7165'
  outline-variant: '#e2bfb2'
  surface-tint: '#a43d00'
  primary: '#a43d00'
  on-primary: '#ffffff'
  primary-container: '#ff6a1a'
  on-primary-container: '#591e00'
  inverse-primary: '#ffb597'
  secondary: '#006b5f'
  on-secondary: '#ffffff'
  secondary-container: '#6df5e1'
  on-secondary-container: '#006f64'
  tertiary: '#785a00'
  on-tertiary: '#ffffff'
  tertiary-container: '#c09100'
  on-tertiary-container: '#3f2e00'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdbcd'
  primary-fixed-dim: '#ffb597'
  on-primary-fixed: '#360f00'
  on-primary-fixed-variant: '#7d2d00'
  secondary-fixed: '#71f8e4'
  secondary-fixed-dim: '#4fdbc8'
  on-secondary-fixed: '#00201c'
  on-secondary-fixed-variant: '#005048'
  tertiary-fixed: '#ffdf9c'
  tertiary-fixed-dim: '#f5bf34'
  on-tertiary-fixed: '#251a00'
  on-tertiary-fixed-variant: '#5b4300'
  background: '#fff8f5'
  on-background: '#27180f'
  surface-variant: '#faddcd'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 48px
    fontWeight: '800'
    lineHeight: 56px
    letterSpacing: -0.02em
  display-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 36px
    fontWeight: '800'
    lineHeight: 44px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 26px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 22px
    fontWeight: '700'
    lineHeight: 28px
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '700'
    lineHeight: 24px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 24px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '700'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.01em
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 14px
    letterSpacing: 0.02em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-desktop: 1.5rem
  margin: 1rem
  margin-tablet: 2rem
  margin-desktop: 3rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2.5rem
---

## Brand & Style
The design system reflects warmth, appetite, rapid reliability, and approachable hospitality. Built for a contemporary on-demand food delivery experience, the visual language avoids sterile corporate ergonomics and cold utilitarian grays, leaning instead into wholesome textures, buttery tones, and vibrant culinary cues.

Key design attributes:
- **Warm & Appetizing:** Creamy canvas tones combined with sun-ripened citrus hues stimulate appetite and comfort.
- **Friendly Modernism:** Generously rounded geometry paired with airy whitespace balances tactile friendliness with premium interface clarity.
- **Deliberate Contrast:** Crisp teal highlights cut cleanly through warm baseline hues to signpost primary logistics, time-sensitive milestones, and verified actions.

## Colors

### Palette Implementation
- **Primary (`#FF6A1A`):** Core call-to-actions, cart badges, active navigational highlights, and vital delivery indicators.
- **Secondary (`#14B8A6`):** Refreshing complement used for verified restaurant badges, fresh food categories, delivery ETA tags, and secondary action confirmations.
- **Tertiary Accent (`#FFC83D`):** Star ratings, loyalty perks, and promo ribbons.
- **Surface Canvas (`#FFF8F0`):** Butter-cream base that replaces harsh `#FFFFFF` backgrounds, creating an organic, appetizing environment.
- **Surface Container Elevated (`#FFFFFF`):** High-level card containers for menu items and merchant profiles resting atop the cream base.
- **On Surface / Typography (`#3B2A20`):** Rich roasted cocoa tone replacing harsh neutral black, preserving contrast while honoring warm palette tones.
- **Subdued Text (`#7C685D`):** Secondary typographic support for metadata, ingredient lists, and delivery distance.
- **Banner Gradient:** Linear transition from `#FF6A1A` at 0% to `#FF9457` at 100% (suggested angle 135deg).

## Typography
Plus Jakarta Sans serves across display, body, and label roles to maintain rhythmic curvature and warm, open apertures. 

Rules for typographic hierarchy:
- Headlines and numeric totals (item pricing, delivery ETAs) employ bold to extra-bold weights (`700` and `800`) to enable effortless scanning.
- Numerical values in checkout flows and order tracking must utilize tabular lining figures when available.
- Tight letter-spacing (`-0.02em` to `-0.01em`) is reserved exclusively for scales above `24px` to keep large titles coherent.
- Microcopy, allergens, and receipt notes never scale below `11px`.

## Layout & Spacing
A fluid 4-column layout on mobile scales to 8 columns on tablet and 12 columns on desktop (max canvas constraint: 1280px centered).

- **Mobile (< 768px):** 4 columns, 16px gutter, 16px horizontal screen margin. Single-column restaurant lists and horizontal scroll rails for food categories.
- **Tablet (768px – 1024px):** 8 columns, 16px gutter, 32px margins. 2-column dish menus with split cart drawers.
- **Desktop (> 1024px):** 12 columns, 24px gutter, 48px margins. Sticky sidebar for order progression, multi-column merchant cards, and fixed bottom utility overlays when triggered.

## Elevation & Depth
Depth relies on soft tonal distinction and warm-tinted ambient diffusion rather than cold drop-shadows.

- **Level 0 (Canvas Base):** Butter-cream ground (`#FFF8F0`), completely flat.
- **Level 1 (Card & Dish Modules):** Pure warm white (`#FFFFFF`) with a delicate translucent perimeter border (`1px solid rgba(59, 42, 32, 0.05)`) and subtle ambient dispersion: `0 4px 16px -2px rgba(59, 42, 32, 0.04)`.
- **Level 2 (Interactive Floating Bars / Cart Peek):** Pure warm white (`#FFFFFF`) with warm glow: `0 8px 24px -4px rgba(255, 106, 26, 0.12), 0 2px 8px -1px rgba(59, 42, 32, 0.04)`.
- **Level 3 (Modals / Order Tracking Sheets):** Crisp white containers bounded by `0 16px 36px -8px rgba(59, 42, 32, 0.16)`.

## Shapes
A rounded architecture (Level 2) underpins the cheerful, welcoming dining aesthetic.

- **Standard Elements (0.5rem / 8px):** Inputs, small thumbnail frames, stepper controls, inline tag pills.
- **Medium Panels & Cards (1rem / 16px):** Menu item units, merchant showcases, promotional banner containers.
- **Large & Floating Sheets (1.5rem / 24px):** Bottom action sheets, order confirmation modals, filter panels.
- **Pill Shape (9999px):** Primary conversion buttons, floating order trackers, and status tags.

## Components

### Buttons
- **Primary Button:** Pill-shaped, gradient or solid fill `#FF6A1A`, text `#FFFFFF`, label weight `700`. Subtle hover shift to `#E55A0F`. Focus ring using 3px solid `rgba(255, 106, 26, 0.3)`.
- **Secondary Button:** Soft mint fill (`rgba(20, 184, 166, 0.1)`), text `#14B8A6`, no border, pill-shaped.
- **Ghost/Tertiary Button:** Warm transparent base, text `#3B2A20`, active press states apply subtle `#FFF0E0` wash.

### Tags & Badges
To keep UI overhead clean, category and status tags strictly pull from 3 variations:
1. **Brand Promo:** Soft warm orange ground (`rgba(255, 106, 26, 0.1)`), text `#FF6A1A`.
2. **Speed & Freshness (Teal):** Soft turquoise ground (`rgba(20, 184, 166, 0.12)`), text `#0D9488`.
3. **Neutral / Category:** `#FFF8F0` ground with a refined outline (`1px solid rgba(59, 42, 32, 0.12)`), text `#3B2A20`.

### Cards & Menu Cells
- Display dishes against pure white container blocks.
- Images utilize 8px to 12px rounded corners with continuous inset contrast to prevent white-on-white bleeding.
- Price labels leverage `headline-sm` in bold `#FF6A1A` or `#3B2A20`.

### Form Inputs & Quantity Selectors
- Text fields utilize `#FFFFFF` surface with `1.5px solid rgba(59, 42, 32, 0.12)`, rounding `8px`, focused with `#FF6A1A`.
- Stepper buttons for dish counts feature a warm cream container with `#FF6A1A` glyphs, tactile press animations, and minimal 32px height touch footprints.

### Rating Indicators
- Ratings combine an accent star (`#FFC83D`) with bold `#3B2A20` numerical scores, nested inside a compact neutral badge.