# Практическая работа №6 — Маршрутизация служебных заявок (вариант 4)

Java SE 21+, без Maven/JUnit.

## Сборка и запуск

./run_selfcheck.sh


## Вариант 4

* **Часть А:** R5 (`STANDARD`) — только при `amount < 50_000 && !urgent && risk == LOW && !personal`.
  Иначе — `FINANCE_REVIEW` (страховочный R6).
* **Часть Б:** `ApprovalRouter.addStandardEligibility(...) → boolean` — проверяет «стандартность» заявки,
  используется внутри `route` в R5.

## Структура

`src/ru/edu/pr03` — исходники, `data/sample.csv` — контрольные значения,
`diagrams/` — Activity и классы (DOT + PNG), `DECISIONS.md`, `REPORT.docx`.
