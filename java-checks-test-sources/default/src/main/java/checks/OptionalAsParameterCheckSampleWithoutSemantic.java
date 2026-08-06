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
 // Noncompliant@+4
class OptionalAsParameterCheckSampleWithoutSemantic {
 // Noncompliant@+2
  @GetMapping("/{id}")
  ResponseEntity<Foo> getFoo(@PathVariable Optional<Long> id, @RequestParam(value = "name") Optional<String> name, @RequestParam(value = "bar") Optional<Integer> bar) { // Noncompliant
    return new ResponseEntity<>(new Foo(), HttpStatus.OK);
  }

  void foo(@Nullable OptionalAsParameterCheckSample a) {} // Compliant

  void foo(Optional<OptionalAsParameterCheckSample> a) {} // Noncompliant

  void bar(Optional o) {} // Noncompliant

  void foo(OptionalInt i) {} // Noncompliant

  void foo(OptionalLong l) {} // Noncompliant

  void foo(OptionalDouble d) {} // Noncompliant

}

class ChildWS extends OptionalAsParameterCheckSample {
  @Override
  void foo(Optional<OptionalAsParameterCheckSample> a) {} // Compliant, as this method is overriding.

  void bar(Optional o) {} // Compliant, as this method is overriding.
}
