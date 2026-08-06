package checks.unused;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.enterprise.event.Observes;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.BaseAction;

class UnusedMethodParameterCheckWS extends BWS {
  void doSomething() { }

  void doSomething(int a, int b) { // Noncompliant

    compute(a);
  }

  void doSomething(int a) {
    compute(a);
  }

  @Override
  void doSomethingElse(int a, int b) {     // no issue reported on b
    compute(a);
  }
}

class BWS {
  void doSomethingElse(int a, int b) {
    compute(a);
    compute(b);
  }
  void compute(int a){
    a++;
  }
}

class CWS extends BWS {
  int bar;
  void doSomethingElse(int a, int b) {     // no issue reported on b
    compute(a);
  }
  void foo(int a) {
    compute(a);
  }
}

class DWS extends CWS {
  void foo(int a, // Noncompliant
           @Nullable Object b,
           int c,
           int d,
           @Nullable Object e) {

    System.out.println(c);
  }
}
class EWS extends CWS {
  void bar(int a){ // Noncompliant

    System.out.println("");
  }
}
interface interWS {
  default void foo(int a) {
    System.out.println(a);
  }
  default void bar(int a) { System.out.println("");} // Compliant - designed for extension
  void qix(int a);
}
class FWS {
  public static void main(String[] args) { }
  public static int main(boolean[] args) { System.out.println(""); return 0; } // Noncompliant
  public static void main(int[] args) { System.out.println("");} // Noncompliant
  public static Object main(long arg) { System.out.println(""); return null; } // Noncompliant
  public static void main(String args) { System.out.println("");} // Noncompliant
  public static void main(Double[] args) { System.out.println("");} // Noncompliant
}

class GWS implements interWS {
  public void foo(int a) {
    System.out.println("plop");
  }

  private void writeObject(ObjectOutputStream out) // Compliant
      throws IOException {
    throw new NotSerializableException(getClass().getName());
  }

  private void readObject(ObjectInputStream in) // Compliant
      throws IOException, ClassNotFoundException {
    throw new NotSerializableException(getClass().getName());
  }

  @Override
  public void qix(int a) {}
}

class OpenForExtensionWS {
  public void foo(int arg) {

  }
  protected void bar(int arg) {

  }
  public void qix(int arg) {
    throw new UnsupportedOperationException("not implemented");
  }

  private void baz(int arg) { // Noncompliant

  }

 // Noncompliant@+1
  private void qiz(int arg1, int arg2) {

  }

  public Supplier<String> parameterNotUsed(final Object o) {
    return o::toString;
  }
}

class MethodFromSerializationWS {
  private void writeObject(ObjectOutputStream out) throws MyException { // Compliant
    throw new MyException();
  }

  private void readObject(ObjectInputStream in) throws MyException { // Compliant
    throw new MyException();
  }

  private static class MyException extends Exception {}
}

class AnnotationsWS {
  public void foo(@Observes Object event, int arg2) { // Compliant
    System.out.println(arg2);
  }

  @MyAnnotationWS
  void qix(int a, int b) { // Compliant
    System.out.println(a);
  }

  @SuppressWarnings("proprietary")
  void unknownWarning(int unused) { // Compliant
  }

  @SuppressWarnings({"rawtypes", "proprietary"})
  void unknownWarningCombinedWithKnown(List list, int unused) { // Compliant
    List<String> strings = (List<String>) list;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  void foobar(List list, int unused) { // Noncompliant

    List<String> strings = (List<String>) list;
  }

  @SuppressWarnings("unchecked")
  void uncheckedFoobar(List<?> list, int unused) { // Noncompliant

    List<String> strings = (List<String>) list;
  }
}

class StrutsActionWS extends Action {
  void foo(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response, String s) { // Compliant
    System.out.println(s);
  }

  void qix(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) { // Compliant
    System.out.println("");
  }

  void qiz(ActionMapping mapping, ActionForm form) { // Compliant
    System.out.println("");
  }
}

class StrutsAction2WS extends BaseAction {

