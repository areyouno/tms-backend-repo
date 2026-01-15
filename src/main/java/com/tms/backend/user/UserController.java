package com.tms.backend.user;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tms.backend.dto.CreateUserDTO;
import com.tms.backend.dto.OwnerDTO;
import com.tms.backend.dto.ProviderDTO;
import com.tms.backend.dto.SetPasswordDTO;
import com.tms.backend.dto.UpdateUserByIdDTO;
import com.tms.backend.dto.UpdateUserDTO;
import com.tms.backend.dto.UserDTO;
import com.tms.backend.request.RegisterRequest;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        try {
            userService.register(request.email(), request.password());
            return ResponseEntity.ok("Registration successful. Please check your email to verify your account.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/complete-signup")
    public ResponseEntity<Map<String, Object>> completeSignup(@RequestBody UpdateUserDTO request, Authentication authentication) {
        // extract uid
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String uid = userDetails.getUid();

        userService.completeUserSignup(uid, request);

        // Fetch the updated user data
        User updatedUser = userService.getUserByUid(uid);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Signup completed successfully");

        // Add user data to response
        Map<String, Object> userData = new HashMap<>();
        userData.put("firstName", updatedUser.getFirstName());
        userData.put("lastName", updatedUser.getLastName());
        userData.put("email", updatedUser.getEmail());
        userData.put("country", updatedUser.getCountry());
        userData.put("organizationName", updatedUser.getOrganizationName());
        userData.put("organizationSize", updatedUser.getOrganizationSize());
        userData.put("roleId", updatedUser.getRole().getId());
        userData.put("isVerified", updatedUser.isVerified());
        userData.put("isProfileComplete", updatedUser.isProfileComplete());

        response.put("userData", userData);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/create")
    public ResponseEntity<String> create(@RequestBody CreateUserDTO request){
        try {
            userService.createUser(request);
            return ResponseEntity.ok("User created successfully.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/set-password")
    public ResponseEntity<Void> setPassword(@RequestBody SetPasswordDTO dto) {
        userService.setPassword(dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('administrator')")
    public List<UserDTO> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PatchMapping("/update-user")
    public ResponseEntity<?> updateUser(@RequestBody UpdateUserDTO request, Authentication authentication) {
        // extract uid
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String uid = userDetails.getUid();

        userService.updateUser(uid, request);
        return ResponseEntity.ok("User updated");
    }

    @PatchMapping("/update-by-userId")
    public ResponseEntity<?> updateUserById(
            @RequestBody UpdateUserByIdDTO request,
            Authentication authentication) {

        userService.updateUserById(request.userId(), request);
        return ResponseEntity.ok("User updated");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDeleteUser(@PathVariable Long id) {
        userService.softDeleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteUser(@PathVariable Long id) {
        userService.hardDeleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/providers")
    public List<ProviderDTO> getProviderUsers() {
        return userService.getProviders().stream()
        .map(user -> new ProviderDTO(
            user.getUid(),
            (user.getLastName() != null ? user.getLastName() : "") +
            " " +
            (user.getFirstName() != null ? user.getFirstName() : "")
        )).toList();
    }

    @GetMapping("/owners")
    public List<OwnerDTO> getOwners() {
        return userService.getOwners().stream()
        .map(user -> new OwnerDTO(
            user.id(),
            user.ownerName()
        )).toList();
    }

    @GetMapping("/verify")
    public void verifyUser(@RequestParam String token, HttpServletResponse response) throws IOException {
        try {
            userService.markUserAsVerified(token);
            // Redirect to login page after successful verification
            response.sendRedirect("https://xliffl10n.latispass.net/?verified=true");
        } catch (Exception e) {
            // Redirect to error page or login with error message
            response.sendRedirect("https://xliffl10n.latispass.net/?error=verification_failed");
        }
    }

    @GetMapping("/verifyUserCreated")
    public void verifyUserCreated(@RequestParam String token, HttpServletResponse response) throws IOException {
        try {
            userService.markUserAsVerified(token);
            // Redirect to login page after successful verification
            response.sendRedirect("https://xliffl10n.latispass.net/setPassword");
        } catch (Exception e) {
            // Redirect to error page or login with error message
            response.sendRedirect("https://xliffl10n.latispass.net/?error=verification_failed");
        }
    }
}
