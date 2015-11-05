package javax.annotation;
import javax.annotation.CheckForNull;
import java.lang.Object;

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
    i = array2.length; // Noncompliant {{NullPointerException might be thrown as 'array2' is nullable here}}
  }

  void testArrayAccess() {
    Object[] foo = new Object[10];
    if (foo[0] == null) {

    }
  }

  @CheckForNull
  public Object[] checkForNullMethod() {
    return null;
  }

  private boolean shouldClosePendingTags(CharactersReader charactersReader) {
    return charactersReader.getCurrentValue() == CR_END_OF_LINE
        || (charactersReader.getCurrentValue() == LF_END_OF_LINE && charactersReader.getPreviousValue() != CR_END_OF_LINE)
        || (charactersReader.getCurrentValue() == CharactersReader.END_OF_STREAM && charactersReader.getPreviousValue() != LF_END_OF_LINE);
  }
}