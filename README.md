# PFT

A simple personal finance tracker CLI.

## Setup

### Prerequisites:
- JDK 21 or higher

### Build, Test, and Run

```bash
./gradlew build # Builds + runs tests

./gradlew run --args="--help"
```

## Example Usage

```bash
# Add a category. 
# Run the CLI via the Gradle wrapper...
> ./gradlew -q run --args="categories add --name Food"
# or via the ./pft wrapper 
> ./pft categories add --name Food
Category added: Category(id=1, name=Food)

# Record an expense and some income (the category is optional)
> ./pft transactions add --amount 100 --type EXPENSE --description "Grocery shopping"
> ./pft transactions add --amount 2500 --type INCOME --description "Salary"

# Categorize an existing transaction
> ./pft transactions edit --id 1 --category "Food"

# List transactions, optionally filtered by category and/or date range
> ./pft transactions list
> ./pft transactions list --category "Food" --after 2026-01-01T00:00:00

# Bulk import transactions from a CSV or TSV file
# (see src/test/resources/transactions.csv for the expected format)
> ./pft transactions import --file transactions.csv --format CSV
Imported 2 transaction(s).

# Summarize spend by category, optionally over a date range
> ./pft summary
Date range: all time
Total spend: 100.00
Total income: 2500.00
Net: 2400.00
Spend by category:
  Food: 100.00

# List the available rules, then run one
> ./pft flag --list-rules
weekend-spending
> ./pft flag --rule weekend-spending
```

Use `./pft --help` to see all available commands and options, e.g.:
```bash
> ./pft --help
> ./pft transactions add --help
```

## Technical Design Decisions

- Simplicity is prioritized (as per the brief). More complex features like recurring expenses, budgets, soft deletes, and audit logging are out of scope.
- SQLite is used for storage to keep persistence and querying simple. The database is stored as `pft.db` in the current working directory.
- IDs are auto-incremented integers to keep them short and importantly: easy to type for CLI usage.
- Constructor injection is used for wiring dependencies, I didn't think a full-on DI container was necessary for a project of this size.

## UX Design Decisions

- Amounts are non-negative decimals (up to cents) and assumes a single currency.
- Timestamps are local date-times with no timezone or offset.
- Transactions can use an existing category, but can't create a new one on the fly. This helps avoid accidental categories caused by typos.
- CSV and TSV are supported for bulk uploads. TSV is useful when importing copied spreadsheet (e.g. from Excel).
- Imports fail as a whole if any row is invalid, so they are atomic.
- The `--before` and `--after` date/time filters are exclusive: transactions at exactly the boundaries are not included.
- Deleting a category also unlinks it from all associated transactions. Uncategorized expenses are included in summaries under `Uncategorized`.

## Testing

- Black-box CLI tests (`PftCliTest`) are the main tests for this project. They cover the main happy paths and some edge cases.
- A simple unit test scaffold (with 3 tests) is also written for the `TransactionImportParser` to demonstrate how unit tests can be implemented. A comprehensive unit test suite was omitted for now due to time constraints.

## Future Improvements

Due to the `In short, keep it as simple as you can. Generally, we won't expect you to spend more than a few hours on this` time constraint, several features and improvements are left for the future, such as:
- Pretty-printing of outputs: the CLI currently outputs just print the string representation of the objects without any formatting or alignment. Table formatting with a library would be nice.
- More testing. Quite self explanatory. As mentioned above, the current test coverage is limited.
