package com.tms.backend.user;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tms.backend.dto.CreateUserDTO;
import com.tms.backend.dto.OwnerDTO;
import com.tms.backend.dto.ReferenceDTO;
import com.tms.backend.dto.SetPasswordDTO;
import com.tms.backend.dto.UpdateUserByIdDTO;
import com.tms.backend.dto.UpdateUserDTO;
import com.tms.backend.dto.UserDTO;
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
        //set role to "user"
        Role role = new Role();
        role.setId(5L);
        user.setRole(role);
        userRepo.save(user);
        
        String tokenValue = UUID.randomUUID().toString();
        VerificationToken token = new VerificationToken(user, tokenValue, 168);
        tokenRepo.save(token);
        
        // Send verification email
        String verLink = "https://xliffl10n.latispass.net/api/users/verify?token=" + tokenValue;
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

    public void updateUserById(Long userId, UpdateUserByIdDTO request) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        updateUserFieldsInSettings(user, request);

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

    private void updateUserFieldsInSettings(User user, UpdateUserByIdDTO request) {
        if (request.firstName() != null)
            user.setFirstName(request.firstName());
        if (request.lastName() != null)
            user.setLastName(request.lastName());
        if (request.email() != null)
            user.setEmail(request.email());

        if (request.roleId() != null) {
        Role role = roleRepo.findById(request.roleId())
            .orElseThrow(() -> new EntityNotFoundException("Role not found with name: " + request.roleId()));
        user.setRole(role);
        }

        if (request.timeZone() != null) {
            user.setTimeZone(request.timeZone());
        }

        if (request.sourceLang() != null) {
            user.setSourceLang(request.sourceLang());
        }

        if (request.targetLanguages() != null) {
            user.getTargetLanguages().clear();
            user.getTargetLanguages().addAll(request.targetLanguages());
        }

        if (request.note() != null) {
            user.setNote(request.note());
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
        user.setActive(dto.isActive());
        
        user.setTimeZone(dto.timeZone() != null ? dto.timeZone() : ZoneId.of("Asia/Manila"));
        user.setSourceLang(dto.sourceLang());
        user.setTargetLanguages(dto.targetLanguages() != null ? dto.targetLanguages() : new HashSet<>());

        userRepo.save(user);

        // send email
        String tokenValue = UUID.randomUUID().toString();
        VerificationToken token = new VerificationToken(user, tokenValue, 168);
        tokenRepo.save(token);

        // Send verification email
        String verLink = "https://xliffl10n.latispass.net/api/users/verify?token=" + tokenValue;
        emailService.sendVerificationEmail(user.getEmail(), verLink);
        
    }

    @Transactional
    public void setPassword(SetPasswordDTO dto) {

        VerificationToken token = tokenRepo
            .findByToken(dto.token())
            .orElseThrow(() -> new RuntimeException("Invalid token"));

        // if (token.isExpired()) {
        //     throw new RuntimeException("Token expired");
        // }

        User user = token.getUser();

        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        user.setVerified(true);

        userRepo.save(user);

        // token should be single-use
        tokenRepo.delete(token);
    }
    
    public List<UserDTO> getAllUsers() {
        return userRepo.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    public User getUserById(Long id) {
        return userRepo.findById(id)
            .orElseThrow(() ->
                new EntityNotFoundException("User not found with id: " + id)
            );
    }

    public User getUserByUid(String uid) {
        return userRepo.findByUid(uid)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<User> getProviders() {
        List<String> providerRoles = Arrays.asList("linguist", "vendor");
        return userRepo.findByRoleNameIn(providerRoles);
    }

    public List<OwnerDTO> getOwners(){
        List<String> ownerRoles = Arrays.asList("project_manager", "administrator", "linguist");
        List<User> users = userRepo.findByRoleNameIn(ownerRoles);

        return users.stream()
            .map(user -> new OwnerDTO(
                    user.getId(),
                    user.getFirstName() + " " + user.getLastName() + " (" + user.getRole().getName() + ")"
            ))
            .toList();
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

    private UserDTO toDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getUid(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getCountry(),
                user.isVerified(),
                user.isProfileComplete(),
                user.getReferralSource(),
                user.getOrganizationName(),
                user.getOrganizationSize(),
                user.getUsername(),
                user.isActive(),
                user.getRole() != null ? new ReferenceDTO(user.getRole().getId(), user.getRole().getName()) : null);
    }
}
