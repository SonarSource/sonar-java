import java.util.stream.Stream;

class A {
  void foo(){
    Stream.of("one", "two", "three", "four")
      .filter(e -> e.length() > 3)
      .peek(e -> System.out.println("Filtered value: " + e)); // Noncompliant {{Remove this use of "Stream.peek".}}
  }
}
