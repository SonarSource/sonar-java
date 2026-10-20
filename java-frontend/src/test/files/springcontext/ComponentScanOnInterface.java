package checks.spring.context;

import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackageClasses = ComponentScanOnInterface.class)
interface ComponentScanOnInterface {
}
