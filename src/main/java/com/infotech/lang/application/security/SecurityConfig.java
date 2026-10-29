package com.infotech.lang.application.security;

import com.infotech.lang.application.views.LoginView;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import com.vaadin.flow.spring.security.VaadinWebSecurity;

/**
 * Security configuration supporting both OAuth2 (Google) and form-based authentication.
 */
@EnableWebSecurity
@Configuration
public class SecurityConfig extends VaadinWebSecurity {

  private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
  private final FormLoginSuccessHandler formLoginSuccessHandler;
  private final CustomUserDetailsService userDetailsService;

  public SecurityConfig(OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
                       FormLoginSuccessHandler formLoginSuccessHandler,
                       CustomUserDetailsService userDetailsService) {
    this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
    this.formLoginSuccessHandler = formLoginSuccessHandler;
    this.userDetailsService = userDetailsService;
  }

  /**
   * Password encoder bean for hashing passwords.
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Republishes the servlet container's session-destroyed callback as a Spring
   * {@code HttpSessionDestroyedEvent}, which is how logout (explicit sign-out, timeout,
   * or browser close) is detected for the login activity report.
   */
  @Bean
  public HttpSessionEventPublisher httpSessionEventPublisher() {
    return new HttpSessionEventPublisher();
  }

  private static final String LOGIN_URL = "/login";

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    // Configure OAuth2 login before calling super
    http.oauth2Login(oauth2 -> oauth2
        .loginPage(LOGIN_URL)
        .successHandler(oAuth2LoginSuccessHandler));
    
    // Configure authentication manager with custom UserDetailsService
    http.userDetailsService(userDetailsService);

    // Allow /api/media/** to pass through Vaadin's filter chain so Spring MVC can handle it.
    // The controller itself enforces authentication via @AuthenticationPrincipal.
    http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/media/**").authenticated());
    
    // Call parent to set up Vaadin's default security
    super.configure(http);
    
    // Set login view - this configures form login internally
    setLoginView(http, LoginView.class);
    
    // Override the form login success handler set by setLoginView
    http.formLogin(form -> form
        .successHandler(formLoginSuccessHandler));
  }
}
