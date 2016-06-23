package com.intuit.wasabi.auditlogobjects;

/**
 * Holds a list of properties to be able to search through or sort by.
 */
public enum AuditLogProperty {

    FIRSTNAME("firstname"), LASTNAME("lastname"), USERNAME("username"), MAIL("mail"), USER("user"),
    APP("app"), EXPERIMENT("experiment"), BUCKET("bucket"),
    TIME("time"),
    ATTR("attr"), BEFORE("before"), AFTER("after"),
    DESCRIPTION("desc"), ACTION("action");

    private final String key;

    /**
     * Constructor.
     *
     * @param key the key to be used
     */
    AuditLogProperty(String key) {
        this.key = key;
    }

    /**
     * Returns the key for the search string.
     *
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the key list.
     *
     * @return the key list.
     */
    public static String[] keys() {
        String[] keys = new String[values().length];
        for (int i = 0; i < keys.length; ++i) {
            keys[i] = values()[i].getKey();
        }

        return keys;
    }

    /**
     * Returns the property for the given key.
     *
     * @param key the key
     * @return the property
     */
    public static AuditLogProperty forKey(String key) {
        for (AuditLogProperty property : values()) {
            if (property.getKey().equalsIgnoreCase(key)) {
                return property;
            }
        }

        return null;
    }
}
