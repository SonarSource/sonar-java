import java.util.List;
import java.util.stream.Stream;

abstract class A {
  List<String> otherItems;

  void foo(Stream<String> myStream, Stream<Integer> myOtherStream, A myA, java.util.function.Consumer<String> myConsumer, int[] values) {
    List<String> items = new java.util.ArrayList<>();
    myStream.forEach(items::add); // Noncompliant {{Use "collect(Collectors.toList())" instead of "forEach(items::add)".}}
    myStream.forEach(this.otherItems::add); // Noncompliant {{Use "collect(Collectors.toList())" instead of "forEach(otherItems::add)".}}

    myStream.forEach(myA::add); //  not the add method from list
    myStream.forEach(items::unknownMethod); // unknown method
    myStream.forEach(getItems()::add); // edge case
    myOtherStream.forEach(items::get); // not the add method

    myStream.forEach(item -> items.add(item)); // Noncompliant {{Use "collect(Collectors.toList())" instead of adding elements in "items" using "forEach(...)".}}
    myStream.forEach(item -> { items.add(item); }); // Noncompliant {{Use "collect(Collectors.toList())" instead of adding elements in "items" using "forEach(...)".}}

    myStream.forEach(item -> add(item)); // Compliant
    myStream.forEach(item -> { // Compliant - edge case with empty statement as placeholder 
      ; // can be anything
      items.add(item);
    });
    myStream.forEach(item -> { ; }); // Compliant
    myStream.forEach(item -> { List<String> newItems = add(item); }); // Compliant
    myOtherStream.forEach(item -> { values[item]++; });

    myStream.forEach(myConsumer); /// Compliant
  }

  abstract List<String> add(String item);
  abstract List<String> getItems();
}
