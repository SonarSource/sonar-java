# Technical Implementation Specification: Zone ID Validation Check

## 1. Implementation Approach

### Base Class Selection
Use **`AbstractMethodDetection`** pattern as this check focuses on validating arguments to a specific method (`ZoneId.of()`).

### AST Node Types
- **Primary**: `MethodInvocationTree` - to detect calls to `ZoneId.of()`
- **Secondary**: `ExpressionTree` - to analyze method arguments

### MethodMatchers Configuration
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
    // Implementation
  }
}
```

## 2. Valid Zone IDs

### Validation Approach
Use a **static set** of valid zone IDs initialized at class loading time using Java's own `ZoneId` API.

### Valid Zone ID Categories

#### A. IANA Time Zone Database Names
```java
private static final Set<String> VALID_ZONE_IDS = ZoneId.getAvailableZoneIds();
```

Examples:
- `"America/New_York"`
- `"Europe/Paris"`
- `"Asia/Tokyo"`
- `"Africa/Cairo"`

#### B. Fixed Offset Patterns
Regular expressions to validate:
- **Format**: `[+/-]HH:MM` or `[+/-]HH:MM:SS` or `[+/-]HH`
- **Examples**: `"+05:30"`, `"-08:00"`, `"+01:00:00"`

```java
private static final Pattern FIXED_OFFSET_PATTERN = 
  Pattern.compile("^[+-]\\d{2}(?::?\\d{2}(?::?\\d{2})?)?$");
```

#### C. Special Zone Identifiers
Additional valid IDs not always in `getAvailableZoneIds()`:
- `"Z"` (UTC)
- `"UTC"` (if not in set)
- `"GMT"` (if not in set)
- `"UT"` (if not in set)

#### D. Zone Offset Prefix Patterns
Pattern for zone offsets with prefix:
- **Format**: `UTC[+/-]offset` or `GMT[+/-]offset`
- **Examples**: `"UTC+1"`, `"GMT-5"`, `"UTC+05:30"`

```java
private static final Pattern ZONE_OFFSET_PREFIX_PATTERN = 
  Pattern.compile("^(?:UTC|GMT|UT)([+-]\\d{1,2}(?::?\\d{2}(?::?\\d{2})?)?)?$");
```

### Complete Validation Method
```java
private static boolean isValidZoneId(String zoneId) {
  if (zoneId == null || zoneId.isEmpty()) {
    return false;
  }
  
  // Check IANA zone IDs
  if (VALID_ZONE_IDS.contains(zoneId)) {
    return true;
  }
  
  // Check special IDs
  if ("Z".equals(zoneId)) {
    return true;
  }
  
  // Check fixed offset pattern (+HH:MM, -HH:MM, etc.)
  if (FIXED_OFFSET_PATTERN.matcher(zoneId).matches()) {
    return true;
  }
  
  // Check zone offset with prefix (UTC+1, GMT-5, etc.)
  if (ZONE_OFFSET_PREFIX_PATTERN.matcher(zoneId).matches()) {
    return true;
  }
  
  return false;
}
```

## 3. Detection Logic

### Primary Detection Flow

```java
@Override
protected void onMethodInvocationFound(MethodInvocationTree mit) {
  Arguments arguments = mit.arguments();
  
  // Ensure we have exactly one argument
  if (arguments.size() != 1) {
    return;
  }
  
  ExpressionTree argument = arguments.get(0);
  
  // Extract string literal value
  Optional<String> constantValue = extractStringLiteral(argument);
  
  // Only check string literals (skip dynamic values)
  if (!constantValue.isPresent()) {
    return;
  }
  
  String zoneId = constantValue.get();
  
  // Skip null values (handled by other checks)
  if (zoneId == null) {
    return;
  }
  
  // Validate zone ID
  if (!isValidZoneId(zoneId)) {
    reportIssue(argument, buildIssueMessage(zoneId));
  }
}

private Optional<String> extractStringLiteral(ExpressionTree expression) {
  ExpressionTree expr = ExpressionUtils.skipParentheses(expression);
  
  // Use constant resolution
  ConstantUtils.Constant constant = expr.asConstant();
  if (constant != null && constant.value() instanceof String) {
    return Optional.of((String) constant.value());
  }
  
  return Optional.empty();
}
```

### Edge Cases Handling

#### Non-Literal Arguments
```java
// Skip - cannot validate at compile time
String userInput = getUserInput();
ZoneId.of(userInput);

