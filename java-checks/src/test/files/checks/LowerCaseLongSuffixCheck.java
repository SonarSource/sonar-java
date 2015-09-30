class Foo {
  long a = 10l; // Noncompliant {{Replace this lower case "l" long suffix by an upper case "L".}}
  long b = 10L;
}
