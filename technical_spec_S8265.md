# Technical Implementation Specification: Zone ID Validation Check

## 1. Implementation Approach

### Base Pattern
Use **`AbstractMethodDetection`** pattern as this check focuses on detecting specific method invocations with invalid arguments.

### AST Node Types
- **Primary Target**: `METHOD_INVOCATION` nodes
- **Focus**: Static method calls to `ZoneId.of(String)`

### MethodMatchers API Usage
```java
private static final MethodMatchers ZONE_ID_OF = MethodMatchers.create()
  .ofTypes("java.time.ZoneId")
  .names("of")
  .addParametersMatcher("java.lang.String")
  .build();
```

### Class Structure
```java
@Rule(key = "S6885")
public class InvalidZoneIdCheck extends AbstractMethodDetection {
  
  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return ZONE_ID_OF;
  }
  
  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    // Implementation here
  }
}
```

---

## 2. Valid Zone IDs

### Validation Approach

#### 2.1 IANA Time Zone Database
- Use `ZoneId.getAvailableZoneIds()` to retrieve the canonical set of valid zone IDs
- Cache this set during check initialization for performance
- This provides all valid IANA identifiers (e.g., "America/New_York", "Europe/Paris")

#### 2.2 Fixed Offset Patterns
Accept strings matching these regex patterns:
- **Signed offsets**: `^[+-]\d{2}:\d{2}(:\d{2})?$` (e.g., "+05:30", "-08:00", "+01:00:00")
- **Compact format**: `^[+-]\d{4}$` (e.g., "+0530", "-0800")
- **Single digit hours**: `^[+-]\d{1}$` (e.g., "+5", "-8")

#### 2.3 Special Identifiers
Hardcode these valid special cases:
- "UTC"
- "GMT"
- "Z" (equivalent to UTC)
- "UT" (Universal Time, less common)

#### 2.4 Zone Offset IDs
Accept patterns:
- `^UTC[+-]\d{1,2}(:\d{2})?$` (e.g., "UTC+1", "UTC-05:30")
- `^GMT[+-]\d{1,2}(:\d{2})?$` (e.g., "GMT+5", "GMT-8")

### Implementation Code
```java
private static final Set<String> VALID_ZONE_IDS;
private static final Set<String> SPECIAL_ZONE_IDS = Set.of("UTC", "GMT", "Z", "UT");
private static final Pattern FIXED_OFFSET_PATTERN = Pattern.compile(
  "^[+-]\\d{1,2}(:\\d{2}(:\\d{2})?)?$|^[+-]\\d{4}$"
);
private static final Pattern ZONE_OFFSET_PATTERN = Pattern.compile(
  "^(UTC|GMT)[+-]\\d{1,2}(:\\d{2})?$"
);

static {
  VALID_ZONE_IDS = ZoneId.getAvailableZoneIds();
}

private static boolean isValidZoneId(String zoneId) {
  return VALID_ZONE_IDS.contains(zoneId)
    || SPECIAL_ZONE_IDS.contains(zoneId)
    || FIXED_OFFSET_PATTERN.matcher(zoneId).matches()
    || ZONE_OFFSET_PATTERN.matcher(zoneId).matches();
}
```

---

## 3. Detection Logic

### 3.1 Method Matching
```java
@Override
protected void onMethodInvocationFound(MethodInvocationTree mit) {
  Arguments arguments = mit.arguments();
  
  // Ensure we have exactly one argument
  if (arguments.size() != 1) {
    return;
  }
  
  ExpressionTree argument = arguments.get(0);
  
  // Extract constant value from argument
  Optional<String> zoneIdValue = extractZoneIdLiteral(argument);
  
  if (zoneIdValue.isEmpty()) {
    // Not a string literal, skip (avoid false positives)
    return;
  }
  
  String zoneId = zoneIdValue.get();
  
  // Validate the zone ID
  if (!isValidZoneId(zoneId)) {
    reportIssue(argument, createIssueMessage(zoneId));
  }
}
```

### 3.2 String Literal Extraction
```java
private Optional<String> extractZoneIdLiteral(ExpressionTree argument) {
  // Use the constant evaluator to get string literal values
  Optional<Object> constant = argument.asConstant();
  
  if (constant.isPresent() && constant.get() instanceof String) {
    return Optional.of((String) constant.get());
  }
  
  return Optional.empty();
}
```

