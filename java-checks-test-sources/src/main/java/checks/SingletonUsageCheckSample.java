package checks;

import java.io.Serial;

// TODO: check code snippet license
public class SingletonUsageCheckSample {

  public static class EagerInitializedSingleton { // Noncompliant [[sc=23;ec=48;secondary=+2]]

    private static final EagerInitializedSingleton instance = new EagerInitializedSingleton();

    // private constructor to avoid client applications using the constructor
    private EagerInitializedSingleton(){}

    public static EagerInitializedSingleton getInstance() {
      return instance;
    }

    public boolean foo() {
      return false;
    }
  }

  public static class StaticBlockSingleton { // Noncompliant

    private static StaticBlockSingleton instance;

    private StaticBlockSingleton(){}

    // static block initialization for exception handling
    static {
      try {
        instance = new StaticBlockSingleton();
      } catch (Exception e) {
        throw new RuntimeException("Exception occurred in creating singleton instance");
      }
    }

    public static StaticBlockSingleton getInstance() {
      return instance;
    }

    public boolean foo() {
      return false;
    }
  }

  public static class LazyInitializedSingleton { // Noncompliant

    private static LazyInitializedSingleton instance;

    private LazyInitializedSingleton(){}

    public static LazyInitializedSingleton getInstance() {
      if (instance == null) {
        instance = new LazyInitializedSingleton();
      }
      return instance;
    }

    public boolean foo() {
      return false;
    }
  }


  public static class ThreadSafeSingleton { // Noncompliant

    private static ThreadSafeSingleton instance;

    private ThreadSafeSingleton(){}

    public static synchronized ThreadSafeSingleton getInstance() {
      if (instance == null) {
        instance = new ThreadSafeSingleton();
      }
      return instance;
    }

    public boolean foo() {
      return false;
    }
  }

  public static class BillPughSingleton { // Noncompliant

    private BillPughSingleton(){}

    private static class SingletonHelper {
      private static final BillPughSingleton INSTANCE = new BillPughSingleton();
    }

    public static BillPughSingleton getInstance() {
      return SingletonHelper.INSTANCE;
    }

    public boolean foo() {
      return false;
    }
  }

  public enum EnumSingleton { // Noncompliant

    INSTANCE;

    public static void doSomething() {
      // do something
    }

    public boolean foo() {
      return false;
    }
  }

  public static class SerializedSingleton implements java.io.Serializable { // Noncompliant

    private static final long serialVersionUID = -7604766932017737115L;

    private SerializedSingleton(){}

    private static class SingletonHelper {
      private static final SerializedSingleton instance = new SerializedSingleton();
    }

    public static SerializedSingleton getInstance() {
      return SingletonHelper.instance;
    }

    @Serial
    protected Object readResolve() {
      return getInstance();
    }

  }

}
