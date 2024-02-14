package android.view;

public class SurfaceControl {
  public SurfaceControl.Transaction setFrameRate(SurfaceControl sc, float frameRate, int compatibility) {
    return new Transaction();
  }

  public SurfaceControl.Transaction setFrameRate(SurfaceControl sc, float frameRate, int compatibility, int changeFrameRateStrategy) {
    return new Transaction();
  }

  public static class Transaction {
  }
}
