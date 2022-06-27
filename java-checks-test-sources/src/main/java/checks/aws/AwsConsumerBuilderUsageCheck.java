package checks.aws;

import java.util.Optional;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

public class AwsConsumerBuilderUsageCheck {

  Destination destinationField = Destination.builder()
    .toAddresses("to-email@domain.com")
    .bccAddresses("bcc-email@domain.com")
    .build();

  void test_destination(Destination destination1) {

    software.amazon.awssdk.services.sesv2.model.SendEmailRequest.builder()
      .destination(Destination.builder() // Noncompliant [[sc=8;ec=19]] {{Consider using the Consumer Builder method instead of creating this nested builder.}}
        .toAddresses("to-email@domain.com")
        .bccAddresses("bcc-email@domain.com")
        .build())
      .build();

    software.amazon.awssdk.services.sesv2.model.SendEmailRequest.builder()
      .destination(d -> d.toAddresses("to-email@domain.com").bccAddresses("bcc-email@domain.com"))
      .build(); // Compliant

    software.amazon.awssdk.services.sesv2.model.SendEmailRequest.builder()
      .destination(destination1) // Compliant
      .build();

    Optional<Destination> optionalDestination = Optional.of(destination1);
    software.amazon.awssdk.services.sesv2.model.SendEmailRequest.builder()
      .destination(optionalDestination.get()) // Compliant
      .build();

    Destination destination2 = Destination.builder()
      .toAddresses("to-email@domain.com")
      .bccAddresses("bcc-email@domain.com")
      .build();

    software.amazon.awssdk.services.sesv2.model.SendEmailRequest.builder()
      .destination(destination2) // Noncompliant
      .build();

    software.amazon.awssdk.services.sesv2.model.SendEmailRequest.builder()
      .destination(destinationField) // Compliant
      .build();

    software.amazon.awssdk.services.sesv2.model.SendEmailRequest.builder()
      .destination((Destination) null) // Compliant
      .build();

    Destination destination3 = null;
    software.amazon.awssdk.services.sesv2.model.SendEmailRequest.builder()
      .destination(destination3) // Compliant
      .build();

    Destination destination4 = null;

    if (true) {
      destination4 = Destination.builder()
        .toAddresses("to-email@domain.com")
        .bccAddresses("bcc-email@domain.com")
        .build();
    }

    software.amazon.awssdk.services.sesv2.model.SendEmailRequest.builder()
      .destination(destination4) // Noncompliant
      .build();

    software.amazon.awssdk.services.ses.model.SendEmailRequest.builder()
      .destination(software.amazon.awssdk.services.ses.model.Destination.builder() // Noncompliant
        .toAddresses("to-email@domain.com")
        .bccAddresses("bcc-email@domain.com")
        .build())
      .build();

    software.amazon.awssdk.services.ses.model.SendEmailRequest.builder()
      .destination(d -> d.toAddresses("to-email@domain.com").bccAddresses("bcc-email@domain.com"))
      .build(); // Compliant

    software.amazon.awssdk.services.pinpointemail.model.SendEmailRequest.builder()
      .destination(software.amazon.awssdk.services.pinpointemail.model.Destination.builder() // Noncompliant
        .toAddresses("to-email@domain.com")
        .bccAddresses("bcc-email@domain.com")
        .build())
      .build();

    software.amazon.awssdk.services.pinpointemail.model.SendEmailRequest.builder()
      .destination(d -> d.toAddresses("to-email@domain.com").bccAddresses("bcc-email@domain.com"))
      .build(); // Compliant

  }

  void test_content(EmailContent content) {

    software.amazon.awssdk.services.sesv2.model.SendEmailRequest.builder()
      .content(content) // Compliant
      .build();

    SendEmailRequest sendEmailRequest = SendEmailRequest.builder()
      .content(EmailContent.builder() // Noncompliant
        .simple(Message.builder() // Noncompliant
          .body(Body.builder() // Noncompliant
            .html(Content.builder() // Noncompliant
              .data("<html />")
              .build())
            .build())
          .build())
        .build())
      .build();

    // coverage
    sendEmailRequest.equalsBySdkFields(null);
    SendEmailRequest.builder().overrideConfiguration();

    software.amazon.awssdk.http.SdkHttpRequest.builder()
      .appendHeader("", "");
  }

}
