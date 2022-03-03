class CodeWithIssues {
  void method(boolean param) {
    param = true;
    if (param) {
      try {
      } catch (java.io.IOException e) {
        log();
      } catch (java.sql.SQLException e) {
        log();
      }
    }
  }
  void log() {
  }
}
