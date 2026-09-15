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
}
