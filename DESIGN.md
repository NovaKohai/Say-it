# DESIGN.md

## Color

### Color Strategy: Committed
One saturated color carries 30–60% of the surface. Brand default for identity-driven pages.

### Primary Palette
- **Emerald 500**: `#10B981` (primary interactive color)
- **Emerald 600**: `#059669` (active states, pressed)
- **Emerald 700**: `#047857` (disabled, deep state)
- **Emerald 300**: `#6EE7B7` (light highlights, success states)

### Accent Palette
- **Cyan Accent**: `#38BDF8` (active highlights, LIVE indicators)
- **Gold Warning**: `#F59E0B` (warnings, overdue items)
- **Red Expense**: `#EF4444` (negative expenses)
- **Green Income**: `#22C55E` (income, positive amounts)

### Neutrals (tinted toward brand hue)
- **Light Background**: `#F8FAFC` (tinted toward Emerald via warm white)
- **Light Surface**: `#FFFFFF` (pure white)
- **Light Surface Elevated**: `#F1F5F9` (subtle emerald tint)
- **Light Surface Border**: `#E2E8F0` (light gray with cool undertone)
- **Dark Background**: `#0B0F17` (deep financial dark)
- **Dark Surface**: `#151D28` (primary dark surface)
- **Dark Surface Elevated**: `#1E293B` (elevated cards/panels)
- **Dark Surface Border**: `#334155` (borders in dark mode)

### Color Usage Rules
- Never use `#000` or `#fff`. Tint every neutral toward the brand hue (Emerald).
- Chrome at extremes (0 or 100 lightness) reduced to prevent garish appearance.
- Accent colors (Cyan, Gold, Red, Green) used deliberately for specific semantics, not as primary brand carriers.

## Typography

### Font Family
- **Cairo**: 5 weights (Light, Normal, Medium, Bold, Black)
- Arabic-localized font with proper glyph support for Arabic numerals and digits

### Typographic Scale
- `displayLarge`: 57sp
- `displayMedium`: 48sp
- `displaySmall`: 40sp
- `headlineLarge`: 36sp
- `headlineMedium`: 32sp
- `headlineSmall`: 28sp
- `titleLarge`: 22sp
- `titleMedium`: 18sp
- `titleSmall`: 16sp
- `bodyLarge`: 15sp
- `bodyMedium`: 14sp
- `bodySmall`: 13sp
- `labelLarge`: 12sp
- `labelMedium`: 11sp
- `labelSmall`: 10sp

### Line Heights
- Display: 1.1x
- Headline: 1.25x
- Title: 1.35x
- Body: 1.5x
- Label: 1.35x

### Hierarchy Rule
- Minimum 1.25 ratio between hierarchical steps
- Avoid flat scales; use Cairo's weight contrast (Light vs Medium vs Bold)

## Elevation

### Surface System
- **Flat**: No shadow (0dp elevation)
- **Subtle**: 1dp shadow - `$elevation-1`
- **Medium**: 2dp shadow - `$elevation-2`  
- **High**: 4dp shadow - `$elevation-4`
- **Maximum**: 8dp shadow - `$elevation-8`

### Shadow Rules
- Never animate CSS layout properties
- Ease out with exponential curves (ease-out-quint)
- Shadows only on surfaces that require visual separation
- Dark mode shadows use warmer tones; light mode uses cooler grays

## Layout

### Spacing Rhythm
- Base unit: 4dp grid
- Spacing scale: 4, 8, 12, 16, 20, 24, 32, 40, 48
- Vary spacing for rhythm - same padding everywhere is monotony
- Negative space is intentional, not wasted

### Component Patterns
- **Cards**: Used when truly the best affordance; nested cards always wrong
- **Containers**: Don't wrap everything in a container - most things don't need one
- **Bottom Nav**: Standard bottom bar with 5 tabs, icon + label pattern
- **Scaffold**: Top app bar with brand logo, language toggle, voice record, add manual, export/share

### Screen Layout
- **Dashboard**: Curved Bézier payoff graph + KPI cards + transaction list
- **Installments**: Installment list with payment cards + payoff graph
- **AI Copilot**: Chat bubble pattern with confirmation cards
- **Settings**: Form-based with toggle switches and budget configuration

## Motion

### Animation Principles
- **Don't animate CSS layout properties** (this is Jetpack Compose, but principle holds)
- **Ease out with exponential curves**: ease-out-quart / quint / expo
- **No bounce, no elastic** - financial interfaces should feel precise and reliable
- **Purposeful only** - every animation serves a feedback purpose

### Specific Animations
- **Curved area graph**: Bézier curve path animation for payoff visualization
- **Confirmation cards**: pressScale(0.96f) + haptic feedback on button press
- **Skeleton screens**: fade-in/out loading placeholders
- **Filter chips**: scale up on selection, subtle background color change
- **Language toggle**: cross-fade transition between Arabic/English layouts

### Reduced Motion
- respect `prefers-reduced-motion` system setting
- For financial app: provide static alternatives for all animated elements
- Key charts/graphs still readable without animation

## Components

### Common Patterns
- **Card**: Rounded corners (16dp max), surface background, subtle border, elevated shadow
- **Empty State**: Illustrative + call-to-action + secondary action link
- **Skeleton**: Shimmer effect matching base color, never pure white on colored surfaces
- **Filter Chip**: Selected/deselected state with border/pill styling
- **Confirmation Card**: Glass-morphic used purposefully (not decorative), with two-action buttons

### Financial-Specific
- **Payoff Graph**: Curved Bézier chart showing debt reduction month-over-month
- **Payment Card**: Status indicators (PENDING/PARTIALLY_PAID/PAID) with color-coded badges
- **Installment Summary**: Total commitment, remaining balance, progress percentage
- **SMS Import Status**: Parsed count vs total messages, with retry action

### Accessibility Variations
- All color information also conveyed via text labels or icons
- Touch targets minimum 48dp high/wide
- Focus indicators visible in both light and dark mode
- Text contrast: minimum 4.5:1 for normal text, 3:1 for large text (WCAG AA)