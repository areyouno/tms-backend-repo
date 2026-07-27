package com.tms.backend.rolePermission;

public enum Permission {
    // Project
    PROJECT_CREATE(PermissionCategory.PROJECT, "Create Project", "Create a new project"),
    PROJECT_VIEW(PermissionCategory.PROJECT, "View Projects", "View projects created by other users"),
    PROJECT_UPDATE(PermissionCategory.PROJECT, "Update Project", "Modify projects created by other user"),
    PROJECT_DELETE(PermissionCategory.PROJECT, "Delete Project", "Delete projects created by other users"),
    WORKFLOW_UPDATE(PermissionCategory.PROJECT, "Update Workflow", "Update workflow assigned in the project"),

    PROJECT_FILE_EDIT(PermissionCategory.PROJECT, "Edit file", "Edit file in the CAT Editor"),
    PROJECT_FILE_DOWNLOAD_TRANSLATED(PermissionCategory.PROJECT, "Download file in xliff format", "Downloads the selected file in xliff format"),
    PROJECT_FILE_DOWNLOAD_TARGET(PermissionCategory.PROJECT, "Download file in original format", "Downloads the selected file in original format"),

    // Project Job Analysis
    PROJECT_ANALYSIS_CREATE(PermissionCategory.ANALYSIS, "Create Analysis", "Initiate Analysis for selected file"),
    PROJECT_ANALYSIS_VIEW(PermissionCategory.ANALYSIS, "View Analytics", "View Analysis data"),
    PROJECT_ANALYSIS_DOWNLOAD_CSV(PermissionCategory.ANALYSIS, "Download as CSV", "Download Analysis as CSV file"),
    PROJECT_ANALYSIS_DOWNLOAD_LOG(PermissionCategory.ANALYSIS, "Download as LOG", "Download Analysis as LOG file"),

    // Project Job Task list creation
    CREATE_TASK_LIST(PermissionCategory.PROJECT, "Create Task List", "Create a task list for a project"),

    // File Editor settings
    EDITOR_CONFIRM_TU(PermissionCategory.EDITOR, "Confirm TU", "Confirm translation unit"),
    EDITOR_UNCONFIRM_TU(PermissionCategory.EDITOR, "Unconfirm TU", "Unconfirm translation unit"),
    EDITOR_COPY_SOURCE_TO_TARGET(PermissionCategory.EDITOR, "Copy Source to Target", "Copy source text to target"),
    EDITOR_DELETE_TARGET(PermissionCategory.EDITOR, "Delete Target", "Delete target text"),
    EDITOR_SPLIT_TU(PermissionCategory.EDITOR, "Split TU", "Split translation unit"),
    EDITOR_JOIN_TU(PermissionCategory.EDITOR, "Join TU", "Join translation units"),
    EDITOR_LOCK_UNLOCK_TU(PermissionCategory.EDITOR, "Lock/Unlock TU", "Lock or unlock translation unit"),
    EDITOR_SELECT_ALL_TU(PermissionCategory.EDITOR, "Select All TU", "Select all translation units"),
    EDITOR_HOME(PermissionCategory.EDITOR, "Home", "Jump to first segment"),
    EDITOR_END(PermissionCategory.EDITOR, "End", "Jump to last segment"),
    EDITOR_EDIT_SOURCE(PermissionCategory.EDITOR, "Edit Source", "Edit source text"),
    EDITOR_QA(PermissionCategory.EDITOR, "QA", "Run QA on current segment"),
    EDITOR_UNDO(PermissionCategory.EDITOR, "Undo", "Undo last action"),
    EDITOR_REDO(PermissionCategory.EDITOR, "Redo", "Redo last undone action"),
    EDITOR_INSERT_ALL_TAGS(PermissionCategory.EDITOR, "Insert All Tags", "Insert all tags into target"),
    EDITOR_INSERT_TAG_NUMBER(PermissionCategory.EDITOR, "Insert Tag Number", "Insert a specific tag by number"),
    EDITOR_LEFT_TO_RIGHT_MARK(PermissionCategory.EDITOR, "Left to Right Mark", "Insert left-to-right mark"),
    EDITOR_RIGHT_TO_LEFT_MARK(PermissionCategory.EDITOR, "Right to Left Mark", "Insert right-to-left mark"),
    EDITOR_NEW_LINE(PermissionCategory.EDITOR, "New Line", "Insert a new line"),
    EDITOR_RUN_QA(PermissionCategory.EDITOR, "Run QA", "Run QA on the whole file"),
    EDITOR_FILTER(PermissionCategory.EDITOR, "Filter", "Filter segments"),
    EDITOR_REPLACE(PermissionCategory.EDITOR, "Replace", "Replace text in segments"),
    EDITOR_FIND_PREVIOUS(PermissionCategory.EDITOR, "Find Previous", "Find previous match"),
    EDITOR_FIND_NEXT(PermissionCategory.EDITOR, "Find Next", "Find next match"),
    EDITOR_ADD_TERM(PermissionCategory.EDITOR, "Add Term", "Add a term to the term base"),
    EDITOR_SEARCH_IN_TM_TB(PermissionCategory.EDITOR, "Search in TM/TB", "Search translation memories and term bases"),
    EDITOR_VIEW_COMMENTS(PermissionCategory.EDITOR, "Comments", "View comments"),
    EDITOR_VIEW_ADD_COMMENTS(PermissionCategory.EDITOR, "Add Comments", "Add comments"),
    EDITOR_VIEW_QA(PermissionCategory.EDITOR, "QA", "View QA panel"),
    EDITOR_VIEW_FILTER(PermissionCategory.EDITOR, "Filter", "View filter panel"),
    EDITOR_VIEW_CONCORDANCE_SEARCH(PermissionCategory.EDITOR, "Concordance Search", "View concordance search panel"),
    EDITOR_VIEW_PARTIAL_FULL_TAGS(PermissionCategory.EDITOR, "Partial/Full Tags", "View partial/full tags panel"),
    EDITOR_GO_TO_TU(PermissionCategory.EDITOR, "Go to TU", "Jump to a specific translation unit"),
    EDITOR_NEXT_TAB(PermissionCategory.EDITOR, "Next Tab", "Switch to the next tab"),
    EDITOR_PREVIOUS_TAB(PermissionCategory.EDITOR, "Previous Tab", "Switch to the previous tab"),
    EDITOR_DOWNLOAD_SOURCE(PermissionCategory.EDITOR, "Download Source", "Download the source file"),
    EDITOR_DOWNLOAD_XLIFF(PermissionCategory.EDITOR, "Download Xliff", "Download the xliff file"),
    EDITOR_MINIMUM_MATCH_RATE(PermissionCategory.EDITOR, "Minimum Match Rate", "Set the minimum match rate"),
    EDITOR_PRETRANSLATE_PRIORITY_TM(PermissionCategory.EDITOR, "Pretranslate Priority: TM", "Set pretranslate priority to TM"),
    EDITOR_PRETRANSLATE_PRIORITY_MT(PermissionCategory.EDITOR, "Pretranslate Priority: MT", "Set pretranslate priority to MT"),
    EDITOR_PRETRANSLATE_PRIORITY_SOURCE(PermissionCategory.EDITOR, "Pretranslate Priority: Source", "Set pretranslate priority to Source"),
    EDITOR_FONT_SIZE(PermissionCategory.EDITOR, "Font Size", "Change the editor font size"),
    EDITOR_HELP(PermissionCategory.EDITOR, "Help", "View help documentation"),
    EDITOR_ABOUT(PermissionCategory.EDITOR, "About", "View about information"),

