package checks;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.function.Function;
import javax.annotation.Resource;
import javax.inject.Inject;
import org.springframework.beans.factory.annotation.Autowired;

class LeastSpecificTypeCheck {

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

  @Resource
  public void resourceAnnotatedMethod3(List<Object> list) { // Noncompliant {{Use 'java.util.Collection' here; it is a more general type than 'List'.}}
    for (Object o : list) {
      o.toString();
    }
  }

  @Inject
  public void injectAnnotatedMethod1(List<Object> list) { // Noncompliant {{Use 'java.util.Collection' here; it is a more general type than 'List'.}}
    for (Object o : list) {
      o.toString();
    }
  }

  @Autowired
  public void autowiredMethod1(List<Object> list) { // Noncompliant {{Use 'java.util.Collection' here; it is a more general type than 'List'.}}
    for (Object o : list) {
      o.toString();
    }
  }

  @Resource
  public void resourceAnnotatedMethod4(Collection<Object> list) { // Compliant - since Spring annotated methods cannot take 'Iterable' as argument
    for (Object o : list) {
      o.toString();
    }
  }

  @Inject
  public void injectAnnotatedMethod2(Collection<Object> list) { // Compliant - since Spring annotated methods cannot take 'Iterable' as argument
    for (Object o : list) {
      o.toString();
    }
  }

  @Autowired
  public void autowiredMethod2(Collection<Object> list) { // Compliant - since Spring annotated methods cannot take 'Iterable' as argument
    for (Object o : list) {
      o.toString();
    }
  }

  @Autowired
  public void autowiredMethod3(List<Object> list) { // Compliant - List interface is used in method body
    list.sort(Comparator.comparingInt(Object::hashCode));
  }

  @Autowired
  public void autowiredMethod4(List<Object> list) { // Noncompliant {{Use 'java.util.Collection' here; it is a more general type than 'List'.}}
    list.size();
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

  public interface IA {
    default void a() {
    }
  }

  public interface IB {
    default void b() {
    }
  }

  public class S implements IA, IB {

  }

  public class C extends S {
  }

  public void test4(C arg) { // Noncompliant {{Use 'checks.LeastSpecificTypeCheck.S' here; it is a more general type than 'C'.}}
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

  public interface IBase {
    void b2();
  }

  abstract static class Base implements IBase {
    public abstract void b();
  }

  protected static class ProtectedBase extends Base {
    @Override
    public void b2() { }

    @Override
    public void b() { }
  }

  public static class Visibility extends ProtectedBase {

  }

  public static void visibility(Visibility vis) { // Compliant - Base has package visibility
    vis.b2();
    vis.b();
  }

  protected static class Visibility2 extends ProtectedBase { }

  public static void visibility(Visibility2 vis) { // False-Negative
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

  private static class PrivateClass implements IBase {
    void m2() {}
    @Override
    public void b2() { }
  }

  protected static class ProtectedClass extends PrivateClass {

  }

  static class PackageClass extends ProtectedClass {

  }

  public static void coverage(PrivateClass c) { // Noncompliant  {{Use 'checks.LeastSpecificTypeCheck.IBase' here; it is a more general type than 'PrivateClass'.}}
    c.b2();
  }

  public static void coverage2(PackageClass c) {
    c.m2();
  }

  public static void primitiveTypesAreIgnored(int i, long l, double d, float f, byte b, short s, char c, boolean boo) { }

  public BigDecimal getUnaryOperator(UnaryOperator<BigDecimal> func) { // Compliant
    // issue exception because UnaryOperator<BigDecimal> is a better functional interface usage than Function<BigDecimal, BigDecimal>
    return func.apply(BigDecimal.ONE);
  }

  public BigDecimal getFunction(Function<BigDecimal, BigDecimal> func) { // S4276 issue to promote UnaryOperator<BigDecimal>
    return func.apply(BigDecimal.ONE);
  }

}