String config = System.getProperty("timezone");
ZoneId.of(config);
```

#### Concatenated Strings
```java
// Skip if not resolvable to constant
String prefix = "America/";
ZoneId.of(prefix + "New_York"); // Skip if not constant-folded
```

#### Null Arguments
```java
// Skip - NullPointerException will be caught by other rules
ZoneId.of(null);
```

#### Method References and Variables
```java
// Skip - not string literals
String zone = "InvalidZone";
ZoneId.of(zone);
```

## 4. False Positive Considerations

### Only Check String Literals
- **Rationale**: Dynamic strings cannot be validated at compile time
- **Implementation**: Use `asConstant()` method to ensure compile-time constant
- **Benefit**: Eliminates false positives from runtime-determined values

### Case Sensitivity
Zone IDs are **case-sensitive**. The check must preserve exact case:
```java
// These are different
ZoneId.of("UTC");        // Valid
ZoneId.of("utc");        // Invalid
ZoneId.of("europe/paris"); // Invalid
ZoneId.of("Europe/Paris"); // Valid
```

### Validation Consistency
Ensure validation logic matches `ZoneId.of()` behavior exactly:
```java
private static final Set<String> VALID_ZONE_IDS;

static {
  // Initialize once at class loading
  VALID_ZONE_IDS = ZoneId.getAvailableZoneIds();
}
```

### Known Valid But Uncommon IDs
Handle special cases that `ZoneId.of()` accepts:
- Offset-based IDs: `"UTC+0"`, `"GMT+0"`
- Variations: `"+00:00"`, `"+0000"`, `"+00"`
- All must be validated as correct

## 5. Issue Reporting

### Issue Message
```java
private String buildIssueMessage(String invalidZoneId) {
  return String.format("\"%s\" is not a valid time zone identifier.", invalidZoneId);
}
```

### Detailed Message with Context
For better developer experience, detect common mistakes:

```java
private String buildIssueMessage(String invalidZoneId) {
  String baseMessage = String.format("\"%s\" is not a valid time zone identifier.", invalidZoneId);
  
  // Check if it's a common three-letter abbreviation
  if (isCommonAbbreviation(invalidZoneId)) {
    return baseMessage + " Use full IANA time zone names instead of abbreviations.";
  }
  
  // Check if it's a deprecated zone ID
  if (isDeprecatedZoneId(invalidZoneId)) {
    return baseMessage + " This is a deprecated time zone identifier.";
  }
  
  return baseMessage;
}

private static final Set<String> COMMON_ABBREVIATIONS = Set.of(
  "PST", "PDT", "MST", "MDT", "CST", "CDT", "EST", "EDT",
  "BST", "CET", "IST", "JST", "AEST", "AEDT"
);

private static final Set<String> DEPRECATED_ZONE_IDS = Set.of(
  "US/Pacific", "US/Mountain", "US/Central", "US/Eastern",
  "US/Alaska", "US/Hawaii"
);

private boolean isCommonAbbreviation(String zoneId) {
  return COMMON_ABBREVIATIONS.contains(zoneId);
}

