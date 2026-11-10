package com.cefet_rj.projeto_final.condominiums.security;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.condominiums.repositories.MembershipRepository;
import com.cefet_rj.projeto_final.shared.exceptions.ForbiddenException;
import com.cefet_rj.projeto_final.users.entities.Role;
import com.cefet_rj.projeto_final.users.entities.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CondominiumAccessServiceUnitTest {

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @InjectMocks
    private CondominiumAccessService accessService;

    @org.junit.jupiter.api.BeforeEach
    @org.junit.jupiter.api.AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private User buildUser(Long id, String roleAuthority) {
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@example.com");
        user.setFirstName("First" + id);
        user.setLastName("Last" + id);
        Role role = new Role();
        role.setAuthority(roleAuthority);
        user.addRole(role);
        return user;
    }

    @Test
    @DisplayName("validateAccess: allows global access for ROLE_ADMIN without querying membership")
    void validateAccess_allowsAdminGlobally() {
        User admin = buildUser(1L, "ROLE_ADMIN");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(admin);

        assertDoesNotThrow(() -> accessService.validateAccess(99L));
        verify(membershipRepository, never()).existsByUserIdAndCondominiumId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("validateAccess: allows access if user is a member of the condominium")
    void validateAccess_allowsMemberUser() {
        User resident = buildUser(2L, "ROLE_USER");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(resident);
        when(membershipRepository.existsByUserIdAndCondominiumId(2L, 99L)).thenReturn(true);

        assertDoesNotThrow(() -> accessService.validateAccess(99L));
        verify(membershipRepository).existsByUserIdAndCondominiumId(2L, 99L);
    }

    @Test
    @DisplayName("validateAccess: throws ForbiddenException if user is not a member of the condominium")
    void validateAccess_throwsForbidden_whenNotMember() {
        User outsider = buildUser(3L, "ROLE_USER");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(outsider);
        when(membershipRepository.existsByUserIdAndCondominiumId(3L, 99L)).thenReturn(false);

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> accessService.validateAccess(99L));
        assertTrue(ex.getMessage().contains("User does not belong to this condominium"));
    }

    @Test
    @DisplayName("validateManagerAccess: allows manager of condominium")
    void validateManagerAccess_allowsManager() {
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(mock(Authentication.class));
        SecurityContextHolder.setContext(securityContext);

        User manager = buildUser(4L, "ROLE_USER");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(manager);
        when(membershipRepository.isManagerOf(4L, 99L)).thenReturn(true);

        User result = accessService.validateManagerAccess(99L);
        assertNotNull(result);
        assertEquals(4L, result.getId());
    }

    @Test
    @DisplayName("validateManagerAccess: throws ForbiddenException for ordinary resident")
    void validateManagerAccess_throwsForbidden_whenResident() {
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(mock(Authentication.class));
        SecurityContextHolder.setContext(securityContext);

        User resident = buildUser(5L, "ROLE_USER");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(resident);
        when(membershipRepository.isManagerOf(5L, 99L)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> accessService.validateManagerAccess(99L));
    }

    @Test
    @DisplayName("validateStaffAccess: allows staff member (doorman / manager)")
    void validateStaffAccess_allowsStaffMember() {
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(mock(Authentication.class));
        SecurityContextHolder.setContext(securityContext);

        User doorman = buildUser(6L, "ROLE_USER");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(doorman);
        when(membershipRepository.isStaffOf(6L, 99L)).thenReturn(true);

        User result = accessService.validateStaffAccess(99L);
        assertNotNull(result);
        assertEquals(6L, result.getId());
    }

    @Test
    @DisplayName("validateStaffAccess: throws ForbiddenException for non-staff")
    void validateStaffAccess_throwsForbidden_whenNonStaff() {
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(mock(Authentication.class));
        SecurityContextHolder.setContext(securityContext);

        User resident = buildUser(7L, "ROLE_USER");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(resident);
        when(membershipRepository.isStaffOf(7L, 99L)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> accessService.validateStaffAccess(99L));
    }

    @Test
    @DisplayName("isManagerOrAdmin and isStaffOrAdmin: properly evaluates null or roles")
    void evaluatesRolesCorrectly() {
        assertFalse(accessService.isManagerOrAdmin(null, 1L));
        assertFalse(accessService.isStaffOrAdmin(null, 1L));

        User admin = buildUser(10L, "ROLE_ADMIN");
        assertTrue(accessService.isManagerOrAdmin(admin, 1L));
        assertTrue(accessService.isStaffOrAdmin(admin, 1L));
    }
}
