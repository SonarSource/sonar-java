public class T {
  private static final String EU_WEST_1 = "EU_WEST_1";

  void nonCompliant() {
    AmazonS3ClientBuilder.standard().withRegion("eu_west_1").build(); // Noncompliant
    AmazonS3ClientBuilder.standard().withRegion(EU_WEST_1).build(); // Noncompliant
  }

  void compliant() {
    AmazonS3ClientBuilder.standard().withRegion(Regions.EU_WEST_1).build(); // Compliant
  }
}
