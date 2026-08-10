package com.tms.backend.auth;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tms.backend.dto.LoginHistoryDTO;
import com.tms.backend.user.User;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class LoginHistoryService {

    private final LoginHistoryRepository loginHistoryRepository;

    public LoginHistoryService(LoginHistoryRepository loginHistoryRepository) {
        this.loginHistoryRepository = loginHistoryRepository;
    }

    public void recordLogin(User user, HttpServletRequest request) {
        LoginHistory history = new LoginHistory();
        history.setUser(user);
        history.setLoginAt(LocalDateTime.now());
        history.setIpAddress(extractIpAddress(request));
        history.setUserAgent(request.getHeader("User-Agent"));
        loginHistoryRepository.save(history);
    }

    public List<LoginHistoryDTO> getHistoryForUser(Long userId) {
        return loginHistoryRepository.findByUserIdOrderByLoginAtDesc(userId).stream()
            .map(this::toDTO)
            .toList();
    }

    private String extractIpAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // The header can contain a client, proxy1, proxy2 chain; the first entry is the original client.
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private LoginHistoryDTO toDTO(LoginHistory history) {
        LocalDateTime loginAt = history.getLoginAt();
        return new LoginHistoryDTO(
            history.getId(),
            loginAt.toLocalDate(),
            loginAt.toLocalTime(),
            history.getIpAddress(),
            history.getUserAgent()
        );
    }
}
