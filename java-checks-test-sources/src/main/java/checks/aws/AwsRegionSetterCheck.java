import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.regions.Regions;

public class T {
  private static final String EU_WEST_1 = "EU_WEST_1";
  private static final Regions ENUM_EU_WEST_1 = Regions.EU_WEST_1;

  void nonCompliant() {
    AmazonS3ClientBuilder.standard().withRegion("eu_west_1").build(); // Noncompliant [[sc=49;ec=60]]
    AmazonS3ClientBuilder.standard().withRegion(EU_WEST_1).build(); // Noncompliant {{Give the enum value for this region instead.}}
  }

  void compliant() {
    AmazonS3ClientBuilder.standard().withRegion(Regions.EU_WEST_1).build(); // Compliant
    AmazonS3ClientBuilder.standard().withRegion(ENUM_EU_WEST_1).build(); // Compliant
    AmazonS3ClientBuilder.standard().withRegion(getRegion()).build(); // Compliant
  }

  String getRegion() {
    return EU_WEST_1;
  }
}
