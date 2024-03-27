package checks;

import scala.reflect.ManifestFactory.IntManifest;

class ForLoopIncrementAndUpdateCheckSample {

  Object foo() {
    int i = 0, j = 0, k = 0, l = 0;
    int[] m = new int[10];

    for (i = 0; j < 10 && l < 10 && i < 50; k++) { // Compliant
      unknown++;
    }

    for (; unknown(); l++) {} // Compliant

    for (i = 0; i< 10; j++, m[0]++) { // Noncompliant
      i++;
    }

    for (i = 0; i< 10; unknown++) { // Noncompliant
      i++;
    }
  }
}
