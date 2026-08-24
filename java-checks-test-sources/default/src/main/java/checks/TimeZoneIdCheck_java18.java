package checks;

import java.util.TimeZone;

class TimeZoneIdCheck_java18 {

  void customOffsetsWithSeconds() {
    TimeZone.getTimeZone("GMT+01:00:30"); // Noncompliant {{Change this invalid time zone ID; an unrecognized identifier makes TimeZone.getTimeZone(String) return GMT with no error.}} [[quickfixes=!]]
//                       ^^^^^^^^^^^^^^
    TimeZone.getTimeZone("GMT-8:00:00"); // Noncompliant [[quickfixes=!]]
//                       ^^^^^^^^^^^^^
    TimeZone.getTimeZone("GMT+00:00:00"); // Noncompliant [[quickfixes=!]]
//                       ^^^^^^^^^^^^^^
    TimeZone.getTimeZone("GMT+23:59:59"); // Noncompliant [[quickfixes=!]]
//                       ^^^^^^^^^^^^^^
  }

  void compliantCases() {
    TimeZone.getTimeZone("America/Los_Angeles");
    TimeZone.getTimeZone("PST");
    TimeZone.getTimeZone("UTC");
    TimeZone.getTimeZone("GMT+10");
    TimeZone.getTimeZone("GMT-8:00");
    TimeZone.getTimeZone("GMT+00:00");
    TimeZone.getTimeZone("GMT-0800");
  }
}
