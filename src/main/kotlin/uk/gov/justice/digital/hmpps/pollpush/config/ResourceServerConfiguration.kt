package uk.gov.justice.digital.hmpps.pollpush.config

import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
class ResourceServerConfiguration : WebSecurityConfigurerAdapter() {
  override fun configure(http: HttpSecurity) {
    http
      .sessionManagement()
      .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      .and().csrf().disable()
      .authorizeRequests { auth ->
        auth.antMatchers(
          "/webjars/**",
          "/favicon.ico",
          "/csrf",
          "/health/**",
          "/info",
          "/v3/api-docs/**",
          "/swagger-ui/**",
          "/swagger-ui.html",
          "/queue-admin/retry-all-dlqs", // This endpoint is secured in the ingress rather than the app so that it can be called from within the namespace without requiring authentication
        )
          .permitAll().anyRequest().authenticated()
      }.oauth2ResourceServer().jwt().jwtAuthenticationConverter(AuthAwareTokenConverter())
  }
}
