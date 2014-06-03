import java.util.stream.IntStream;

class Lambdas { 
  void method(){
     IntStream.range(1, 5)
        .map((x)-> x*x )
        .map(x -> x * x)
        .map((int x) -> x * x)
        .map((x)-> x*x )
      ;
  }

}
