---
title: Check license changes for new dependencies
description: Check that new plugin dependencies have correct generated license evidence.
type: check
when: Pull requests add Maven dependencies or change the packaged dependency set.
actions: Report missing license updates for new shipped dependencies in PR Verification.
---

# Check license changes for new dependencies

Read `docs/systems/licenses.md`. For each newly shipped dependency, check whether `sonar-java-plugin/src/main/resources/licenses/` covers its license and whether `missing-dep-licenses.properties` needs an entry after license verification. Follow the actual packaging scope. Do not require new bundled files for test-only or provided dependencies, or where the existing generated inventory already covers the change.
