package test;

import java.util.Comparator;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.maxBy;
import static java.util.stream.Collectors.minBy;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.summingDouble;
import static java.util.stream.Collectors.summingInt;
import static java.util.stream.Collectors.summingLong;

public class Streams {

  void test(Stream<Object> stream, BinaryOperator<Object> binaryOperator, Comparator<Object> comparator) {
    long count = stream.collect(counting()); // Noncompliant {{Use "count()" instead.}}
    stream.collect(maxBy(comparator)); // Noncompliant {{Use "max()" instead.}}
    stream.collect(minBy(comparator)); // Noncompliant {{Use "min()" instead.}}
    stream.collect(mapping(Object::toString, Collectors.toList())); // Noncompliant {{Use "map(...).collect()" instead.}}
    stream.collect(reducing(binaryOperator));  // Noncompliant {{Use "reduce(...).collect()" instead.}}
    stream.collect(summingInt(Object::hashCode));  // Noncompliant {{Use "mapToInt(...).sum()" instead.}}
    stream.collect(summingLong(Object::hashCode)); // Noncompliant {{Use "mapToLong(...).sum()" instead.}}
    stream.collect(summingDouble(Object::hashCode)); // Noncompliant {{Use "mapToDouble(...).sum()" instead.}}
  }

  void compliant(Stream<Object> stream) {
    stream.collect(Collectors.groupingBy(Object::toString, Collectors.summingInt(Object::hashCode)));
  }

}
