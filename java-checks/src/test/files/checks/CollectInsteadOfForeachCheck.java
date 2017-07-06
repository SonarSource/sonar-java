import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

abstract class A {
  List<String> otherItems;

  void foo(Stream<String> myStream, Stream<Integer> myOtherStream, A myA) {
    List<String> items = new ArrayList<>();
    myStream.forEach(items::add); // Noncompliant {{Use "collect(Collectors.toList())" instead of "forEach(items::add)".}}

    myStream.forEach(this.otherItems::add); // Compliant - not a list created only for collect 
    myStream.forEach(myA::add); // Compliant - not the add method from list
    myStream.forEach(items::unknownMethod); // Compliant - unknown method
    myStream.forEach(item -> items.add(item)); // Compliant - Only target method references
    myOtherStream.forEach(items::get); // Compliant - not the add method
  }
  
  void add(String item);
}
