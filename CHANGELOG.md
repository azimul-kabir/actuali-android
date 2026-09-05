# Changelog

All notable user-facing changes to Actuali for Android are recorded here. This project uses [Semantic Versioning](https://semver.org/) where practical. Versions marked `alpha` are testing builds and may contain incomplete workflows or require a clean reinstall before a future production release.

## Unreleased

## [0.1.0-alpha.2] - 2026-09-05

Second public testing release.

### Added

- Account details now show working, cleared, uncleared, and reconciled balances
- Synced notes for accounts and budget categories using Actual's native notes data
- Credit-card account details with available credit, limit, current billing cycle, cycle spend, and payment due date
- Category rollover-overspending control and history-based quick assign suggestions
- Full split transaction entry and editing with per-line category, amount, optional payee, note, direction, remaining amount, line addition/removal, and collapse back to a normal transaction

### Fixed

- Transaction forms now scroll through fields and actions within the available screen and keyboard space
- Expense, Income, and Transfer selector labels are centered consistently
- Add mode now has an explicit Cancel action; Edit mode has working Save, Delete, and Cancel actions
- Split edits retain existing child transaction identities instead of unnecessarily replacing every line
- Existing split transactions can be safely converted back to standard transactions

## [0.1.0-alpha.1] - 2026-09-05

Initial public testing release.

### Added

- Native Jetpack Compose interface for Budget, Accounts, Add, Reports, and More
- Password connection to self-hosted Actual servers
- Remote budget selection, download, local SQLite storage, and offline access
- Actual-compatible encrypted CRDT synchronization with manual and background sync
- Budget overview, month navigation, category groups, collapsible rows, totals, progress bars, and editable budget amounts
- Persistent category and group hiding with hidden-category management
- Account balances, on-budget/off-budget grouping, monthly income/expense/net summary, and transaction browsing
- Full local transaction history with search and optional date grouping
- Expense, income, and transfer entry with searchable account, payee, and category fields
- Transaction editing, clearing, deletion, splitting, date pickers, notes, and calculator amount entry
- New-payee creation from transaction entry
- Long-press actions for accounts, groups, categories, and transactions
- Local backup, restore, retention, and pre-restore revert support
- Rules and scheduled-transaction backend processing
- Credit-card limits, statement cycles, due dates, cycle spend, and available-credit display
- Basic reports backed by local budget data
- Light, dark, and system appearance modes
- Configurable start page, decimal visibility, balance privacy, transaction grouping, and account summary
- Currency display options for None, BDT, USD, EUR, GBP, CAD, AUD, JPY, INR, CNY, SGD, AED, and SAR
- Optional symbol-only currency formatting
- App name and icon matching the original Actuali visual identity

### Known limitations

- This build is alpha software and should be used with tested backups
- The APK is debug-signed for sideload testing, not Play Store distribution
- OpenID Connect, custom proxy headers, advanced dashboards, bank-feed setup, rule editing, and schedule-management UI are not yet included
- Some advanced entity merge, reorder, template, goal, and automation workflows remain incomplete
- Apple-only features from the iOS project are intentionally excluded

### Credits

- [Matt Farrell's Actuali for iOS](https://github.com/MattFaz/actuali) is the upstream behavioral and design reference
- [Actual Budget](https://github.com/actualbudget/actual) provides the underlying budgeting platform and source reference for CRDT behavior
- The Actuali icon was designed by [u/bdownz](https://www.reddit.com/user/bdownz/)

[0.1.0-alpha.1]: https://github.com/azimul-kabir/actuali-android/releases/tag/v0.1.0-alpha.1
[0.1.0-alpha.2]: https://github.com/azimul-kabir/actuali-android/releases/tag/v0.1.0-alpha.2
