```java
import java.time.ZoneId;

public class ValidZoneIdCheckSample {

  // ============================================================================
  // NONCOMPLIANT CASES - These should raise issues
  // ============================================================================

  void invalidZoneIds() {
    // Invalid/random zone ID strings
    ZoneId zone1 = ZoneId.of("InvalidZone"); // Noncompliant
    ZoneId zone2 = ZoneId.of("NotATimeZone"); // Noncompliant
    ZoneId zone3 = ZoneId.of("Random/String"); // Noncompliant
    ZoneId zone4 = ZoneId.of("abc123"); // Noncompliant
    ZoneId zone5 = ZoneId.of(""); // Noncompliant
  }

  void threeLetterAbbreviations() {
    // Three-letter time zone abbreviations are NOT valid
    ZoneId zone1 = ZoneId.of("PST"); // Noncompliant
    ZoneId zone2 = ZoneId.of("EST"); // Noncompliant
    ZoneId zone3 = ZoneId.of("CST"); // Noncompliant
    ZoneId zone4 = ZoneId.of("MST"); // Noncompliant
    ZoneId zone5 = ZoneId.of("PDT"); // Noncompliant
    ZoneId zone6 = ZoneId.of("EDT"); // Noncompliant
    ZoneId zone7 = ZoneId.of("CDT"); // Noncompliant
    ZoneId zone8 = ZoneId.of("MDT"); // Noncompliant
    ZoneId zone9 = ZoneId.of("IST"); // Noncompliant
    ZoneId zone10 = ZoneId.of("JST"); // Noncompliant
    ZoneId zone11 = ZoneId.of("BST"); // Noncompliant
    ZoneId zone12 = ZoneId.of("CET"); // Noncompliant
  }

  void deprecatedZoneIds() {
    // Deprecated US/ zone IDs
    ZoneId zone1 = ZoneId.of("US/Pacific"); // Noncompliant
    ZoneId zone2 = ZoneId.of("US/Eastern"); // Noncompliant
    ZoneId zone3 = ZoneId.of("US/Central"); // Noncompliant
    ZoneId zone4 = ZoneId.of("US/Mountain"); // Noncompliant
    ZoneId zone5 = ZoneId.of("US/Alaska"); // Noncompliant
    ZoneId zone6 = ZoneId.of("US/Hawaii"); // Noncompliant
  }

  void typosInValidZoneIds() {
    // Typos in otherwise valid zone IDs
    ZoneId zone1 = ZoneId.of("Europe/Pari"); // Noncompliant (should be Paris)
    ZoneId zone2 = ZoneId.of("America/New_Yor"); // Noncompliant (should be New_York)
    ZoneId zone3 = ZoneId.of("Asia/Toky"); // Noncompliant (should be Tokyo)
    ZoneId zone4 = ZoneId.of("America/Los_Angles"); // Noncompliant (should be Los_Angeles)
    ZoneId zone5 = ZoneId.of("Europe/Londn"); // Noncompliant (should be London)
  }

  void caseSensitivityIssues() {
    // Zone IDs are case-sensitive
    ZoneId zone1 = ZoneId.of("utc"); // Noncompliant (should be UTC)
    ZoneId zone2 = ZoneId.of("gmt"); // Noncompliant (should be GMT)
    ZoneId zone3 = ZoneId.of("z"); // Noncompliant (should be Z)
    ZoneId zone4 = ZoneId.of("europe/paris"); // Noncompliant (should be Europe/Paris)
    ZoneId zone5 = ZoneId.of("EUROPE/PARIS"); // Noncompliant (should be Europe/Paris)
    ZoneId zone6 = ZoneId.of("America/new_york"); // Noncompliant (should be America/New_York)
  }

  void invalidOffsetFormats() {
    // Invalid offset formats
    ZoneId zone1 = ZoneId.of("+25:00"); // Noncompliant (hours > 24)
    ZoneId zone2 = ZoneId.of("+5:3"); // Noncompliant (invalid format)
    ZoneId zone3 = ZoneId.of("++05:30"); // Noncompliant (double plus)
    ZoneId zone4 = ZoneId.of("05:30"); // Noncompliant (missing sign)
    ZoneId zone5 = ZoneId.of("+5:"); // Noncompliant (incomplete)
    ZoneId zone6 = ZoneId.of("+-05:30"); // Noncompliant (both signs)
  }

  void invalidZoneOffsetIds() {
    // Invalid UTC/GMT offset formats
    ZoneId zone1 = ZoneId.of("UTC+25"); // Noncompliant (hours > 24)
    ZoneId zone2 = ZoneId.of("GMT+"); // Noncompliant (incomplete)
    ZoneId zone3 = ZoneId.of("UT+5"); // Noncompliant (UT doesn't support offsets)
    ZoneId zone4 = ZoneId.of("UTC +5"); // Noncompliant (space not allowed)
    ZoneId zone5 = ZoneId.of("GMT-100"); // Noncompliant (invalid offset)
  }

  void multipleInvalidCallsInOneMethod() {
    // Multiple invalid calls in the same method
    ZoneId zone1 = ZoneId.of("PST"); // Noncompliant
    ZoneId zone2 = ZoneId.of("InvalidZone"); // Noncompliant
    ZoneId zone3 = ZoneId.of("US/Pacific"); // Noncompliant
    
    if (zone1 != null) {
      ZoneId zone4 = ZoneId.of("EST"); // Noncompliant
    }
  }

  // ============================================================================
  // COMPLIANT CASES - These should NOT raise issues
  // ============================================================================

  void validIANAZoneIds() {
    // Valid IANA Time Zone Database identifiers
    ZoneId zone1 = ZoneId.of("Europe/Paris");
    ZoneId zone2 = ZoneId.of("America/Los_Angeles");
    ZoneId zone3 = ZoneId.of("America/New_York");
    ZoneId zone4 = ZoneId.of("Asia/Tokyo");
    ZoneId zone5 = ZoneId.of("America/Chicago");
    ZoneId zone6 = ZoneId.of("America/Denver");
    ZoneId zone7 = ZoneId.of("Europe/London");
    ZoneId zone8 = ZoneId.of("Asia/Shanghai");
    ZoneId zone9 = ZoneId.of("Australia/Sydney");
    ZoneId zone10 = ZoneId.of("Africa/Cairo");
    ZoneId zone11 = ZoneId.of("Pacific/Auckland");
    ZoneId zone12 = ZoneId.of("America/Sao_Paulo");
    ZoneId zone13 = ZoneId.of("Europe/Berlin");
    ZoneId zone14 = ZoneId.of("Asia/Kolkata");
    ZoneId zone15 = ZoneId.of("America/Mexico_City");
  }

  void validFixedOffsets() {
    // Valid fixed offset formats
    ZoneId zone1 = ZoneId.of("+05:30");
    ZoneId zone2 = ZoneId.of("-08:00");
    ZoneId zone3 = ZoneId.of("+00:00");
    ZoneId zone4 = ZoneId.of("+01:00");
    ZoneId zone5 = ZoneId.of("-05:00");
    ZoneId zone6 = ZoneId.of("+09:00");
    ZoneId zone7 = ZoneId.of("-07:00");
    ZoneId zone8 = ZoneId.of("+0530"); // Compact format
    ZoneId zone9 = ZoneId.of("-0800"); // Compact format
    ZoneId zone10 = ZoneId.of("+5"); // Single digit
    ZoneId zone11 = ZoneId.of("-8"); // Single digit
    ZoneId zone12 = ZoneId.of("+10:30");
    ZoneId zone13 = ZoneId.of("-03:30");
    ZoneId zone14 = ZoneId.of("+01:00:00"); // With seconds
    ZoneId zone15 = ZoneId.of("-05:30:00"); // With seconds
  }

  void validSpecialIdentifiers() {
    // Valid special zone identifiers
    ZoneId zone1 = ZoneId.of("UTC");
    ZoneId zone2 = ZoneId.of("GMT");
    ZoneId zone3 = ZoneId.of("Z");
    ZoneId zone4 = ZoneId.of("UT");
  }

  void validZoneOffsetIds() {
    // Valid UTC/GMT offset IDs
    ZoneId zone1 = ZoneId.of("UTC+1");
    ZoneId zone2 = ZoneId.of("UTC-5");
    ZoneId zone3 = ZoneId.of("GMT+5");
    ZoneId zone4 = ZoneId.of("GMT-8");
    ZoneId zone5 = ZoneId.of("UTC+01:00");
    ZoneId zone6 = ZoneId.of("GMT-05:30");
    ZoneId zone7 = ZoneId.of("UTC+9");
    ZoneId zone8 = ZoneId.of("GMT+0");
    ZoneId zone9 = ZoneId.of("UTC-12");
    ZoneId zone10 = ZoneId.of("GMT+14");
  }

  void dynamicStrings(String zoneIdParam) {
    // Dynamic strings - should NOT be checked to avoid false positives
    String zoneIdVariable = "SomeZoneId";
    ZoneId zone1 = ZoneId.of(zoneIdVariable); // Compliant - variable
    ZoneId zone2 = ZoneId.of(zoneIdParam); // Compliant - parameter
    ZoneId zone3 = ZoneId.of(getZoneIdFromConfig()); // Compliant - method call
    ZoneId zone4 = ZoneId.of(System.getProperty("timezone")); // Compliant - method call
  }

  void concatenatedStrings() {
    // Concatenated strings that are NOT compile-time constants
    String prefix = "America/";
    ZoneId zone1 = ZoneId.of(prefix + "New_York"); // Compliant - runtime concatenation
  }

  void conditionalZoneIds(boolean useUtc) {
    // Ternary with valid zone IDs
    ZoneId zone1 = ZoneId.of(useUtc ? "UTC" : "Europe/Paris"); // Compliant - both valid
  }

  void nullArguments() {
    // Null argument - different rule's responsibility (will cause NPE)
    String nullZone = null;
    ZoneId zone1 = ZoneId.of(nullZone); // Compliant - not checking null
  }

  // ============================================================================
  // EDGE CASES
  // ============================================================================

  void compileTimeConstants() {
    // Compile-time constants - may or may not be detected depending on implementation
    final String VALID_ZONE = "Europe/Paris";
    final String INVALID_ZONE = "InvalidZone";
    
    ZoneId zone1 = ZoneId.of(VALID_ZONE); // Compliant if constant is resolved
    ZoneId zone2 = ZoneId.of(INVALID_ZONE); // Noncompliant if constant is resolved
  }

  static final String STATIC_VALID_ZONE = "America/New_York";
  static final String STATIC_INVALID_ZONE = "PST";

  void staticConstants() {
    ZoneId zone1 = ZoneId.of(STATIC_VALID_ZONE); // Compliant if constant is resolved
    ZoneId zone2 = ZoneId.of(STATIC_INVALID_ZONE); // Noncompliant if constant is resolved
  }

  void mixedValidAndInvalid() {
    // Mix of valid and invalid in the same method
    ZoneId valid1 = ZoneId.of("UTC");
    ZoneId invalid1 = ZoneId.of("PST"); // Noncompliant
    ZoneId valid2 = ZoneId.of("Europe/Paris");
    ZoneId invalid2 = ZoneId.of("InvalidZone"); // Noncompliant
    ZoneId valid3 = ZoneId.of("+05:30");
  }

  void nestedCalls() {
    // ZoneId.of() called within other method calls
    String formatted1 = String.format("Zone: %s", ZoneId.of("Europe/Paris"));
    String formatted2 = String.format("Zone: %s", ZoneId.of("PST")); // Noncompliant
  }

  void inTryCatch() {
    // ZoneId.of() in try-catch blocks
    try {
      ZoneId zone1 = ZoneId.of("InvalidZone"); // Noncompliant
    } catch (Exception e) {
      // Handle exception
    }

    try {
      ZoneId zone2 = ZoneId.of("Europe/Paris"); // Compliant
    } catch (Exception e) {
      // Handle exception
    }
  }

  void inLambdas() {
    // ZoneId.of() in lambda expressions
    Runnable r1 = () -> {
      ZoneId zone = ZoneId.of("PST"); // Noncompliant
    };

    Runnable r2 = () -> {
      ZoneId zone = ZoneId.of("UTC"); // Compliant
    };
  }

  // Helper method for testing
  private String getZoneIdFromConfig() {
    return "Europe/Paris";
  }

  // ============================================================================
  // ADDITIONAL VALID ZONE IDS (for comprehensive coverage)
  // ============================================================================

  void moreValidIANAZoneIds() {
    // More valid IANA zone IDs
    ZoneId zone1 = ZoneId.of("America/Anchorage");
    ZoneId zone2 = ZoneId.of("America/Toronto");
    ZoneId zone3 = ZoneId.of("Europe/Amsterdam");
    ZoneId zone4 = ZoneId.of("Europe/Rome");
    ZoneId zone5 = ZoneId.of("Europe/Madrid");
    ZoneId zone6 = ZoneId.of("Europe/Vienna");
    ZoneId zone7 = ZoneId.of("Asia/Hong_Kong");
    ZoneId zone8 = ZoneId.of("Asia/Singapore");
    ZoneId zone9 = ZoneId.of("Asia/Dubai");
    ZoneId zone10 = ZoneId.of("Asia/Seoul");
    ZoneId zone11 = ZoneId.of("Pacific/Honolulu");
    ZoneId zone12 = ZoneId.of("Pacific/Fiji");
    ZoneId zone13 = ZoneId.of("Africa/Johannesburg");
    ZoneId zone14 = ZoneId.of("America/Argentina/Buenos_Aires");
    Z