package checks.tests;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.assertj.core.description.TextDescription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatObject;

public class AssertJContextBeforeAssertionCheck {

  void foo() {
    assertThat("").isEqualTo("").as("Description1").isEqualTo(""); // Compliant
    assertThat("").isEqualTo("").as("Description"); // Noncompliant [[sc=34;ec=36]] {{Add an assertion predicate after calling this method.}}
    assertThat("").isEqualTo("").as(new TextDescription("Description")); // Noncompliant
    assertThat("").isEqualTo("").as("Description", new Object()); // Noncompliant
    assertThat("").as("Description1").isEqualTo("").as("Description2"); // Noncompliant [[sc=53;ec=55]]
    assertThat("").as("Description").isEqualTo(""); // Compliant
    assertThat("").isEqualTo("").as("Description1").isEqualTo(""); // Compliant

    assertThat("").isEqualTo("").describedAs("Description"); // Noncompliant [[sc=34;ec=45]]
    assertThat("").isEqualTo("").describedAs(new TextDescription("Description")); // Noncompliant
    assertThat("").isEqualTo("").describedAs("Description", new Object()); // Noncompliant
    assertThat("").describedAs("Description").isEqualTo(""); // Compliant

    assertThat("").isEqualTo("").withFailMessage("fail message"); // Noncompliant
    assertThat("").isEqualTo("").withFailMessage("fail message", new Object()); // Noncompliant
    assertThat("").withFailMessage("fail message").isEqualTo(""); // Compliant

    assertThat("").isEqualTo("").overridingErrorMessage("new error message"); // Noncompliant
    assertThat("").isEqualTo("").overridingErrorMessage("new error message", new Object()); // Noncompliant
    assertThat("").withFailMessage("new error message").isEqualTo(""); // Compliant

    // Comparison
    assertThat(1).isEqualTo(2).usingComparator(new MyComparator()); // Noncompliant
    assertThat(1).isEqualTo(2).usingComparator(new MyComparator(), "My comparator"); // Noncompliant
    assertThat(1).isEqualTo(2).usingComparator(new Comparator<Integer>() { // Noncompliant
      @Override
      public int compare(Integer o1, Integer o2) {
        return 0;
      }
    });
    assertThat(1).usingComparator(new MyComparator()).isEqualTo(2); // Compliant
    assertThat(1).usingComparator(new MyComparator()).isEqualTo(2).usingDefaultComparator().isEqualTo(1); // Compliant

    assertThat(1).isEqualTo(2).usingDefaultComparator(); // Noncompliant
    assertThat(1).isEqualTo(2).usingRecursiveComparison(); // Noncompliant
    assertThat(1).isEqualTo(2).usingRecursiveComparison(new RecursiveComparisonConfiguration()); // Noncompliant
    assertThat(1).isEqualTo(2).usingComparatorForFields(new MyComparator()); // Noncompliant
    assertThat(1).isEqualTo(2).usingComparatorForFields(new MyComparator(), "a", "b"); // Noncompliant
    assertThat(1).isEqualTo(2).usingComparatorForType(new MyComparator(), Integer.class); // Noncompliant

    // Element comparison
    assertThat(getList()).isEqualTo(new ArrayList<>()).usingDefaultElementComparator(); // Noncompliant
    assertThat(getList()).isEqualTo(new ArrayList<>()).usingFieldByFieldElementComparator(); // Noncompliant
    assertThat(getList()).isEqualTo(new ArrayList<>()).usingComparatorForElementFieldsWithNames(new MyComparator(), ""); // Noncompliant
    assertThat(getList()).isEqualTo(new ArrayList<>()).usingComparatorForElementFieldsWithType(new MyComparator(), Object.class); // Noncompliant
    assertThat(getList()).isEqualTo(new ArrayList<>()).usingElementComparator(new MyComparator()); // Noncompliant
    assertThat(getList()).isEqualTo(new ArrayList<>()).usingElementComparatorIgnoringFields("field"); // Noncompliant
    assertThat(getList()).isEqualTo(new ArrayList<>()).usingElementComparatorOnFields(); // Noncompliant
    assertThat(getList()).isEqualTo(new ArrayList<>()).usingRecursiveFieldByFieldElementComparator(); // Noncompliant

    // Extracting
    assertThat(getObject()).isEqualTo("field").extracting("f"); // Noncompliant
    assertThat(getObject()).isEqualTo("field").extracting(Object::toString); // Noncompliant
    assertThat(getObject()).isEqualTo("field").extracting(Object::toString).isEqualToComparingFieldByField(new Object()); // Compliant

    // Filtering
    assertThat(getList()).isEqualTo(new ArrayList<>()).filteredOn("field", new Object()); // Noncompliant
    assertThat(getList()).isEqualTo(new ArrayList<>()).filteredOnNull("field"); // Noncompliant
    assertThat(getList()).isEqualTo(new ArrayList<>()).filteredOnAssertions(o -> {}); // Noncompliant

    // Only one issue when multiple missplaced calls.
    assertThat("").isEqualTo("").as("message").withFailMessage("fail message"); // Noncompliant [[sc=48;ec=63]]

    // assertThatObject
    assertThatObject("").isEqualTo("").as("Description1"); // Noncompliant
    assertThatObject("").as("Description").isEqualTo(""); // Compliant

    // Can overlap AssertionsCompletenessCheck (S2970), but it will complement the other issue.
    assertThat("").as("Description"); // Noncompliant
    Assertions.assertThat("").as("Description"); // Noncompliant
    assertThatObject("").as("Description"); // Noncompliant
    assertThat("").usingComparator(new MyComparator()); // Noncompliant
    org.assertj.core.api.AssertionsForClassTypes.assertThat("").as("Description1"); // Noncompliant

    // Assertion started in another place
    AbstractObjectAssert<?, ?> variableAssert = assertThat(getObject()).isEqualTo("field").extracting("f"); // Compliant
    variableAssert.isEqualTo(""); // Compliant
    getAssert().isEqualTo("expected"); // Compliant
    getAssert().as("expected"); // Noncompliant
  }

  protected AbstractObjectAssert<?, ?> getAssert() {
    return assertThat(getObject()).isEqualTo("field").extracting("f"); // Compliant, can be asserted somewhere else
  }

  Object getObject() {
    return new Object();
  }

  List<Integer> getList() {
    return new ArrayList<>();
  }

  class MyComparator implements Comparator<Object> {
    @Override
    public int compare(Object o1, Object o2) {
      return 0;
    }
  }

}
