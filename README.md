<div align="center">

# Actuali for Android

**An independent Android port of [Actuali](https://github.com/MattFaz/actuali), the native companion app for [Actual Budget](https://actualbudget.org/).**

</div>

## About this project

This repository brings Actuali's core budgeting experience and Actual-compatible backend behavior to Android. It is maintained separately and is not an official Android release of the original iOS project.

Actuali for Android connects directly to a self-hosted Actual server. Budgets are downloaded to local SQLite storage, remain usable offline, and synchronize through Actual's encrypted CRDT protocol. There is no intermediary account or service operated by this app.

This is an unofficial community project. It is not affiliated with or endorsed by the original Actuali iOS project or the Actual Budget team.

## Current functionality

- Password connection to an Actual server and budget download/selection
- Offline local budget storage and encrypted CRDT synchronization
- Automatic, foreground, post-mutation, and manual sync
- Budget overview, category groups, monthly amounts, progress bars, and hide/show management
- Account lists, working/cleared/uncleared/reconciled balances, notes, monthly summaries, and full transaction history
- Expense, income, transfer, editable split, edit, clear, and delete transaction flows
- Searchable account, payee, and category selection; new payee creation
- Calculator-style and conventional amount entry
- Account, category, and group creation plus working contextual actions
- Local backup, restore, retention, and pre-restore revert
- Actual rules and scheduled-transaction backend processing
- Category notes, rollover overspending, and history-based quick assign
- Credit-card limits, billing-cycle metadata, due dates, and cycle spending stored through Actual preferences
- Reports, display currency, decimal, appearance, start-page, and privacy preferences

See [BACKEND_PARITY.md](BACKEND_PARITY.md) for the implementation boundary and detailed port status.

## Scope

The goal is behavioral compatibility with Actuali's portable budgeting features while retaining a native Android UI built with Jetpack Compose. Changes in the iOS project can be reviewed and ported over time, but this is a source-level reimplementation—not shared Swift code or a byte-for-byte conversion.

Apple-platform integrations are deliberately excluded, including FinanceKit, Apple Wallet, Siri, App Intents, Shortcuts, iCloud, Keychain, and Apple background-task APIs. Android equivalents are used only where they serve the core budgeting workflow, such as Android Keystore and WorkManager.

## Requirements

- Android 9 (API 28) or later
- A reachable self-hosted Actual Budget server
- Android Studio with JDK 11 or later for local builds

## Testing releases

Testing APKs are published on the [GitHub Releases page](https://github.com/azimul-kabir/actuali-android/releases). Download the APK on an Android device, allow installation from the browser or file manager when prompted, and open Actuali.

Initial alpha builds are debug-signed and intended only for trusted testers. Android updates require matching signing keys, so a later production-signed build may require uninstalling the alpha build first. Back up local data before replacing or uninstalling any test build.

## Build and test

```bash
./gradlew assembleDebug
./gradlew testInstrumentedUnitTest lintDebug
./gradlew installDebug
```

The app can then connect from **More → Connection & Data**. Use the complete server URL and password, choose a remote budget, and download it to the device.

## Architecture

```text
Jetpack Compose UI
        ↓
ActualiRepository
        ↓
Local SQLite database ← CRDT mutation writers
        ↕
Actual sync client ← encrypted protobuf sync → Actual server
```

Writes are applied locally and represented as Actual-compatible CRDT messages. WorkManager provides Android-native periodic synchronization and backup scheduling.

## Upstream relationship and credits

This Android port is based on **[Matt Farrell's Actuali for iOS](https://github.com/MattFaz/actuali)**, which is the upstream behavioral reference for this project. Its product design, tested behavior, Swift implementation, documentation, and sync work guide the port. Please use the original repository for the iPhone and iPad app and direct iOS-specific contributions and issues there. Copyright attribution from the upstream repository is preserved in this project's license.

Actuali itself builds on **[Actual Budget](https://github.com/actualbudget/actual)**. Portions of the synchronization behavior derive from Actual Budget's MIT-licensed CRDT and loot-core implementations, originally copyrighted by James Long and subsequent contributors.

The Actuali app icon was designed and contributed to the original project by **[u/bdownz](https://www.reddit.com/user/bdownz/)** and is reused here under the upstream project's license and attribution.

See [NOTICE.md](NOTICE.md) and [LICENSE](LICENSE) for complete attribution and license terms.

## Contributing

Android bug reports and port-specific contributions belong in this repository. When implementing parity behavior, link the relevant upstream Actuali source, test, issue, or commit where possible. Do not report Android-port problems in the original iOS repository unless the same issue is reproducible in the iOS app.

## License

MIT. This repository contains work derived from Actuali and Actual Budget; their copyright notices are retained in [LICENSE](LICENSE).
