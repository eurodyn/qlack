package com.eurodyn.qlack.fuse.demo.war.config;

import com.eurodyn.qlack.fuse.aaa.service.UserService;
import com.eurodyn.qlack.fuse.security.access.AAAPermissionEvaluator;
import com.eurodyn.qlack.fuse.security.filters.JwtTokenAuthenticationFilter;
import com.eurodyn.qlack.fuse.security.providers.AAAUsernamePasswordProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${cxf.path}")
    private String cxfPath;

    private final UserService userService;

    @Autowired
    public SecurityConfig(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void configure(WebSecurity web) {
        DefaultWebSecurityExpressionHandler handler = new DefaultWebSecurityExpressionHandler();
        handler.setPermissionEvaluator(new AAAPermissionEvaluator());
        web.expressionHandler(handler);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and().csrf().disable()
            .addFilterBefore(jwtTokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .authorizeRequests()
            .expressionHandler(webExpressionHandler())
            .antMatchers( "/login").permitAll()
            .antMatchers("/operations").permitAll()
            .antMatchers(cxfPath + "/auth/login").permitAll()
            .anyRequest().permitAll();
    }

    /**
     * Enable JWT authentication.
     */
    @Bean
    public JwtTokenAuthenticationFilter jwtTokenAuthenticationFilter() {
        return new JwtTokenAuthenticationFilter();
    }

    /**
     * Configures AAA operations to be evaluated as spring permissions.
     */
    @Bean
    public DefaultWebSecurityExpressionHandler webExpressionHandler() {
        DefaultWebSecurityExpressionHandler webSecurityExpressionHandler = new DefaultWebSecurityExpressionHandler();
        webSecurityExpressionHandler.setPermissionEvaluator(new AAAPermissionEvaluator());
        return webSecurityExpressionHandler;
    }

    /**
     * Configures AAA authentication provider with a user service and a password encoder.
     * The AAA user service should be used.
     */
    @Bean
    public AAAUsernamePasswordProvider authenticationProvider() {
        AAAUsernamePasswordProvider authProvider = new AAAUsernamePasswordProvider();
        authProvider.setUserDetailsService(userService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Sets bcrypt as the password encoding practice.
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
