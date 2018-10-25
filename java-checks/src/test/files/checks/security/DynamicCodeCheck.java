
class Reflection {

  public static void run(java.lang.ClassLoader loader, String className, String methodName, String fieldName,
                         Class<?> parameterTypes)
    throws NoSuchMethodException, SecurityException, ClassNotFoundException, NoSuchFieldException {

    Class<?> clazz = Class.forName(className); // Noncompliant [[sc=28;ec=35]] {{Make sure that this dynamic injection or execution of code is safe.}}
    clazz.getMethod(methodName, parameterTypes); // Noncompliant
    clazz.getMethods(); // Noncompliant
    clazz.getField(fieldName); // Noncompliant
    clazz.getFields(); // Noncompliant
    clazz.getDeclaredField(fieldName); // Noncompliant
    clazz.getDeclaredFields(); // Noncompliant
    clazz.getDeclaredClasses(); // Noncompliant

    loader.loadClass(className); // Noncompliant

    // Exceptions
    loader.loadClass("MyClassName");
    clazz.getField("MyClassName");
  }
}
