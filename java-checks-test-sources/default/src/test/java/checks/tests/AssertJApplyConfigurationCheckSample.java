package checks.tests;

import org.assertj.core.configuration.Configuration;

public class AssertJApplyConfigurationCheckSample {
  void notApplied() {
    Configuration configuration = new Configuration(); // Noncompliant [[sc=19;ec=32]] {{Apply this configuration with apply() or applyAndDisplay().}}
    configuration.setComparingPrivateFields(true);
    configuration.setMaxElementsForPrinting(1001);
  }

  void apply() {
    Configuration configuration = new Configuration(); // Compliant
    configuration.setComparingPrivateFields(true);
    configuration.apply();
  }

  void applyAndDisplay() {
    Configuration configuration = new Configuration(); // Compliant
    configuration.setComparingPrivateFields(true);
    configuration.applyAndDisplay();
  }

  void notAppliedFN() {
    Configuration configuration = getConfiguration(); // Compliant, FN
    configuration.setComparingPrivateFields(true);
  }

  void appliedInOtherMethod() {
    Configuration configuration = new Configuration(); // Compliant
    configuration.setComparingPrivateFields(true);
    applyConfiguration(configuration);
  }

  void appliedChangedInOtherMethod() {
    Configuration configuration = new Configuration(); // Compliant, FN, we can not know if the configuration will be applied in the method call.
    setComparingConfiguration(configuration);
  }

  Configuration getConfiguration() {
    return new Configuration();
  }

  void applyConfiguration(Configuration configuration) {
    configuration.apply();
  }

  void setComparingConfiguration(Configuration configuration) {
    configuration.setComparingPrivateFields(true);
  }

}
