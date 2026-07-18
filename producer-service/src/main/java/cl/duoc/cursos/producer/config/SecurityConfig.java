package cl.duoc.cursos.producer.config;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable())
            .authorizeRequests(auth -> auth
                    // Endpoint público de salud de AWS/Actuator
                    .antMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                    
                    // Cualquier token autenticado pasa el filtro HTTP base ya que el @PreAuthorize del controlador se encarga con los roles
                    .antMatchers("/api/plataforma/**").authenticated()
                    
                    .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            .build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter defaultScopesConverter = new JwtGrantedAuthoritiesConverter();

        Converter<Jwt, Collection<GrantedAuthority>> authoritiesConverter = jwt -> {
            Set<GrantedAuthority> authorities = new HashSet<>(defaultScopesConverter.convert(jwt));
            
            // Mapeo seguro multiclaiam compatible con Azure AD B2C
            addClaimAuthorities(jwt.getClaim("extension_role"), authorities);
            addClaimAuthorities(jwt.getClaim("role"), authorities);
            addClaimAuthorities(jwt.getClaim("roles"), authorities);
            
            return authorities;
        };

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return authenticationConverter;
    }

    private void addClaimAuthorities(Object claimValue, Set<GrantedAuthority> authorities) {
        if (claimValue instanceof String role) {
            addSingleRole(role, authorities);
            return;
        }

        if (claimValue instanceof Collection<?> roles) {
            for (Object role : roles) {
                if (role instanceof String roleValue) {
                    addSingleRole(roleValue, authorities);
                }
            }
        }
    }

    private void addSingleRole(String role, Set<GrantedAuthority> authorities) {
        String normalizedRole = role == null ? "" : role.trim();

        if (normalizedRole.isEmpty()) {
            return;
        }

        // Normaliza el rol para asegurar el prefijo estándar "ROLE_"
        String authority = normalizedRole.startsWith("ROLE_")
                ? normalizedRole.toUpperCase()
                : "ROLE_" + normalizedRole.toUpperCase();

        authorities.add(new SimpleGrantedAuthority(authority));
    }
}