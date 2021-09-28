package checks.security;

import android.content.Context;
import android.os.Environment;
import java.io.File;

public class AndroidExternalStorage {

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

    // Direct access to properties
    Object o1 = context.externalMediaDirs; // Noncompliant [[sc=25;ec=42]] {{Make sure that external files are accessed safely here.}}
    Object o2 = context.externalCacheDir; // Noncompliant
    Object o3 = context.externalCacheDirs; // Noncompliant
    Object o4 = context.obbDir; // Noncompliant
    Object o5 = context.obbDirs; // Noncompliant

    // Compliant example: not related to Android
    MyClass myClass = new MyClass();
    Object o6 = myClass.obbDir; // Compliant
    Object o7 = myClass.unrelatedField; // Compliant
    myClass.getExternalCacheDirs(); // Compliant
  }

  class MyClass {
    File obbDir;
    Object unrelatedField;

    File[] getExternalCacheDirs() {
      return new File[0];
    }

  }
}
