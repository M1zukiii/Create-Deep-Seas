# Changelog

## [May 27, 2026] - Experimental Branch Updates & Enhancements

### Configuration & User Interface
- **In-Game Mod Configuration UI:** Implemented a new custom split-pane configuration screen (`HullStrengthConfigScreen`) to allow editing block-by-block depth limits and implosion chances directly in-game.
- **Config Persistence:** Changes made in-game are automatically saved to `config/submarine_hull.json` and applied at runtime.

### Pressure Physics & Visual Glitches
- **Refactored Stress Cues:** Restructured pressure calculations to only play metal creaking stress sounds and spawn water dripping particles when a block actually sustains crack or implosion damage. Blocks set to 0% implosion chance are now completely silent and dry.
- **Configurable Floater Limits:** Updated the Floater block to respect customized depth limits set in the configuration instead of using a hardcoded threshold.

### New Items & Create Integration
- **Survival Friendly:** All custom blocks and items (including Phycological Membranes, Pressurized Glasses, and Floater variants) now have proper survival recipes, making the mod fully survival-friendly.
- **Phycological Membrane:** Registered and added the Phycological Membrane item, which can only be crafted by pressing a Kelp block under a Create Mechanical Press.
- **Iron & Copper Pressurized Glasses:** Reworked the generic glass pressurizer block into distinct Iron and Copper variants with new recipes, models, and textures.
- **Colored Floater Variants:** Added crafting recipes for Floater blocks in all vanilla wool colors.

### Abyss Biome & Custom World Generation
- **Abyss Biome:** Introduced a deep ocean Abyss biome ALPHA
- **Biome Modifiers:** Implemented world generation modifiers for amplified, large biomes, and default world types.

### Renders, Compatibility & Performance
- **Sodium & Veil Shaders:** Integrated critical rendering compatibility mixins for Sodium and Veil pipelines.
- **Sable Network Synchronizations:** Resolved package de-synchronization issues under active tracking states.
- **General Asset Cleanup:** Removed outdated model textures and unused assets to optimize mod footprint.
- **Sable Dependency Upgrade:** Upgraded Sable to version 1.2.2 for improved sub-level physics and performance stability.


## For developers (Modrinth Maven)

```groovy
repositories {
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = "https://api.modrinth.com/maven"
            }
        }
        filter { includeGroup "maven.modrinth" }
    }
}

dependencies {
    implementation "maven.modrinth:create-deep-seas:2.0.0"
}
```
