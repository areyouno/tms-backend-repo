package com.tms.demoservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Commit;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.tms.backend.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepo;

    // @Autowired
    // private EmailVerificationCodeRepository codeRepo;

    @BeforeEach
    void setUp() {
        // Optional: clear & seed test data here
    }

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void cleanDatabase() {
        // codeRepo.deleteAll(); // child first
        userRepo.deleteAll(); // parent next

        entityManager.createNativeQuery("ALTER TABLE users AUTO_INCREMENT = 1")
                 .executeUpdate();
    }

    @Test
    @Commit
    void endToEnd_verify_happyPath() throws Exception {

        /* ----- step 1: register ----- */
        String registerJson = """
            {
              "email": "@gmail.com",
              "password": "secret123",
              "firstName": "Gecko"
            }
        """;

        mockMvc.perform(post("/register")        
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson))
               .andExpect(status().isOk());

        // user is now verified
        assertTrue(userRepo.findByEmail("@gmail.com")
                           .orElseThrow()
                           .isVerified());

        // verification code row was deleted
        // assertTrue(codeRepo.findByUser(user).isEmpty());
    }
}
