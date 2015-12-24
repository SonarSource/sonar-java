
class A {

  public void method() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
    String className = "name";
    Class clazz = Class.forName(className);  // Noncompliant [[sc=25;ec=32]]

    ClassLoader loader = null;
    Object main = loader.loadClass(className).newInstance(); // Noncompliant
  }

}
