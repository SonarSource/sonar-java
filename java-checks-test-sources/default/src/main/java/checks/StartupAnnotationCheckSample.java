package checks;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@ApplicationScoped
public class StartupAnnotationCheckSample {

  @Startup
// ^^^^^^^ >
  static void staticMethod() { // Noncompliant {{"@Startup" annotation should not be applied to static methods}}
//            ^^^^^^^^^^^^
  }

  @Startup
// ^^^^^^^ >
  void withParameters(String config) { // Noncompliant {{"@Startup" annotation should only be applied to no-arg methods}}
//     ^^^^^^^^^^^^^^
  }

  @Startup
// ^^^^^^^ >
  @Produces
  String producer() { // Noncompliant {{"@Startup" annotation should not be applied to producer methods}}
//       ^^^^^^^^
    return "config";
  }

  @Startup
// ^^^^^^^ >
  static void staticWithParams(String arg) { // Noncompliant {{"@Startup" annotation should not be applied to static methods}}
//            ^^^^^^^^^^^^^^^^
  }

  @Startup
// ^^^^^^^ >
  @Produces
  static String staticProducer() { // Noncompliant {{"@Startup" annotation should not be applied to static methods}}
//              ^^^^^^^^^^^^^^
    return "value";
  }

  @Startup
// ^^^^^^^ >
  @Produces
  Integer producerWithParams(String input) { // Noncompliant {{"@Startup" annotation should not be applied to producer methods}}
//        ^^^^^^^^^^^^^^^^^^
    return 42;
  }

  @Startup
// ^^^^^^^ >
  @Produces
  static Double allViolations(String arg1, int arg2) { // Noncompliant {{"@Startup" annotation should not be applied to static methods}}
//              ^^^^^^^^^^^^^
    return 3.14;
  }

  @Startup
  void initialize() {
  }

  @Inject
  DatabaseService db;

  @Startup
  void onStart() {
    db.connect();
  }

  static void staticHelper() {
  }

  void setupWithParams(String config, int port) {
  }

  @Produces
  String createConfig() {
    return "config";
  }

  @Startup
  private void privateInit() {
  }

  static class DatabaseService {
    void connect() {
    }
  }
}
