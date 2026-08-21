package checks;

import java.util.List;
import java.util.Map;
import java.util.Set;
import static java.util.Map.entry;

class DuplicateImmutableCollectionArgumentsCheckSample {

  private static final String CONST_A = "keyA";
  private static final String CONST_B = "keyB";
  private static final String CONST_A_ALIAS = "keyA";

  void testMapOf() {
    Map<String, Integer> empty = Map.of(); // Compliant
    Map<String, Integer> single = Map.of("a", 1); // Compliant
    Map<String, Integer> distinct = Map.of("a", 1, "b", 2, "c", 3); // Compliant

    Map<String, Integer> duplicateLiteral = Map.of(
      "timeout", 30,
      "retries", 3,
      "timeout", 60 // Noncompliant {{Remove or rename this duplicate key; "Map.of" throws an "IllegalArgumentException" at runtime when keys are duplicated.}} [[secondary=-2]]
    );

    Map<String, Integer> multipleDuplicates = Map.of(
      "k1", 1,
      "k2", 2,
      "k1", 3, // Noncompliant {{Remove or rename this duplicate key; "Map.of" throws an "IllegalArgumentException" at runtime when keys are duplicated.}} [[secondary=-2]]
      "k2", 4, // Noncompliant {{Remove or rename this duplicate key; "Map.of" throws an "IllegalArgumentException" at runtime when keys are duplicated.}} [[secondary=-2]]
      "k1", 5  // Noncompliant {{Remove or rename this duplicate key; "Map.of" throws an "IllegalArgumentException" at runtime when keys are duplicated.}} [[secondary=-4]]
    );

    Map<String, Integer> duplicateConstants = Map.of(
      CONST_A, 1,
      CONST_B, 2,
      CONST_A_ALIAS, 3 // Noncompliant {{Remove or rename this duplicate key; "Map.of" throws an "IllegalArgumentException" at runtime when keys are duplicated.}} [[secondary=-2]]
    );

    Map<Integer, String> duplicateIntKeys = Map.of(
      1, "one",
      2, "two",
      1, "uno" // Noncompliant {{Remove or rename this duplicate key; "Map.of" throws an "IllegalArgumentException" at runtime when keys are duplicated.}} [[secondary=-2]]
    );

    Map<Boolean, String> duplicateBoolKeys = Map.of(
      true, "yes",
      false, "no",
      true, "oui" // Noncompliant {{Remove or rename this duplicate key; "Map.of" throws an "IllegalArgumentException" at runtime when keys are duplicated.}} [[secondary=-2]]
    );
  }

  void testMapOfVariables(String varKey1, String varKey2) {
    Map<String, Integer> distinctVars = Map.of(varKey1, 1, varKey2, 2); // Compliant
    Map<String, Integer> sameVar = Map.of(
      varKey1, 1,
      varKey2, 2,
      varKey1, 3 // Noncompliant {{Remove or rename this duplicate key; "Map.of" throws an "IllegalArgumentException" at runtime when keys are duplicated.}} [[secondary=-2]]
    );
  }

  void testMapOfEntries() {
    Map<String, String> empty = Map.ofEntries(); // Compliant
    Map<String, String> single = Map.ofEntries(Map.entry("a", "b")); // Compliant
    Map<String, String> distinct = Map.ofEntries(
      Map.entry("k1", "v1"),
      Map.entry("k2", "v2"),
      entry("k3", "v3")
    ); // Compliant

    Map<String, String> duplicateEntries = Map.ofEntries(
      Map.entry("host", "localhost"),
      Map.entry("port", "8080"),
      Map.entry("host", "remotehost") // Noncompliant {{Remove or rename this duplicate key; "Map.ofEntries" throws an "IllegalArgumentException" at runtime when keys are duplicated.}} [[secondary=-2]]
    );

    Map<String, String> duplicateStaticImport = Map.ofEntries(
      entry("user", "admin"),
      entry("user", "guest") // Noncompliant {{Remove or rename this duplicate key; "Map.ofEntries" throws an "IllegalArgumentException" at runtime when keys are duplicated.}} [[secondary=-1]]
    );
  }

  void testSetOf() {
    Set<String> empty = Set.of(); // Compliant
    Set<String> single = Set.of("a"); // Compliant
    Set<String> distinct = Set.of("a", "b", "c"); // Compliant

    Set<String> duplicateStrings = Set.of(
      "read",
      "write",
      "read" // Noncompliant {{Remove or replace this duplicate element; "Set.of" throws an "IllegalArgumentException" at runtime when elements are duplicated.}} [[secondary=-2]]
    );

    Set<Integer> duplicateNumbers = Set.of(
      10,
      20,
      10 // Noncompliant {{Remove or replace this duplicate element; "Set.of" throws an "IllegalArgumentException" at runtime when elements are duplicated.}} [[secondary=-2]]
    );

    Set<String> duplicateConsts = Set.of(
      CONST_A,
      CONST_B,
      CONST_A // Noncompliant {{Remove or replace this duplicate element; "Set.of" throws an "IllegalArgumentException" at runtime when elements are duplicated.}} [[secondary=-2]]
    );

    Set<String> multipleSetDuplicates = Set.of(
      "x",
      "x", // Noncompliant {{Remove or replace this duplicate element; "Set.of" throws an "IllegalArgumentException" at runtime when elements are duplicated.}} [[secondary=-1]]
      "x"  // Noncompliant {{Remove or replace this duplicate element; "Set.of" throws an "IllegalArgumentException" at runtime when elements are duplicated.}} [[secondary=-2]]
    );
  }

  void testSetOfVariables(String v1, String v2) {
    Set<String> distinctVars = Set.of(v1, v2); // Compliant
    Set<String> duplicateVars = Set.of(
      v1,
      v2,
      v1 // Noncompliant {{Remove or replace this duplicate element; "Set.of" throws an "IllegalArgumentException" at runtime when elements are duplicated.}} [[secondary=-2]]
    );
  }

  void testListOfPermitsDuplicates() {
    List<String> list = List.of("dup", "dup", "dup"); // Compliant: List.of allows duplicates
  }
}
