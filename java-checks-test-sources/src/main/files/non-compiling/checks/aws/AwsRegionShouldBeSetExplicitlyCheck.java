package checks.aws;

public class AwsRegionShouldBeSetExplicitlyCheck {
  public void getBuilderFromElsewhere() {
    S3Client s3Client = unknown.credentialsProvider(EnvironmentVariableCredentialsProvider.create()) // Compliant FN
      .build();
  }
}
