package checks.S3252_StaticMemberAccessCheckSample;

import checks.S3252_StaticMemberAccessCheckSample.hide_non_public.StaticMemberAccessCheckSampleHelper;

public class ImportFromOtherPackage {
  public void foo(){
    int x = StaticMemberAccessCheckSampleHelper.B.CONSTANT; // Compliant A is not accessible so we should not raise an issue
    int y = StaticMemberAccessCheckSampleHelper.Bar.CONSTANT; // Noncompliant
  }
}
