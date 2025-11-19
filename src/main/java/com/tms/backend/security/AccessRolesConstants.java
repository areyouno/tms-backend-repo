package com.tms.backend.security;

public class AccessRolesConstants {

    // Single role access
    public static final String ADMIN_ONLY = "hasRole('administrator')";
    public static final String MANAGER_ONLY = "hasRole('project_manager')";
    
    // Multiple role access
    public static final String ADMIN_OR_PM = "hasAnyRole('administrator', 'project_manager')";
    
    // Authentication-based
    public static final String AUTHENTICATED = "isAuthenticated()";
    public static final String ANONYMOUS = "isAnonymous()";
    
    // Private constructor to prevent instantiation
    private AccessRolesConstants() {
        throw new IllegalStateException("Constants class");
    }
    
}
