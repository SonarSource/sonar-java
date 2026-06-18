import java.time.ZoneId;

public class ValidZoneIdCheckSample {

  void invalidZoneIds() {
    ZoneId zone1 = ZoneId.of("InvalidZone");  // Noncompliant {{"InvalidZone" is not a valid time zone identifier.}}
    ZoneId zone2 = ZoneId.of("NotATimeZone"); // Noncompliant {{"NotATimeZone" is not a valid time zone identifier.}}
    ZoneId zone3 = ZoneId.of(""); // Noncompliant {{"" is not a valid time zone identifier. Did you mean "GB"?}}
  }

  void threeLetterAbbreviations() {
    ZoneId zone1 = ZoneId.of("PST"); // Noncompliant {{"PST" is not a valid time zone identifier. Did you mean "America/Los_Angeles"?}}
    ZoneId zone2 = ZoneId.of("EST"); // Noncompliant {{"EST" is not a valid time zone identifier. Did you mean "America/New_York"?}}
    ZoneId zone3 = ZoneId.of("CST"); // Noncompliant {{"CST" is not a valid time zone identifier. Did you mean "America/Chicago"?}}
    ZoneId zone4 = ZoneId.of("MST"); // Noncompliant {{"MST" is not a valid time zone identifier. Did you mean "America/Denver"?}}
  }

  void deprecatedZoneIds() {
    // Note: US/* zone IDs are actually valid in IANA database, but deprecated
    // They are included for backwards compatibility
    ZoneId zone1 = ZoneId.of("US/Pacific");  // Compliant - valid but deprecated
    ZoneId zone2 = ZoneId.of("US/Eastern");  // Compliant - valid but deprecated
  }

  void typosInValidZoneIds() {
    ZoneId zone1 = ZoneId.of("Europe/Pari"); // Noncompliant {{"Europe/Pari" is not a valid time zone identifier. Did you mean "Europe/Paris"?}}
    ZoneId zone2 = ZoneId.of("Asia/Toky");   // Noncompliant {{"Asia/Toky" is not a valid time zone identifier. Did you mean "Asia/Tokyo"?}}
  }

  void caseSensitivityIssues() {
    ZoneId zone1 = ZoneId.of("utc"); // Noncompliant {{"utc" is not a valid time zone identifier. Did you mean "GMT"?}}
    ZoneId zone2 = ZoneId.of("gmt"); // Noncompliant {{"gmt" is not a valid time zone identifier. Did you mean "GMT"?}}
    ZoneId zone3 = ZoneId.of("europe/paris"); // Noncompliant {{"europe/paris" is not a valid time zone identifier. Did you mean "Europe/Paris"?}}
  }

  void invalidOffsetFormats() {
    // Note: +25:00 is actually accepted by ZoneId.of() (wraps around)
    ZoneId zone2 = ZoneId.of("05:30");  // Noncompliant {{"05:30" is not a valid time zone identifier.}}
  }

  void validIANAZoneIds() {
    ZoneId zone1 = ZoneId.of("Europe/Paris");
    ZoneId zone2 = ZoneId.of("America/Los_Angeles");
    ZoneId zone3 = ZoneId.of("America/New_York");
    ZoneId zone4 = ZoneId.of("Asia/Tokyo");
    ZoneId zone5 = ZoneId.of("America/Chicago");
    ZoneId zone6 = ZoneId.of("America/Denver");
  }

  void validFixedOffsets() {
    ZoneId zone1 = ZoneId.of("+05:30");
    ZoneId zone2 = ZoneId.of("-08:00");
    ZoneId zone3 = ZoneId.of("+00:00");
    ZoneId zone4 = ZoneId.of("+0530");
    ZoneId zone5 = ZoneId.of("+5");
    ZoneId zone6 = ZoneId.of("-8");
  }

  void validSpecialIdentifiers() {
    ZoneId zone1 = ZoneId.of("UTC");
    ZoneId zone2 = ZoneId.of("GMT");
    ZoneId zone3 = ZoneId.of("Z");
    ZoneId zone4 = ZoneId.of("UT");
  }

  void validZoneOffsetIds() {
    ZoneId zone1 = ZoneId.of("UTC+1");
    ZoneId zone2 = ZoneId.of("UTC-5");
    ZoneId zone3 = ZoneId.of("GMT+5");
    ZoneId zone4 = ZoneId.of("GMT-8");
    ZoneId zone5 = ZoneId.of("UTC+01:00");
    ZoneId zone6 = ZoneId.of("GMT-05:30");
  }

  void dynamicStrings(String zoneIdParam) {
    String zoneIdVariable = "SomeZoneId";
    ZoneId zone1 = ZoneId.of(zoneIdVariable); // Compliant - variable
    ZoneId zone2 = ZoneId.of(zoneIdParam); // Compliant - parameter
    ZoneId zone3 = ZoneId.of(getZoneId()); // Compliant - method call
  }

  void compileTimeConstants() {
    final String VALID_ZONE = "Europe/Paris";
    final String INVALID_ZONE = "InvalidZone";

    // Note: asConstant() doesn't resolve local final variables
    ZoneId zone1 = ZoneId.of(VALID_ZONE); // Compliant
    ZoneId zone2 = ZoneId.of(INVALID_ZONE); // Compliant - not detected as constant
  }

  private String getZoneId() {
    return "Europe/Paris";
  }
}
