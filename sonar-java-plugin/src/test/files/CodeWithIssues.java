class CodeWithIssues {
  void method() {
    try {
    } catch (java.io.IOException e) {
      log();
    } catch (java.sql.SQLException e) {
      log();
    }
  }
  void log() {
  }
}
