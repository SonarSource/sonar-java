class A {
  void f(String str, String other, int from, int to) {    
    
    "".contains("str");
    "".contains("");     // Noncompliant {{Remove this "contains" call; it has predictable results.}}
    str.contains(str);   // Noncompliant
    str.contains(other);

    "".compareTo("str");
    "".compareTo("");     // Noncompliant {{Remove this "compareTo" call; it has predictable results.}}
    str.compareTo(str);   // Noncompliant
    str.compareTo(other);

    "".compareToIgnoreCase("str");
    "".compareToIgnoreCase("");     // Noncompliant {{Remove this "compareToIgnoreCase" call; it has predictable results.}}
    str.compareToIgnoreCase(str);   // Noncompliant
    str.compareToIgnoreCase(other);

    "".endsWith("str");
    "".endsWith("");     // Noncompliant {{Remove this "endsWith" call; it has predictable results.}}
    str.endsWith(str);   // Noncompliant
    str.endsWith(other);

    "".indexOf("str");
    "".indexOf("");     // Noncompliant {{Remove this "indexOf" call; it has predictable results.}}
    str.indexOf(str);   // Noncompliant
    str.indexOf(other);

    "".indexOf("str", from);
    "".indexOf("", from);     // Noncompliant {{Remove this "indexOf" call; it has predictable results.}}
    str.indexOf(str, from);   // Noncompliant
    str.indexOf(other, from);

    "".lastIndexOf("str");
    "".lastIndexOf("");     // Noncompliant {{Remove this "lastIndexOf" call; it has predictable results.}}
    str.lastIndexOf(str);   // Noncompliant
    str.lastIndexOf(other);

    "".lastIndexOf("str", from);
    "".lastIndexOf("", from);     // Noncompliant {{Remove this "lastIndexOf" call; it has predictable results.}}
    str.lastIndexOf(str, from);   // Noncompliant
    str.lastIndexOf(other, from);

    "".matches("str");
    "".matches("");     // Noncompliant {{Remove this "matches" call; it has predictable results.}}
    str.matches(str);   // Noncompliant
    str.matches(other);

    "".split("str");
    "".split("");     // Noncompliant {{Remove this "split" call; it has predictable results.}}
    str.split(str);   // Noncompliant
    str.split(other);

    "".startsWith("str");
    "".startsWith("");     // Noncompliant {{Remove this "startsWith" call; it has predictable results.}}
    str.startsWith(str);   // Noncompliant
    str.startsWith(other);

    "".replaceFirst("str", "");
    "".replaceFirst("", "str");     // Noncompliant {{Remove this "replaceFirst" call; it has predictable results.}}
    "".replaceFirst("str", "str");  // Noncompliant
    "".replaceFirst(other, other);  // Noncompliant
    "".replaceFirst("str", "");
    str.replaceFirst(str, "str");   // Noncompliant
    str.replaceFirst("str", "str"); // Noncompliant
    str.replaceFirst(other, other); // Noncompliant

    "".substring(0);               // Noncompliant {{Remove this "substring" call; it has predictable results.}}
    "".substring(from);
    "".substring("".length());
    "".substring(other.length());
    str.substring(0);              // Noncompliant
    str.substring(from);
    str.substring(str.length());   // Noncompliant
    str.substring(other.length());

    "".substring(0, "".length());
    "".substring(0, other.length());
    "".substring(1, to);
    "".substring("".length(), to);
    "".substring(from, "".length());
    str.substring(0, str.length());    // Noncompliant {{Remove this "substring" call; it has predictable results.}}
    str.substring(0, other.length());
    str.substring(1, to);
    str.substring(str.length(), to);   // Noncompliant
    str.substring(from, str.length());
  }
}
