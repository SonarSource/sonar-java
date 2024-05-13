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

  public static final String TLSV_1_0 = "TLSv1.0";

  @Autowired
  public WeakSSLContextCheckSample(SslProperties props, DefaultSslBundleRegistry registry, Set<String> propsSet) {
    props.getBundle().getJks().get("").getOptions().setEnabledProtocols(Set.of("TLSv1.1", // Noncompliant {{Change this code to use a stronger protocol.}}
//                                                  ^^^^^^^^^^^^^^^^^^^
//                                                                             ^^^^^^^^^@-1<
      "test",
      "TLSv1.0"));
//    ^^^^^^^^^<
    props.getBundle().getJks().get("").getOptions().setEnabledProtocols(Set.of("TLSv1.0")); // Noncompliant

    props.getBundle().getJks().get("").getOptions().setEnabledProtocols(Set.of(TLSV_1_0)); // Noncompliant {{Change this code to use a stronger protocol.}}
//                                                  ^^^^^^^^^^^^^^^^^^^
//                                                                             ^^^^^^^^@-1<
    props.getBundle().getJks().get("").getOptions().setEnabledProtocols(Set.of(null)); // coverage

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
