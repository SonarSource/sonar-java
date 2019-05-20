package test;

import android.content.Context;
import android.os.Environment;

public class AccessExternalFiles {

  public void accessFiles(Context context) {
    Environment.getExternalStorageDirectory(); // Noncompliant {{Make sure that external files are accessed safely here.}}
    Environment.getExternalStoragePublicDirectory("pictures"); // Noncompliant
    context.getExternalFilesDir("type"); // Noncompliant
    context.getExternalFilesDirs("type"); // Noncompliant
    context.getExternalMediaDirs(); // Noncompliant
    context.getExternalCacheDir(); // Noncompliant
    context.getExternalCacheDirs(); // Noncompliant
    context.getObbDir(); // Noncompliant
    context.getObbDirs(); // Noncompliant
  }
}
