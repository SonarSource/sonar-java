package software.amazon.awssdk.services.config;

import java.util.function.Consumer;
import software.amazon.awssdk.services.sesv2.model.Destination;

public class AwsConsumerBuilderUsageCheckSample {

  public void test() {
    AwsConsumerBuilderUsageCheckSample.builder()
      .validDestination(Destination.builder() // Noncompliant [[sc=8;ec=24]] {{Consider using the Consumer Builder method instead of creating this nested builder.}}
        .toAddresses("to-email@domain.com")
        .bccAddresses("bcc-email@domain.com")
        .build())
      .build();

    AwsConsumerBuilderUsageCheckSample.builder()
      .validDestination("Not a Destination object") // Compliant, wrong argument type
      .build();

    AwsConsumerBuilderUsageCheckSample.builder()
      .validDestination(new UnknownType()) // Compliant, unknown argument type
      .build();

    AwsConsumerBuilderUsageCheckSample.builder()
      .testUnknownReturnType(Destination.builder() // Compliant, unknown return type
        .toAddresses("to-email@domain.com")
        .bccAddresses("bcc-email@domain.com")
        .build())
      .build();

    AwsConsumerBuilderUsageCheckSample.builder()
      .testInvalidConsumerType(Destination.builder() // Compliant, unknown return type
        .toAddresses("to-email@domain.com")
        .bccAddresses("bcc-email@domain.com")
        .build())
      .build();
  }

  public Builder builder() {
    return new Builder();
  }

  public class Builder {

    // not a method but a field
    public Builder validDestination;

    public Builder validDestination(String notADestinationObject) {
      return this;
    }
    public String validDestination(int notSameReturnType) {
      return this;
    }
    public Builder validDestination(Destination destination) {
      return this;
    }
    public Builder validDestination(Consumer<Destination.Builder> destination, String unexpectedSecondArgument) {
      return this;
    }
    public Builder validDestination(Consumer<Destination.Builder> destination) {
      return this;
    }
    public UnknownType testUnknownReturnType(Destination destination) {
      return this;
    }
    public UnknownType testUnknownReturnType(Consumer<Destination.Builder> destination) {
      return this;
    }

    public Builder testUnknowType(String argument) {
      return this;
    }
    public Builder testUnknowType(UnknownType<Destination.Builder> argument) {
      return this;
    }
    public Builder testUnknowType(Consumer<UnknownType.Builder> argument) {
      return this;
    }

    public Builder testInvalidConsumerType(Destination destination) {
      return this;
    }
    public Builder testInvalidConsumerType(Consumer destination) {
      return this;
    }
  }

}
