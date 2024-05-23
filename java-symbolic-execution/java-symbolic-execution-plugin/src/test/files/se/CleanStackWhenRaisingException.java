import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.NoSuchMethodException;


class A {

  static boolean foo() {
    if (System.getProperty("com.google.appengine.runtime.environment") == null) {
      return false;
    }
    try {
      // note that this signature is blacklisted and behavior is nor computed for methods from java.lang.Class
      // java.lang.Class.forName(String) throws ClassNotFoundException
      return Class.forName("com.google.apphosting.api.ApiProxy")
        // java.lang.Class.getMethod(String, Class<?>...) throws NoSuchMethodException, SecurityException
        .getMethod("getCurrentEnvironment")
        // [JDK17-] java.lang.reflect.Method.invoke(Object, Object...) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
        // [JDK18+] java.lang.reflect.Method.invoke(Object, Object...) throws IllegalAccessException, InvocationTargetException
        .invoke(null) != null;
    } catch (ClassNotFoundException e) {
      // thrown by forName
      return false;
    } catch (InvocationTargetException e) {
      // thrown by invoke
      return false;
    } catch (IllegalAccessException e) {
      // thrown by invoke
      return false;
    } catch (NoSuchMethodException e) {
      // thrown by getMethod
      return false;
    }
  }


}
