# 001 — Polish core motion and respect reduced motion

- **Status**: DONE
- **Commit**: 36f609e
- **Severity**: HIGH
- **Category**: Accessibility, cohesion, easing and duration
- **Estimated scope**: 5 files, small focused change

## Problem

Core navigation uses a plain 140 ms crossfade at `app/src/main/java/com/example/sayit/presentation/main/MainFintechScreen.kt:352`, so unrelated screens briefly overlap with no spatial cue. Bottom navigation at `BottomNavBar.kt:16` changes selection without tactile icon feedback. Infinite pulse and thinking animations in `VoiceInputDialog.kt:137` and `AiCopilotScreen.kt:890` run even when Android animator duration scale is zero. The parsed voice result at `VoiceInputDialog.kt:280` uses the framework default transition rather than the app motion tokens.

## Target

- Replace tab `Crossfade` with interruptible `AnimatedContent`: 180 ms fade plus an 8 dp horizontal slide determined by tab order, using `EmilEasings.StrongEaseOut`; reverse direction when navigating backward. When motion is disabled, use a 100 ms fade only and no translation.
- Animate only the selected bottom-navigation icon to scale 1.08 with `animateFloatAsState`, 160 ms `StrongEaseOut`; keep unselected at 1.0. When motion is disabled, snap to 1.0.
- Gate recording pulse and AI thinking-dot scale animation with `motionEnabled()`. Reduced-motion states remain visible and static at scale 1.0.
- Give the parsed voice result `enter = emilScaleFadeEnter(220, 0.96f)` and `exit = emilScaleFadeExit(160, 0.98f)` when motion is enabled; reduced motion uses 100 ms fade only.

## Repo conventions to follow

- Shared curves and helpers live in `app/src/main/java/com/example/sayit/presentation/common/EmilAnimations.kt`.
- Motion accessibility comes from `motionEnabled()` in `MotionPreferences.kt`.
- Animate only `graphicsLayer` transform and opacity; do not animate layout dimensions.

## Steps

1. Add compact reusable transition specs or duration constants to `EmilAnimations.kt` only if this avoids duplication.
2. Replace the tab crossfade in `MainFintechScreen.kt` with directional `AnimatedContent`, using the existing `NavTab` ordering to determine direction and a fade-only reduced-motion branch.
3. Add selection scale feedback to `BottomNavBar.kt` using `graphicsLayer`; preserve Material3 selection colors and behavior.
4. Update `VoiceInputDialog.kt` so pulse and parsed-result entrance honor `motionEnabled()` and use the shared transitions.
5. Update `ThinkingIndicatorBubble` in `AiCopilotScreen.kt` so the dots remain static at scale 1 when motion is disabled.

## Boundaries

- Do not change navigation destinations, state handling, copy, colors, layout sizes, or business logic.
- Do not add dependencies.
- Do not touch generated resources or the app icon.
- Preserve every existing user change in the dirty worktree.

## Verification

- **Mechanical**: run `.\\gradlew.bat assembleDebug lintDebug --console=plain`; both tasks must pass.
- **Feel check**: rapidly switch adjacent and non-adjacent tabs; each transition must retarget cleanly and finish within 180 ms. Selected nav icons should give subtle feedback without bouncing. Toggle Android animator duration scale to 0; the mic ring and thinking dots must be static, tab changes must have no positional movement, and content must remain understandable.
- **Done when**: the build and lint pass, all four surfaces use the shared motion language, and reduced-motion mode removes continuous and positional movement.
