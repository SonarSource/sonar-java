package checks.aws;

import java.util.Random;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

public class AwsCredentialsShouldBeSetExplicitlyCheck {
  public static final AwsClientBuilder BUILDER = S3Client.builder(); // Compliant FN - Could be configured closer to build call

  void nonCompliant() {
    S3Client.builder().build(); // Noncompliant [[sc=5;ec=31]] {{Set the credentials provider explicitly on this builder.}}
    S3Client client = S3Client.builder().build(); // Noncompliant [[sc=23;ec=49]] {{Set the credentials provider explicitly on this builder.}}
    S3Client otherClient = S3Client.builder() // Noncompliant [[sl=+0;sc=28;el=+2;ec=15]] {{Set the credentials provider explicitly on this builder.}}
      .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
      .build();
  }

  void nonCompliantBuilderAsVariable() {
    S3ClientBuilder builder = S3Client.builder(); // Noncompliant [[sc=5;ec=50]] {{Set the credentials provider explicitly on this builder.}}
    builder.build();

    S3ClientBuilder secondBuilder = S3Client.builder(); // Noncompliant [[sc=5;ec=56]] {{Set the credentials provider explicitly on this builder.}}
    secondBuilder.region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())));
    secondBuilder.build();

    S3ClientBuilder thirdBuilder = S3Client.builder() // Noncompliant [[sl=+0;sc=5;el=+1;ec=92]] {{Set the credentials provider explicitly on this builder.}}
      .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())));
    thirdBuilder.build();
  }

  void compliant(AwsClientBuilder builderArgument) {
    S3Client client = S3Client.builder() // Compliant
      .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
      .build();

    AwsClientBuilder locallyCraftedBuilder = getABuilder(); // Compliant FN
    S3Client otherClient = (S3Client) locallyCraftedBuilder.build();

    AwsClientBuilder remotelyCraftedBuilder = AwsRegionShouldBeSetExplicitlyCheck.getABuilder(); // Compliant FN
    S3Client yetAnotherClient = (S3Client) remotelyCraftedBuilder.build();

    AwsClientBuilder remotelyPreCraftedBuilder = AwsRegionShouldBeSetExplicitlyCheck.BUILDER; // Compliant FN
    S3Client stillAnotherClient = (S3Client) remotelyPreCraftedBuilder.build();

    S3Client sourceDirectlyFromBuilder = (S3Client) AwsRegionShouldBeSetExplicitlyCheck.BUILDER.build(); // Compliant FN

    S3Client clientOutOfNowhere = (S3Client) BUILDER.build(); // Compliant FN

    S3Client clientFromBuilderArgument = (S3Client) builderArgument.build();

    S3ClientBuilder initializedLater = null;
    initializedLater = S3Client.builder()
      .credentialsProvider(EnvironmentVariableCredentialsProvider.create());
    S3Client someClient = initializedLater.build();

    S3ClientBuilder initializedConditionally; // Compliant FN
    if (new Random().nextBoolean()) {
      initializedConditionally = S3Client.builder();
      S3Client someOtherClient = initializedConditionally.build();
    }
  }

  static AwsClientBuilder getABuilder() {
    return S3Client.builder();
  }
}
