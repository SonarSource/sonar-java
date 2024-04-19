package checks.tests;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AssertJConsecutiveAssertionCheckSample {
  String myString = "42";
  String myOtherString = "24";
  List<String> myList = new ArrayList<>();
  Optional<String> myOptional = Optional.of("abc");

  @Test
  void simple_example() {
    assertThat(myString).hasSize(2); // Noncompliant [[sc=5;ec=15;secondary=+1,+2]] {{Join these multiple assertions subject to one assertion chain.}}
    assertThat(myString).startsWith("4");
    assertThat(myString).isEqualTo("42");
  }

  @Test
  void assert_that_between() {
    assertThat(myString).hasSize(2); // Compliant
    assertThat(myString.charAt(2)).isEqualTo('2');
    assertThat(myString).isEqualTo("10");
  }

  @Test
  void assert_that_between_2() {
    assertThat(myString).hasSize(2); // Compliant
    assertThat("myString").isEqualTo("10");
    assertThat(myString).isEqualTo("10");
  }

  @Test
  void two_assert_subject_argument() {
    assertThat(myOtherString).hasSize(2);
    assertThat(myString).startsWith("4"); // Noncompliant
    assertThat(myString).isEqualTo("42");
  }

  @Test
  void two_assert_subject_argument_2() {
    assertThat(myOtherString).hasSize(2); // Noncompliant [[sc=5;ec=15;secondary=+1]]
    assertThat(myOtherString).startsWith("2");
    assertThat(myString).startsWith("4"); // Noncompliant [[sc=5;ec=15;secondary=+1]]
    assertThat(myString).isEqualTo("42");
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
  void flat_extracting() {
    assertThat(myList).flatExtracting("field1").isEqualTo("f1"); // Compliant
    assertThat(myList).flatExtracting("field2").isEqualTo("f2");
    assertThat((Object) myString).isEqualTo("42");
  }

  @Test
  void filtered() {
    assertThat(myList).filteredOn("", "").isEqualTo("");  // Compliant
    assertThat(myList).isEmpty();
  }

  @Test
  void map() {
    // Assertions chained afterward are performed on the Optional resulting from the call
    assertThat(myOptional).map(String::toString).isEmpty(); // Compliant
    assertThat(myOptional).isEmpty();
  }

  @Test
  void flatMap() {
    // Assertions chained afterward are performed on the Optional resulting from the call
    assertThat(myOptional).flatMap(s -> Optional.of(s + "")).isEmpty(); // Compliant
    assertThat(myOptional).isEmpty();
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
  void assert_on_member_select_3() {
    assertThat(getList()).hasSize(2); // Compliant
    assertThat(getList()).hasSize(1);
  }

  @Test
  void assert_on_member_select_4() {
    assertThat(getList().size()).isEqualTo(2); // Compliant
    assertThat(getList().size()).isLessThan(1);
  }

  @Test
  void assert_on_member_select_5() {
    assertThat(getArray().length).isNotEqualTo(2); // Compliant
    assertThat(getArray().length).isNotEqualTo(1);
  }

  @Test
  void assert_on_member_select_6() {
    assertThat(getFieldOfField().field.list.length).isNotEqualTo(2); // Compliant
    assertThat(getFieldOfField().field.list.length).isNotEqualTo(1);
  }

  @Test
  void assert_on_member_select_7() {
    FieldOfField fof = new FieldOfField();
    assertThat(fof.field.list.length).isNotEqualTo(2); // Noncompliant
    assertThat(fof.field.list.length).isNotEqualTo(1);
  }

  @Test
  void assert_on_constructor() {
    assertThat(new java.util.Random()).isNotNull(); // Compliant
    assertThat(new java.util.Random()).isNotEqualTo(new java.util.Random());
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

  List<String> getList() {
    return new ArrayList<>();
  }

  String[] getArray() {
    return new String[1];
  }

  FieldOfField getFieldOfField() {
    return new FieldOfField();
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

class FieldOfField {
  FieldOfField field = new FieldOfField();
  String[] list = new String[1];
}

class CustomAssertions {

  MyType myType = new MyType();

  @Test
  void nonCompliantTypeTest1() {
    assertThat(myType).isNotNull(); // Noncompliant [[secondary=+1]]
    assertThat(myType).isEqualTo(new MyType());

    CustomAssert.assertThat(myType).isFoo(); // Noncompliant [[secondary=+1]]
    CustomAssert.assertThat(myType).isBar();
  }

  @Test
  void nonCompliantTypeTest2() {
    CustomAssert.assertThat(myType).isFoo(); // Noncompliant [[secondary=+1,+3,+4]]
    CustomAssert.assertThat(myType).isBar();

    assertThat(myType).isNotNull(); // will be included
    assertThat(myType).isEqualTo(new MyType());
  }

  @Test
  void nonCompliantTypeTest3() {
    CustomAssert.assertThat(myType) // Noncompliant [[secondary=+4]]
      .isFoo()
      .isBar();

    assertThat(myType)
      .isNotNull()
      .isEqualTo(new MyType());
  }

  @Test
  void nonCompliantTypeTest4() {
    assertThat(myType).isNotNull(); // Compliant - mixed, can not be chained
    CustomAssert.assertThat(myType).isBar(); // Noncompliant [[secondary=+1,+2]]
    assertThat(myType).isEqualTo(new MyType());
    CustomAssert.assertThat(myType).isBar();
  }

  @Test
  void compliantTypeTest1() {
    assertThat(myType)
      .isNotNull()
      .isEqualTo(new MyType());

    CustomAssert.assertThat(myType) // can not be chained if written in that order
      .isFoo()
      .isBar();
  }

  @Test
  void compliantTypeTest2() {
    CustomAssert.assertThat(myType)
      .isFoo()
      .isBar()
      .isNotNull()
      .isEqualTo(new MyType());
  }

  @Test
  void compliantTypeTest3() {
    CustomAssert.assertThat(myType).isFoo();

    assertThat(myType);
  }

  public static class CustomAssert extends org.assertj.core.api.AbstractAssert<CustomAssert, MyType> {
    private CustomAssert(MyType actual) { super(actual, CustomAssert.class); }
    public static CustomAssert assertThat(MyType actual) { return new CustomAssert(actual); }
    public CustomAssert isFoo() { return this; }
    public CustomAssert isBar() { return this; }
  }

  public static class MyType { }
}
