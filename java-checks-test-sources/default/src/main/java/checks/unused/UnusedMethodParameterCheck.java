package checks.unused;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.enterprise.event.Observes;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.BaseAction;

class UnusedMethodParameterCheck extends B {
  void doSomething() { }

  void doSomething(int a, int b) { // Noncompliant {{Remove this unused method parameter "b".}} [[quickfixes=qf_1_1]]
//                            ^
    // fix@qf_1_1 {{Remove "b"}}
    // edit@qf_1_1 [[sc=25;ec=32]] {{}}
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

class B {
  void doSomethingElse(int a, int b) {
    compute(a);
    compute(b);
  }
  void compute(int a){
    a++;
  }
}

class C extends B {
  int bar;
  void doSomethingElse(int a, int b) {     // no issue reported on b
    compute(a);
  }
  void foo(int a) {
    compute(a);
  }
}

class D extends C {
  void foo(int a, // Noncompliant {{Remove these unused method parameters "a", "b", "d", "e".}} [[quickfixes=qf_2_1,qf_2_2,qf_2_3,qf_2_4]]
           @Nullable Object b,
           int c,
           int d,
           @Nullable Object e) {
//             ^@-4
//                          ^@-4<
//             ^@-3<
//                          ^@-3<
    // fix@qf_2_1 {{Remove "a"}}
    // edit@qf_2_1 [[sl=+0;sc=12;el=+1;ec=12]] {{}}
    // fix@qf_2_2 {{Remove "b"}}
    // edit@qf_2_2 [[sl=+1;sc=12;el=+2;ec=12]] {{}}
    // fix@qf_2_3 {{Remove "d"}}
    // edit@qf_2_3 [[sl=+3;sc=12;el=+4;ec=12]] {{}}
    // fix@qf_2_4 {{Remove "e"}}
    // edit@qf_2_4 [[sl=+3;sc=17;el=+4;ec=30]] {{}}
    System.out.println(c);
  }
}
class E extends C {
  void bar(int a){ // Noncompliant {{Remove this unused method parameter "a".}} [[quickfixes=qf_3_1]]
//             ^
    // fix@qf_3_1 {{Remove "a"}}
    // edit@qf_3_1 [[sc=12;ec=17]] {{}}
    System.out.println("");
  }
}
interface inter {
  default void foo(int a) {
    System.out.println(a);
  }
  default void bar(int a) { System.out.println("");} // Compliant - designed for extension
  void qix(int a);
}
class F {
  public static void main(String[] args) { }
  public static int main(boolean[] args) { System.out.println(""); return 0; } // Noncompliant
  public static void main(int[] args) { System.out.println("");} // Noncompliant
  public static Object main(long arg) { System.out.println(""); return null; } // Noncompliant
  public static void main(String args) { System.out.println("");} // Noncompliant
  public static void main(Double[] args) { System.out.println("");} // Noncompliant
}

class G implements inter {
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

class OpenForExtension {
  public void foo(int arg) {
    //no-op
  }
  protected void bar(int arg) {
    //no-op
  }
  public void qix(int arg) {
    throw new UnsupportedOperationException("not implemented");
  }

  private void baz(int arg) { // Noncompliant
    //no-op
  }

 // Noncompliant@+1
  private void qiz(int arg1, int arg2) {

  }

  public Supplier<String> parameterNotUsed(final Object o) {
    return o::toString;
  }
}

class MethodFromSerialization {
  private void writeObject(ObjectOutputStream out) throws MyException { // Compliant
    throw new MyException();
  }

  private void readObject(ObjectInputStream in) throws MyException { // Compliant
    throw new MyException();
  }

  private static class MyException extends Exception {}
}

class Annotations {
  public void foo(@Observes Object event, int arg2) { // Compliant
    System.out.println(arg2);
  }

  public void bar(@Nonnull Object event, int arg2) { // Noncompliant {{Remove this unused method parameter "event".}}
//                                ^^^^^
    System.out.println(arg2);
  }

  @MyAnnotation
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
  void foobar(List list, int unused) { // Noncompliant {{Remove this unused method parameter "unused".}}
//                           ^^^^^^
    List<String> strings = (List<String>) list;
  }

  @SuppressWarnings("unchecked")
  void uncheckedFoobar(List<?> list, int unused) { // Noncompliant {{Remove this unused method parameter "unused".}}
//                                       ^^^^^^
    List<String> strings = (List<String>) list;
  }
}

class StrutsAction extends Action {
  void foo(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response, String s) { // Compliant
    System.out.println(s);
  }

  void bar(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response, String unused) { // Noncompliant {{Remove this unused method parameter "unused".}}
    System.out.println("");
  }

  void qix(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) { // Compliant
    System.out.println("");
  }

  void qiz(ActionMapping mapping, ActionForm form) { // Compliant
    System.out.println("");
  }
}

class StrutsAction2 extends BaseAction {
  void foo(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response, String unused) { // Noncompliant {{Remove this unused method parameter "unused".}}
    System.out.println("");
  }

  void bar(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) { // Compliant
    System.out.println("");
  }

  void qiz(ActionMapping mapping, ActionForm form, HttpServletRequest request, Object unusedResponse) { // Noncompliant {{Remove this unused method parameter "unusedResponse".}}
    System.out.println("");
  }
}

class NotStrutsAction {
  void bar(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) { // Noncompliant {{Remove this unused method parameter "response".}}
    System.out.println(mapping);
    System.out.println(form);
    System.out.println(request);
    System.out.println("");
  }
}

class DocumentedMethod {
  /**
   * @param firstArg proper javadoc description
   * @param secondArg proper javadoc description
   * @param fourthArg proper javadoc description
   */
  void foo(String firstArg, int secondArg, double thirdArg, float fourthArg) { // Noncompliant {{Remove this unused method parameter "thirdArg".}}
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
  private void nonOverrideableMethod(String firstArg) { // Noncompliant {{Remove this unused method parameter "firstArg".}}
    System.out.println();
  }

  /**
   * @param firstArg proper javadoc description
   */
  static void nonOverrideableMethod(int firstArg) { // Noncompliant {{Remove this unused method parameter "firstArg".}}
    System.out.println();
  }

  /**
   * @param firstArg proper javadoc description
   */
  final void nonOverrideableMethod(Object firstArg) { // Noncompliant {{Remove this unused method parameter "firstArg".}}
    System.out.println();
  }
}

final class FinalDocumentedMethod {
  /**
   * @param firstArg proper javadoc description
   */
  void nonOverrideableMethod(int firstArg) { // Noncompliant {{Remove this unused method parameter "firstArg".}}
    System.out.println();
  }
}

class Parent {
  public void foo(Object param) {
    throw new RuntimeException();
  }
}
final class FinalClass extends Parent {

  @Override
  public void foo(Object param) { // Compliant
    // do nothing
  }

  void barPackage(Object o) { // Noncompliant
    // do something
  }

  protected void barProtected(Object o) { // Noncompliant
    // do something
  }

  public void barPublic(Object o) { // Noncompliant
    // do something
  }

  private void barPrivate(Object o) { // Noncompliant
    // do something
  }
}

@interface MyAnnotation {}

class UnknownUsage {
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

class UsingMethodReference {

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

class JakartaAnnotations {
  void fooBar(int a, // Noncompliant {{Remove these unused method parameters "a", "b", "d", "e".}}
//                ^
    @jakarta.annotation.Nullable Boolean b,
//                                       ^<
    int c,
    int d,
//      ^<
    @jakarta.annotation.Nullable Object e) {
//                                      ^<
    System.out.println(c);
  }
  public void foo(@jakarta.enterprise.event.Observes Object event, int arg2) { // Compliant
    System.out.println(arg2);
  }

  public void bar(@jakarta.annotation.Nonnull Object event, int arg2) { // Noncompliant {{Remove this unused method parameter "event".}}
//                                                   ^^^^^
    System.out.println(arg2);
  }
}


class JakartaStrutsAction extends Action {
  void foo(ActionMapping mapping, ActionForm form, jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, String s) { // Compliant
    System.out.println(s);
  }

  void bar(ActionMapping mapping, ActionForm form, jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, String unused) { // Noncompliant {{Remove this unused method parameter "unused".}}
    System.out.println("");
  }

  void qix(ActionMapping mapping, ActionForm form, jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) { // Compliant
    System.out.println("");
  }
}

class JakartaStrutsAction2 extends BaseAction {
  void foo(ActionMapping mapping, ActionForm form, jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, String unused) { // Noncompliant {{Remove this unused method parameter "unused".}}
    System.out.println("");
  }

  void bar(ActionMapping mapping, ActionForm form, jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) { // Compliant
    System.out.println("");
  }
}

class JakartaNotStrutsAction {
  void bar(ActionMapping mapping, ActionForm form, jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) { // Noncompliant {{Remove this unused method parameter "response".}}
    System.out.println(mapping);
    System.out.println(form);
    System.out.println(request);
    System.out.println("");
  }
}
