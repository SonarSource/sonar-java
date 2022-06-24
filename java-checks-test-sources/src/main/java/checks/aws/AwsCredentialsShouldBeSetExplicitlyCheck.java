package checks.aws;

import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

public class AwsCredentialsShouldBeSetExplicitlyCheck {
  void method() {
    S3Client.builder() // Noncompliant
      .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
      .build();
  }
}