### 3.3 Edge Cases

| Case | Handling | Rationale |
|------|----------|-----------|
| Non-literal arguments | Skip (no issue) | Cannot validate dynamic strings statically |
| Null arguments | Skip (no issue) | Will cause NPE, but different rule's responsibility |
| Empty string | Report issue | Invalid zone ID |
| Concatenated strings | Check if resolvable to constant | `asConstant()` handles compile-time constants |
| Variables | Skip (no issue) | Cannot determine value statically |

---

## 4. False Positive Considerations

### 4.1 String Literal Validation
**Only check string literals** to avoid false positives:
```java
// CHECK: String literal
ZoneId.of("InvalidZone");  // ✓ Should report

// SKIP: Variable
String zone = config.getZone();
ZoneId.of(zone);  // ✗ Should NOT report

// SKIP: Method call
ZoneId.of(getZoneFromConfig());  // ✗ Should NOT report

// CHECK: Compile-time constant
static final String ZONE = "InvalidZone";
ZoneId.of(ZONE);  // ✓ Should report (if asConstant() resolves it)
```

### 4.2 Case Sensitivity
- Zone IDs are **case-sensitive**
- "UTC" is valid, but "utc" is **invalid**
- Do not normalize case before validation
```java
// Correct behavior
isValidZoneId("UTC");     // true
isValidZoneId("utc");     // false (should report issue)
isValidZoneId("America/New_York");  // true
isValidZoneId("america/new_york");  // false (should report issue)
```

### 4.3 Validation Determinism
- Use `ZoneId.getAvailableZoneIds()` from the JDK running the analyzer
- This ensures consistency across analysis runs
- Document that the check validates against the analyzer's JDK version

### 4.4 Short Zone IDs
Three-letter abbreviations are **never valid** for `ZoneId.of()`:
```java
ZoneId.of("PST");  // Always invalid - report issue
ZoneId.of("EST");  // Always invalid - report issue
ZoneId.of("CST");  // Always invalid - report issue
```

---

## 5. Quick Fix Potential

### 5.1 Suggest Valid Alternatives
Use Levenshtein distance or similar algorithm to suggest corrections:

```java
private String suggestAlternative(String invalidZoneId) {
  // Common typos and their corrections
  Map<String, String> commonCorrections = Map.of(
    "PST", "America/Los_Angeles",
    "EST", "America/New_York",
    "CST", "America/Chicago",
    "MST", "America/Denver",
    "US/Pacific", "America/Los_Angeles",
    "US/Eastern", "America/New_York"
  );
  
  if (commonCorrections.containsKey(invalidZoneId)) {
    return commonCorrections.get(invalidZoneId);
  }
  
  // Find closest match by string distance
  return VALID_ZONE_IDS.stream()
    .min(Comparator.comparingInt(valid -> 
      levenshteinDistance(invalidZoneId, valid)))
    .filter(closest -> 
      levenshteinDistance(invalidZoneId, closest) <= 3)
    .orElse(null);
}
```

### 5.2 Issue Message with Suggestions
```java
private String createIssueMessage(String invalidZoneId) {
  String suggestion = suggestAlternative(invalidZoneId);
  
  if (suggestion != null) {
    return String.format(
      "\"%s\" is not a valid time zone identifier. Did you mean \"%s\"?",
      invalidZoneId, suggestion
    );
  }
  
  return String.format(
    "\"%s\" is not a valid time zone identifier.",
    invalidZoneId
  );
}
```

### 5.3 Quick Fix Implementation
While SonarJava doesn't support automatic quick fixes in the traditional IDE sense, provide helpful secondary locations:

```java
private void reportIssue(ExpressionTree argument, String message) {
  context().reportIssue(this, argument, message);
}
```

---

## 6. Implementation Pattern Examples

### 6.1 Reference Similar Checks

#### DateFormatWeekYearCheck
Similar pattern validation check that validates date format strings:
- Uses `AbstractMethodDetection`
- Extracts string literals with `asConstant()`
- Validates against known patterns
- Reports issues on invalid patterns

