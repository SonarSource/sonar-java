package files.checks.spring;

import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@EnableWebSecurity(debug = true) // Noncompliant [[sc=20;ec=32]] {{Deactivate Spring Security's debug mode.}}
@EnableWebSecurity(unknown = true, debug = Boolean.TRUE) // Noncompliant [[sc=36;ec=56]]
@EnableWebSecurity(debug = false)
@EnableWebSecurity(debug)
@EnableWebSecurity(true)
@EnableWebSecurity
public class WebSecurityConfig {
}
