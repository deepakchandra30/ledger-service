# AI-assisted development

How generative tools were used on this project, and what was not delegated.

## Used for

- Scaffolding: Gradle setup, Spring configuration boilerplate, the React shell.
- First drafts of test cases, particularly edge cases worth covering.
- Reviewing the concurrency test for holes before running it.
- Explaining Postgres behaviour to check my understanding against the docs.

## Not used for

- Design decisions. The three ADRs are mine; the trade-offs are argued from the
  behaviour of the system, not from a suggestion.
- Anything accepted without reading. Every generated block was read line by
  line, and several were rejected: one suggested `SERIALIZABLE` everywhere,
  which would have hidden the lost-update problem instead of solving it, and one
  published to Kafka inside the transaction, which is the exact failure the
  outbox exists to prevent.

## How output was verified

Generated code was checked against the tests, not against how plausible it
looked. The invariant test (`signedTotal() == 0`) is the backstop: any change
that breaks double entry fails it regardless of how the code reads.
