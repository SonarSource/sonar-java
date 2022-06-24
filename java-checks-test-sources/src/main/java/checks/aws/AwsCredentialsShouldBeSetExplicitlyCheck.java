package checks.aws;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;

public class AwsCredentialsShouldBeSetExplicitlyCheck {
  void nonCompliant() {
    S3Client.builder().build(); // Noncompliant [[sc=5;ec=31]] {{Set the credentials provider explicitly on this builder.}}
  }

  void compliant() {
    S3Client.builder()
      .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
      .build();
  }
}
