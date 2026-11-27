---
title: External context
description: Retrieve canonical specifications and shared guidance without copying private content into SonarJava.
sources:
  - resource: "5742002202"
  - resource: "5739839551"
  - resource: "2259484684"
  - resource: "5691441188"
  - resource: "4034199608"
  - resource: "4119691265"
  - resource: "5563351042"
  - resource: "5770248413"
generated:
  by: tirion
---

# External context

## Public sources

- [RSPEC](https://github.com/SonarSource/rspec) is the canonical rule specification and metadata source. [Sonar rules](https://rules.sonarsource.com/java/) is the published Java rule catalog; use it for released behavior, not as a substitute for the source specification when implementing a change.
- [Java analysis documentation](https://docs.sonarsource.com/sonarqube-server/analyzing-source-code/languages/java) is the customer-facing reference for scanner settings and prerequisites.

## Access-controlled sources

Use your approved SonarSource Confluence connection and retrieve these pages by ID (for example, the Confluence page lookup's `page_id` field). If you lack access and need a page for the change, ask the repository owners. Do not copy content or access URLs into this public repository.

### Repository Baseline

- **Agentic Development Repository Baseline**, page `5742002202`: repository requirements shared across projects.
- **Agent-facing documentation**, page `5739839551`: shared documentation guideline, not a source of SonarJava build commands.

### Java analyzer and related projects

| Page | ID | Retrieve when |
| --- | --- | --- |
| Java Analyzer | `2259484684` | Looking for the performance-monitoring entry point; repository-owned PVF behavior is in [verification](verification.md#performance-validation). |
| Proposal: Replace ECJ with javac in Sonar Java | `5691441188` | Investigating the compiler proposal; it is not an active architecture decision. |
| Release sonar-java-jdt | `4034199608` | Coordinating a release or upgrade of the separate ECJ fork. |
| Release sonar-java-symbolic-execution | `4119691265` | Coordinating with the separate symbolic-execution repository. |
| SonarJava SE Release Instructions | `5563351042` | Looking up release instructions for that engine, not this plugin. |
| KTLO Daily Checklist | `5770248413` | Looking up operations shared across repositories. |
