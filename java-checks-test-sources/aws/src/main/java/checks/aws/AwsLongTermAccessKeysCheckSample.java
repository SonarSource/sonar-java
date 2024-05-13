package checks.aws;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;

public class AwsLongTermAccessKeysCheckSample {
  void noncompliant() {
    BasicAWSCredentials foo = new BasicAWSCredentials("", ""); // Noncompliant {{Make sure using a long-term access key is safe here.}}
//                                ^^^^^^^^^^^^^^^^^^^
  }

  void compliant() {
    BasicAWSCredentials foo = null;
    BasicSessionCredentials sessionCredentials = new BasicSessionCredentials(
      "session_creds.getAccessKeyId()",
      "session_creds.getSecretAccessKey()",
      "session_creds.getSessionToken()");
  }
}
