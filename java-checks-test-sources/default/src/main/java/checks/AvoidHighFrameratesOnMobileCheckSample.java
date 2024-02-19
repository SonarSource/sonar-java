package checks;

import android.view.Surface;
import android.view.SurfaceControl;

public class AvoidHighFrameratesOnMobileCheckSample {

  private static final int HIGH_THRESHOLD = 120;
  private int HIGH_THRESHOLD_NON_FINAL = 120;

  void noncompliant(Surface surface, SurfaceControl surfaceControl) {
    surface.setFrameRate(120, 0, 0); // Noncompliant [[sc=26;ec=29]] {{Avoid setting high frame rates higher than 60 on mobile devices.}}
    surface.setFrameRate(120, 0); // Noncompliant
    surfaceControl.setFrameRate(surfaceControl, 120, 0, 0); // Noncompliant
    surfaceControl.setFrameRate(surfaceControl, 120, 0); // Noncompliant

    surface.setFrameRate(61, 0, 0); // Noncompliant
    surface.setFrameRate(HIGH_THRESHOLD, 0); // Noncompliant
  }

  void compliant(Surface surface, SurfaceControl surfaceControl) {
    surface.setFrameRate(60, 0, 0);
    surface.setFrameRate(60, 0);
    surfaceControl.setFrameRate(surfaceControl, 60, 0, 0);
    surfaceControl.setFrameRate(surfaceControl, 60, 0);

    surface.setFrameRate(1, 0, 0);
    surface.setFrameRate(HIGH_THRESHOLD_NON_FINAL, 0); // Compliant FN, as we don't currently resolve non-final non-static fields.
  }
}
