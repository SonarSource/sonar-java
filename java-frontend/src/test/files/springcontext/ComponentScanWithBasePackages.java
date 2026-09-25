package checks.spring.context;

import org.springframework.context.annotation.ComponentScan;

@ComponentScan({"com.example.service", "com.example.repository"})
class AppConfigValue {}

@ComponentScan(basePackages = {"com.example.controller", "com.example.domain"})
class AppConfigBasePackages {}

@ComponentScan(basePackages = "com.example.single")
class AppConfigSingleBasePackage {}

@ComponentScan(basePackageClasses = PackageMarker.class)
class AppConfigBasePackageClasses {}

interface PackageMarker {}
