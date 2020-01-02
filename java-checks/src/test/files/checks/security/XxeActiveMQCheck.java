import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.activemq.ActiveMQConnectionFactory;

class A {

	static final boolean BOOLEAN_TRUE = true;
	
	List<String> packages = Arrays.asList("org.apache.activemq.test", "org.apache.camel.test");

  ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616"); // false-negative, no enclosing method to check

  ActiveMQConnectionFactory secure_processing() {
    ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");
    factory.setTrustedPackages(packages);
    // "*" is only a problem for "setTrustedPackages", not here
    factory.setUserName("*");
    return factory;
  }

  ActiveMQConnectionFactory dont_restrict_packages() {
		ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616"); // Noncompliant {{Secure this "ActiveMQConnectionFactory" by whitelisting the trusted packages using the "setTrustedPackages" method and make sure the "setTrustAllPackages" is not set to true.}}
		return factory;
	}
	
	ActiveMQConnectionFactory restrict_packages_with_null() {
		ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616"); // Noncompliant
		factory.setTrustedPackages(null);
		return factory;
	}
	
	ActiveMQConnectionFactory trust_all_packages() {
		ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616"); // Noncompliant
		factory.setTrustAllPackages(true);
		return factory;
	}

  ActiveMQConnectionFactory trust_all_packages_true_with_restrict_packages() {
    ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");   // Noncompliant
    factory.setTrustAllPackages(BOOLEAN_TRUE);
    factory.setTrustedPackages(packages);
    return factory;
  }

  ActiveMQConnectionFactory trust_all_packages_false_without_restrict_packages() {
		ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");   // Noncompliant
		factory.setTrustAllPackages(false);
		return factory;
	}	

	ActiveMQConnectionFactory trust_all_packages_false_with_restrict_packages() {
		ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");
		factory.setTrustAllPackages(false);
    factory.setTrustedPackages(packages);
		return factory;
	}

	ActiveMQConnectionFactory empty_restrict_packages() {
		ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616"); // false-negative
    factory.setTrustedPackages(new ArrayList<String>(0));
		return factory;
	}

	ActiveMQConnectionFactory conditional_trust_all_packages(boolean condition) {
		ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616"); // Noncompliant
    if (condition) {
      factory.setTrustAllPackages(true);
    } else {
      // ignore "setTrustAllPackages(false)" because there's a path where it can be "true"
      factory.setTrustAllPackages(false);
      factory.setTrustedPackages(Arrays.asList("org.apache.activemq.test"));
    }
		return factory;
	}

  ActiveMQConnectionFactory trust_all_packages_using_placeholder() {
    ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");   // Noncompliant
    factory.setTrustedPackages(Arrays.asList("*"));
    return factory;
  }

}
