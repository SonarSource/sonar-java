Migrate test sample files for $ARGUMENTS from the old location (`java-checks/src/test/files/checks/`) to the new location.

Before starting, ask the user whether the sample is **compiling** or **non-compiling**:
- **Compiling** samples go to: `java-checks-test-sources/default/src/main/java/checks/`
- **Non-compiling** samples go to: `java-checks-test-sources/default/src/main/files/non-compiling/checks/`

Follow these steps for each `*CheckTest.java` file being migrated:

## 1. Identify the test file and its sample files

Find the test class at `java-checks/src/test/java/org/sonar/java/checks/<CheckName>CheckTest.java`. Read it and identify ALL `.onFile("src/test/files/checks/...")` references. Each such reference points to an old sample file that needs migrating.

## 2. Move each sample file to the new location with the correct name

For each old sample file:

- The **primary** sample (the main one used in the `test()` method) should be renamed to `<CheckName>CheckSample.java` in the destination directory.
- **Variant** samples (used in other test methods, e.g. with custom config or different Java versions) should follow the convention `<CheckName>CheckSample_<variant>.java` or `<CheckName>Check_<variant>.java`, matching whatever variant suffix the old file had (e.g. `Custom`, `no_version`, `java8`).
- The destination depends on the user's choice:
  - **Compiling**: `java-checks-test-sources/default/src/main/java/checks/`
  - **Non-compiling**: `java-checks-test-sources/default/src/main/files/non-compiling/checks/`

Use `git mv` to move the file, then rename it if the filename changed.

## 3. Add `package checks;` to the sample file (compiling samples only)

For **compiling** samples: Insert `package checks;` as the very first line of the moved sample file, followed by a blank line before any existing content.

For **non-compiling** samples: Do NOT add a package declaration.

In both cases, this is the ONLY content modification to make to the sample. Do NOT rename classes, fix compilation errors, update imports, or make any other changes. Leave those for the user.

## 4. Update the test file

In the `*CheckTest.java` file:

For **compiling** samples:
- Add the import if not already present:
  ```java
  import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;
  ```
- Replace each old-style `.onFile("src/test/files/checks/<OldName>.java")` with:
  ```java
  .onFile(mainCodeSourcesPath("checks/<NewName>.java"))
  ```

For **non-compiling** samples:
- Add the import if not already present:
  ```java
  import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;
  ```
- Replace each old-style `.onFile("src/test/files/checks/<OldName>.java")` with:
  ```java
  .onFile(nonCompilingTestSourcesPath("checks/<NewName>.java"))
  ```

In both cases, `<NewName>` is the new filename chosen in step 2.

## 5. Verify

Run the specific test to verify nothing is broken:
```
mvn test -pl java-checks -Dtest=<CheckName>CheckTest -Dsurefire.failIfNoSpecifiedTests=false
```

If the test fails, report the failure to the user but do NOT attempt to fix the sample file content.

## Example: migrating `LambdaTypeParameterCheck`

**Old sample** at `java-checks/src/test/files/checks/LambdaTypeParameterCheck.java`:
```java
import java.util.function.BiConsumer;
import java.util.function.Consumer;

abstract class A {
  public void foo() {
    foo((String s) -> s.length()); // Compliant
    foo(s -> {return s.length();}); // Noncompliant {{Specify a type for: 's'}}
//      ^
  }

  abstract void foo(Consumer<String> s);
  abstract void foo(BiConsumer<String, Object> bc);
}
```

**New sample** at `java-checks-test-sources/default/src/main/java/checks/LambdaTypeParameterCheckSample.java`:
```java
package checks;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

abstract class LambdaTypeParameterCheckSample {
  public void foo() {
    foo((String s) -> s.length()); // Compliant
    foo(s -> {return s.length();}); // Noncompliant {{Specify a type for: 's'}}
//      ^
  }

  abstract void foo(Consumer<String> s);
  abstract void foo(BiConsumer<String, Object> bc);
}
```

Note: `package checks;` was added as the first line. The class was renamed from `A` to `LambdaTypeParameterCheckSample` by the user after migration — the skill only adds the package declaration.

**Old test** `LambdaTypeParameterCheckTest.java`:
```java
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

class LambdaTypeParameterCheckTest {
  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/LambdaTypeParameterCheck.java")
      .withCheck(new LambdaTypeParameterCheck())
      .verifyIssues();
  }
}
```

**New test** `LambdaTypeParameterCheckTest.java`:
```java
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class LambdaTypeParameterCheckTest {
  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/LambdaTypeParameterCheckSample.java"))
      .withCheck(new LambdaTypeParameterCheck())
      .verifyIssues();
  }
}
```
