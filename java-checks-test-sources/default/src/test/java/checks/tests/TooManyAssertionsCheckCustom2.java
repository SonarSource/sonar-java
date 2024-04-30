package checks.tests;

import org.junit.jupiter.api.Test;
import rx.Observable;

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

  void customAssert() {
    assertEquals(2, f(2));
    assertEquals(3, f(1));
  }

  int f(int x) {
    return x;
  }

}
