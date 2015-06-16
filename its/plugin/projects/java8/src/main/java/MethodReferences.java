import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MethodReferences<T extends Math> {


  public static void printer(int i){
    System.out.println(i);
  }

  public int square(int i ){
    return i*i;
  }
  public int sumAbs(int start, int end){
   return IntStream.rangeClosed(start, end).map(T::abs).sum();
  }
  public static void main(String[] args) {  

    //static method reference
    IntStream.range(1,12).forEach(MethodReferences::printer);
    IntStream.range(1,12).map(new MethodReferences()::square).forEach(System.out::println);
    System.out.println(new MethodReferences<Math>().sumAbs(-2, 3));
    Stream<String> list =Arrays.asList("foo", "barqix").stream();

    Utilities ut1 = new Utilities(100);
    Utilities ut2 = new Utilities(10);

    IntStream.range(1, 5).map((true ? ut1:ut2)::multiply).forEach(System.out::println);
    IntStream.range(1, 5).map((false ? ut1:ut2)::multiply).forEach(System.out::println);

  }

  public static class Utilities {
    int mul;
    public Utilities(int mul){
      this.mul = mul;
    }

    public int multiply(int a){
      return a*mul;
    }
  }


}
