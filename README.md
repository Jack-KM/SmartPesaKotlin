# SmartPesa

Android app for tracking M-Pesa money flow from SMS, notification listener, or manual paste.

## What it does
- Parses M-Pesa SMS into saved transactions.
- Lets you paste an SMS if you do not want SMS permission.
- Watches M-Pesa notifications as another capture path.
- Organizes transactions into categories and budgets.
- Shows home, transactions, budget, and settings screens in a Compose UI.
- Adds detail routes for transactions, budgets, and loans.
- Adds placeholder pages for loans, Fuliza, reports, costs, and about.
- Stores data locally with Room; no backend in code.

## Supported transaction types
- Send
- Receive
- Paybill
- Buy Goods
- Withdrawal
- Airtime
- Token purchase
- Deposit
- Fuliza repayment

## Tech stack
- Kotlin
- Jetpack Compose + Material 3
- Hilt
- Room
- WorkManager
- Navigation Compose
- DataStore
- Vico charts

## Permissions
- `RECEIVE_SMS` and `READ_SMS` for automatic SMS capture.
- Notification listener for notification-based capture.
- Manual paste path works without SMS permission.

## Project layout
- `app/src/main/java/com/example/smartpesa/data/mpesa` — parser and parsed transaction model.
- `app/src/main/java/com/example/smartpesa/data/sms` — SMS intake and WorkManager pipeline.
- `app/src/main/java/com/example/smartpesa/data/local` — Room entities, DAOs, database.
- `app/src/main/java/com/example/smartpesa/ui` — Compose screens, navigation, theme.

## Run
```sh
./gradlew assembleDebug
```

## Current app flow
See `APP_FLOW.md` for screen inventory, navigation, and current gaps.

Open the project in Android Studio or install the debug APK on an Android 7.0+ device.

## Notes
- Categories are seeded on first launch.
- Data stays on device.
- Parser tests live in `app/src/test/java/com/example/smartpesa/data/mpesa/MpesaSmsParserTest.kt`.
