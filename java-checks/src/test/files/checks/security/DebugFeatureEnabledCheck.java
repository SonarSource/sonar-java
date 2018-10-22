package files.checks.spring;

import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@EnableWebSecurity(debug = true) // Noncompliant [[sc=20;ec=32]] {{Make sure this debug feature is deactivated before delivering the code in production.}}
@EnableWebSecurity(unknown = true, debug = Boolean.TRUE) // Noncompliant [[sc=36;ec=56]]
@EnableWebSecurity(debug = false)
@EnableWebSecurity(debug)
@EnableWebSecurity(true)
@EnableWebSecurity
public class WebSecurityConfig {
}
