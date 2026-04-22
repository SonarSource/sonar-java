package checks;

import java.util.Collection;
import java.util.List;

public class StreamForeachCheck {

  void unnecessaryStreamForEach(Collection<T> collection) {
    collection.stream().forEach(System.out::println);  // Noncompliant
  }

  void compliantCollectionForEach(Collection<T> collection) {
    collection.forEach(System.out::println);  // Compliant
  }

  void necessaryFilterForEach(List<String> list) {list.stream().filter(s -> !s.isEmpty()).forEach(System.out::println);}

}
