package checks.tests;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import rx.Observable;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;

public class TooManyAssertionsCheckCustom2 {

  @Test
  void test1() { // Noncompliant {{Refactor this method to reduce the number of assertions from 3 to less than 2.}}
//     ^^^^^
    assertEquals(1, f(1));
//  ^^^^^^^^^^^^^^^^^^^^^<
    assertEquals(2, f(2));
//  ^^^^^^^^^^^^^^^^^^^^^<
    assertEquals(3, f(3));
//  ^^^^^^^^^^^^^^^^^^^^^<
    Observable<Object> objectObservable = Observable.create(null, null);
    objectObservable.test();
  }

  @Test
  void test2() { // Compliant
    assertEquals(2, f(2));
    assertEquals(3, f(1));
  }

  @Test
  void test3() { // Noncompliant {{Refactor this method to reduce the number of assertions from 3 to less than 2.}}
//     ^^^^^
    assertEquals(2, f(2));
//  ^^^^^^^^^^^^^^^^^^^^^<
    assertEquals(3, f(1));
//  ^^^^^^^^^^^^^^^^^^^^^<
    customAssert();
//  ^^^^^^^^^^^^^^<
  }

  @Test
  void test4() { // Compliant, chained assertions only count as a single instance
    var trueCondition = true;
    var falseCondition = false;

    assertThat(trueCondition).isTrue().as("true").isTrue().describedAs("isTrue").isTrue();
    assertThat(falseCondition).isFalse().as("false").isFalse().describedAs("isFalse").isFalse();
  }

  @Test
  void test5() { // Compliant, chained assertions only count as a single instance
    var myObject = new Object();

    Assertions.assertThat(myObject).as("someObject").isNotNull().withFailMessage("fail").isNotNull().overridingErrorMessage("error").isInstanceOf(Object.class);
  }

  @Test
  void test6() { // Noncompliant {{Refactor this method to reduce the number of assertions from 3 to less than 2.}}
//     ^^^^^
    var myObject = new Object();

    assertThat(myObject).as("description").isNotNull().describedAs("description").isNotNull();
//  ^^^^^^^^^^^^^^^^^^^^<
    assertThat(myObject).describedAs("description").isNotNull();
//  ^^^^^^^^^^^^^^^^^^^^<
    assertThat(myObject).withFailMessage("failure").describedAs("someObject").isNotNull();
//  ^^^^^^^^^^^^^^^^^^^^<
  }

  void customAssert() {
    assertEquals(2, f(2));
    assertEquals(3, f(1));
  }

  int f(int x) {
    return x;
  }

}
