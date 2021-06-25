package checks;

import static java.util.Objects.requireNonNull;

class LeftCurlyBraceStartLineCheck_java16
{
  record Person(String name)
  {
    Person { // Noncompliant
      requireNonNull(name);
    }
  }
}
