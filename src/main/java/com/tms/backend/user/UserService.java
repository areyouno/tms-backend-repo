package com.tms.backend.user;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tms.backend.dto.CreateUserDTO;
import com.tms.backend.dto.UpdateUserDTO;
import com.tms.backend.email.EmailService;
import com.tms.backend.exception.ResourceNotFoundException;
import com.tms.backend.role.Role;
import com.tms.backend.role.RoleRepository;
import com.tms.backend.verificationToken.VerificationToken;
import com.tms.backend.verificationToken.VerificationTokenRepository;

import jakarta.persistence.EntityNotFoundException;


@Service
public class UserService {
    private final PasswordEncoder passwordEncoder;
    private UserRepository userRepo;
    private RoleRepository roleRepo;
    private EmailService emailService;
    private VerificationTokenRepository tokenRepo;

    public UserService(PasswordEncoder passwordEncoder, UserRepository userRepo, RoleRepository roleRepo, EmailService emailService,
                       VerificationTokenRepository tokenRepo) {
        this.passwordEncoder = passwordEncoder;
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.emailService = emailService;
        this.tokenRepo = tokenRepo;
    }

    @Transactional
    public void register(String email, String password) {
        if (userRepo.findByEmail(email).isPresent())
            throw new RuntimeException("User with this email already exists");

        String hashedPassword = passwordEncoder.encode(password);

        User user = new User();
        user.setEmail(email);
        user.setPassword(hashedPassword);
        userRepo.save(user);

        String tokenValue = UUID.randomUUID().toString();
        VerificationToken token = new VerificationToken(user, tokenValue, 168);
        tokenRepo.save(token);
        
        // Send verification email
        String verLink = "http://localhost:8080/verify?token=" + tokenValue;
        emailService.sendVerificationEmail(user.getEmail(), verLink);
    }

    @Transactional
    public User completeUserSignup(String uid, UpdateUserDTO request) {
        User user = userRepo.findByUid(uid)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            updateUserFields(user, request);
        user.setProfileComplete(true);

       return userRepo.save(user);
    }

    public void updateUser(String uid, UpdateUserDTO request) {
        User user = userRepo.findByUid(uid)
                .orElseThrow(() -> new RuntimeException("User not found"));

        updateUserFields(user, request);

        userRepo.save(user);
    }

    private void updateUserFields(User user, UpdateUserDTO request) {
        if (request.firstName() != null)
            user.setFirstName(request.firstName());
        if (request.lastName() != null)
            user.setLastName(request.lastName());
        if (request.country() != null)
            user.setCountry(request.country());
        if (request.email() != null)
            user.setEmail(request.email());
        if (request.isVerified() != null)
            user.setVerified(request.isVerified());
        if (request.organizationName() != null)
            user.setOrganizationName(request.organizationName());
        if (request.organizationSize() != null)
            user.setOrganizationSize(request.organizationSize());
        if (request.roleId() != null) {
        Role role = roleRepo.findById(request.roleId())
            .orElseThrow(() -> new EntityNotFoundException("Role not found with name: " + request.roleId()));
        user.setRole(role);
        }
        if (request.referralSource() != null)
            user.setReferralSource(request.referralSource());
        if (request.agreedToTerms() != null)
            user.setAgreedToTerms(request.agreedToTerms());

        if (request.isProfileComplete() != null) {
            user.setProfileComplete(request.isProfileComplete());
        }
    }

    public void createUser(CreateUserDTO dto){
        if (userRepo.existsByEmail(dto.email())){
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setEmail(dto.email());
        user.setUsername(dto.username());
        
        Role role = roleRepo.findById(dto.roleId())
            .orElseThrow(() -> new EntityNotFoundException("Role not found with name: " + dto.roleId()));
        user.setRole(role);

        
        // TODO: Confirm how to handle active and inactive setting
        if (dto.isActive()){
            // if isActive
            String tokenValue = UUID.randomUUID().toString();
            VerificationToken token = new VerificationToken(user, tokenValue, 168);
            tokenRepo.save(token);

            // Send verification email
            String verLink = "http://localhost:8080/verify?token=" + tokenValue;
            emailService.sendVerificationEmail(user.getEmail(), verLink);
        }
        
    }
    
    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    public User getUserByUid(String uid) {
        return userRepo.findByUid(uid)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<User> getProviders() {
        List<String> providerRoles = Arrays.asList("linguist", "vendor");
        return userRepo.findByRoleNameIn(providerRoles);
    }

    public List<User> getOwners(){
        List<String> ownerRoles = Arrays.asList("project_manager", "administrator");
        return userRepo.findByRoleNameIn(ownerRoles);
    }
    
    @Transactional
    public void markUserAsVerified(String token) {
        VerificationToken vToken = tokenRepo.findByToken(token)
            .orElseThrow(() -> new RuntimeException("Invalid verification token"));
        
        if (vToken.isUsed()) {
            throw new RuntimeException("Verification token has already been used");
        }

        User user = vToken.getUser();
        
        // Verify user
        user.setVerified(true);
        userRepo.save(user);

        // Mark token as used
        vToken.setUsed(true);
        tokenRepo.save(vToken);
    }

    public Optional<User> findByUid(String uid) {
        return userRepo.findByUid(uid);
    }
}