private boolean isDeprecatedZoneId(String zoneId) {
  return DEPRECATED_ZONE_IDS.contains(zoneId);
}
```

## 6. Quick Fix Potential

While quick fixes are implemented separately, design the check to support them:

### Suggestion Strategies

#### A. String Similarity for Typos
Use Levenshtein distance to suggest similar valid zone IDs:

```java
private List<String> findSimilarZoneIds(String invalidZoneId, int maxSuggestions) {
  return VALID_ZONE_IDS.stream()
    .filter(validId -> levenshteinDistance(invalidZoneId, validId) <= 2)
    .sorted(Comparator.comparingInt(validId -> 
      levenshteinDistance(invalidZoneId, validId)))
    .limit(maxSuggestions)
    .collect(Collectors.toList());
}
```

#### B. Mapping Common Abbreviations
```java
private static final Map<String, String> ABBREVIATION_TO_ZONE = Map.ofEntries(
  Map.entry("PST", "America/Los_Angeles"),
  Map.entry("PDT", "America/Los_Angeles"),
  Map.entry("MST", "America/Denver"),
  Map.entry("MDT", "America/Denver"),
  Map.entry("CST", "America/Chicago"),
  Map.entry("CDT", "America/Chicago"),
  Map.entry("EST", "America/New_York"),
  Map.entry("EDT", "America/New_York"),
  Map.entry("BST", "Europe/London"),
  Map.entry("CET", "Europe/Paris"),
  Map.entry("JST", "Asia/Tokyo")
);
```

#### C. Mapping Deprecated IDs
```java
private static final Map<String, String> DEPRECATED_TO_CURRENT = Map.of(
  "US/Pacific", "America/Los_Angeles",
  "US/Mountain", "America/Denver",
  "US/Central", "America/Chicago",
  "US/Eastern", "America/New_York",
  "US/Alaska", "America/Anchorage",
  "US/Hawaii", "Pacific/Honolulu"
);
```

#### D. System Default Suggestion
For completely invalid zones, suggest using system default:
```java
// When no good alternative exists
// Quick fix: ZoneId.systemDefault()
```

## 7. Implementation Pattern Examples

### Reference Similar Checks

#### DateFormatWeekYearCheck
Similar pattern validation check:
```java
// Reference implementation pattern
public class DateFormatWeekYearCheck extends AbstractMethodDetection {
  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree firstArgument = mit.arguments().get(0);
    Optional<String> stringConstant = firstArgument.asConstant(String.class);
    if (stringConstant.isPresent()) {
      String pattern = stringConstant.get();
      // Validate pattern
    }
  }
}
```

### Complete Check Implementation

```java
package org.sonar.java.checks;

import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;

@Rule(key = "S6885")
public class InvalidZoneIdCheck extends AbstractMethodDetection {

  private static final MethodMatchers ZONE_ID_OF = MethodMatchers.create()
    .ofTypes("java.time.ZoneId")
    .names("of")
    .addParametersMatcher("java.lang.String")
    .build();

  private static final Set<String> VALID_ZONE_IDS = ZoneId.getAvailableZoneIds();
  
  private static final Pattern FIXED_OFFSET_PATTERN = 
    Pattern.compile("^[+-]\\d{2}(?::?\\d{2}(?::?\\d{2})?)?$");
  
  private static final Pattern ZONE_OFFSET_PREFIX_PATTERN = 
    Pattern.compile("^(?:UTC|GMT|UT)([+-]\\d{1,2}(?::?\\d{2}(?::?\\d{2})?)?)?$");

  private static final Set<String> COMMON_ABBREVIATIONS = Set.of(
    "PST", "PDT", "MST", "MDT", "CST", "CDT", "EST", "EDT",
    "BST", "CET", "IST", "JST", "AEST", "AEDT"
  );

  private static final Set<String> DEPRECATED_ZONE_IDS = Set.of(
    "US/Pacific", "US/Mountain", "US/Central", "US/Eastern",
    "US/Alaska", "US/Hawaii"
  );

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return ZONE_ID_OF;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (mit.arguments().size() != 1) {
      return;
    }

    ExpressionTree argument = mit.arguments().get(0);
    ExpressionTree expr = ExpressionUtils.skipParentheses(argument);
    
    // Extract string literal using constant resolution
    Optional<String> constantValue = expr.asConstant(String.class);
    
    if (!constantValue.isPresent()) {
      // Not a string literal, skip
      return;
    }

    String zoneId = constantValue.get();
    
    if (zoneId == null) {
      // Null value, skip (handled by other rules)
      return;
    }

    if (!isValidZoneId(zoneId)) {
      reportIssue(argument, buildIssueMessage(zoneId));
    }
  }

  private static boolean isValidZoneId(String zoneId) {
    if (zoneId.isEmpty()) {
      return false;
    }

    // Check IANA zone IDs from ZoneId.getAvailableZoneIds()
    if (VALID_ZONE_IDS.contains(zoneId)) {
      return true;
    }

    // Check special ID: "Z" (UTC)
    if ("Z".equals(zoneId)) {
      return true;
    }

    // Check fixed offset pattern: +HH:MM, -HH:MM