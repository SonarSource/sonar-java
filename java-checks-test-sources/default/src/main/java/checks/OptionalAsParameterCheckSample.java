package checks;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import javax.annotation.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

class OptionalAsParameterCheckSample {

  @GetMapping("/{id}")
  ResponseEntity<Foo> getFoo(@PathVariable Optional<Long> id, @RequestParam(value = "name") Optional<String> name, @RequestParam(value = "bar") Optional<Integer> bar) { // Compliant
    return new ResponseEntity<>(new Foo(), HttpStatus.OK);
  }

  void foo(@Nullable OptionalAsParameterCheckSample a) {} // Compliant

  void foo(Optional<OptionalAsParameterCheckSample> a) {} // Noncompliant {{Specify a "OptionalAsParameterCheckSample" parameter instead.}}
//         ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  void bar(Optional o) {} // Noncompliant {{Specify a type instead.}}
//         ^^^^^^^^

  void foo(com.google.common.base.Optional<OptionalAsParameterCheckSample> a) {} // Noncompliant {{Specify a "OptionalAsParameterCheckSample" parameter instead.}}
//         ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  void bar(com.google.common.base.Optional o) {} // Noncompliant {{Specify a type instead.}}
//         ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

  void foo(OptionalInt i) {} // Noncompliant {{Specify a "int" parameter instead.}}
//         ^^^^^^^^^^^
  void foo(OptionalLong l) {} // Noncompliant {{Specify a "long" parameter instead.}}
//         ^^^^^^^^^^^^
  void foo(OptionalDouble d) {} // Noncompliant {{Specify a "double" parameter instead.}}
//         ^^^^^^^^^^^^^^
}

class Child extends OptionalAsParameterCheckSample {
  @Override
  void foo(Optional<OptionalAsParameterCheckSample> a) {} // Compliant, as this method is overriding.

  void bar(Optional o) {} // Compliant, as this method is overriding.
}
