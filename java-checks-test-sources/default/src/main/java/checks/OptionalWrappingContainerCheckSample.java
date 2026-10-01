package checks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

class OptionalWrappingContainerCheckSample {

  Optional<Collection<String>> collection() { // Noncompliant {{Return an empty collection instead of wrapping it in Optional.}}
//^^^^^^^^
    return Optional.empty();
  }

  Optional<List<String>> list() { // Noncompliant
//^^^^^^^^
    return Optional.empty();
  }

  Optional<Set<String>> set() { // Noncompliant
//^^^^^^^^
    return Optional.empty();
  }

  Optional<Map<String, String>> map() { // Noncompliant {{Return an empty map instead of wrapping it in Optional.}}
//^^^^^^^^
    return Optional.empty();
  }

  Optional<ArrayList<String>> collectionImplementation() { // Noncompliant
//^^^^^^^^
    return Optional.empty();
  }

  Optional<HashMap<String, String>> mapImplementation() { // Noncompliant
//^^^^^^^^
    return Optional.empty();
  }

  Optional<String[]> objectArray() { // Noncompliant {{Return an empty array instead of wrapping it in Optional.}}
//^^^^^^^^
    return Optional.empty();
  }

  Optional<byte[]> primitiveArray() { // Noncompliant
//^^^^^^^^
    return Optional.empty();
  }

  Optional<String[][]> multidimensionalArray() { // Noncompliant
//^^^^^^^^
    return Optional.empty();
  }

  <T extends Collection<String>> Optional<T> boundedCollection() { // Noncompliant
//                               ^^^^^^^^
    return Optional.empty();
  }

  Optional<String> scalar() {
    return Optional.empty();
  }

  Optional<Iterable<String>> iterable() {
    return Optional.empty();
  }

  Optional<Stream<String>> stream() {
    return Optional.empty();
  }

  Optional rawOptional() {
    return Optional.empty();
  }

  OptionalInt primitiveOptional() {
    return OptionalInt.empty();
  }

  Fake.Optional<List<String>> unrelatedOptional() {
    return new Fake.Optional<>();
  }

  static class Fake {
    static class Optional<T> {
    }
  }

  static class OverridingMethod implements Supplier<Optional<List<String>>> {
    @Override
    public Optional<List<String>> get() {
      return Optional.empty();
    }
  }
}
