package checks.aws;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

public class AwsRegionShouldBeSetExplicitlyCheck {
  public static final AwsClientBuilder BUILDER = getABuilder();

  void nonCompliantChained() {
    S3Client s3Client = S3Client.builder() // Noncompliant [[sl=+0;sc=25;el=+2;ec=15]] {{Set the region explicitly on this builder.}}
      .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
      .build();
  }

  void nonCompliantVariable() {
    S3ClientBuilder builder = S3Client.builder(); // Noncompliant [[sc=5;ec=50]] {{Set the region explicitly on this builder.}}
    builder.build();
  }

  class NonCompliantClientAsField {
    S3Client incompleteClient = S3Client.builder().build(); // Noncompliant [[sc=33;ec=59]] {{Set the region explicitly on this builder.}}
  }

  void compliantChained() {
    S3Client.builder()
      .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
      .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
      .build();
  }

  void compliantVariable() {
    S3ClientBuilder builder = S3Client.builder();
    builder.region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())));
    builder.build();

    S3ClientBuilder secondBuilder = S3Client.builder();
    secondBuilder
      .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
      .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())));
    secondBuilder.build();

    S3ClientBuilder thirdBuilder = S3Client.builder();
    thirdBuilder
      .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
      .credentialsProvider(EnvironmentVariableCredentialsProvider.create());
    thirdBuilder.build();

    S3ClientBuilder fourthBuilder = S3Client.builder()
      .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())));
    fourthBuilder.credentialsProvider(EnvironmentVariableCredentialsProvider.create());
    fourthBuilder.build();
  }

  void compliantVariableSetInOtherMethod() {
    S3ClientBuilder builder = S3Client.builder(); // Compliant FN
    couldSetTheRegionButWillNot(builder);
    builder.build();
  }

  void couldSetTheRegionButWillNot(AwsClientBuilder ignored) {
  }

  class ClientAsField {
    S3Client client = S3Client.builder()
      .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
      .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
      .build();
  }

  void compliantInitializedInOtherVariable() {
    AwsClientBuilder firstBuilder = S3Client.builder();
    AwsClientBuilder builder = firstBuilder; // Compliant FN
    builder.build();
  }

  static AwsClientBuilder getABuilder() {
    return S3Client.builder();
  }

  void compliantInitializedInOtherMethod() {
    AwsClientBuilder builder = getABuilder(); // Compliant FN
    builder.build();


  }
}
