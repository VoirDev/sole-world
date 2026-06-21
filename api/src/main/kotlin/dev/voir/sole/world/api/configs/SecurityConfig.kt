package dev.voir.sole.world.api.configs

import dev.voir.sole.world.api.graphql.security.AccessKeyHeaderAuthFilter
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Configures HTTP security for GraphQL and static asset endpoints.
 * @property cors CORS settings loaded from application configuration.
 */
@Configuration
@EnableConfigurationProperties(CorsProperties::class, AdminSecurityProperties::class, ClientSecurityProperties::class)
@EnableMethodSecurity
class SecurityConfig(
    private val cors: CorsProperties,
    private val adminSecurity: AdminSecurityProperties,
) {
    /**
     * Builds the Spring Security filter chain used by the admin UI and REST API.
     * @param http Spring Security HTTP configuration builder.
     * @return Configured admin security filter chain.
     */
    @Bean
    @Order(1)
    fun adminSecurityFilterChain(http: HttpSecurity): SecurityFilterChain = http
        .securityMatcher("/admin", "/admin/**")
        .csrf { }
        .cors { it.configurationSource(corsConfigurationSource()) }
        .authorizeHttpRequests {
            it.requestMatchers(
                "/admin/api/csrf",
                "/admin/login",
                "/admin/login.html",
                "/admin/styles.css",
            ).permitAll()
            it.anyRequest().hasRole("SUPER_ADMIN")
        }
        .formLogin {
            it.loginPage("/admin/login")
            it.loginProcessingUrl("/admin/login")
            it.defaultSuccessUrl("/admin", true)
            it.failureUrl("/admin/login?error")
            it.permitAll()
        }
        .exceptionHandling {
            it.accessDeniedPage("/admin/login?forbidden")
        }
        .logout {
            it.logoutUrl("/admin/logout")
            it.logoutSuccessUrl("/admin/login?logout")
            it.invalidateHttpSession(true)
            it.deleteCookies("JSESSIONID")
        }
        .build()

    /**
     * Builds the Spring Security filter chain used by the API.
     * @param http Spring Security HTTP configuration builder.
     * @return Configured security filter chain.
     */
    @Bean
    @Order(2)
    fun securityFilterChain(
        http: HttpSecurity,
        accessKeyHeaderAuthFilter: AccessKeyHeaderAuthFilter,
    ): SecurityFilterChain = http
        // Disable browser-oriented protections because this API uses explicit access keys.
        .csrf { it.disable() }
        // Apply the configured CORS source to every route.
        .cors { it.configurationSource(corsConfigurationSource()) }
        // Disable default login mechanisms that are not part of the API contract.
        .httpBasic { it.disable() }
        .formLogin { it.disable() }
        .authorizeHttpRequests {
            // Allow preflight requests and public assets without an API key.
            it.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            it.requestMatchers("/assets/**").permitAll()
            it.requestMatchers("/graphiql/**").permitAll()
            // GraphQL is reachable so schema tooling can introspect; resolver annotations enforce API keys.
            it.requestMatchers("/graphql").permitAll()
            // Keep every other route authenticated by default.
            it.anyRequest().authenticated()
        }
        .addFilterBefore(accessKeyHeaderAuthFilter, AnonymousAuthenticationFilter::class.java)
        .build()

    /**
     * Creates the single configured super-admin user for session login.
     * @param passwordEncoder Password encoder used when the configured password is not pre-encoded.
     * @return In-memory user detail service for the super admin.
     */
    @Bean
    fun userDetailsService(passwordEncoder: PasswordEncoder): UserDetailsService {
        val configuredPassword = adminSecurity.password
        val encodedPassword = if (configuredPassword.startsWith("{")) {
            configuredPassword
        } else {
            passwordEncoder.encode(configuredPassword)
        }

        val user = User
            .withUsername(adminSecurity.username)
            .password(encodedPassword)
            .roles("SUPER_ADMIN")
            .build()

        return InMemoryUserDetailsManager(user)
    }

    /** Password encoder used for the configured super-admin password. */
    @Bean
    fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

    /**
     * Creates the CORS configuration source used by Spring Security.
     * @return CORS configuration source registered for all routes.
     */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        // Build CORS settings from configuration with practical defaults for local deployments.
        val cfg = CorsConfiguration().apply {
            allowedOriginPatterns = cors.allowedOrigins
            allowedMethods = cors.allowedMethods
                .ifEmpty { listOf("GET", "POST", "PUT", "DELETE", "OPTIONS") }
            allowedHeaders = cors.allowedHeaders
                .ifEmpty { listOf("*") }
            exposedHeaders = listOf("Authorization")
            allowCredentials = false
            maxAge = 3600
        }

        // Register the CORS settings against every application path.
        return UrlBasedCorsConfigurationSource().also {
            it.registerCorsConfiguration("/**", cfg)
        }
    }
}

/** Super-admin login settings loaded from application configuration. */
@ConfigurationProperties(prefix = "admin.security")
data class AdminSecurityProperties(
    val username: String = "admin",
    val password: String = "admin",
)

/** Client API-key hashing settings loaded from application configuration. */
@ConfigurationProperties(prefix = "client.security")
data class ClientSecurityProperties(
    val accessKeyHashSecret: String,
)
