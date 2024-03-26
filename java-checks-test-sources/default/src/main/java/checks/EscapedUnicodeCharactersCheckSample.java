package checks;

class EscapedUnicodeCharactersCheckSample {
  void method() {
    String prefix = "n\uuuu00E9e"; // Noncompliant [[sc=21;ec=34]] {{Remove this Unicode escape sequence and use the character instead.}}
    String prefix2= "n\\\uuuu00E9e"; // Noncompliant [[sc=21;ec=36]] {{Remove this Unicode escape sequence and use the character instead.}}
    prefix = "n\u00E9e\u0001v"; // Noncompliant
    // compliant, only escaped
    prefix = "\u00E9\u00E9\u00E9\u00E9";
    // compliant, only unprintable
    prefix = "a\u0001b\u0002c\u00A0";
    prefix = "\u007f";
    prefix = "a\u0080b\u0002c\u00A0\u0083\u0164"; // Noncompliant [[sc=14;ec=49]]
    prefix = "n\\u00E9e";
    prefix = "née";
    prefix = "";

    prefix = "a\u0045"; // Noncompliant
    prefix = "aE"; // Compliant

    prefix = "(\u25E3_\u25E2)"; // Noncompliant
    prefix = "(◣_◢)";

    // should allow all control characters < 0x80
    prefix = "a\u0000" + "a\u0001" + "a\u0002" + "a\u0003" + "a\u0004" + "a\u0005" + "a\u0006" + "a\u0007" + "a\u0008";
    prefix = "a\u0009" + "a\u000B" + "a\u000C" + "a\u000E" + "a\u000F" + "a\u0010" + "a\u0011" + "a\u0012" + "a\u0013";
    prefix = "a\u0014" + "a\u0015" + "a\u0016" + "a\u0017" + "a\u0018" + "a\u0019" + "a\u001A" + "a\u001B" + "a\u001C";
    prefix = "a\u001D" + "a\u001E" + "a\u001F" + "a\u007F";

    // should allow all whitespaces > 0x80
    // see https://en.wikipedia.org/wiki/Unicode_character_property#Whitespace

    // Unicode characters with White_Space property
    prefix = "a\u0085" + "a\u00A0" + "a\u1680" + "a\u2000" + "a\u2001" + "a\u2002" + "a\u2003" + "a\u2004" + "a\u2005";
    prefix = "a\u2006" + "a\u2007" + "a\u2008" + "a\u2009" + "a\u200A" + "a\u2028" + "a\u2029" + "a\u202F" + "a\u205F" + "a\u3000";
    prefix = "a\u00a0" + "a\u200a" + "a\u202f" + "a\u205f" + "a\u00a0";

    // Related Unicode characters without White_Space property
    prefix = "a\u180E" + "a\u200B" + "a\u200C" + "a\u200D" + "a\u2060" + "a\uFEFF";
  }
}
