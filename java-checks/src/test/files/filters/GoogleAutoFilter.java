import com.google.auto.value.AutoOneOf;
import com.google.auto.value.AutoValue;

public class GoogleAutoFilter {

  // Filter "EqualsOverridenWithHashCodeCheck"
  @AutoValue
  abstract class OverrideHashCode {
    @Override
    public int hashCode() { // NoIssue
      return 1234;
    }
  }

  abstract class OverrideHashCodeNotAutoValue {
    @Override
    public int hashCode() { // WithIssue
      return 1234;
    }
  }

  // Filter "EqualsNotOverridenWithCompareToCheck"
  @AutoValue
  class CompareTo implements Comparable<Integer> {
    @Override
    public int compareTo(Integer foo) { return 1; } // NoIssue
  }

  class CompareTo2 implements Comparable<Integer> {
    @Override
    public int compareTo(Integer foo) { return 1; } // WithIssue
  }
}

// Filter "AbstractClassNoFieldShouldBeInterfaceCheck"

abstract class AbstractClassNoFieldShouldBeInterfaceCheckTest { // WithIssue
  static Foo create(String name) {
    return new AutoValue_Foo(name);
  }
}

@AutoValue
abstract class AbstractClassNoFieldShouldBeInterfaceCheckTest2 { // NoIssue
  static Foo create(String name) {
    return new AutoValue_Foo(name);
  }
  abstract String name();
  @AutoValue.Builder
  abstract static class Builder { // NoIssue
    abstract Builder namer(String name);
  }

  abstract static class Builder2 { // WithIssue
    abstract Builder namer(String name);
  }
}

@AutoOneOf(StringOrInteger.Kind.class)
abstract class AbstractClassNoFieldShouldBeInterfaceCheckTestStringOrInteger { // NoIssue
  public enum Kind {
    STRING, INTEGER
  }
}

