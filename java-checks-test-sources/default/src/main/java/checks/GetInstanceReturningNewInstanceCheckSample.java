package checks;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

class GetInstanceReturningNewInstanceCheckSample {

  static class NoCache {

    private final Map<String, Boolean> flags = new HashMap<>();

    public static NoCache getInstance() {
//                        ^^^^^^^^^^^>
      return new NoCache(); // Noncompliant {{Rename this method, as "getInstance" is conventionally reserved for singletons, or make it return a cached instance instead of a new one.}}
//           ^^^^^^^^^^^^^
    }

    void enable(String name) {
      flags.put(name, true);
    }
  }

  static class NoCacheWithArguments {

    private NoCacheWithArguments(String name) {
    }

    static NoCacheWithArguments getInstance() {
//                              ^^^^^^^^^^^>
      return new NoCacheWithArguments("default"); // Noncompliant
//           ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    }
  }

  static class EagerlyCached {

    private static final EagerlyCached INSTANCE = new EagerlyCached();

    private EagerlyCached() {
    }

    static EagerlyCached getInstance() {
      return INSTANCE;
    }
  }

  static class LazilyCached {

    private static LazilyCached instance;

    private LazilyCached() {
    }

    static LazilyCached getInstance() {
      if (instance == null) {
        instance = new LazilyCached();
      }
      return instance;
    }
  }

  static class SynchronizedLazilyCached {

    private static SynchronizedLazilyCached instance;

    static synchronized SynchronizedLazilyCached getInstance() {
      if (instance == null) {
        instance = new SynchronizedLazilyCached();
      }
      return instance;
    }
  }

  static class HolderCached {

    private static class Holder {
      private static final HolderCached INSTANCE = new HolderCached();
    }

    static HolderCached getInstance() {
      return Holder.INSTANCE;
    }
  }

  static class NotStatic {

    NotStatic getInstance() {
      return new NotStatic();
    }
  }

  static class ParameterizedFactory {

    static ParameterizedFactory getInstance(Locale locale) {
      return new ParameterizedFactory();
    }

    static ParameterizedFactory getInstance(Locale locale, String variant) {
      return new ParameterizedFactory();
    }
  }

  static class DifferentlyNamedFactories {

    static DifferentlyNamedFactories newInstance() {
      return new DifferentlyNamedFactories();
    }

    static DifferentlyNamedFactories create() {
      return new DifferentlyNamedFactories();
    }

    static DifferentlyNamedFactories of() {
      return new DifferentlyNamedFactories();
    }

    static DifferentlyNamedFactories valueOf() {
      return new DifferentlyNamedFactories();
    }
  }

  static class ReturnsAnotherType {

    static Map<String, String> getInstance() {
      return new HashMap<>();
    }
  }

  static class ReturnsAnonymousSubclass {

    static ReturnsAnonymousSubclass getInstance() {
      return new ReturnsAnonymousSubclass() {
      };
    }
  }

  static class DoesMoreThanCreate {

    private static int calls;

    static DoesMoreThanCreate getInstance() {
      calls++;
      return new DoesMoreThanCreate();
    }
  }

  static class ReturnsNull {

    static ReturnsNull getInstance() {
      return null;
    }
  }

  interface WithoutBody {

    static WithoutBody getInstance() {
      return null;
    }
  }

  static abstract class AbstractGetInstance {

    abstract AbstractGetInstance getInstance();
  }

  enum EnumSingleton {
    INSTANCE;

    static EnumSingleton getInstance() {
      return INSTANCE;
    }
  }
}
