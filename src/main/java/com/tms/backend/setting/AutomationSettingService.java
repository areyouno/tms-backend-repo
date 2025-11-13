package com.tms.backend.setting;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.tms.backend.project.ProjectAutomationRule;
import com.tms.backend.project.StatusAutomationSetting;
import com.tms.backend.user.User;
import com.tms.backend.user.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class AutomationSettingService {

    private final AutomationSettingRepository repo;
    private final UserRepository userRepo;
    
    // set default rule values
    private static final Set<ProjectAutomationRule> DEFAULT_RULES = EnumSet.of(
            ProjectAutomationRule.ASSIGNED_2,
            ProjectAutomationRule.COMPLETED_2,
            ProjectAutomationRule.CANCELLED
    );

    public AutomationSettingService(AutomationSettingRepository repo, UserRepository userRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
    }

    @Transactional
    public AutomationSetting getUserAutomationSettingByUid(String uid) {
        User user = userRepo.findByUid(uid)
            .orElseThrow(() -> new RuntimeException("User not found with uid: " + uid));

        return getOrCreateUserAutomationSetting(user);
    }

    @Transactional
    public AutomationSetting getOrCreateUserAutomationSetting(User user) {
        return repo.findByUserUid(user.getUid())
                .orElseGet(() -> {
                    AutomationSetting setting = new AutomationSetting();
                    setting.setUser(user); 

                    StatusAutomationSetting statusAutomationSetting = new StatusAutomationSetting();
                    statusAutomationSetting.setEnabledRules(DEFAULT_RULES);
                    setting.setStatusAutomationSetting(statusAutomationSetting);

                    return repo.save(setting);
                });
    }

    @Transactional
    public AutomationSetting updateUserAutomationRules(String uid, Set<String> ruleNames) {
        AutomationSetting setting = repo.findByUserUid(uid)
            .orElseGet(() -> {
                AutomationSetting s = new AutomationSetting();
                User user = userRepo.findByUid(uid)
                    .orElseThrow(() -> new RuntimeException("User not found"));
                s.setUser(user);
                return s;
            });

        Set<ProjectAutomationRule> newRules = ruleNames.stream()
            .map(ProjectAutomationRule::valueOf)
            .collect(Collectors.toSet());

        setting.getStatusAutomationSetting().setEnabledRules(newRules);
        return repo.save(setting);
    }

}
