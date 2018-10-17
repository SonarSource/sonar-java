class A {

  public void method() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
    String className = System.getProperty("messageClassName");
    Class clazz = Class.forName(className);  // Noncompliant [[sc=25;ec=32]]

    ClassLoader loader = null;
    Object main = loader.loadClass(className).newInstance(); // Noncompliant
  }

  public static void main(String[] args) {
    try {
      Class cls1 = Class.forName("java.lang.ClassLoader");
      // test with a constant from JDK because this test file is not compiled and constants evaluation needs .class
      Class cls2 = Class.forName(java.util.jar.JarFile.MANIFEST_NAME);
    } catch (ClassNotFoundException ex) {
    }
  }
}
