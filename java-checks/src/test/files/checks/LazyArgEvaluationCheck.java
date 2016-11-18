package org.test;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.io.*;
import java.util.logging.Level;

import static com.google.common.base.Preconditions.checkState;

class LazyArgEvaluationCheck {

  public static final Logger LOG = LoggerFactory.getLogger(LazyArgEvaluationCheck.class);
  public static final java.util.logging.Logger logger = java.util.logging.Logger.getGlobal();

  public static void main(String[] args) {
    String csvPath = "";
    String message = "";

    logger.log(Level.SEVERE, message); // Compliant

    logger.log(Level.SEVERE, "Something went wrong: " + message);  // Noncompliant {{Use the built-in formatting to construct this argument.}}

    LOG.error("Unable to open file " + csvPath, new RuntimeException());  // Noncompliant {{Use the built-in formatting to construct this argument.}}

    checkState(System.currentTimeMillis() == new Date().getTime(), "Arg must be positive, but got " + System.currentTimeMillis());  // Noncompliant {{Invoke method(s) only conditionally. Use the built-in formatting to construct this argument.}}

    Preconditions.checkState(System.currentTimeMillis() > 0, formatMessage());  // Noncompliant {{Invoke method(s) only conditionally. }}

    checkState(System.currentTimeMillis() > 0, "message: %s", formatMessage());  // Noncompliant {{Invoke method(s) only conditionally. }}

    checkState(System.currentTimeMillis() > 0, "message: %s", LazyArgEvaluationCheck.formatMessage());  // Noncompliant {{Invoke method(s) only conditionally. }}
  }

  public static void cachingOnDisk(File path) {
    LOG.info("Caching on disk @ {}", path.getAbsolutePath()); // Compliant - getters are OK
    LOG.info("Caching on disk @ {}", path.isAbsolutePath()); // Compliant - getters are OK
  }

  public void exceptionalPaths() {
    try {

    } catch (Exception e) {
      LOG.info("Caching on disk @ {}", path.getAbsolutePath()); // Compliant - because we don't care about small performance loss in exceptional paths
    }
  }

  public void multiArgs() {
    checkState(System.currentTimeMillis() > 0, "message: %s %s", formatMessage(), "Something went wrong: " + System.currentTimeMillis());  // Noncompliant {{Invoke method(s) only conditionally. }}
  }

  private static String formatMessage() {
    return "Expensive computation";
  }

  public void classTree() {
    logger.log(Level.SEVERE, "Something went wrong: " + new Object() { // Noncompliant {{Invoke method(s) only conditionally. Use the built-in formatting to construct this argument.}}
      @Override
      public String toString() {
        return "tostring";
      }
    });
  }

}