```java
// Pattern from DateFormatWeekYearCheck
@Override
protected void onMethodInvocationFound(MethodInvocationTree mit) {
  ExpressionTree firstArg = mit.arguments().get(0);
  Optional<Object> constant = firstArg.asConstant();
  if (constant.isPresent() && constant.get() instanceof String) {
    String pattern = (String) constant.get();
    if (containsWeekYear(pattern)) {
      reportIssue(firstArg, "Use 'y' for year instead of 'Y'.");
    }
  }
}
```

### 6.2 Complete Implementation Template

```java
package org.sonar.java.checks;

import java.time.ZoneId;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S6885")
public class InvalidZoneIdCheck extends AbstractMethodDetection {

  private static final Set<String> VALID_ZONE_IDS;
  private static final Set<String> SPECIAL_ZONE_IDS = 
    Set.of("UTC", "GMT", "Z", "UT");
  
  private static final Pattern FIXED_OFFSET_PATTERN = Pattern.compile(
    "^[+-]\\d{1,2}(:\\d{2}(:\\d{2})?)?$|^[+-]\\d{4}$"
  );
  
  private static final Pattern ZONE_OFFSET_PATTERN = Pattern.compile(
    "^(UTC|GMT)[+-]\\d{1,2}(:\\d{2})?$"
  );
  
  private static final Map<String, String> COMMON_CORRECTIONS = Map.of(
    "PST", "America/Los_Angeles",
    "PDT", "America/Los_Angeles",
    "EST", "America/New_York",
    "EDT", "America/New_York",
    "CST", "America/Chicago",
    "CDT", "America/Chicago",
    "MST", "America/Denver",
    "MDT", "America/Denver",
    "US/Pacific", "America/Los_Angeles",
    "US/Eastern", "America/New_York",
    "US/Central", "America/Chicago",
    "US/Mountain", "America/Denver"
  );

  static {
    VALID_ZONE_IDS = ZoneId.getAvailableZoneIds();
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofTypes("java.time.ZoneId")
      .names("of")
      .addParametersMatcher("java.lang.String")
      .build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    Arguments arguments = mit.arguments();
    
    if (arguments.size() != 1) {
      return;
    }
    
    ExpressionTree argument = arguments.get(0);
    Optional<Object> constant = argument.asConstant();
    
    if (constant.isEmpty() || !(constant.get() instanceof String)) {
      // Not a string literal, skip to avoid false positives
      return;
    }
    
    String zoneId = (String) constant.get();
    
    if (!isValidZoneId(zoneId)) {
      String message = createIssueMessage(zoneId);
      reportIssue(argument, message);
    }
  }

  private static boolean isValidZoneId(String zoneId) {
    return VALID_ZONE_IDS.contains(zoneId)
      || SPECIAL_ZONE_IDS.contains(zoneId)
      || FIXED_OFFSET_PATTERN.matcher(zoneId).matches()
      || ZONE_OFFSET_PATTERN.matcher(zoneId).matches();
  }

  private String createIssueMessage(String invalidZoneId) {
    String suggestion = suggestAlternative(invalidZoneId);
    
    if (suggestion != null) {
      return String.format(
        "\"%s\" is not a valid time zone identifier. Did you mean \"%s\"?",
        invalidZoneId, suggestion
      );
    }
    
    return String.format(
      "\"%s\" is not a valid time zone identifier.",
      invalidZoneId
    );
  }

  private String suggestAlternative(String invalidZoneId) {
    // Check common corrections first
    if (COMMON_CORRECTIONS.containsKey(invalidZoneId)) {
      return COMMON_CORRECTIONS.get(invalidZoneId);
    }
    
    // Find closest match by Levenshtein distance
    return VALID_ZONE_IDS.stream()
      .min(Comparator.comparingInt(valid -> 
        levenshteinDistance(invalidZoneId, valid)))
      .filter(closest -> 
        levenshteinDistance(invalidZoneId, closest) <= 3)
      .orElse(null);
  }

  private static int levenshteinDistance(String s1, String s2) {
    int[][] dp = new int[s1.length() + 1][s2.length() + 1];
    
    for (int i = 0; i <= s1.length(); i++) {
      dp[i][0] = i;
    }
    for (int j = 0; j <= s2.length(); j++) {
      dp[0][j] = j;
    }
    
    for (int i = 1; i <= s1.length(); i++) {
      for (int j = 1; j <= s2.length(); j++) {
        int cost = s1.charAt(i - 1) == s2