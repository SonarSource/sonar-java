---
title: Documentation conventions
description: Keep public SonarJava documentation current, discoverable and checkable.
sources:
  - resource: "5739839551"
generated:
  by: tirion
---

# Documentation conventions

`docs/` is the single documentation root. Keep the public custom-rule tutorial, Maven example and assets there together so the tutorial and its executable example stay in sync without changing the Maven reactor or public entry URL. Module READMEs may stay next to code but should link to the canonical engineering instructions instead of repeating them.

- `docs/systems/` describes current behavior, constraints and boundaries that are not apparent from code alone.
- `docs/adrs/` records decisions in force: what we require, why, and why alternatives do not fit. Delete superseded decisions; Git preserves history.
- `docs/custom-rules/` contains the public tutorial topics. `docs/java-custom-rules-example/` is the buildable example, not a separate documentation root.
- Keep an index file in every directory that holds documentation, following the example in `docs/index.md`. Assets and source-only directories do not need an index. Each doc must be reachable from `AGENTS.md` via indexes or direct pointers.

## Write and cite

1. Use one topic and one authoritative home per fact. Lead with the code or CI entry point and explain why a constraint exists; add a short Javadoc pointer from key entry classes to the relevant doc rather than paraphrasing the doc in code.
2. Write repo path references as plain backticked paths relative to the root, for example `java-frontend/pom.xml`. Use exact case, no elisions or globs. Use relative Markdown links for navigation and anchors. Put example paths and invocations in appropriately labeled code fences, distinct from actual repo pointers.
3. Describe only current public-safe facts, using names from code. Do not copy internal pages, future plans, release logs or private URLs. Cite access-controlled facts by stable resource ID and explain how an authorized reader can retrieve them.
4. Update the doc in the same change as the code or workflow it describes. Keep root `AGENTS.md` at most 150 lines outside tool-managed blocks, and other managed Markdown at most 300 lines. Keep tool-managed blocks between their markers.
5. Keep skills canonical under `.agents/skills/`, with only pointers in per-tool adapters. Link each skill and any component `AGENTS.md` from the root.

## OKF metadata

Each Markdown file under `docs/` starts with YAML frontmatter. Use a title and a one-sentence description. Record `generated.by` as the tool's name for agent-written docs. Add a `sources` list of `resource` IDs or public URLs for Jira or Confluence facts. Only the root `docs/index.md` declares `okf_version: "0.2"`. Use `stale_after` only when a document has a real expiry; omit it for living docs.

```yaml
---
title: Example topic
description: Explain when the example applies and what it requires.
generated:
  by: tirion
---
```

The [documentation workflow](../.github/workflows/docs.yml) validates the bundle and agent entry points on every pull request.
