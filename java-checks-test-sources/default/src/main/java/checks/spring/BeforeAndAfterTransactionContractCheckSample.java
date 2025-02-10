package checks.spring;

import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;

public class BeforeAndAfterTransactionContractCheckSample {

  @BeforeTransaction
//^^^^^^^^^^^^^^^^^^> {{Annotation}}
  private String beforeTransaction() { // Noncompliant {{@BeforeTransaction method should return void.}}
//        ^^^^^^
    return "before";
  }

  @AfterTransaction
  private String afterTransaction() { // Noncompliant {{@AfterTransaction method should return void.}}
//        ^^^^^^
    return "after";
  }

  @BeforeTransaction
  public void beforeTransaction2(String name) { // Noncompliant {{@BeforeTransaction method should not have parameters.}}
    assert name != null;
  }

  @AfterTransaction
  public void afterTransaction2(String name, String surname) { // Noncompliant {{@AfterTransaction method should not have parameters.}}
    //                          ^^^^^^^^^^^^^^^^^^^^^^^^^^^
    assert name != null;
    assert surname != null;
  }

  @BeforeTransaction
  public void beforeTransaction5(@Autowired Object notAComponent) { // Noncompliant {{@BeforeTransaction method should not have parameters.}}
    //                           ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  }

  @BeforeTransaction
  public void beforeTransaction3(TestInfo info) { // Compliant, jupiter TestInfo is allowed
    // ...
  }

  @BeforeTransaction
  public void beforeTransaction4(@Autowired MyService myService) { // Compliant, autowired components are allowed
    // ...
  }

  @Service
  class MyService {
    // ...
  }

}
