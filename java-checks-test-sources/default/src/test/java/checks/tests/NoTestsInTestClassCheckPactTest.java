package checks.tests;

import au.com.dius.pact.provider.junit.PactRunner;
import au.com.dius.pact.provider.junit.RestPactRunner;
import au.com.dius.pact.provider.junitsupport.Consumer;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import org.junit.runner.RunWith;

@Provider("derp")
@Consumer("glory")
@PactBroker
@RunWith(PactRunner.class)
class NoTestsInTestClassCheckPactTest { //Compliant

  @State("Test State A")
  public void testA() {
    // Prepare service before interaction that require "default" state
    // ...
    System.out.println("Now service in default state");
  }

  @au.com.dius.pact.provider.junit.State("Test State B")
  public void testB() {
    // Prepare service before interaction that require B state
    // ...
    System.out.println("Now service in B state");
  }
}

@Provider("derp")
@Consumer("glory")
@PactBroker
@RunWith(RestPactRunner.class)
class NoTestsInTestClassCheckPactSpringTest { //Compliant

  @State("Test State A")
  public void testA() {
    // Prepare service before interaction that require "default" state
    // ...
    System.out.println("Now service in default state");
  }

  @au.com.dius.pact.provider.junit.State("Test State B")
  public void testB() {
    // Prepare service before interaction that require B state
    // ...
    System.out.println("Now service in B state");
  }
}

@Provider("derp")
@Consumer("glory")
@PactBroker
@RunWith(RestPactRunner.class)
class NoTestsInTestClassCheckPactNonCompliantTest{ // Noncompliant
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

  public void testA() {
    // Prepare service before interaction that require "default" state
    // ...
    System.out.println("Now service in default state");
  }

  public void testB() {
    // Prepare service before interaction that require B state
    // ...
    System.out.println("Now service in B state");
  }
}
