import java.util.function.Function;
import java.util.function.Predicate;

class DoubleLambda {

  private void test() {
    String my = "my";
    Function<String, Predicate<String>> functionOfPredicate = aString -> anotherString -> my.equals(anotherString) && my.equals(aString);
    functionOfPredicate.apply("hello");
  }

}