import com.google.auto.value.AutoValue;

public class GoogleAutoValueFilter {

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


