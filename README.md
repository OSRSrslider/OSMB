# rslider OSMB Scripts

OSMB scripts by rslider.

## Scripts

### BlisterwoodChopper

Chops Blisterwood trees and drops logs when inventory is full.

**Features:**

- My first attempt at chopping a stationary, permanent tree
- Drops logs when inventory is full
- An attempt at animation timeouts for tree chopping detection
- An attempt at some humanization
- Ignorantly sprinked gaussian-influenced randomness (probably better than ignorantly sprinkled regular randomness)

**Setup:**

1. Place the `BlisterwoodChopper.jar` file into your `%USERPROFILE%\.osmb\Scripts\` folder
2. Select BlisterwoodChopper in OSMB
3. Position your character near the Blisterwood tree in Darkmeyer
4. Run the script

**Technical Details:**

- **Version:** 1.1
- **Author:** rslider
- **Date:** 2025-07-10
- **Skill Category:** Woodcutting
- **Dependencies:** OSMB API (`../API.jar`)

## Development

Scripts are built with direct javac/jar commands using JDK 17.
Developed, compiled, and built on MacOS, and automatically deployed to `.osmb\Scripts` folder of remote Windows PC through CI/CD pipeline through VSCode tasks.
