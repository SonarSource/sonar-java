package checks;

import java.util.TimeZone;

class TimeZoneIdCheckSample {

  private static final String VALID_CONST = "America/Los_Angeles";
  private static final String INVALID_CONST = "America/Los_Angele";
  private static final String SPACED_CONST = "America/Los Angeles";

  void namedIds() {
    TimeZone.getTimeZone("America/Los_Angele"); // Noncompliant {{Change this invalid time zone ID; an unrecognized identifier makes TimeZone.getTimeZone(String) return GMT with no error.}} [[quickfixes=!]]
//                       ^^^^^^^^^^^^^^^^^^^^
    TimeZone.getTimeZone("America/New_Yorkk"); // Noncompliant [[quickfixes=!]]
//                       ^^^^^^^^^^^^^^^^^^^
    TimeZone.getTimeZone("Europe/Pariss"); // Noncompliant [[quickfixes=!]]
//                       ^^^^^^^^^^^^^^^
    TimeZone.getTimeZone("Unknown/Invalid_Zone"); // Noncompliant [[quickfixes=!]]
//                       ^^^^^^^^^^^^^^^^^^^^^^
  }

  void quickFixes() {
    TimeZone.getTimeZone("America/Los Angeles"); // Noncompliant [[quickfixes=qf1]]
//                       ^^^^^^^^^^^^^^^^^^^^^
    // fix@qf1 {{Replace spaces with underscores}}
    // edit@qf1 [[sc=26;ec=47]] {{"America/Los_Angeles"}}

    TimeZone.getTimeZone("America/New York"); // Noncompliant [[quickfixes=qf2]]
//                       ^^^^^^^^^^^^^^^^^^
    // fix@qf2 {{Replace spaces with underscores}}
    // edit@qf2 [[sc=26;ec=44]] {{"America/New_York"}}

    TimeZone.getTimeZone("America/Port of Spain"); // Noncompliant [[quickfixes=qf3]]
//                       ^^^^^^^^^^^^^^^^^^^^^^^
    // fix@qf3 {{Replace spaces with underscores}}
    // edit@qf3 [[sc=26;ec=49]] {{"America/Port_of_Spain"}}

    TimeZone.getTimeZone("Some Unknown City"); // Noncompliant [[quickfixes=!]]
//                       ^^^^^^^^^^^^^^^^^^^
  }

  void customOffsets() {
    TimeZone.getTimeZone("GMT+24"); // Noncompliant [[quickfixes=!]]
//                       ^^^^^^^^
    TimeZone.getTimeZone("GMT-25"); // Noncompliant [[quickfixes=!]]
//                       ^^^^^^^^
    TimeZone.getTimeZone("GMT+01:60"); // Noncompliant [[quickfixes=!]]
//                       ^^^^^^^^^^^
    TimeZone.getTimeZone("GMT-8:99"); // Noncompliant [[quickfixes=!]]
//                       ^^^^^^^^^^
    TimeZone.getTimeZone("GMT+1260"); // Noncompliant [[quickfixes=!]]
//                       ^^^^^^^^^^
    TimeZone.getTimeZone("GMT+01:00:60"); // Noncompliant [[quickfixes=!]]
//                       ^^^^^^^^^^^^^^
    TimeZone.getTimeZone("GMT+010030"); // Noncompliant [[quickfixes=!]]
//                       ^^^^^^^^^^^^
    TimeZone.getTimeZone("GMT-080030"); // Noncompliant [[quickfixes=!]]
//                       ^^^^^^^^^^^^
    TimeZone.getTimeZone("GMT+0100:30"); // Noncompliant [[quickfixes=!]]
//                       ^^^^^^^^^^^^^
    TimeZone.getTimeZone("GMT+01:0030"); // Noncompliant [[quickfixes=!]]
//                       ^^^^^^^^^^^^^
    TimeZone.getTimeZone("GMT1"); // Noncompliant [[quickfixes=!]]
//                       ^^^^^^
    TimeZone.getTimeZone("GMT12"); // Noncompliant [[quickfixes=!]]
//                       ^^^^^^^
    TimeZone.getTimeZone("GMT+"); // Noncompliant [[quickfixes=!]]
//                       ^^^^^^
    TimeZone.getTimeZone("GMT+ "); // Noncompliant [[quickfixes=!]]
//                       ^^^^^^^
    TimeZone.getTimeZone("GMT- "); // Noncompliant [[quickfixes=!]]
//                       ^^^^^^^
    TimeZone.getTimeZone("UTC+0"); // Noncompliant [[quickfixes=!]]
//                       ^^^^^^^
    TimeZone.getTimeZone("UTC+1"); // Noncompliant [[quickfixes=!]]
//                       ^^^^^^^
    TimeZone.getTimeZone("UT+1"); // Noncompliant [[quickfixes=!]]
//                       ^^^^^^
  }

