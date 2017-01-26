import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

static class Qualifier {
  static Optional<Qualifier> getResult(String q) {
    return Optional.empty();
  }
  private List<Qualifier> buggy(String query) {
    return Arrays
      .stream(new String[] {"foo", "bar"})
      .flatMap(qualifier -> Qualifier.getResult(qualifier).map(Stream::of).orElseGet(Stream::empty))
      .collect(Collectors.toList());
  }
}
