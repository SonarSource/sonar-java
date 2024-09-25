package checks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

class FalsePositivesFromTheCommunity {

  private static final Marker myMarker = MarkerFactory.getMarker("MY_MARKER");
  private static final Logger logger = LoggerFactory.getLogger(FalsePositivesFromTheCommunity.class);

  Object doSomething() {
    return null;
  }

  // https://community.sonarsource.com/t/false-positive-on-java-s2629/42091
  void foo() {

    logger.debug(myMarker, "message1: {}.", doSomething()); // Compliant - because we don't care about small performance loss in exceptional paths

    if (logger.isDebugEnabled(myMarker)) {
      logger.debug(myMarker, "message2: {}.", doSomething() + "yolo"); // Compliant as method(s) invoked conditionally - WAS FP due to missing MARKER param handling
      logger.debug(myMarker, "message2a: {}.", doSomething()); // Compliant
    }
  }

  // https://community.sonarsource.com/t/s2629-despite-using-isinfoenabled/120810
  void foo2() {

    logger.debug(myMarker, "message3: {}.", doSomething()); // Compliant - because we don't care about small performance loss in exceptional paths
    logger.debug(myMarker, "message4: {}.", doSomething() + "yolo"); // Noncompliant {{Invoke method(s) only conditionally. Use the built-in formatting to construct this argument.}}

    // this return makes the later debug statements in this method compliant
    if (!logger.isDebugEnabled(myMarker)) {
      return;
    }
    logger.debug(myMarker, "message4: {}.", doSomething() + "yolo"); // Compliant as method(s) invoked conditionally - IS FP
    logger.debug(myMarker, "message4a: {}.", doSomething()); // Compliant
  }

  void fooBar() {

    // test without return statement
    logger.debug(myMarker, "message3: {}.", doSomething()); // Compliant - because we don't care about small performance loss in exceptional paths
    logger.debug(myMarker, "message4: {}.", doSomething() + "yolo"); // Noncompliant {{Invoke method(s) only conditionally. Use the built-in formatting to construct this argument.}}
  }

}
