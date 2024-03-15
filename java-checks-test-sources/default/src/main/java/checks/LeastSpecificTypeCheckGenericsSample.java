package checks;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;

public class LeastSpecificTypeCheckGenericsSample {

  public static class Parent {
    public Parent getMe() {
      return this;
    }

    public Number getNumber() {
      return 23;
    }

    public <T> T getT() {
      return null;
    }

    public <T> List<T> getList() {
      return null;
    }

    public List<? extends Number> getListWithBoundedType() {
      return null;
    }

    public <T extends Number> List<T> getListWithBoundedGeneric() {
      return null;
    }

    public List<? extends List<? extends Number>> getListOfListWithBoundedType() {
      return null;
    }

    public <T> List<List<T>> getListOfListWithGeneric() {
      return null;
    }

    public <K, V> Map<K, V> getMap() {
      return null;
    }

    public <V> Map<String, V> getMapPartiallyGeneric() {
      return null;
    }

    public Number[] getNumberArray() {
      return null;
    }

    public <T> T[] getArray() {
      return null;
    }
  }

  public static class Child extends Parent {
    @Override
    public Child getMe() {
      return this;
    }

    @Override
    public Integer getNumber() {
      return 42;
    }

    @Override
    public Object getT() {
      return null;
    }

    @Override
    public List<String> getList() {
      return null;
    }

    @Override
    public List<Integer> getListWithBoundedType() {
      return null;
    }

    @Override
    public List<Integer> getListWithBoundedGeneric() {
      return null;
    }

    @Override
    public List<List<Integer>> getListOfListWithBoundedType() {
      return null;
    }

    public List<List<String>> getListOfListWithGeneric() {
      return null;
    }

    @Override
    public Map<String, Integer> getMap() {
      return null;
    }

    @Override
    public Map<String, Integer> getMapPartiallyGeneric() {
      return null;
    }

    @Override
    public Integer[] getNumberArray() {
      return null;
    }

    @Override
    public String[] getArray() {
      return null;
    }

    public void doSomethingOnlyOnB() {
    }
  }

  public Child getChild(Child child) {
    return child.getMe();
  }

  public Parent getParent(Parent b) { // Compliant
    return b.getMe();
  }

  public Parent getParentFromChild(Child child) { // Complaint
    child.doSomethingOnlyOnB();
    return child.getMe();
  }

  public Parent getParentFromChild2(Child child) { // FN
    return child.getMe();
  }

  public Integer getNumber(Child child) { // Compliant
    return child.getNumber();
  }

  public Number getNumber2(Child child) { // FN
    return child.getNumber();
  }

  public Object testGetT(Child child) { // Noncompliant {{Use 'checks.LeastSpecificTypeCheckGenericsSample.Parent' here; it is a more general type than 'Child'.}}
    return child.getT();
  }

  public List<String> testGetList(Child child) { // Noncompliant
    return child.getList();
  }

  public Map<String, Integer> testGetMap(Child child) { // Noncompliant
    return child.getMap();
  }

  public Map<String, Integer> testGetMapPartiallyGeneric(Child child) { // FN
    return child.getMapPartiallyGeneric();
  }

  public Integer[] testGetNumberArray(Child child) { // Compliant
    return child.getNumberArray();
  }

  public String[] testGetArray(Child child) { // FN can't distinguish Array Generic Types
    return child.getArray();
  }

  public List<Integer> testListWithBoundedType(Child child) { // Compliant
    return child.getListWithBoundedType();
  }

  public List<? extends Number> testListWithBoundedType2(Child child) { // FN
    return child.getListWithBoundedType();
  }

  public List<Integer> testListWithBoundedGeneric(Child child) { // Noncompliant
    return child.getListWithBoundedGeneric();
  }

  public List<List<Integer>> testListOfListWithBoundedType(Child child) { // Compliant
    return child.getListOfListWithBoundedType();
  }

  public List<List<String>> testListOfListWithGeneric(Child child) { // Noncompliant
    return child.getListOfListWithGeneric();
  }

  public void paramsErrorMessage(Class clazz) { // Noncompliant {{Use 'java.lang.reflect.AnnotatedElement' here; it is a more general type than 'Class'.}}
    clazz.getAnnotation(Resource.class);
  }

  public static void testConstructor(Constructor constructor) { // FN can't distinguish Array Generic Types
    constructor.getName();
    Class[] parameterTypes = constructor.getParameterTypes();
  }
}
