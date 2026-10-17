# Controlify Chords

A standalone Fabric client addon intended to provide configurable ordered controller-button chords for Controlify.

## Baseline target

- Minecraft Java Edition **26.2**
- Fabric Loader **0.19.0+**
- Controlify **3.5.3+mc26.2**
- Java **25**

The version values are centralized in `gradle.properties`. New Minecraft/Controlify versions must be tested in CI before being declared supported; future versions are not assumed compatible just because this project is structured for upgrades.

## Build automation

GitHub Actions runs on **every push/commit to every branch**, on pull requests, and manually. A successful build uploads the JAR under the run's **Artifacts** section for 14 days. A failed build uploads available Gradle reports to help diagnose the failure.

Open **Actions → Build → latest run → Artifacts** to download the compiled JAR.

The manual workflow also accepts an optional Minecraft version override for compatibility experiments. This does not guarantee that the selected version is supported by the chosen Loom, loader, or Controlify release.

## Current implementation status

This is the foundational project skeleton. It contains the Fabric client entrypoint, metadata, centralized version settings, and CI workflow. The actual chord engine, configuration screen, and safe handling of overlapping ordinary bindings still need implementation and in-game testing.

## Intended input semantics

- Modifier order matters: `LB → RB → A` differs from `RB → LB → A`.
- Required modifiers remain held until the trigger is released; releasing one cancels the chord.
- Trigger presses execute on release, once.
- Only explicitly configured triggers participate in chord interception.
- Normal input must remain unchanged when no matching chord is active.
- Ordinary bindings that overlap a chord must not fire twice.

## Upgrade policy

Keep version pins centralized, test each new Minecraft/Controlify combination in CI, and update compatibility metadata only after a successful build and runtime verification. See [GitHub Actions](https://github.com/youhaoyu159028/controlify-chords/actions).
