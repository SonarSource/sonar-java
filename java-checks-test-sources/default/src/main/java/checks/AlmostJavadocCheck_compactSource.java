void main() {
  System.out.println("compact source");
}

// Noncompliant@+1
/* @since 1.0 */
int version;

/** @since 1.0 */
int documentedVersion;

// Noncompliant@+1
/* @param n unused */
int a, b, c;

// Noncompliant@+1
/* Returns <code>ok</code>. */
String status() {
  return "ok";
}
