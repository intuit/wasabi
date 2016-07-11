package com.intuit.wasabi.auditlog.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.intuit.wasabi.auditlog.AuditLog;
import com.intuit.wasabi.auditlogobjects.AuditLogAction;
import com.intuit.wasabi.auditlogobjects.AuditLogComparator;
import com.intuit.wasabi.auditlogobjects.AuditLogEntry;
import com.intuit.wasabi.auditlogobjects.AuditLogProperty;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.repository.AuditLogRepository;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Implements the AuditLog with a default implementation.
 */
public class AuditLogImpl implements AuditLog {

    private final AuditLogRepository repository;
    private int limit;

    /**
     * Constructs the AuditLogImpl. Should be called by Guice.
     *
     * @param repository the repository to query from.
     * @param limit      maximum audit log to fetch
     */
    @Inject
    public AuditLogImpl(final AuditLogRepository repository, final @Named("auditlog.fetchlimit") int limit) {
        this.repository = repository;
        this.limit = limit;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AuditLogEntry> getAuditLogs(Application.Name applicationName, String filterMask, String sortOrder) {
        return filterAndSort(repository.getAuditLogEntryList(applicationName, limit), filterMask, sortOrder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AuditLogEntry> getAuditLogs(String filterMask, String sortOrder) {
        return filterAndSort(repository.getCompleteAuditLogEntryList(limit), filterMask, sortOrder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AuditLogEntry> getGlobalAuditLogs(String filterMask, String sortOrder) {
        return filterAndSort(repository.getGlobalAuditLogEntryList(limit), filterMask, sortOrder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AuditLogEntry> filter(List<AuditLogEntry> auditLogEntries, String filterMask) {
        if (StringUtils.isBlank(filterMask)) {
            return auditLogEntries;
        }

        String[] mask = prepareMask(filterMask);
        boolean filterFullText = !mask[0].contains("=");
        String fullTextPattern = null;
        if (filterFullText) {
            fullTextPattern = mask[0].startsWith("\\-") ? mask[0].substring(2, mask[0].length()) : mask[0];
        }

        Map<String, String> optionsMap = new HashMap<>();

        for (Iterator<AuditLogEntry> iter = auditLogEntries.iterator(); iter.hasNext(); ) {
            AuditLogEntry auditLogEntry = iter.next();
            boolean remove = false;

            // filter for each key value pair until none is left or the entry should be removed
            for (int i = filterFullText ? 1 : 0; i < mask.length && !remove; ++i) {
                // determine key and value, fail if not well formatted
                String[] keyValue = mask[i].split("=");
                String options = "";
                if (keyValue.length > 2) { // return empty list if invalid
                    return Collections.emptyList();
                } else if (keyValue.length == 1) { // skip empty filters
                    continue;
                } else {
                    // options
                    if (keyValue[1].startsWith("{") && keyValue[1].contains("}")) {
                        options = keyValue[1].substring(1, keyValue[1].indexOf("}"));
                        keyValue[1] = keyValue[1].substring(keyValue[1].indexOf("}") + 1);
                        optionsMap.put(keyValue[0], options);
                    }
                }
                String pattern = keyValue[1].startsWith("\\-") ? keyValue[1].substring(2, keyValue[1].length()) : keyValue[1];
                remove = !singleFieldSearch(auditLogEntry, keyValue[0], pattern, options, !keyValue[1].startsWith("\\-"));
            }

            if (filterFullText && !remove) {
                remove = !fullTextSearch(auditLogEntry, fullTextPattern, optionsMap, !mask[0].startsWith("\\-"));
            }

            if (remove) {
                iter.remove();
            }
        }

        return auditLogEntries;
    }

    /**
     * Prepares the filter mask as it is defined by the rules described in {@link #filter(List, String)}.
     *
     * @param filterMask the filter mask
     * @return the individual filters
     */
    private String[] prepareMask(String filterMask) {
        String filterMaskEsc = filterMask.replaceAll(",(" + StringUtils.join(AuditLogProperty.keys(), "|") + ")+", "\\\\,$1");
        return filterMaskEsc.toLowerCase().split("\\\\,");
    }

    /**
     * Calls all distinguishing getters of the auditLogEntry until a match is found and returns {@code filter}.
     * If no match is found, {@code !filter} is returned.
     * Note: This method is case-insensitive.
     *
     * @param auditLogEntry the auditlog entry
     * @param pattern the pattern to search
     * @param filter the return value on success
     * @return {@code filter} on match, {@code !filter} otherwise
     */
    private boolean fullTextSearch(AuditLogEntry auditLogEntry, String pattern, Map<String, String> options, boolean filter) {
        for (String key : AuditLogProperty.keys()) {
            if (singleFieldSearch(auditLogEntry, key, pattern, options.get(key), filter) == filter) {
                return filter;
            }
        }
        if (contains(auditLogEntry.getExperimentId(), pattern)) return filter;
        return !filter;
    }

    /**
     * Returns true iff both, container and contained, are not blank and if
     * {@code container.toString().toLowerCase().contains(contained.toString().toLowerCase())}.
     * Note: This method is case-insensitive!
     *
     * @param container the string to search in
     * @param contained the string to search for
     * @return true if container contains contained
     */
    private boolean contains(Object container, Object contained) {
        if (container == null || contained == null) {
            return false;
        }
        String s1 = container.toString().toLowerCase();
        String s2 = contained.toString().toLowerCase();
        return s1.contains(s2);
    }
    
    /**
     * Calls the getter identified by {@code key.toLowerCase()} of the auditLogEntry returns {@code filter} on success.
     * If no match is found, {@code !filter} is returned.
     *
     * The allowed keys and their fields are:
     * <ul>
     *     <li>firstname {@link AuditLogEntry#getUser()}{@link UserInfo#getFirstName()}</li>
     *     <li>lastname {@link AuditLogEntry#getUser()}{@link UserInfo#getLastName()}</li>
     *     <li>username {@link AuditLogEntry#getUser()}{@link UserInfo#getUsername()} and {@link UserInfo#getUserId()}</li>
     *     <li>mail {@link AuditLogEntry#getUser()}{@link UserInfo#getEmail()}</li>
     *     <li>action {@link AuditLogEntry#getAction()} and {@link AuditLogAction#getDescription(AuditLogEntry)}</li>
     *     <li>desc {@link AuditLogEntry#getAction()} and {@link AuditLogAction#getDescription(AuditLogEntry)}</li>
     *     <li>experiment {@link AuditLogEntry#getExperimentLabel()}</li>
     *     <li>bucket {@link AuditLogEntry#getBucketLabel()}</li>
     *     <li>app {@link AuditLogEntry#getApplicationName()}</li>
     *     <li>time {@link AuditLogEntry#getTimeString()}</li>
     *     <li>attr {@link AuditLogEntry#getChangedProperty()}</li>
     *     <li>before {@link AuditLogEntry#getBefore()}</li>
     *     <li>after {@link AuditLogEntry#getAfter()}</li>
     *     <li>fullname {@link AuditLogEntry#getUser()}{@link UserInfo#getFirstName()} combined with {@link UserInfo#getLastName()}</li>
     * </ul>
     * Note: This method is case-insensitive!
     *
     * @param auditLogEntry the auditLogEntry
     * @param key the key determining the checked field
     * @param pattern the pattern to search
     * @param filter the return value on success
     * @return {@code filter} on match, {@code !filter} otherwise
     */
    /*test*/ boolean singleFieldSearch(AuditLogEntry auditLogEntry, String key, String pattern, String options, boolean filter) {
        AuditLogProperty property = AuditLogProperty.forKey(key);
        boolean filtered = !filter;
        
        if (property == null) {
            return filtered;
        }
        switch (property) {
            case FIRSTNAME: filtered = contains(auditLogEntry.getUser().getFirstName(), pattern) ? filter : !filter;
                break;
            case LASTNAME: filtered = contains(auditLogEntry.getUser().getLastName(), pattern) ? filter : !filter;
                break;
            case USERNAME: filtered = (contains(auditLogEntry.getUser().getUsername(), pattern) || (contains(auditLogEntry.getUser().getUserId(), pattern)) ? filter : !filter);
                break;
            case MAIL: filtered = contains(auditLogEntry.getUser().getEmail(), pattern) ? filter : !filter;
                break;
            case ACTION: // fall through
            case DESCRIPTION: filtered = (contains(auditLogEntry.getAction(), pattern) ? filter : !filter) || (contains(AuditLogAction.getDescription(auditLogEntry), pattern) ? filter : !filter);
                break;
            case EXPERIMENT: filtered = contains(auditLogEntry.getExperimentLabel(), pattern) ? filter : !filter;
                break;
            case BUCKET: filtered = contains(auditLogEntry.getBucketLabel(), pattern) ? filter : !filter;
                break;
            case APP: filtered = contains(auditLogEntry.getApplicationName(), pattern) ? filter : !filter;
                break;
            case TIME:
                filtered = contains(formatDateLikeUI(auditLogEntry.getTime(), StringUtils.isBlank(options) ? "+0000" : options), pattern) ? filter : !filter;
                break;
            case ATTR: filtered = contains(auditLogEntry.getChangedProperty(), pattern) ? filter : !filter;
                break;
            case BEFORE: filtered = contains(auditLogEntry.getBefore(), pattern) ? filter : !filter;
                break;
            case AFTER: filtered = contains(auditLogEntry.getAfter(), pattern) ? filter : !filter;
                break;
            case USER: filtered = contains(auditLogEntry.getUser().getFirstName() + " " + auditLogEntry.getUser().getLastName(), pattern) ? filter : !filter;
                break;
            default:
                break;
        }
        return filtered;
    }

    private String formatDateLikeUI(Calendar date, String timeZoneOffset) {
        TimeZone timeZone = TimeZone.getTimeZone("GMT" + timeZoneOffset);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, YYYY HH:mm:ss a");
        sdf.setTimeZone(timeZone);
        return sdf.format(date.getTime());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AuditLogEntry> sort(List<AuditLogEntry> auditLogEntries, final String sortOrder) {
        if (StringUtils.isBlank(sortOrder) || "-time".equalsIgnoreCase(sortOrder)) {
            return auditLogEntries;
        }
        Collections.sort(auditLogEntries, new AuditLogComparator(sortOrder));
        return auditLogEntries;
    }

    /**
     * Filters first and sorts then by subsequent calls to {@link #filter(List, String)} and {@link #sort(List, String)}.
     * Note: This method is case-insensitive!
     *
     * @param filterMask      the filter mask
     * @param sortOrder       the sort order
     * @return a list of filtered and sorted audit logs
     */
    private List<AuditLogEntry> filterAndSort(List<AuditLogEntry> auditLogEntries, String filterMask, String sortOrder) {
        return sort(filter(auditLogEntries, filterMask), sortOrder);
    }
}
