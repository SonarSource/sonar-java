package checks;

import java.util.Collection;

public class StreamForeachCheck {

  void unnecessaryStreamForEach(Collection<T> collection) {
    collection.stream().forEach(System.out::println);  // Noncompliant
  }

  void compliantCollectionForEach(Collection<T> collection) {
    collection.forEach(System.out::println);  // Compliant
  }
}
