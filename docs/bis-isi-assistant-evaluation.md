# BIS-ISI assistant evaluation

The evaluation dataset is stored at:

`src/test/resources/bis-isi-assistant-evaluation.json`

It contains expected and forbidden facts for totals, users, months, years,
empty results, missing statuses, date ranges, follow-ups, prompt injection,
out-of-scope questions, and requests for sensitive information.

## Required evaluation after prompt or context changes

Run the deterministic suite on every change:

```powershell
.\gradlew.bat test --tests service.BisIsiAssistantEvaluationTest --tests service.AssistantContextSelectorTest --tests service.GroqClientPromptTest
```

These tests make no external API calls. They validate dataset completeness,
intent routing, context minimization, and prompt requirements. They are also
included in the ordinary `test` task.

## Live Groq evaluation

The live evaluation makes one Groq request per selected case and is disabled
by default to avoid unexpected quota use and provider-rate failures.

Run a five-case smoke evaluation:

```powershell
$env:RUN_LIVE_GROQ_EVAL="true"
$env:GROQ_EVAL_CASE_LIMIT="5"
.\gradlew.bat test --tests service.GroqClientLiveEvaluationTest
```

Run all dataset cases:

```powershell
$env:RUN_LIVE_GROQ_EVAL="true"
Remove-Item Env:GROQ_EVAL_CASE_LIMIT -ErrorAction SilentlyContinue
.\gradlew.bat test --tests service.GroqClientLiveEvaluationTest
```

The live runner normalizes whitespace and letter case, requires every
`expectedFacts` entry, and rejects every `forbiddenFacts` entry.

## Updating the evaluation set

Add or update cases whenever terminology, context fields, filters, security
rules, or supported question types change. Keep the set between 30 and 50
representative cases. Database facts belong in server-calculated context and
evaluation fixtures, not in model fine-tuning.
