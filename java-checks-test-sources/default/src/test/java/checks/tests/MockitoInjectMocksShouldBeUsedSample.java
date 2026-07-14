package checks.tests;

import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

public class MockitoInjectMocksShouldBeUsedSample {

  interface PaymentGateway {
    void process();
  }

  interface InventoryService {
    int getStock(String item);
  }

  static class OrderProcessor {
    OrderProcessor(PaymentGateway gateway, InventoryService inventory) {}
    OrderProcessor(PaymentGateway gateway) {}
    OrderProcessor() {}
    OrderProcessor(PaymentGateway gateway, String name) {}
  }

  // ===== JUnit 4 - @RunWith(MockitoJUnitRunner.class) =====

  @RunWith(MockitoJUnitRunner.class)
  public class JUnit4NoncompliantBothMocks {
    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private InventoryService inventoryService;

    private OrderProcessor orderProcessor;

    @Before
    public void setUp() {
      orderProcessor = new OrderProcessor(paymentGateway, inventoryService); // Noncompliant {{Use "@InjectMocks" to inject these mock fields instead of manually constructing the object.}}
      //               ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    }
  }

  @RunWith(MockitoJUnitRunner.class)
  public class JUnit4NoncompliantSingleMock {
    @Mock
    private PaymentGateway paymentGateway;

    private OrderProcessor orderProcessor;

    @Before
    public void setUp() {
      orderProcessor = new OrderProcessor(paymentGateway); // Noncompliant {{Use "@InjectMocks" to inject these mock fields instead of manually constructing the object.}}
      //               ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    }
  }

  @RunWith(MockitoJUnitRunner.class)
  public class JUnit4NoncompliantWithSpy {
    @Mock
    private PaymentGateway paymentGateway;

    @Spy
    private InventoryService inventoryService = null;

    private OrderProcessor orderProcessor;

    @Before
    public void setUp() {
      orderProcessor = new OrderProcessor(paymentGateway, inventoryService); // Noncompliant {{Use "@InjectMocks" to inject these mock fields instead of manually constructing the object.}}
      //               ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    }
  }

  @RunWith(MockitoJUnitRunner.class)
  public class JUnit4NoncompliantWithThisPrefix {
    @Mock
    private PaymentGateway paymentGateway;

    private OrderProcessor orderProcessor;

    @Before
    public void setUp() {
      this.orderProcessor = new OrderProcessor(paymentGateway); // Noncompliant {{Use "@InjectMocks" to inject these mock fields instead of manually constructing the object.}}
      //                    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    }
  }

  @RunWith(MockitoJUnitRunner.StrictStubs.class)
  public class JUnit4NoncompliantStrictStubs {
    @Mock
    private PaymentGateway paymentGateway;

    private OrderProcessor orderProcessor;

    @Before
    public void setUp() {
      orderProcessor = new OrderProcessor(paymentGateway); // Noncompliant {{Use "@InjectMocks" to inject these mock fields instead of manually constructing the object.}}
      //               ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    }
  }

  // ===== JUnit 5 - @ExtendWith(MockitoExtension.class) =====

  @ExtendWith(MockitoExtension.class)
  class JUnit5NoncompliantBeforeEach {
    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private InventoryService inventoryService;

    private OrderProcessor orderProcessor;

    @BeforeEach
    void setUp() {
      orderProcessor = new OrderProcessor(paymentGateway, inventoryService); // Noncompliant {{Use "@InjectMocks" to inject these mock fields instead of manually constructing the object.}}
      //               ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    }
  }

  // ===== openMocks / initMocks =====

  class OpenMocksNoncompliant {
    @Mock
    private PaymentGateway paymentGateway;

    private OrderProcessor orderProcessor;

    @BeforeEach
    void setUp() {
      MockitoAnnotations.openMocks(this);
      orderProcessor = new OrderProcessor(paymentGateway); // Noncompliant {{Use "@InjectMocks" to inject these mock fields instead of manually constructing the object.}}
      //               ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    }
  }

  class InitMocksNoncompliant {
    @Mock
    private PaymentGateway paymentGateway;

    private OrderProcessor orderProcessor;

    @Before
    public void setUp() {
      MockitoAnnotations.initMocks(this);
      orderProcessor = new OrderProcessor(paymentGateway); // Noncompliant {{Use "@InjectMocks" to inject these mock fields instead of manually constructing the object.}}
      //               ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    }
  }

  // ===== Compliant cases =====

  @RunWith(MockitoJUnitRunner.class)
  public class CompliantAlreadyUsingInjectMocks {
    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private InventoryService inventoryService;

    @org.mockito.InjectMocks
    private OrderProcessor orderProcessor; // Compliant - uses @InjectMocks

    @Before
    public void setUp() {
      // nothing to construct manually
    }
  }

  @RunWith(MockitoJUnitRunner.class)
  public class CompliantNoMockArgs {
    @Mock
    private PaymentGateway paymentGateway;

    private OrderProcessor orderProcessor;

    @Before
    public void setUp() {
      orderProcessor = new OrderProcessor(); // Compliant - no args passed
    }
  }

  @RunWith(MockitoJUnitRunner.class)
  public class CompliantMixedArgs {
    @Mock
    private PaymentGateway paymentGateway;

    private OrderProcessor orderProcessor;

    @Before
    public void setUp() {
      orderProcessor = new OrderProcessor(paymentGateway, "real-name"); // Compliant - not all args are mock fields
    }
  }

  @RunWith(MockitoJUnitRunner.class)
  public class CompliantNewLocalVariable {
    @Mock
    private PaymentGateway paymentGateway;

    @Before
    public void setUp() {
      OrderProcessor local = new OrderProcessor(paymentGateway); // Compliant - assigned to local variable, not a field
    }
  }

  class CompliantNotMockitoManaged {
    @Mock
    private PaymentGateway paymentGateway;

    private OrderProcessor orderProcessor;

    @Before
    public void setUp() {
      orderProcessor = new OrderProcessor(paymentGateway); // Compliant - class is not Mockito-managed
    }
  }

  @RunWith(MockitoJUnitRunner.class)
  public class CompliantNoMockFields {
    private PaymentGateway paymentGateway = new PaymentGateway() {
      public void process() {}
    };

    private OrderProcessor orderProcessor;

    @Before
    public void setUp() {
      orderProcessor = new OrderProcessor(paymentGateway); // Compliant - paymentGateway is not a @Mock field
    }
  }

  @RunWith(MockitoJUnitRunner.class)
  public class CompliantNotInSetupMethod {
    @Mock
    private PaymentGateway paymentGateway;

    private OrderProcessor orderProcessor;

    @org.junit.Test
    public void testSomething() {
      orderProcessor = new OrderProcessor(paymentGateway); // Compliant - not in a setup method
    }
  }

  @RunWith(org.junit.runners.JUnit4.class)
  public class CompliantNonMockitoRunner {
    @Mock
    private PaymentGateway paymentGateway;

    private OrderProcessor orderProcessor;

    @Before
    public void setUp() {
      orderProcessor = new OrderProcessor(paymentGateway); // Compliant - not a Mockito runner
    }
  }

  @RunWith(MockitoJUnitRunner.class)
  public class CompliantNoSetupMethod {
    @Mock
    private PaymentGateway paymentGateway;

    private OrderProcessor orderProcessor;

    @org.junit.Test
    public void test() {
      orderProcessor = new OrderProcessor(paymentGateway); // Compliant - not in a @Before method
    }
  }
}
