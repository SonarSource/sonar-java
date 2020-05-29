package checks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AssertJConsecutiveAssertionCheck {
  String myString = "42";
  String myOtherString = "24";
  List<String> myList = new ArrayList<>();

  @Test
  void simple_example() {
    assertThat(myString).hasSize(2); // Noncompliant [[sc=5;ec=15;secondary=20,21]] {{Join these multiple assertions subject to one assertion chain.}}
    assertThat(myString).startsWith("4");
    assertThat(myString).isEqualTo("42");
  }

  @Test
  void assert_that_between() {
    assertThat(myString).hasSize(2); // Noncompliant [[sc=5;ec=15;secondary=28]] {{Join these multiple assertions subject to one assertion chain.}}
    assertThat("myString").isEqualTo("10");
    assertThat(myString).isEqualTo("10");
  }

  @Test
  void something_between() {
    assertThat(myString).hasSize(2); // Compliant, modify() can modify the actual value tested
    modify();
    assertThat(myString).isEqualTo("10");
  }

  @Test
  void two_issues_something_between() {
    assertThat(myString).hasSize(2); // Noncompliant
    assertThat(myString).isEqualTo("10");
    modify();
    assertThat(myString).hasSize(2); // Noncompliant
    assertThat(myString).isEqualTo("10");
  }

  @Test
  void something_before() {
    if (true) {
    }
    assertThat(myString).hasSize(2); // Noncompliant
    assertThat(myString).isEqualTo("10");
  }

  @Test
  void something_after() {
    assertThat(myString).hasSize(2); // Noncompliant
    assertThat(myString).isEqualTo("10");
    myString += "a";
  }

  @Test
  void multiple_predicates() {
    assertThat(myString).hasSize(2).startsWith("4"); // Noncompliant
    assertThat(myString).isEqualTo("42");
  }

  @Test
  void lonely_assert_that() {
    assertThat(myString).hasSize(2); // Compliant
    assertThat(myString);
  }

  @Test
  void with_description() {
    assertThat(myString).as("message").hasSize(2); // Noncompliant
    assertThat(myString).describedAs("message").isEqualTo("42");
  }
  @Test
  void assert_on_two_symbol() {
    assertThat(myString).hasSize(2); // Compliant
    assertThat(myOtherString).isEqualTo("42");
  }

  @Test
  void using_comparator() {
    assertThat(myString).usingComparator(new MyComparator()).isEqualTo("42"); // Compliant, cannot merge the two
    assertThat(myString).isEqualTo("42");
  }

  @Test
  void using_comparator_2() {
    assertThat(myString).hasSize(2); // Compliant, we could merge it with the third one, but might be acceptable to keep it as it is for clarity
    assertThat(myString).usingComparator(new MyComparator()).isEqualTo("42"); // Compliant, cannot merge the two
    assertThat(myString).isEqualTo("42");
  }

  @Test
  void extracting() {
    assertThat((Object) myString).extracting("field1").isEqualTo("f1"); // Compliant
    assertThat((Object) myString).extracting("field2").isEqualTo("f2");
    assertThat((Object) myString).isEqualTo("42");
  }

  @Test
  void filtered() {
    assertThat(myList).filteredOn("", "").isEqualTo("");  // Compliant
    assertThat(myList).isEmpty();
  }


  @Test
  void assert_on_cast() {
    assertThat((Object) "a").isNotNull(); // Noncompliant
    assertThat((Object) "a").isEqualTo("42");
  }

  @Test
  void assert_on_literals_1() {
    assertThat("a").hasSize(2); // Noncompliant
    assertThat("a").isEqualTo("42");
  }

  @Test
  void assert_on_literals_2() {
    assertThat("a").hasSize(2); // Compliant
    assertThat("b").isEqualTo("42");
  }

  @Test
  void assert_on_member_select() {
    assertThat(myList.get(0)).hasSize(2); // Compliant, we can not know if both method call return the same value
    assertThat(myList.get(0)).isEqualTo("42");
  }

  @Test
  void assert_on_member_select_2() {
    assertThat(myList.get(0)).hasSize(2); // Compliant
    assertThat(myList.get(1)).isEqualTo("42");
  }

  @Test
  void assert_iterator() {
    Iterator<String> it = (new ArrayList<String>()).iterator();
    assertThat(it.next()).isEqualTo("1"); // Compliant
    assertThat(it.next()).isEqualTo("2");
  }

  @Test
  void assert_on_binary_operation_1() {
    assertThat(myOtherString + myString).hasSize(2); // Noncompliant
    assertThat(myOtherString + myString).isEqualTo("42");
  }

  @Test
  void assert_on_binary_operation_2() {
    assertThat(myOtherString + myString).hasSize(2); // Compliant
    assertThat(myString + myOtherString).isEqualTo("42");
  }

  @Test
  void onVariable() {
    AbstractStringAssert<?> variableAssert = assertThat(myString);
    variableAssert.hasSize(1); // Compliant
    variableAssert.isEqualTo("42");
  }

  void not_test_method() {
    assertThat(myString).hasSize(2); // Compliant, not a test method
    assertThat(myString).isEqualTo("42");
  }

  void modify() {
    myString = "A";
  }
}

class MyComparator implements Comparator<Object> {
  @Override
  public int compare(Object o1, Object o2) {
    return 0;
  }
}

abstract class AbstractTestNullBlock {
  @Test
  abstract void simple_example();
}
