package checks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

// https://community.sonarsource.com/t/false-positive-on-java-s2629/42091
class AFalsePositveFromTheCommunity {
  void foo() {
    final Marker myMarker = MarkerFactory.getMarker("MY_MARKER");
    final Logger logger = LoggerFactory.getLogger(A.class);

    logger.debug(myMarker, "message1: {}.", doSomething()); // Compliant - because we don't care about small performance loss in exceptional paths

    if (logger.isDebugEnabled(myMarker)) {
      logger.debug(myMarker, "message2: {}.", doSomething() + "yolo"); // Compliant as method(s) invoked conditionally - WAS FP due to missing MARKER param handling
      logger.debug(myMarker, "message2a: {}.", doSomething()); // Compliant
    }
  }

  Object doSomething() {
    return null;
  }
}

// https://community.sonarsource.com/t/s2629-despite-using-isinfoenabled/120810
//class AnotherFalsePositveFromTheCommunity {
//  void foo() {
//    final Marker myMarker = MarkerFactory.getMarker("MY_MARKER");
//    final Logger logger = LoggerFactory.getLogger(A.class);
//
//    logger.debug(myMarker, "message3: {}.", doSomething()); // Compliant - because we don't care about small performance loss in exceptional paths
//
//    if (!logger.isDebugEnabled(myMarker)) {
//      return;
//    }
//    logger.debug(myMarker, "message4: {}.", doSomething() + "yolo"); // Compliant as method(s) invoked conditionally - IS FP
//    logger.debug(myMarker, "message4a: {}.", doSomething()); // Compliant
//  }
//
//  Object doSomething() {
//    return null;
//  }
//}
