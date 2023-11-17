package checks.aws;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

public class AwsRegionSetterCheck {

  public static final String CONST = "const";
  private static final String EU_WEST_1 = "EU_WEST_1";
  private static final Regions ENUM_EU_WEST_1 = Regions.EU_WEST_1;

  void nonCompliant() {
    AmazonS3ClientBuilder.standard().withRegion("eu_west_1").build(); // Noncompliant [[sc=49;ec=60]]
    AWSLambdaClientBuilder.standard().setRegion("eu_west_1"); // Noncompliant {{Give the enum value for this region instead.}}
  }

  void compliant() {
    AmazonS3ClientBuilder.standard().withRegion(Regions.EU_WEST_1).build();
    AmazonS3ClientBuilder.standard().withRegion(ENUM_EU_WEST_1).build();

    // String not being literal or defined through a call => Compliant
    AmazonS3ClientBuilder.standard().withRegion(getRegion()).build();
    AmazonS3ClientBuilder.standard().withRegion(EU_WEST_1).build();
    AmazonS3ClientBuilder.standard().withRegion(CONST).build();
  }

  String getRegion() {
    return EU_WEST_1;
  }
}
