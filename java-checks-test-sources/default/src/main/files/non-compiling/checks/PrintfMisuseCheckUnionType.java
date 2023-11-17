package checks;

abstract class PrintfMisuseCheckUnionType {

  private static final org.apache.logging.log4j.Logger LOG = org.apache.logging.log4j.LogManager.getLogger();

  void foo(int port) {
    try {
      doSomething();
    } catch (IllegalAccessError e) {
      LOG.error("message: {}", port, this); // Noncompliant
    } catch (Unknown1 | Unknown2 e1) {
      LOG.error("message: {}", port, e1); // Compliant
    } catch (Unknown3 | IllegalArgumentException e2) {
      LOG.error("message: {}", port, e2); // Compliant
    } catch (java.io.IOException | IllegalArgumentException e3) {
      LOG.error("message: {}", port, e3); // Compliant
    } catch (Unknown4 e4) {
      LOG.error("message: {}", port, unknown); // Compliant
    }
  }

  abstract void doSomething();
}
