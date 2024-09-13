public interface Graceful {
  static void shutdown(Object component) {
    // test no FP raised on interface
    Logger LOG = LoggerFactory.getLogger(component.getClass()); // Compliant
  }
}
