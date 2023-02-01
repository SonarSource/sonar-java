package checks;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

class OptionalAsParameterCheck {
  void foo(OptionalAsParameterCheck a) {} // Compliant

  void foo(Optional<OptionalAsParameterCheck> a) {} // Noncompliant [[sc=12;ec=46]] {{Specify a "OptionalAsParameterCheck" parameter instead.}}
  void bar(Optional o) {} // Noncompliant [[sc=12;ec=20]] {{Specify a type instead.}}

  void foo(com.google.common.base.Optional<OptionalAsParameterCheck> a) {} // Noncompliant [[sc=12;ec=69]] {{Specify a "OptionalAsParameterCheck" parameter instead.}}
  void bar(com.google.common.base.Optional o) {} // Noncompliant [[sc=12;ec=43]] {{Specify a type instead.}}

  void foo(OptionalInt i) {} // Noncompliant [[sc=12;ec=23]] {{Specify a "int" parameter instead.}}
  void foo(OptionalLong l) {} // Noncompliant [[sc=12;ec=24]] {{Specify a "long" parameter instead.}}
  void foo(OptionalDouble d) {} // Noncompliant [[sc=12;ec=26]] {{Specify a "double" parameter instead.}}
}

class Child extends OptionalAsParameterCheck {
  @Override
  void foo(Optional<OptionalAsParameterCheck> a) {} // Compliant, as this method is overriding.

  void bar(Optional o) {} // Compliant, as this method is overriding.
}
