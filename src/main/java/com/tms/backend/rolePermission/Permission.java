package com.tms.backend.rolePermission;

public enum Permission {
    // Project
    PROJECT_CREATE(PermissionCategory.PROJECT, "Create Projects", "Create a new project"),
    PROJECT_VIEW(PermissionCategory.PROJECT, "View Projects", "View projects created by other users"),
    PROJECT_UPDATE(PermissionCategory.PROJECT, "Update Projects", "Modify projects created by other users"),
    PROJECT_DELETE(PermissionCategory.PROJECT, "Delete Projects", "Delete projects created by other users"),
    JOB_PROVIDER_VIEW(PermissionCategory.PROJECT, "View Job Providers", "Show provider names"),
    JOB_EDITOR_VIEW(PermissionCategory.PROJECT, "View Job Editors", "Edit jobs in Phrase CAT editor"),
    JOB_REJECT(PermissionCategory.PROJECT, "Reject Jobs", "Reject jobs"),

    // Project Templates
    PROJECT_TEMP_CREATE(PermissionCategory.PROJECT_TEMPLATES, "Create Project Templates", "Create project templates"),
    PROJECT_TEMP_VIEW(PermissionCategory.PROJECT_TEMPLATES, "View Project Templates", "View project templates created by other users"),
    PROJECT_TEMP_UPDATE(PermissionCategory.PROJECT_TEMPLATES, "Update Project Templates", "Modify project templates created by other users"),
    PROJECT_TEMP_DELETE(PermissionCategory.PROJECT_TEMPLATES, "Delete Project Templates", "Delete project templates created by other users"),

    // Translation Memories
    TM_CREATE(PermissionCategory.TRANSLATION_MEMORIES, "Create Translation Memories", "Create TMs"),
    TM_VIEW(PermissionCategory.TRANSLATION_MEMORIES, "View Translation Memories", "View TMs created by other users"),
    TM_UPDATE(PermissionCategory.TRANSLATION_MEMORIES, "Update Translation Memories", "Modify TMs created by other users"),
    TM_DELETE(PermissionCategory.TRANSLATION_MEMORIES, "Delete Translation Memories", "Delete TMs created by other users"),
    TM_EXPORT(PermissionCategory.TRANSLATION_MEMORIES, "Export Translation Memories", "Export TMs created by other users"),
    TM_IMPORT(PermissionCategory.TRANSLATION_MEMORIES, "Import Translation Memories", "Import into TMs created by other users"),
    TM_UPDATE_TRANSLATION(PermissionCategory.TRANSLATION_MEMORIES, "Update Translations", "Edit translations in TM"),

    // Term Base
    TB_CREATE(PermissionCategory.TERM_BASE, "Create Term Bases", "Create TBs"),
    TB_VIEW(PermissionCategory.TERM_BASE, "View Term Bases", "View TBs created by other users"),
    TB_UPDATE(PermissionCategory.TERM_BASE, "Update Term Bases", "Modify TBs created by other users"),
    TB_DELETE(PermissionCategory.TERM_BASE, "Delete Term Bases", "Delete TBs created by other users"),
    TB_EXPORT(PermissionCategory.TERM_BASE, "Export Term Bases", "Export TBs created by other users"),
    TB_IMPORT(PermissionCategory.TERM_BASE, "Import Term Bases", "Import into TBs created by other users"),
    TB_APPROVE_TERMS(PermissionCategory.TERM_BASE, "Approve Terms", "Approve terms in TBs created by other users"),
    TB_UPDATE_TERM(PermissionCategory.TERM_BASE, "Update Terms", "Edit all terms in TB"),

    // Users
    USER_CREATE(PermissionCategory.USERS, "Create Users", "Create users"),
    USER_VIEW(PermissionCategory.USERS, "View Users", "View users created by other users"),
    USER_UPDATE(PermissionCategory.USERS, "Update Users", "Modify users created by other users"),
    USER_DELETE(PermissionCategory.USERS, "Delete Users", "Delete users created by other users"),

    // Client / Domain / Subdomain
    CDS_CREATE(PermissionCategory.CLIENT_DOMAIN_SUBDOMAIN, "Create Client/Domain/Subdomain", "Create clients, domains, subdomains"),
    CDS_VIEW(PermissionCategory.CLIENT_DOMAIN_SUBDOMAIN, "View Client/Domain/Subdomain", "View clients, domains, subdomains created by other users"),
    CDS_UPDATE(PermissionCategory.CLIENT_DOMAIN_SUBDOMAIN, "Update Client/Domain/Subdomain", "Modify clients, domains, subdomains created by other users"),
    CDS_DELETE(PermissionCategory.CLIENT_DOMAIN_SUBDOMAIN, "Delete Client/Domain/Subdomain", "Delete clients, domains, subdomains created by other users"),

    // Vendors
    VENDOR_CREATE(PermissionCategory.VENDORS, "Create Vendors", "Create vendors"),
    VENDOR_VIEW(PermissionCategory.VENDORS, "View Vendors", "View vendors created by other users"),
    VENDOR_UPDATE(PermissionCategory.VENDORS, "Update Vendors", "Modify vendors created by other users"),
    VENDOR_DELETE(PermissionCategory.VENDORS, "Delete Vendors", "Delete vendors created by other users"),

    // Analytics
    ANALYTICS_VIEW(PermissionCategory.ANALYTICS, "View Analytics", "View all data"),
    ANALYTICS_VIEW_BY_USER(PermissionCategory.ANALYTICS, "View Analytics by User", "View data owned by the user"),
    ANALYTICS_VIEW_NO_DATA(PermissionCategory.ANALYTICS, "View Analytics (No Data)", "View no data"),

    // Settings
    GLOBAL_SERVER_SETTINGS_UPDATE(PermissionCategory.SETTINGS, "Update Server Settings", "Modify global server settings"),
    MT_ENABLE(PermissionCategory.SETTINGS, "Enable Machine Translation", "Enable or disable Machine translation");

    private final PermissionCategory category;
    private final String displayName;
    private final String description;

    Permission(PermissionCategory category, String displayName, String description) {
        this.category = category;
        this.displayName = displayName;
        this.description = description;
    }

    public PermissionCategory getCategory() {
        return category;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
