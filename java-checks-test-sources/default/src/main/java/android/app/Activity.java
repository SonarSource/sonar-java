package android.app;

import android.content.SharedPreferences;
import android.view.ContextThemeWrapper;

public class Activity extends ContextThemeWrapper {
  public SharedPreferences getPreferences(int mode) {
    return new SharedPreferences();
  }
}
