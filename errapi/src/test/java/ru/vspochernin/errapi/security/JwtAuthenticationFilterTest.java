package ru.vspochernin.errapi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ru.vspochernin.errapi.exception.ErrapiErrorType;
import ru.vspochernin.errapi.model.auth.User;
import ru.vspochernin.errapi.model.auth.UserRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueChainWhenNoAuthHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).isValid(anyString());
    }

    @Test
    void shouldSetAuthWhenTokenValid() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.jwt.token");
        when(jwtService.isValid("valid.jwt.token")).thenReturn(true);
        when(jwtService.extractLogin("valid.jwt.token")).thenReturn("testuser");

        var user = new User();
        user.setId(1);
        user.setLogin("testuser");
        user.setRole(UserRole.READER);
        var details = new AuthUserDetails(user);
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(details);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        // Контекст должен быть установлен
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isInstanceOf(AuthUserDetails.class);
    }

    @Test
    void shouldSetAttributeWhenTokenInvalid() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid.token");
        when(jwtService.isValid("invalid.token")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(request).setAttribute(
                JsonAuthenticationEntryPoint.ATTR_ERROR_TYPE,
                ErrapiErrorType.INVALID_TOKEN);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSetAttributeWhenUserNotFound() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.jwt.token");
        when(jwtService.isValid("valid.jwt.token")).thenReturn(true);
        when(jwtService.extractLogin("valid.jwt.token")).thenReturn("ghost");
        when(userDetailsService.loadUserByUsername("ghost"))
                .thenThrow(new UsernameNotFoundException("not found"));

        filter.doFilterInternal(request, response, filterChain);

        verify(request).setAttribute(
                JsonAuthenticationEntryPoint.ATTR_ERROR_TYPE,
                ErrapiErrorType.USER_DOES_NOT_EXIST);
        verify(filterChain).doFilter(request, response);
    }
}
