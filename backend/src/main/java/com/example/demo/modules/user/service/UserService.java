package com.example.demo.modules.user.service;

import com.example.demo.modules.user.dto.LoginRequest;
import com.example.demo.modules.user.dto.LoginResponse;
import com.example.demo.modules.user.dto.UserRequest;
import com.example.demo.modules.user.dto.PlayerUpdateRequest;
import com.example.demo.modules.user.dto.UserResponse;
import com.example.demo.modules.user.repository.UserPageRepository;
import com.example.demo.modules.user.security.JwtService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class UserService {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 帳號與密碼只能使用英文字母與數字
    private static final String ALPHANUMERIC_REGEX = "^[A-Za-z0-9]+$";

    private final UserPageRepository userPageRepository;
    private final PasswordService passwordService;
    private final OperationLogService operationLogService;
    private final JwtService jwtService;

    public UserService(UserPageRepository userPageRepository,
                       PasswordService passwordService,
                       OperationLogService operationLogService,
                       JwtService jwtService) {
        this.userPageRepository = userPageRepository;
        this.passwordService = passwordService;
        this.operationLogService = operationLogService;
        this.jwtService = jwtService;
    }

    public List<UserResponse> findAll() {
        return userPageRepository.findAll();
    }

    @Transactional
    public UserResponse create(UserRequest request, String operator) {
        validate(request, false);

        String account = normalizeRequired(request.account(), "Account");
        String role = normalizeRole(request.role());
        String status = normalizeStatus(request.status());

        if (userPageRepository.existsByAccount(account, null)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Account already exists");
        }

        String now = now();

        userPageRepository.insert(
                account,
                passwordService.encode(request.password()),
                normalizeRequired(request.username(), "Username"),
                request.avatar(),
                request.description(),
                role,
                status,
                now
        );

        UserResponse user = findByAccount(account);

        operationLogService.log(
                operator, "CREATE", user.id(), role,
                "Created user " + account
        );

        return user;
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request, String operator) {
        validate(request, true);

        findById(id);

        String account = normalizeRequired(request.account(), "Account");
        String role = normalizeRole(request.role());
        String status = normalizeStatus(request.status());

        if (userPageRepository.existsByAccount(account, id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Account already exists");
        }

        String now = now();

        if (request.password() == null || request.password().isBlank()) {
            userPageRepository.updateWithoutPassword(
                    id,
                    account,
                    normalizeRequired(request.username(), "Username"),
                    request.avatar(),
                    request.description(),
                    role,
                    status,
                    now
            );
        } else {
            userPageRepository.updateWithPassword(
                    id,
                    account,
                    passwordService.encode(request.password()),
                    normalizeRequired(request.username(), "Username"),
                    request.avatar(),
                    request.description(),
                    role,
                    status,
                    now
            );
        }

        operationLogService.log(
                operator, "UPDATE", id, role,
                "Updated user " + account
        );

        return findById(id);
    }

    @Transactional
    public void delete(Long id, String operator) {
        UserResponse user = findById(id);

        userPageRepository.delete(id);

        operationLogService.log(
                operator, "DELETE", id, user.role(),
                "Deleted user " + user.account()
        );
    }

    public LoginResponse login(LoginRequest request) {
        if (request == null
                || request.account() == null
                || request.account().isBlank()
                || request.password() == null
                || request.password().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Account and password are required");
        }

        String account = request.account().trim();
        UserPageRepository.LoginRow user = userPageRepository.findLoginUser(account);

        if (user == null || !passwordService.matches(request.password(), user.password())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid account or password");
        }

        if (!"Active".equalsIgnoreCase(user.status())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "User account is not active");
        }

        String now = now();
        userPageRepository.updateLastLogin(user.id(), now);

        operationLogService.log(
                user.account(), "LOGIN", user.id(), user.role(), "User logged in"
        );

        // 登入成功後產生 JWT
        String token = jwtService.generateToken(
                user.id(),
                user.account(),
                user.role()
        );

        LoginResponse response = new LoginResponse();

        response.setUserId(user.id());
        response.setAccount(user.account());
        response.setUsername(user.username());
        response.setRole(user.role());
        response.setStatus(user.status());
        response.setToken(token);

        return response;
    }

    @Transactional
    public LoginResponse updatePlayer(String currentAccount, PlayerUpdateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        UserResponse current = findByAccountForSession(currentAccount);

        String account = normalizeRequired(request.account(), "Account");
        String username = normalizeRequired(request.username(), "Username");

        if (!account.matches(ALPHANUMERIC_REGEX)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Account can contain only English letters and numbers");
        }

        if (userPageRepository.existsByAccount(account, current.id())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Account already exists");
        }

        String avatar = request.avatar();
        if (avatar != null) avatar = avatar.trim();
        if (avatar == null || avatar.isBlank()) {
            avatar = "/src/User/Player/avatar/user.png";
        }

        String now = now();
        userPageRepository.updatePlayer(
                current.id(), account, username, avatar, request.description(), now);

        operationLogService.log(
                account, "UPDATE", current.id(), current.role(),
                "Player updated own profile");

        UserResponse updated = findById(current.id());
        String token = jwtService.generateToken(
                updated.id(), updated.account(), updated.role());

        LoginResponse response = new LoginResponse();
        response.setUserId(updated.id());
        response.setAccount(updated.account());
        response.setUsername(updated.username());
        response.setRole(updated.role());
        response.setStatus(updated.status());
        response.setToken(token);
        return response;
    }

    @Transactional
    public void disablePlayer(String currentAccount) {
        UserResponse current = findByAccountForSession(currentAccount);
        userPageRepository.updateStatus(current.id(), "Disabled", now());

        operationLogService.log(
                current.account(), "DELETE", current.id(), current.role(),
                "Player disabled own account");
    }

    public UserResponse findByAccountForSession(String account) {
        if (account == null || account.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登入");
        }
        UserResponse user = userPageRepository.findByAccount(account.trim());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登入已過期或使用者不存在");
        }
        return user;
    }

    public UserResponse findById(Long id) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user id");
        }

        UserResponse user = userPageRepository.findById(id);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        return user;
    }

    private UserResponse findByAccount(String account) {
        UserResponse user = userPageRepository.findByAccount(account);

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Created user could not be found");
        }

        return user;
    }

    private void validate(UserRequest request, boolean update) {
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Request body is required");
        }

        String account = normalizeRequired(request.account(), "Account");
        normalizeRequired(request.username(), "Username");

        if (!account.matches(ALPHANUMERIC_REGEX)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Account can contain only English letters and numbers");
        }

        if (!update && (request.password() == null || request.password().isBlank())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Password is required");
        }

        if (request.password() != null
                && !request.password().isBlank()) {
            if (request.password().length() < 8) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
            }

            if (!request.password().matches(ALPHANUMERIC_REGEX)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Password can contain only English letters and numbers");
            }
        }

        normalizeRole(request.role());
        normalizeStatus(request.status());
    }

    private String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, field + " is required");
        }
        return value.trim();
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Role is required");
        }

        return switch (role.trim().toUpperCase()) {
            case "PLAYER" -> "PLAYER";
            case "ADMIN" -> "ADMIN";
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid role");
        };
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Status is required");
        }

        return switch (status.trim().toLowerCase()) {
            case "active" -> "Active";
            case "disabled" -> "Disabled";
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid status");
        };
    }

    private String now() {
        return LocalDateTime.now().format(FORMATTER);
    }
}
