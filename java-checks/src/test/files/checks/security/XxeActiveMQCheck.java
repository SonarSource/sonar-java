import java.util.ArrayList;
import java.util.List;

import org.apache.activemq.ActiveMQConnectionFactory;

class A {

	static final boolean BOOLEAN_TRUE = true;
	
	List<String> packages = new ArrayList<>();

	ActiveMQConnectionFactory secure_processing() {
		ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");
		factory.setTrustedPackages(packages);
		return factory;
	}
	
	ActiveMQConnectionFactory dont_restrict_packages() {
		ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616"); // Noncompliant
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
	
	ActiveMQConnectionFactory trust_all_packages_false() {
		ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");
		factory.setTrustAllPackages(false);
		return factory;
	}	

	ActiveMQConnectionFactory trust_all_packages_false() {
		ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");   // Noncompliant
		factory.setTrustAllPackages(true);
		return factory;
	}	
	
	public String useString() {
		String s = new String();
		return s;
	}
}
