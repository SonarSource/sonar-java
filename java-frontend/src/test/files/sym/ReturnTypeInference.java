import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class A {

  public Stream<String> test() {
    return StreamSupport
      .stream(
        Spliterators.spliterator(new int[] {1, 2, 3}, 0),
        false)
      .map(this::mapToString);
  }

  private String mapToString(Integer someInt) {
    return someInt.toString();
  }

}
