package checks;

import java.util.Locale;

class StringFormatCheckSample {

  String simple(Object value) {
    return String.format("%s", value); // Noncompliant {{Use String.valueOf() or string concatenation instead of String.format().}}
  }

  String path(String directory, String filename) {
    return String.format("%s/%s", directory, filename); // Noncompliant {{Use String.valueOf() or string concatenation instead of String.format().}}
  }

  String label(String key, Object value) {
    return String.format("%s: %s", key, value); // Noncompliant {{Use String.valueOf() or string concatenation instead of String.format().}}
  }

  String wrapped(String value) {
    return String.format("prefix-%s-suffix", value); // Noncompliant {{Use String.valueOf() or string concatenation instead of String.format().}}
  }

  String localized(Object value) {
    return String.format(Locale.ROOT, "%s", value); // Noncompliant {{Use String.valueOf() or string concatenation instead of String.format().}}
  }

  String escapedPercent(String value) {
    return String.format("100%%: %s", value); // Noncompliant {{Use String.valueOf() or string concatenation instead of String.format().}}
  }

  String numeric(double amount) {
    return String.format("%.2f", amount); // compliant
  }

  String padded(int value) {
    return String.format("0x%08X", value); // compliant
  }

  String aligned(String name) {
    return String.format("%-20s", name); // compliant
  }

  String date(long timestamp) {
    return String.format("%tF", timestamp); // compliant
  }

  String dynamic(String format, Object value) {
    return String.format(format, value); // compliant
  }

  String composed(String prefix, Object value) {
    return String.format(prefix + "%s", value); // compliant
  }

  String extra(Object value, Object unused) {
    return String.format("%s", value, unused); // compliant
  }

  String missing() {
    return String.format("%s"); // compliant
  }

  String indexed(Object value) {
    return String.format("%1$s", value); // compliant
  }

  String literalPercent() {
    return String.format("100%%"); // compliant
  }

  String array(Object[] values) {
    return String.format("%s", values); // compliant
  }

  String stringArray(String[] values) {
    return String.format("%s", values); // compliant
  }

  String charArray(char[] values) {
    return String.format("%s", values); // compliant
  }

  String intArray(int[] values) {
    return String.format("%s", values); // compliant
  }

  String emptyFormat() {
    return String.format(""); // compliant
  }

  String noPlaceholders() {
    return String.format("hello"); // compliant
  }

  String trailingPercent() {
    return String.format("value%"); // compliant
  }

  String localizedConcat(String a, String b) {
    return String.format(Locale.ROOT, "%s/%s", a, b); // Noncompliant {{Use String.valueOf() or string concatenation instead of String.format().}}
  }

  String multipleEscapedPercents(String value) {
    return String.format("%%:%s:%%", value); // Noncompliant {{Use String.valueOf() or string concatenation instead of String.format().}}
  }

  String threeArguments(String a, String b, String c) {
    return String.format("%s-%s-%s", a, b, c); // Noncompliant {{Use String.valueOf() or string concatenation instead of String.format().}}
  }

  String localizedNoArgs() {
    return String.format(Locale.ROOT, "hello"); // compliant
  }

  String localizedArray(Object[] values) {
    return String.format(Locale.ROOT, "%s", values); // compliant
  }

  String localizedMissing() {
    return String.format(Locale.ROOT, "%s"); // compliant
  }

  String localizedExtra(Object value, Object unused) {
    return String.format(Locale.ROOT, "%s", value, unused); // compliant
  }

  String localizedNumeric(double value) {
    return String.format(Locale.ROOT, "%.2f", value); // compliant
  }

  String localizedTrailingPercent() {
    return String.format(Locale.ROOT, "value%"); // compliant
  }
}
