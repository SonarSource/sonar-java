import java.util.*;

class A {

  public void test1(ArrayList<Object> list) { // Noncompliant {{Use 'java.util.Collection' here; it is a more general type than 'ArrayList'.}}
    System.out.println(list.size());
  }

  void testNotPublic(ArrayList<Object> list) { // Compliant - not public
    System.out.println(list.size());
  }

  public void test2(ArrayList<Object> list) { // Noncompliant {{Use 'java.util.List' here; it is a more general type than 'ArrayList'.}}
    list.sort(Comparator.comparingInt(Object::hashCode));
  }

  public void test3(ArrayList<Object> list) { // Compliant
    list.trimToSize();
  }

  public void dontSuggestObject(List list) { // Compliant
    list.equals("Test");
  }

  public void ignoreStringParams(String s) { // Compliant
    s.charAt(0);
  }

  public static void staticMethod(List<Object> list) { // Noncompliant {{Use 'java.util.Collection' here; it is a more general type than 'List'.}}
    list.size();
  }

  public static List returnArg(List list) { // Compliant
    if (list.size() > 0) {
      return list;
    }
    return null;
  }

  interface IA {
    default void a() {
    }
  }

  interface IB {
    default void b() {
    }
  }

  class S implements IA, IB {

  }

  class C extends S {
  }

  public void test4(C arg) { // Noncompliant {{Use 'A.S' here; it is a more general type than 'C'.}}
    arg.a();
    arg.b();
  }

  interface ITest {
    int doIt(List<Object> list);
  }

  class Test implements ITest {

    @Override
    public int doIt(List<Object> list) { // Compliant - overrides are ignored
      return list.size();
    }
  }

  public static void passedAsArgument(List<Integer> list) { // Compliant
    Integer min = Collections.min(list);
  }

  abstract static class Base {
    public abstract void b();
  }

  protected static class ProtectedBase extends Base {

  }

  public static class Visibility extends ProtectedBase {

  }

  public static void visibility(Visibility vis) { // Compliant - Base has package visibility
    vis.b();
  }

  protected static class Visibility2 extends ProtectedBase { }

  public static void visibility(Visibility2 vis) { // // Noncompliant {{Use 'A.ProtectedBase' here; it is a more general type than 'Visibility2'.}}
    vis.b();
  }

  enum MyEnum {
    A
  }

  public static void testEnum(MyEnum myEnum) {
    myEnum.name();
  }

  public static void enhancedForLoop(List<Object> list) { // Noncompliant {{Use 'java.lang.Iterable' here; it is a more general type than 'List'.}}
    for (Object o : list) {
      o.toString();
    }
  }

  class BaseField {
    public Object field;
  }

  class E extends BaseField {

  }

  public static void fieldsAreIgnored(E e) { // Compliant
    e.field.toString();
  }

  interface II1 {}

  interface I1 extends II1 {
    void m();
  }

  interface I2 extends I1 {
    void m();
  }

  static class T1 {
    void m() {}
    void mt1() {}
  }

  static class T2 extends T1 implements I2 {
    void ma();
  }

  interface IMB {
    void ma();
  }

  interface IMA {
    void ma();
  }

  static class T extends T2 implements IMB, IMA {}

  public static void bla(T t) { // Noncompliant  {{Use 'A.I1' here; it is a more general type than 'T'.}}
    t.m();
  }

  public static void foo(T t) { // Noncompliant  {{Use 'A.T1' here; it is a more general type than 'T'.}}
    t.m();
    t.mt1();
  }

  public static void meh(T t) { // Noncompliant  {{Use 'A.IMA' here; it is a more general type than 'T'.}}
    // defined in both T2 and IMA, interface is preferred
    t.ma();
  }

  interface IG<T> {
    T get();
  }

  abstract class GImpl implements IG<GImpl> {
    GImpl get();
  }

  class GImplSub extends GImpl {

  }

  public static void generics(GImplSub s) { // Noncompliant  {{Use 'A.GImpl' here; it is a more general type than 'GImplSub'.}}
    s.get();
  }

  class Generic<T> implements IG<T> {

  }

  public static void generics2(Generic<Object> g) { // Noncompliant  {{Use 'A.IG' here; it is a more general type than 'Generic'.}}
    g.get();
  }


}
