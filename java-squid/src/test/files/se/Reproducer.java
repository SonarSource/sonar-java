package javax.annotation;
import javax.annotation.CheckForNull;

@interface CheckForNull {}

@interface Nonnull {}

@interface Nullable {}

class A {
  public void testCheckNotNull2(@CheckForNull Object parameter) {
    String x = a == b ? foo(a) : foo(b);
  }
  public void testCheckNotNull(@CheckForNull Object parameter) {
    int i;
    Object o;

    Object[] array1 = checkForNullField;
    i = array1.length; // False negative

    i = checkForNullField.length; // False negative, instance and static fields are not checked

    Object[] array2 = checkForNullMethod();
    i = array2.length; // Noncompliant 2 {{NullPointerException might be thrown as 'array2' is nullable here}}
  }

  @CheckForNull
  public Object[] checkForNullMethod() {
    return null;
  }

}