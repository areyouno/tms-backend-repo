package com.tms.backend.setting;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tms.backend.project.ProjectAutomationRule;
import com.tms.backend.user.User;
import com.tms.backend.user.UserService;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/settings/automation")
public class AutomationSettingController {

    private final AutomationSettingService service;
    private final UserService userService;

    public AutomationSettingController(AutomationSettingService service, UserService userService) {
        this.service = service;
        this.userService = userService;
    }

    @GetMapping("/user/{uid}")
    public ResponseEntity<Set<String>> getOrCreateUserAutomationSetting(@PathVariable String uid) {
        User user = userService.findByUid(uid)
        .orElseThrow(() -> new RuntimeException("User not found with uid: " + uid));

        AutomationSetting setting = service.getOrCreateUserAutomationSetting(user);

        Set<String> ruleNames = setting.getStatusAutomationSetting()
                .getEnabledRules()
                .stream()
                .map(ProjectAutomationRule::name)
                .collect(Collectors.toSet());
    
        return ResponseEntity.ok(ruleNames);
    }

    @PutMapping("/user/{uid}")
    public ResponseEntity<Set<String>> updateUserAutomationSetting(
        @PathVariable String uid,
        @RequestBody Set<String> ruleNames) {
    
        AutomationSetting setting = service.updateUserAutomationRules(uid, ruleNames);

        Set<String> updatedRules = setting.getStatusAutomationSetting()
                .getEnabledRules()
                .stream()
                .map(ProjectAutomationRule::name)
                .collect(Collectors.toSet());

        return ResponseEntity.ok(updatedRules);
    }
}
