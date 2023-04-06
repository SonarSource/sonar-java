package checks;

import java.io.Serial;

// TODO: check code snippet license
public class SingletonUsageCheckSample {

  public static class EagerInitializedSingleton { // Noncompliant [[sc=23;ec=48;secondary=+2,+5]] {{A Singleton implementation was detected. Make sure the use of the Singleton pattern is required and the implementation is the right one for the context.}}

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

  public static class StaticBlockSingleton { // Noncompliant [[sc=23;ec=43;secondary=+2,+4,+9]]

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

  public enum EnumSingleton { // Noncompliant [[sc=15;ec=28]] {{An Enum-based Singleton implementation was detected. Make sure the use of the Singleton pattern is required and an Enum-based implementation is the right one for the context.}}

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

  public static class MultipleStaticFieldsOfDifferentTypesDoNotPreventDetection { // Noncompliant
    public static final MultipleStaticFieldsOfDifferentTypesDoNotPreventDetection ONE = new MultipleStaticFieldsOfDifferentTypesDoNotPreventDetection();
    public static final String MESSAGE = "Hello, World!";
    private int value;

    private MultipleStaticFieldsOfDifferentTypesDoNotPreventDetection() {
      this.value = 1;
    }

    public int getValue() {
      return value;
    }
  }

  interface WithSides {
    int sides();
  }

  enum Shape implements WithSides { // Compliant because single enum constants are not singletons
    TRIANGLE(3),;
    private int sides;

    Shape(int sides) {
      this.sides = sides;
    }

    @Override
    public int sides() {
      return sides;
    }
  }

  public static class TooManyConstructors { // Compliant
    public static final TooManyConstructors INSTANCE = new TooManyConstructors();
    private int field;

    private TooManyConstructors() {
      field = 42;
    }

    private TooManyConstructors(int value) {
      field = value;
    }
  }

  public static class SinglePrivateConstructorWithParameter { // Compliant
    public static final SinglePrivateConstructorWithParameter INSTANCE = new SinglePrivateConstructorWithParameter(42);
    private int field;

    private SinglePrivateConstructorWithParameter(int value) {
      field = value;
    }

    public int value() {
      return field;
    }
  }

  public static class LackNonPublicFieldOrInstanceMethod { // Compliant
    public static final LackNonPublicFieldOrInstanceMethod INSTANCE = new LackNonPublicFieldOrInstanceMethod();
    private int field;

    private LackNonPublicFieldOrInstanceMethod() {
      field = 42;
    }
  }

  public static class Numbers { // Compliant because there are multiple constant instances of the same type and not a singleton
    public static final Numbers ONE = new Numbers();
    public static final Numbers POSITIVE_ONE = new Numbers();
    private int value;

    private Numbers() {
      this.value = 1;
    }

    public int getValue() {
      return value;
    }
  }

  public static class MultipleReassignmentsPossible { // Compliant
    private static MultipleReassignmentsPossible INSTANCE = new MultipleReassignmentsPossible();
    private int value;

    private MultipleReassignmentsPossible() {
      value = 0;
    }

    public int increment() {
      return value++;
    }

    public int getValue() {
      return value;
    }

    public MultipleReassignmentsPossible reset() {
      INSTANCE = new MultipleReassignmentsPossible();
      return INSTANCE;
    }
  }
}
