public class NoSonar {
  private void foo() {
    // Squid:S1197
    int var[]; //NOSONAR
  }
}
