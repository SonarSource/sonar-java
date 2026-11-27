---
title: Third-party licenses
description: Check new dependency licenses and regenerate the license files shipped in the plugin.
generated:
  by: tirion
---

# Third-party licenses

`LICENSE.txt` contains SSALv1. When plugin dependencies change, validate their licenses and regenerate the bundled files under `sonar-java-plugin/src/main/resources/licenses/`. Use `missing-dep-licenses.properties` for dependencies whose license Maven cannot determine, after confirming the license. `sonar-java-plugin/license-resources/README.md` explains why some dependency license files are replaced with text versions.

Run from the repository root after [toolchain setup](build.md):

```bash
mise run lint:licenses
mise exec -- mvn clean package -PupdateLicenses
```

Review and commit the generated license changes. If a new dependency does not change the bundled licenses, explain why in the PR rather than editing generated files just to satisfy the check. The license goal runs in **Custom Rules and License Check** in `.github/workflows/build.yml`.