    // Task list
    TASK_LIST_VIEW(PermissionCategory.TASK_LIST, "View Task List", "View task list created by other users"),
    TASK_LIST_EDIT_FILE(PermissionCategory.TASK_LIST, "Edit Task List File", "Edit files in the task list"),
    TASK_LIST_DELETE(PermissionCategory.TASK_LIST, "Delete Task List", "Delete task list created by other users"),

    JOB_REJECT(PermissionCategory.TASK_LIST, "Reject Job", "Reject job"),

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

    // Settings
    GLOBAL_SERVER_SETTINGS_UPDATE(PermissionCategory.SETTINGS, "Update Server Settings", "Modify global server settings"),
    MT_ENABLE(PermissionCategory.SETTINGS, "Enable Machine Translation", "Enable or disable Machine translation"),

    // Project Templates
    PROJECT_TEMP_CREATE(PermissionCategory.PROJECT_TEMPLATES, "Create Project Templates", "Create project templates"),
    PROJECT_TEMP_VIEW(PermissionCategory.PROJECT_TEMPLATES, "View Project Templates", "View project templates created by other users"),
    PROJECT_TEMP_UPDATE(PermissionCategory.PROJECT_TEMPLATES, "Update Project Templates", "Modify project templates created by other users"),
    PROJECT_TEMP_DELETE(PermissionCategory.PROJECT_TEMPLATES, "Delete Project Templates", "Delete project templates created by other users"),

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

    // User Profile
    VIEW_PROFILE(PermissionCategory.USER_PROFILE, "View Profile", "View own user profile"),
    UPDATE_PASSWORD(PermissionCategory.USER_PROFILE, "Update Password", "Update own account password"),
    VIEW_LOGIN_HISTORY(PermissionCategory.USER_PROFILE, "View Login History", "View own login history"),

    // Vendors
    VENDOR_CREATE(PermissionCategory.VENDORS, "Create Vendors", "Create vendors"),
    VENDOR_VIEW(PermissionCategory.VENDORS, "View Vendors", "View vendors created by other users"),
    VENDOR_UPDATE(PermissionCategory.VENDORS, "Update Vendors", "Modify vendors created by other users"),
    VENDOR_DELETE(PermissionCategory.VENDORS, "Delete Vendors", "Delete vendors created by other users");

    

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
