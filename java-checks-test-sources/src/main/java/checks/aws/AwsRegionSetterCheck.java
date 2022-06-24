package checks.aws;

import checks.aws.externalpackage.ExternalClass;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

public class AwsRegionSetterCheck {
  private static final String EU_WEST_1 = "EU_WEST_1";
  private static final Regions ENUM_EU_WEST_1 = Regions.EU_WEST_1;

  void nonCompliant() {
    AmazonS3ClientBuilder.standard().withRegion("eu_west_1").build(); // Noncompliant [[sc=49;ec=60]]
    AmazonS3ClientBuilder.standard().withRegion(EU_WEST_1).build(); // Noncompliant {{Give the enum value for this region instead.}}
    AWSLambdaClientBuilder.standard().setRegion("eu_west_1"); // Noncompliant
  }

  void compliant() {
    AmazonS3ClientBuilder.standard().withRegion(Regions.EU_WEST_1).build();
    AmazonS3ClientBuilder.standard().withRegion(ENUM_EU_WEST_1).build();

    // String not defined locally or through a call => Compliant
    AmazonS3ClientBuilder.standard().withRegion(getRegion()).build();
    AmazonS3ClientBuilder.standard().withRegion(new ExternalClass().getRegion()).build();
    AmazonS3ClientBuilder.standard().withRegion(ExternalClass.US_EAST_2).build();
  }

  String getRegion() {
    return EU_WEST_1;
  }
}
