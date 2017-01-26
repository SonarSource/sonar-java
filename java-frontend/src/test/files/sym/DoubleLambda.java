import java.util.function.Function;
import java.util.function.Predicate;
import java.util.List;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import static java.util.stream.Collectors.toList;
class DoubleLambda {

  private void test() {
    String my = "my";
    Function<String, Predicate<String>> functionOfPredicate = aString -> anotherString -> my.equals(anotherString) && my.equals(aString);
    functionOfPredicate.apply("hello");
  }

}
class NestedLambdas implements Issue{
  private List<List<IssueLocation>> flows = new ArrayList<>();

  public List<Flow> flows() {
    return this.flows.stream()
      .<Flow>map(l ->
        () -> ImmutableList.copyOf(l))
      .collect(toList());
  }
}
public interface IssueLocation {}

public interface Issue {
  interface Flow {
    List<IssueLocation> locations();
  }
}