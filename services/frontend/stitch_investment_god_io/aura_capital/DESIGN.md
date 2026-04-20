```markdown
# Design System Strategy: The Sovereign Exchange

## 1. Overview & Creative North Star
**The Creative North Star: "The Ethereal Trading Floor"**
This design system moves away from the static, spreadsheet-heavy nature of traditional finance apps and leans into a high-stakes, cinematic gaming environment. We are building "The Ethereal Trading Floor"—a space where professional-grade data visualization meets the immersive atmosphere of a premium IO game.

To achieve this, we break the "standard grid" by utilizing **intentional asymmetry** and **tonal depth**. Instead of boxing data into rigid containers, we allow information to breathe across layered, translucent surfaces. We use high-contrast typography scales—pairing the technical precision of *Inter* with the architectural weight of *Space Grotesk*—to signal authority while maintaining a cutting-edge, "God-tier" aesthetic.

---

## 2. Colors
Our palette is rooted in a deep, nocturnal foundation, allowing neon action tokens to "vibrate" against the background.

*   **Primary Palette:** 
    *   `background` (#0b1326) & `surface`: The void of the market.
    *   `primary` (#4edea3): The "Emerald Buy" – a vibrant, high-energy green for growth and acquisition.
    *   `secondary` (#ffb2b7): The "Ruby Sell" – a sharp, urgent red for liquidation.
    *   `tertiary` (#ffb95f): The "Amber Hold" – a cautious, golden tone for strategic waiting.

*   **The "No-Line" Rule:** 
    Prohibit 1px solid borders for sectioning. Structural boundaries must be defined solely through background color shifts. For example, a `surface-container-low` chart module sitting on a `surface` background creates a natural, soft edge. We define space through mass and tone, not outlines.

*   **Surface Hierarchy & Nesting:** 
    Treat the UI as stacked sheets of frosted glass. 
    *   Use `surface-container-lowest` for the deepest background elements.
    *   Use `surface-container-high` for active trading modules.
    *   This nesting creates "perceived depth," making the "God of Investment" dashboard feel like a physical, high-tech console.

*   **The "Glass & Gradient" Rule:** 
    For floating elements (like the AI Learning Notice or HUD overlays), use Glassmorphism. Apply a semi-transparent `surface-variant` with a 20px-40px backdrop-blur. 

*   **Signature Textures:** 
    Main CTAs (Buy/Sell) should not be flat. Use a subtle linear gradient from `primary` to `primary-container` to give buttons a "gem-like" luster, suggesting value and premium quality.

---

## 3. Typography
We use a dual-font system to balance "Finance" (Precision) and "Gaming" (Impact).

*   **Display & Headlines (`Space Grotesk`):** This is our "Editorial" voice. Use `display-lg` for portfolio totals and `headline-md` for stock names. The geometric, slightly futuristic terminals of Space Grotesk convey the "God" persona—authoritative and modern.
*   **Body & Labels (`Inter`):** The "Technical" voice. All data points, ticker symbols, and fine print use Inter. It provides maximum legibility at small sizes (`label-sm`) for dense stock charts.
*   **Visual Hierarchy:** Use `primary` color for positive price movements and `secondary` for negative, but keep the typography weight bold to ensure the "Gaming" energy isn't lost in the data.

---

## 4. Elevation & Depth
Depth in this system is a measure of "Market Importance."

*   **The Layering Principle:** Stacking tiers replaces shadows. A `surface-container-highest` card placed on a `surface-container-low` page body creates an immediate focal point without the clutter of traditional UI depth markers.
*   **Ambient Shadows:** For high-stakes modals (e.g., "Confirm Trade"), use a highly diffused shadow. 
    *   *Spec:* `0px 24px 48px rgba(0, 0, 0, 0.4)`. The shadow should be tinted with `surface-container-lowest` to feel like an ambient occlusion rather than a "drop" shadow.
*   **The "Ghost Border" Fallback:** If a separation is required for accessibility, use the `outline-variant` token at 15% opacity. It should be felt, not seen.
*   **Glow States:** Active items (the currently selected stock) should utilize a `surface-tint` outer glow (8px blur, 30% opacity) to mimic a neon "active" light on a trading rig.

---

## 5. Components

### **Buttons (The Action Hub)**
*   **Primary (Buy):** `primary` background, `on-primary` text. Apply a subtle outer glow on hover.
*   **Secondary (Sell):** `secondary_container` background with `on-secondary_container` text for a sophisticated "hot" look that isn't jarring.
*   **Tertiary (Hold):** Ghost style with `tertiary` text and a `tertiary` ghost border (20% opacity).

### **Cards & Trading Modules**
*   **Constraint:** No divider lines. Separate "Current Price" from "Market Cap" using a 16px vertical gap (from the Spacing Scale) or a shift from `surface-container` to `surface-container-low`.
*   **Glassmorphism:** AI Learning notices must use a `surface-variant` background at 60% opacity with a heavy backdrop blur to signify it is a "smart layer" hovering over the data.

### **Ranking Tables**
*   **Zebra-Strips are forbidden.** Use a subtle `surface-bright` highlight only for the "User's Rank" to make it pop. Use `title-md` for names and `label-md` for the numeric rank to create editorial contrast.

### **Stock Charts**
*   **The "Vibrant Path":** The line graph should use a 2px stroke of `primary`. Use a gradient fill below the line (from `primary` at 20% opacity to `transparent`) to create a "holographic" volume effect.

---

## 6. Do's and Don'ts

*   **DO:** Use `display-lg` typography for "God-like" moments (e.g., hitting a 100% return).
*   **DO:** Leverage `surface-container-lowest` for the main background to make neon accents feel like they are floating in deep space.
*   **DON'T:** Use solid white (#FFFFFF). Use `on-surface` (#dae2fd) to maintain the cinematic, low-light atmosphere.
*   **DON'T:** Use hard-edged corners for cards. Stick to the `xl` (0.75rem) or `lg` (0.5rem) roundedness to keep the interface feeling sophisticated and "liquid."
*   **DO:** Ensure the AI learning notice is clearly distinct by using a unique blur radius—it should feel like an intelligence layer added *on top* of the game world.

---
**Director's Note:** Every pixel should feel like it costs a million dollars. If a screen feels "busy," remove a border and add a background tone shift instead. Let the data be the hero, but let the glow be the vibe.```