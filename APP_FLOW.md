# App Flow

## Screens
- `home` — dashboard and recent transactions.
- `transactions` — searchable transaction list.
- `budget` — budget progress list.
- `settings` — capture mode, categories, preferences, and app actions.
- `add_transaction` — transaction form.
- `transaction/{transactionId}` — transaction details.
- `budget/{budgetId}` — budget details.
- `loan/{loanId}` — loan details.
- `loans` — loans placeholder / entry point.
- `fuliza` — Fuliza placeholder / entry point.
- `transaction_costs` — cost placeholder / entry point.
- `reports` — reports placeholder.
- `about` — app info.
- `capture_mode` / `sms_permission` / `manual_paste` — capture flow.

## Navigation
- Bottom nav: Home, Transactions, Budget, Settings.
- Quick add: Home and Transactions open `add_transaction`.
- Settings links to Loans, Fuliza, Transaction Costs, Reports, About.
- List items open details pages.

## Data Flow
- Transactions feed Home, Transactions, Budget details, and transaction details.
- Budgets derive from Room transactions and category rows.
- Transaction costs store linked fee rows for saved transactions.
- Loans and Fuliza entities persist separately in Room.

## Known Gaps
- Add-transaction edit mode still routes to create form.
- Loans, Fuliza, and transaction costs still use placeholder screens.
- Settings preferences are in ViewModel state only; not persisted yet.
- Reports remains placeholder.

## Build
- `./gradlew :app:assembleDebug`
