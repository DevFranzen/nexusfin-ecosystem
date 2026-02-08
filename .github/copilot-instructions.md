# NexusFin Copilot Instructions

## Project Context
NexusFin (nxf) is a dual-landscape fintech system consisting of an **Exchange** (high performance) and a **Broker** (user-centric).

## Language & Communication
- **Response Language:** Always respond and generate content in **English**, even if the prompt is any other language.
- **Code Documentation:** All comments, JSDoc, and commit messages must be in English.
- **Variable Naming:** Use English exclusively for all identifiers (variables, functions, classes).

## Architectural Rules
- **Domain Boundaries:** Services in `apps/exchange/` (Prefix: `ex-`) and `apps/broker/` (Prefix: `br-`) must remain decoupled.

## Code Style
- **Types:** Interfaces over Types where possible. Strict null checks are mandatory.

## Context Verification Policy
- **Check for Context:** Before generating code or architectural suggestions for any service in `apps/`, you MUST verify if a corresponding context file exists (either in `docs/context/` or linked in the service's `README.md`).
- **Missing Context Protocol:** If no specific `.context.md` file is found for the service you are currently working on:
  1. **DO NOT** generate any implementation or logic.
  2. **STOP** and inform the user that the service-specific context is missing.
  3. **ASK** the user to provide the necessary context or point you to the correct documentation before proceeding.