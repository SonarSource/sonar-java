import java.util.function.Function;
import java.util.stream.Stream;
class CastTargetType {
  public static void fun() {
    Stream.of("a")
      .map((Function<String, String>) input -> {
        String s = input + "b";
        return s + "c";
      })
      .forEach(System.out::println);
  }
}
