import java.util.concurrent.Callable;
import java.util.stream.IntStream;

public class LambdaExpressions {

  public static void main(String[] args) {

    boolean flag = true;
    Callable<Integer> c = flag ? () -> 23 : () -> 42;
    IntStream.range(1, 5).map((x)->x*x).forEach(System.out::println);
    IntStream.range(1, 5).map((x)-> { return x*x-1; } ).forEach(System.out::println);
    Callable<Integer> c1 = ()-> {
      return 1;
    };

  }

}
