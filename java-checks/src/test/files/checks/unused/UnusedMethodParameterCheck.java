import javax.annotation.Nonnull;
import javax.enterprise.event.Observes;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForm;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.Action;
import org.apache.struts.actions.BaseAction;

class A extends B{
  void doSomething(int a, int b) { // Noncompliant {{Remove this unused method parameter "b".}} [[sc=31;ec=32]]
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
  void foo(int b, int a) { // Noncompliant {{Remove this unused method parameter "b".}} [[sc=16;ec=17;secondary=51]]
    System.out.println("");
  }
}
class E extends C {
  void bar(int a){ // Noncompliant
    System.out.println("");
  }
}
interface inter {
  default void foo(int a) {
    compute(a);
  }
  default void bar(int a) { System.out.println("");} // Compliant - designed for extension
  void qix(int a);
}
class F {
  public static void main(String[] args) { }
  public static int main(String[] args) { System.out.println("");} // Noncompliant
  public static void main(int[] args) { System.out.println("");} // Noncompliant
  public static Object main(String[] args) { System.out.println("");} // Noncompliant
  public static void main(String args) { System.out.println("");} // Noncompliant
  public static void main(Double[] args) { System.out.println("");} // Noncompliant
}

class G implements inter {
  void foo(int a) {
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
}

class OpenForExtension {
  public foo(int arg) {
    //no-op
  }
  protected bar(int arg) {
    //no-op
  }
  public void qix(int arg) {
    throw new UnsupportedOperationException("not implemented");
  }

  private baz(int arg) { // Noncompliant
    //no-op
  }

  // Noncompliant@+1
  private qiz(int arg1, int arg2) {

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

  public void bar(@Nonnull Object event, int arg2) { // Noncompliant {{Remove this unused method parameter "event".}} [[sc=35;ec=40]]
    System.out.println(arg2);
  }

  @MyAnnotation
  void qix(int a, int b) { // Compliant
    compute(a);
  }

  @SuppressWarnings("proprietary")
  void unknownWarning(int unused) { // Compliant
  }

  @SuppressWarnings({"rawtypes", "proprietary"})
  void unknownWarningCombinedWithKnown(List list, int unused) { // Compliant
    List<String> strings = (List<String>) list;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  void foobar(List list, int unused) { // Noncompliant {{Remove this unused method parameter "unused".}} [[sc=30;ec=36]]
    List<String> strings = (List<String>) list;
  }

  @SuppressWarnings("unchecked")
  void uncheckedFoobar(List<?> list, int unused) { // Noncompliant {{Remove this unused method parameter "unused".}} [[sc=42;ec=48]]
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

  void qiz(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpFakeResponse unusedResponse) { // Noncompliant {{Remove this unused method parameter "unusedResponse".}}
    System.out.println("");
  }
}

class NotStrutsAction {
  void bar(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) { // Noncompliant {{Remove this unused method parameter "response".}}
    doSomething(mapping);
    doSomething(form);
    doSomething(request);
    System.out.println("");
  }
}

@interface MyAnnotation {}
