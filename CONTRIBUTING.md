# Contributing to Sona

Thanks for taking a look at Sona. The project is an early Android music player MVP, so focused feedback and small pull requests are especially useful.

## Good First Contributions

- Bug reports with device model, Android version, and clear reproduction steps.
- UI polish that keeps the existing Material 3 style.
- Small downloader reliability fixes.
- Tests for library, playlist, downloader, or formatting logic.
- Documentation improvements.

## Development Workflow

1. Fork the repository.
2. Create a focused branch.
3. Keep changes small and related.
4. Run verification before opening a pull request:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

## Pull Request Notes

- Describe what changed and why.
- Include screenshots or recordings for UI changes.
- Mention any downloader URLs used for manual testing.
- Avoid committing local signing files, build outputs, IDE workspace files, or `local.properties`.

## Release Signing

Release signing files are intentionally ignored by Git. Use `keystore.properties.example` as a template for local release builds.
