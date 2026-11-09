The plugin JAR contains a `licenses/` folder with the licenses of its dependencies.

This directory supplies local plain-text license sources and dependency metadata overrides for Maven license generation. URL replacements and stable shared output names are configured in the plugin module's `pom.xml`. Dependencies with the same license text share one file under `licenses/THIRD_PARTY_LICENSES`.

Run the existing `updateLicenses` profile after changing dependencies. Normal verification compares regenerated licenses with the committed resources.
