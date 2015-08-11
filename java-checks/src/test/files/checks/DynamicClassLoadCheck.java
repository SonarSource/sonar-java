
class A {

  public void method() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
    String className = "name";
    Class clazz = Class.forName(className);  // Noncompliant

    ClassLoader loader = null;
    Object main = loader.loadClass(className).newInstance(); // Noncompliant
  }

}