  void constantsAndConcatenation() {
    TimeZone.getTimeZone(VALID_CONST); // Compliant
    TimeZone.getTimeZone(INVALID_CONST); // Noncompliant [[quickfixes=!]]
//                       ^^^^^^^^^^^^^
    TimeZone.getTimeZone(SPACED_CONST); // Noncompliant [[quickfixes=!]]
//                       ^^^^^^^^^^^^
    TimeZone.getTimeZone("America/" + "Los_Angeles"); // Compliant
    TimeZone.getTimeZone("America/" + "Los_Angele"); // Noncompliant [[quickfixes=!]]
//                       ^^^^^^^^^^^^^^^^^^^^^^^^^
    TimeZone.getTimeZone("America/" + "Los Angeles"); // Noncompliant [[quickfixes=!]]
//                       ^^^^^^^^^^^^^^^^^^^^^^^^^^
  }

  void dynamicAndEdgeCases(String dynamicZone) {
    TimeZone.getTimeZone(dynamicZone); // Compliant
    TimeZone.getTimeZone(getDynamicZone()); // Compliant
    TimeZone.getTimeZone((String) null); // Compliant
    TimeZone.getTimeZone(java.time.ZoneId.of("UTC")); // Compliant
    java.time.ZoneId.of("America/Los_Angeles"); // Compliant
    new OtherClass().getTimeZone("America/Los_Angele"); // Compliant
  }

  void compliantCases() {
    TimeZone.getTimeZone("America/Los_Angeles");
    TimeZone.getTimeZone("America/New_York");
    TimeZone.getTimeZone("Europe/Paris");
    TimeZone.getTimeZone("UTC");
    TimeZone.getTimeZone("GMT");
    TimeZone.getTimeZone("PST");
    TimeZone.getTimeZone("EST");
    TimeZone.getTimeZone("HST");
    TimeZone.getTimeZone("CST");
    TimeZone.getTimeZone("JST");
    TimeZone.getTimeZone("US/Pacific");
    TimeZone.getTimeZone("Etc/GMT+8");
    TimeZone.getTimeZone("SystemV/PST8");

    TimeZone.getTimeZone("GMT+10");
    TimeZone.getTimeZone("GMT-8");
    TimeZone.getTimeZone("GMT+0");
    TimeZone.getTimeZone("GMT-0");
    TimeZone.getTimeZone("GMT+00:00");
    TimeZone.getTimeZone("GMT-8:00");
    TimeZone.getTimeZone("GMT+12:30");
    TimeZone.getTimeZone("GMT+0800");
    TimeZone.getTimeZone("GMT-0800");
    TimeZone.getTimeZone("GMT+123");
    TimeZone.getTimeZone("GMT-800");
    TimeZone.getTimeZone("GMT+23");
    TimeZone.getTimeZone("GMT-23");
    TimeZone.getTimeZone("GMT+23:59");
    TimeZone.getTimeZone("GMT-2359");

    TimeZone.getTimeZone("GMT+01:00:30");
    TimeZone.getTimeZone("GMT-8:00:00");
    TimeZone.getTimeZone("GMT+00:00:00");
    TimeZone.getTimeZone("GMT+23:59:59");
    TimeZone.getTimeZone("GMT-23:59:59");
  }

  private String getDynamicZone() {
    return "America/New_York";
  }

  private static class OtherClass {
    public TimeZone getTimeZone(String id) {
      return null;
    }
  }
}
