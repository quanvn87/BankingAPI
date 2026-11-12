package com.example.minibank.security;
import com.example.minibank.common.*;
import com.fasterxml.jackson.databind.*;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
@Configuration
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
 @Bean Jackson2ObjectMapperBuilderCustomizer strictJson() {
  return b->b.featuresToDisable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
    .featuresToDisable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
    .featuresToEnable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .postConfigurer(mapper->mapper.coercionConfigFor(com.fasterxml.jackson.databind.type.LogicalType.Textual)
      .setCoercion(com.fasterxml.jackson.databind.cfg.CoercionInputShape.Integer,com.fasterxml.jackson.databind.cfg.CoercionAction.Fail)
      .setCoercion(com.fasterxml.jackson.databind.cfg.CoercionInputShape.Float,com.fasterxml.jackson.databind.cfg.CoercionAction.Fail)
      .setCoercion(com.fasterxml.jackson.databind.cfg.CoercionInputShape.Boolean,com.fasterxml.jackson.databind.cfg.CoercionAction.Fail));
 }
 @Bean SecurityFilterChain apiSecurity(HttpSecurity http,ObjectMapper json) throws Exception {
  var unauthorized=(org.springframework.security.web.AuthenticationEntryPoint)(req,res,e)->{
   res.setHeader("WWW-Authenticate","Basic realm=\"MiniBank\"");
   ApiProblems.write(json,req,res,401,"UNAUTHENTICATED","Thông tin xác thực không hợp lệ.");
  };
  http.csrf(c->c.disable()) // Stateless API for CLI clients; no browser session/cookie authentication.
    .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .requestCache(c->c.disable())
    .authorizeHttpRequests(a->a
      .requestMatchers(HttpMethod.POST,"/api/v1/customers","/api/v1/accounts").hasRole("STAFF")
      .requestMatchers(HttpMethod.PATCH,"/api/v1/accounts/*/status").hasRole("STAFF")
      .requestMatchers(HttpMethod.POST,"/api/v1/transfers").hasRole("CUSTOMER")
      .anyRequest().authenticated())
    .httpBasic(b->b.authenticationEntryPoint(unauthorized))
    .exceptionHandling(e->e.authenticationEntryPoint(unauthorized).accessDeniedHandler((req,res,ex)->
       ApiProblems.write(json,req,res,403,"FORBIDDEN","Bạn không có quyền thực hiện thao tác này.")))
    .addFilterBefore(new RequestIdFilter(),UsernamePasswordAuthenticationFilter.class);
  return http.build();
 }
}
