package checks;

import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ssl.PropertiesSslBundle;
import org.springframework.boot.autoconfigure.ssl.SslProperties;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WeakSSLContextCheckSample {

  @Autowired
  public WeakSSLContextCheckSample(SslProperties props, DefaultSslBundleRegistry registry, Set<String> propsSet) {
    props.getBundle().getJks().get("").getOptions().setEnabledProtocols(Set.of("TLSv1.1")); // Noncompliant
    props.getBundle().getJks().get("").getOptions().setEnabledProtocols(Set.of("TLSv1.0")); // Noncompliant

    props.getBundle().getJks().get("").getOptions().setEnabledProtocols(Set.of("TLSv1")); // Compliant
    props.getBundle().getJks().get("").getOptions().setEnabledProtocols(getSet()); // Compliant - FN
    props.getBundle().getJks().get("").getOptions().setEnabledProtocols(propsSet); // Compliant
    props.getBundle().getJks().get("").getOptions().getCiphers(); // coverage

    registry.updateBundle("", PropertiesSslBundle.get(props.getBundle().getJks().get("")));
  }

  Set<String> getSet() {
    Set<String> set = new HashSet<>();
    set.add("TLSv1.1");
    return set;
  }

}
