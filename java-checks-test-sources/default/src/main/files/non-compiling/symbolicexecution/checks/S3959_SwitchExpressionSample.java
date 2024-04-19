package symbolicexecution.checks;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class S3959_SwitchExpressionSample {
  List<String> foo(MyEnum e, Stream<String> stream) {
    return switch (e) {
      case SOME_TYPE -> stream.collect(Collectors.toList());
      case ANOTHER_TYPE -> stream.filter(s -> s.length() > 4).collect(Collectors.toList());
    };
  }

  int foo(MyEnum e) {
    Stream<String> s = Stream.of("hello");
    long count = switch (e) {
      case SOME_TYPE -> s.count();
      case ANOTHER_TYPE -> s.filter(n -> n.length() > 4).count();
    };
    return s.count(); // Noncompliant
  }

  public enum MyEnum {
    SOME_TYPE,
    ANOTHER_TYPE;
  }
}
