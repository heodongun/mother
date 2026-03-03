# SmartPetTodo - Agent Guidelines

## Default Delegation (Sub-Agents)
- Use sub-agents automatically when it reduces wall-clock time without increasing merge/conflict risk.
- Prefer `explorer` for read-only codebase mapping, locating files, and root-cause analysis.
- Prefer `worker` for isolated implementation tasks with explicit file ownership.

## When To Spawn
- The task has parallelizable subtasks (e.g., investigate + implement + verify).
- The change spans multiple layers/modules where discovery can be delegated safely.
- Builds/tests/lint are expected to take long enough to overlap with other work.

## Coordination Rules
- Always assign each sub-agent a strict scope (files/modules + a concrete goal).
- Never have multiple agents edit the same file at the same time.
- The main agent owns final integration, conflict resolution, and end-to-end verification.

## Project Commands (Android/Gradle)
- `./gradlew test`
- `./gradlew :app:assembleDebug`
- `./gradlew lint` (if configured)
