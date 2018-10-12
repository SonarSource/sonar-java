import java.util.function.Function;
import java.util.stream.Stream;
import java.util.Comparator;
import java.io.Serializable;
class CastTargetType {
  public static void fun() {
    Stream.of("a")
      .map((Function<String, String>) input -> {
        String s = input + "b";
        return s + "c";
      })
      .forEach(System.out::println);
  }

  public static final Comparator<Object> UNIQUE_ID_COMPARATOR =  (Comparator<Object> & Serializable) (o1, o2) -> o1.toString().compareTo(o2.toString());

}
