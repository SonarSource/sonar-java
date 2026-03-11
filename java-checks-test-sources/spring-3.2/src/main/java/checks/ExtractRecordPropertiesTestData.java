package checks;

import org.springframework.web.bind.annotation.BindParam;

public class ExtractRecordPropertiesTestData {
  // Record with components
  record RecordWithComponents(String project, int year, String month) {
  }

  // Empty record
  record EmptyRecord() {
  }

  // Record with @BindParam annotation
  record RecordWithBindParam(@BindParam("order-name") String orderName, String details) {
  }

  // Record with mixed @BindParam and regular components
  record RecordMixedBindParam(
    @BindParam("project-id") String projectId,
    String name,
    @BindParam("user-id") String userId
  ) {
  }
}
