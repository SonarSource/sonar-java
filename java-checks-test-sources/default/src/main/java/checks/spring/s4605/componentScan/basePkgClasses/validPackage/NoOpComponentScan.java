package checks.spring.s4605.componentScan.basePkgClasses.validPackage;

import checks.spring.s4605.componentScan.basePkgClasses.anotherValidPackage.NoOpMarkerReferencedSomewhereElse;
import org.springframework.context.annotation.ComponentScan;

// SONARJAVA-4967
// basePackageClasses: All classes under the package of this interface will be scanned
@ComponentScan(basePackageClasses = {NoOpComponentScan.class, NoOpMarkerReferencedSomewhereElse.class})
public interface NoOpComponentScan {
}
