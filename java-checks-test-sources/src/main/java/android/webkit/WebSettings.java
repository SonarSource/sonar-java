package android.webkit;

public abstract class WebSettings {
  public static final int MIXED_CONTENT_ALWAYS_ALLOW = 0;
  public static final int MIXED_CONTENT_NEVER_ALLOW = 1;
  public static final int MIXED_CONTENT_COMPATIBILITY_MODE = 2;

  public abstract void setMixedContentMode(int mode);

  public abstract void setJavaScriptEnabled(boolean flag);

  public abstract void setAllowFileAccess(boolean allow);
  public abstract void setAllowContentAccess(boolean allow);

  @Deprecated
  public abstract void setAllowFileAccessFromFileURLs(boolean flag);

  @Deprecated
  public abstract void setAllowUniversalAccessFromFileURLs(boolean flag);

}
