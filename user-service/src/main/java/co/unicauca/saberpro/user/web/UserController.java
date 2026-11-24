package co.unicauca.saberpro.user.web;

import co.unicauca.saberpro.common.contract.Role;
import co.unicauca.saberpro.common.security.AuthenticatedUser;
import co.unicauca.saberpro.common.security.CurrentUserProvider;
import co.unicauca.saberpro.common.web.BusinessException;
import co.unicauca.saberpro.common.web.ResourceNotFoundException;
import co.unicauca.saberpro.user.service.OperationResult;
import co.unicauca.saberpro.user.service.UserService;
import co.unicauca.saberpro.user.web.UserDtos.RegisterUserRequest;
import co.unicauca.saberpro.user.web.UserDtos.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * Consulta y registro de usuarios, sobre Keycloak.
 *
 * <ul>
 *   <li>GET /api/users/me: el usuario de la sesion (lo usa el frontend al iniciar sesion).</li>
 *   <li>GET /api/users/{login} y GET /api/users?role=...: los usan otros
 *       servicios con su token de servicio (rol SERVICIO): el review-service
 *       para validar revisores y el notification-service para los correos.</li>
 *   <li>POST /api/users: auto-registro (publico, roles Autor o Revisor); un
 *       administrador autenticado tambien puede crear administradores.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Usuarios")
public class UserController {

    private final UserService userService;
    private final CurrentUserProvider currentUser;

    public UserController(UserService userService, CurrentUserProvider currentUser) {
        this.userService = userService;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un usuario en Keycloak (publico para Autor/Revisor; Administrador solo por un admin)")
    public UserResponse register(@RequestBody RegisterUserRequest request) {
        boolean byAdmin = caller().map(AuthenticatedUser::isAdministrator).orElse(false);
        OperationResult result = userService.register(request.login(), request.fullName(), request.email(),
                request.role(), request.password(), byAdmin);
        if (!result.isSuccess()) {
            throw new BusinessException(result.getErrors());
        }
        return userService.findByLogin(request.login().trim().toLowerCase()).map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro el usuario recien creado."));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Datos del usuario autenticado, tomados del token JWT")
    public UserResponse me() {
        AuthenticatedUser user = currentUser.current();
        return userService.findByLogin(user.login()).map(UserResponse::from)
                .orElseGet(() -> UserResponse.from(user, null));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SERVICIO')")
    @Operation(summary = "Lista los usuarios; con ?role= solo los de ese rol")
    public List<UserResponse> list(@RequestParam(required = false) Role role) {
        return (role == null ? userService.listUsers() : userService.listByRole(role)).stream()
                .map(UserResponse::from).toList();
    }

    @GetMapping("/reviewers")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SERVICIO')")
    @Operation(summary = "Lista los revisores activos (HU04)")
    public List<UserResponse> reviewers() {
        return userService.listReviewers().stream().map(UserResponse::from).toList();
    }

    /** El nombre de la autenticacion es el preferred_username del token (KeycloakJwtAuthenticationConverter). */
    @GetMapping("/{login}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SERVICIO') or #login == authentication.name")
    @Operation(summary = "Consulta un usuario por su login (el mismo usuario, administradores y servicios)")
    public UserResponse find(@PathVariable String login) {
        return userService.findByLogin(login).map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el usuario " + login));
    }

    private Optional<AuthenticatedUser> caller() {
        try {
            return Optional.of(currentUser.current());
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }
}