  void bar(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) { // Compliant
    System.out.println("");
  }
}

class NotStrutsActionWS {
  void bar(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) { // Noncompliant
    System.out.println(mapping);
    System.out.println(form);
    System.out.println(request);
    System.out.println("");
  }
}

class DocumentedMethodWS {
  /**
   * @param firstArg proper javadoc description
   * @param secondArg proper javadoc description
   * @param fourthArg proper javadoc description
   */
  void foo(String firstArg, int secondArg, double thirdArg, float fourthArg) { // Noncompliant
    System.out.println();
  }

  /**
   * @param firstArg proper javadoc description
   */
  protected void bar(String firstArg) { // Compliant - parameter has proper javadoc
    System.out.println();
  }

  /**
   * Overridable method, but a proper javadoc description is missing for unused parameter
   * @param firstArg
   */
  public void foobar(String firstArg) { // Noncompliant
    System.out.println();
  }

  /**
   * @param firstArg proper javadoc description
   */
  private void nonOverrideableMethod(String firstArg) { // Noncompliant
    System.out.println();
  }

  /**
   * @param firstArg proper javadoc description
   */
  static void nonOverrideableMethod(int firstArg) { // Noncompliant
    System.out.println();
  }

  /**
   * @param firstArg proper javadoc description
   */
  final void nonOverrideableMethod(Object firstArg) { // Noncompliant
    System.out.println();
  }
}

final class FinalDocumentedMethodWS {
  /**
   * @param firstArg proper javadoc description
   */
  void nonOverrideableMethod(int firstArg) { // Noncompliant
    System.out.println();
  }
}

class ParentWS {
  public void foo(Object param) {
    throw new RuntimeException();
  }
}
final class FinalClassWS extends ParentWS {

  @Override
  public void foo(Object param) { // Compliant

  }

  void barPackage(Object o) { // Noncompliant

  }

  protected void barProtected(Object o) { // Noncompliant

  }

  public void barPublic(Object o) { // Noncompliant

  }

  private void barPrivate(Object o) { // Noncompliant

  }
}

@interface MyAnnotationWS {}

class UnknownUsageWS {
  static class Member {
    private Member(String firstName, String lastName, String memberID) { } // Noncompliant

    public static LastNameBuilder member(String firstName) { // Compliant
      return lastName -> memberID -> new Member(firstName, lastName, memberID);
    }

    @FunctionalInterface
    public interface LastNameBuilder {
      MemberIDBuilder lastName(String lastName);
    }

    @FunctionalInterface
    public interface MemberIDBuilder {
      Member memberID(String memberID);
    }
  }
}

class UsingMethodReferenceWS {

  void foo() {
    java.util.function.Predicate<Object> bar = bar("hello", "world")::equals; // uses 'bar', but not as targeted method reference
    java.util.function.BiFunction<String, String, String> foo = this::bar; // uses 'bar', contract of BiConsumer forces 2 parameters
    bar("hello", "world"); // other irrelevant usage
  }

  private String bar(String a, String b) { // Compliant - used as method reference
    System.out.println(a);
    return a;
  }
}

class JakartaAnnotationsWS {
  void fooBar(int a, // Noncompliant

    @jakarta.annotation.Nullable Boolean b,

    int c,
    int d,

    @jakarta.annotation.Nullable Object e) {

    System.out.println(c);
  }
  public void foo(@jakarta.enterprise.event.Observes Object event, int arg2) { // Compliant
    System.out.println(arg2);
  }
}

class JakartaStrutsActionWS extends Action {
  void foo(ActionMapping mapping, ActionForm form, jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, String s) { // Compliant
    System.out.println(s);
  }

  void qix(ActionMapping mapping, ActionForm form, jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) { // Compliant
    System.out.println("");
  }
}

class JakartaStrutsAction2WS extends BaseAction {

  void bar(ActionMapping mapping, ActionForm form, jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) { // Compliant
    System.out.println("");
  }
}

class JakartaNotStrutsActionWS {
  void bar(ActionMapping mapping, ActionForm form, jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) { // Noncompliant
    System.out.println(mapping);
    System.out.println(form);
    System.out.println(request);
    System.out.println("");
  }
}
