package com.tms.backend.user;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tms.backend.dto.OwnerDTO;
import com.tms.backend.dto.UpdateUserDTO;
import com.tms.backend.request.RegisterRequest;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
// @RequestMapping("/users")
public class UserController {

    private final UserService userService;


    // @Value("${app.frontend-url}")
    // private String frontendUrl;

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

    @PatchMapping("/update-user")
    public ResponseEntity<?> updateUser(@RequestBody UpdateUserDTO request, Authentication authentication) {
        // extract uid
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String uid = userDetails.getUid();

        userService.updateUser(uid, request);
        return ResponseEntity.ok("User updated");
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/providers")
    public List<User> getProviderUsers() {
        return userService.getProviders();
    }

    @GetMapping("/owners")
    public List<OwnerDTO> getOwners() {
        return userService.getOwners().stream()
        .map(u -> new OwnerDTO(
            u.getUid(),
            (u.getFirstName() != null ? u.getFirstName() : "") +
            " " +
            (u.getLastName() != null ? u.getLastName() : "") 
        )).toList();
    }

    @GetMapping("/verify")
    public void verifyUser(@RequestParam String token, HttpServletResponse response) throws IOException {
        try {
            userService.markUserAsVerified(token);
            // Redirect to login page after successful verification
            response.sendRedirect("http://localhost:5173/login?verified=true");
        } catch (Exception e) {
            // Redirect to error page or login with error message
            response.sendRedirect("http://localhost:5173/login?error=verification_failed");
        }
    }
}
