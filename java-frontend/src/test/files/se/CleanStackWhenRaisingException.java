import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.NoSuchMethodException;


class A {

  static boolean foo() {
    if (System.getProperty("com.google.appengine.runtime.environment") == null) {
      return false;
    }
    try {
      // note that this signature is blacklisted and behavior is nor computed for methods from java.lang.Class
      return Class.forName("com.google.apphosting.api.ApiProxy")
        .getMethod("getCurrentEnvironment")
        .invoke(null) != null;
    } catch (ClassNotFoundException e) {
      return false;
    } catch (InvocationTargetException e) {
      return false;
    } catch (IllegalAccessException e) {
      return false;
    } catch (NoSuchMethodException e) {
      return false;
    }
  }


}
