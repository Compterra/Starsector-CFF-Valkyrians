# Valkyrians

Current version: `4.0.1` for Starsector `0.98a-RC8`.

A restored and polished release of the Valkyrians faction mod for modern Starsector. The Valkyrians are an independent military power descended from a Domain-era frontier state, fielding disciplined fleets built around advanced lasers, missiles, heavy gun batteries, carrier formations, and rare super-capital hulls.

This fork focuses on compatibility, English polish, lore grounding, and safer gameplay behavior while preserving the faction's original identity as a severe, industrial, high-tech military state.

## Features

- A full Valkyrian faction with its own systems, markets, fleets, portraits, missions, and faction presentation.
- Large ship roster ranging from destroyers and carriers to battleships, dreadnoughts, and rare super-capital hulls.
- Valkyrian weapon families, fighter LPCs, ship systems, hullmods, sounds, decorative lights, and GraphicsLib support.
- Orken Manufacturing Group, Horizon Shipyards, and Enigma Company industrial identities for hulls, weapons, and doctrine hardware.
- Valkyrian Logistics Liaison and Orken-Enigma War Coordination Bureau colony structure chain with a screened procurement storefront.
- Polished mission text, codex descriptions, hullmod tooltips, ship class language, and faction lore.
- Version Checker/TriOS support through `valkyrians.version`.

## Links

- Forum thread: https://fractalsoftworks.com/forum/index.php?topic=35651.0
- Version Checker file: https://raw.githubusercontent.com/Compterra/Starsector-CFF-Valkyrians/master/valkyrians.version
- Changelog: https://raw.githubusercontent.com/Compterra/Starsector-CFF-Valkyrians/master/CHANGELOG.txt

## Requirements

- Starsector `0.98a-RC8`
- LazyLib
- MagicLib
- GraphicsLib

## Optional Compatibility

The mod includes support/configuration for common ecosystem mods where present, including Nexerelin random-sector handling and Version Checker/TriOS release metadata.

## Current Restoration Notes

This repository contains a local maintenance and restoration fork. Recent work focused on:

- Starsector `0.98a-RC8` compatibility
- GitHub-backed Version Checker support
- mission text and codex polish
- cleaner faction lore and ship class terminology
- hullmod tooltip fixes and S-mod clarity
- Valkyrian colony structure implementation
- stale compiled-class cleanup
- safer portrait defaults for generated Valkyrian officers

## Install

Place this folder in your Starsector `mods` directory and enable **Valkyrians** in the launcher.

## Credits

- **ValkyriaL**: original author
- **Originem**: current maintainer
- **Laomi**: sprite remaster
- **FAX**, **Shadowlight**: additional support

## Repository Hygiene

The repository keeps live game data, graphics, sounds, Java override source, and the compiled mod jar. Audit scratch data, temporary files, logs, and local build output should stay out of release packaging unless specifically needed for the shipped mod.
