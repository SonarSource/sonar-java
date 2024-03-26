package checks.tests;

import java.util.List;
import java.util.function.Consumer;
import org.assertj.core.api.Condition;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.atIndex;

public abstract class AssertJAssertionsInConsumerCheckSample {

  private static final Consumer<String> classRequirements = s -> assertThat(s).isEqualTo("b");

  @Test
  public void testIsInstanceOfSatisfying(Consumer<String> unknownRequirements) {
    Object myObj = getSomeObject();
    Consumer<String> myPredicateAsConsumer = s -> s.equals("b");
    assertThat(myObj).isInstanceOfSatisfying(String.class, "b"::equals); // Noncompliant [[sc=23;ec=45;secondary=20]] {{Rework this assertion to assert something inside the Consumer argument.}}
    assertThat(myObj).isInstanceOfSatisfying(String.class, s -> s.equals("b")); // Noncompliant [[sc=23;ec=45;secondary=21]]
    assertThat(myObj).isInstanceOfSatisfying(String.class, myPredicateAsConsumer); // Noncompliant [[sc=23;ec=45;secondary=22]]

    Consumer<String> myRequirements = s -> assertThat(s).isEqualTo("b");
    assertThat(myObj).isInstanceOfSatisfying(String.class, s -> assertThat(s).isEqualTo("b"));
    assertThat(myObj).isInstanceOfSatisfying(String.class, myRequirements);
    assertThat(myObj).isInstanceOfSatisfying(String.class, classRequirements);
    assertThat(myObj).isInstanceOfSatisfying(String.class, this::localMethodWithAssertion);
    assertThat(myObj).isInstanceOfSatisfying(String.class, unknownRequirements); // Ok - we consider expressions that cannot be resolved as compliant
  }

  @Test
  public void testSatisfies(Consumer<String> unknownRequirements) {
    assertThat(getSomeObject()).satisfies("b"::equals); // Noncompliant
    assertThat("a").satisfies(s -> s.equals("b")); // Noncompliant
    ThrowingConsumer<String> myPredicateAsConsumer = s -> s.equals("b");
    assertThat("a").satisfies(myPredicateAsConsumer); // Noncompliant

    Consumer<String> myRequirements = s -> assertThat(s).isEqualTo("b");
    assertThat("a").satisfies(s -> assertThat(s).isEqualTo("b"));
    assertThat("a").satisfies(myRequirements);
    assertThat("a").satisfies(classRequirements);
    assertThat("a").satisfies(unknownRequirements);

    Condition<String> myCondition = new Condition<>("a"::equals, "");
    assertThat("a").satisfies(myCondition);
  }

  @Test
  public void testSatisfiesAnyOf(Consumer<String> unknownRequirements) {
    Consumer<String> myPredicateAsConsumer = s -> s.equals("b");
    Consumer<String> myRequirements = s -> assertThat(s).isEqualTo("b");

    assertThat("a").satisfiesAnyOf("b"::equals, "c"::equals); // Noncompliant [[secondary=54,54]]
    assertThat("a").satisfiesAnyOf("b"::equals, "c"::equals, "d"::equals); // Noncompliant [[secondary=55,55,55]]
    assertThat("a").satisfiesAnyOf("b"::equals, "c"::equals, "d"::equals, "e"::equals); // Noncompliant
    assertThat("a").satisfiesAnyOf("b"::equals, s -> s.equals("b"), myPredicateAsConsumer); // Noncompliant
    assertThat("a").satisfiesAnyOf(myRequirements, myRequirements, myRequirements, "c"::equals); // Noncompliant [[secondary=58]] - only last argument is missing an assertion

    assertThat("a").satisfiesAnyOf(myRequirements, myRequirements);
    assertThat("a").satisfiesAnyOf(myRequirements, myRequirements, myRequirements);
    assertThat("a").satisfiesAnyOf(myRequirements, myRequirements, myRequirements, myRequirements);
    assertThat("a").satisfiesAnyOf(s -> assertThat(s).isEqualTo("b"), myRequirements, classRequirements, unknownRequirements);
  }

  @Test
  public void testIterableSatisfyMethods(Consumer<Object> unknownRequirements) {
    List<Object> myList = getSomeList();
    assertThat(myList).allSatisfy("b"::equals); // Noncompliant
    assertThat(myList).anySatisfy("b"::equals); // Noncompliant
    assertThat(myList).hasOnlyOneElementSatisfying("b"::equals); // Noncompliant
    assertThat(myList).noneSatisfy("b"::equals); // Noncompliant
    assertThat(myList).satisfies("b"::equals); // Noncompliant
    assertThat(myList).satisfies("b"::equals, atIndex(2)); // Noncompliant
    assertThat(myList).zipSatisfy(myList, (a, b) -> a.equals(b)); // Noncompliant

    Consumer<Object> myRequirements = s -> assertThat(s).isEqualTo("b");
    assertThat(myList).allSatisfy(s -> assertThat(s).isEqualTo("b"));
    assertThat(myList).anySatisfy(myRequirements);
    assertThat(myList).hasOnlyOneElementSatisfying(unknownRequirements);
    assertThat(myList).noneSatisfy(myRequirements);
    assertThat(myList).satisfies(l -> assertThat(l).isNullOrEmpty());
    assertThat(myList).satisfies(myRequirements, atIndex(2));
    assertThat(myList).satisfies(new Condition<>("a"::equals, ""));
    assertThat(myList).zipSatisfy(myList, (a, b) -> assertThat(a).isEqualTo(b));
  }

  private void localMethodWithAssertion(Object objectToAssert) {
    assertThat(objectToAssert).isEqualTo("b");
  }

  protected abstract Object getSomeObject();

  protected abstract List<Object> getSomeList();
}
