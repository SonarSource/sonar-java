public class T {
    void nonCompliant() {
        AmazonS3ClientBuilder.standard().withRegion("eu_west_1").build(); // Noncompliant
    }

    void compliant() {
        AmazonS3ClientBuilder.standard().withRegion(Regions.EU_WEST_1).build(); // Compliant
    }
}
