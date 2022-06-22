package checks.aws;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;

public class AwsRegionShouldBeSetExplicitlyCheck {
  void method() {
    S3Client.builder()
      .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
      .build();
  }
}
