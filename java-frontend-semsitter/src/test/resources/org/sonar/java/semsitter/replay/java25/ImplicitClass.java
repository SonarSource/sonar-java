import java.util.List;

/// The number of greetings
/// printed so far.
int counter = 0;

void main() {
  for (String name : List.of("world", "moon")) {
    counter++;
    System.out.println(greet(name) + counter);
  }
}

/// Greets `name`.
String greet(String name) {
  return "Hello " + name;
}
