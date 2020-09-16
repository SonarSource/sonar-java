package checks.tests;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static checks.tests.MockingAllMethodsCheck_Helper.staticMockedObject;

public class MockingAllMethodsCheck {

  @Test
  void test_mocking_MyClass() {
    MyClass myClassMock = mock(MyClass.class); // Noncompliant [[sc=5;ec=47;secondary=12,13,14]] {{Refactor this test instead of mocking every non-private member of this class.}}
    when(myClassMock.f()).thenReturn(1);
    when(myClassMock.g()).thenReturn(2);
    when(myClassMock.h()).thenReturn(3);
    //...
  }

  @Test
  void test_mocking_MyClass_with_only_two_stubs() {
    MyClass myClassMock = mock(MyClass.class); // Compliant because only two out of the three methods are mocked
    when(myClassMock.f()).thenReturn(1);
    when(myClassMock.g()).thenReturn(2);
    //...
  }

  @Test
  void test_mocking_MyClass3_with_different_instances() {
    MyClass myClassMock = mock(MyClass.class); // Compliant because f and g/h are mocked on different objects
    when(myClassMock.f()).thenReturn(1);
    //...
    MyClass myClassMock2 = mock(MyClass.class);
    when(myClassMock2.g()).thenReturn(2);
    when(myClassMock2.h()).thenReturn(3);
    //...
  }

  abstract class MyClass {
    abstract int f();
    abstract int g();
    abstract int h();
  }


  @Test
  void test_mocking_MyConcreteClass() {
    MyConcreteClass myClassMock = mock(MyConcreteClass.class); // Noncompliant [[secondary=47,48]]
    when(myClassMock.f()).thenReturn(1);
    when(myClassMock.g()).thenReturn(2);
    //...
  }

  class MyConcreteClass {
    int x;

    MyConcreteClass() {
    }

    int f() {
      return 42;
    }

    int g() {
      return 23;
    }
  }

  @Test
  void test_mocking_MyClass2() {
    MyClass2 myClassMock = mock(MyClass2.class); // Compliant because we don't consider classes with only one non-private method
    when(myClassMock.f()).thenReturn(1);
    //...
  }


  abstract class MyClass2 {
    abstract int f();
  }

  @Test
  void test_mocking_MyClass3() {
    MyClass3 myClassMock = mock(MyClass3.class); // Compliant because we don't consider classes with only one non-private method
    when(myClassMock.f()).thenReturn(1);
    //...
  }

  abstract class MyClass3 {
    abstract int f();
    private int g() {
      return 42;
    }
  }

  @Test
  void test_mocking_MyClass4() {
    MyClass4 myClassMock = mock(MyClass4.class); // Noncompliant [[secondary=96,97]]
    when(myClassMock.f()).thenReturn(1);
    when(myClassMock.g()).thenReturn(2);
    //...
  }

  abstract static class MyClass4 {
    abstract int f();
    abstract int g();
    private int h() {
      return 42;
    }
  }

  // Unsupported cases

  @Test
  void test_mocking_MyClass4_weirdly() {
    MyClass4 myClassMock = mock(MyClass4.class); // FN because we only consider mocks when the argument to `when` is a method call
    myClassMock.f();
    when(42).thenReturn(1);
    myClassMock.g();
    when(23).thenReturn(2);
    //...
  }

  void test_mocking_MyClass4_weirdly2() {
    MyClass4 myClassMock = mock(MyClass4.class); // FN because we only consider mocks when the argument to `when` is a method call
    when(myClassMock.f() + 42).thenReturn(1);
    when(myClassMock.g() - 23).thenReturn(2);
    //...
  }

  void test_mocking_MyClass4_extremely_weirdly() {
    // FP because we falsely make the analysis think that realInstance's methods are being mocked
    MyConcreteClass realInstance = new MyConcreteClass(); // Noncompliant [[secondary=132,134]]
    mock(MyClass4.class).f();
    when(realInstance.f()).thenReturn(1);
    mock(MyClass4.class).f();
    when(realInstance.g()).thenReturn(2);
    //...
  }

  @Test
  void test_mocking_MyClass4_through_multiple_variables() {
    MyClass4 myClassMock = mock(MyClass4.class); // FN because we don't track the same mock object being assigned to multiple variables
    when(myClassMock.f()).thenReturn(1);
    MyClass4 myClassMock2 = myClassMock;
    when(myClassMock2.g()).thenReturn(2);
    //...
  }

  void mockF(MyClass4 myClassMock) {
    when(myClassMock.f()).thenReturn(1);
  }

  void mockG(MyClass4 myClassMock) {
    when(myClassMock.g()).thenReturn(2);
  }

  int callF(MyClass4 myClassMock) {
    return myClassMock.f();
  }

  int callG(MyClass4 myClassMock) {
    return myClassMock.g();
  }

  MyClass4 myClassMockField;

  MyClass4 getMyClass4MockField() {
    return myClassMockField;
  }

  @Test
  void test_mocking_MyClass4_through_separate_methods() {
    MyClass4 myClassMock = mock(MyClass4.class); // FN because we don't track mocking through separate methods
    mockF(myClassMock);
    mockG(myClassMock);

    MyClass4 myClassMock2 = mock(MyClass4.class); // FN because we don't track method calls through separate methods
    when(callF(myClassMock2)).thenReturn(1);
    when(callG(myClassMock2)).thenReturn(2);

    myClassMockField = mock(MyClass4.class); // FN because we don't track the value of `myClassMockField` through the getter calls
    when(getMyClass4MockField().f()).thenReturn(1);
    when(getMyClass4MockField().g()).thenReturn(2);
  }

  @Test
  void test_mocking_MyClass4_through_a_mutable_variable() {
    // FP because we don't recognize that `myClassMock` is reassigned
    MyClass4 myClassMock = mock(MyClass4.class); // Noncompliant [[secondary=188,190]]
    when(myClassMock.f()).thenReturn(1);
    myClassMock = mock(MyClass4.class);
    when(myClassMock.g()).thenReturn(2);
  }

  @Test
  void test_mocking_MyClass4_through_static_member() {
    // FN because staticMockedObject is defined somewhere else
    when(staticMockedObject.f()).thenReturn(1);
    when(staticMockedObject.g()).thenReturn(2);
  }

}
