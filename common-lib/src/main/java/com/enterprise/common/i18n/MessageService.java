package com.enterprise.common.i18n;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Thin facade over Spring's {@link MessageSource} that:
 * <ul>
 *   <li>Uses the locale from {@link LocaleContextHolder} (set by
 *       {@code AcceptHeaderLocaleResolver}) when none is provided.</li>
 *   <li>Never throws — returns the key itself if the translation is missing.</li>
 * </ul>
 *
 * <p>Injected anywhere a message has to be rendered (exception handler,
 * email templates, API warnings, etc.).</p>
 */
public class MessageService {

    private final MessageSource messageSource;

    public MessageService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Resolve {@code key} using the <em>request</em> locale.
     *
     * @param key  bundle key
     * @param args optional positional substitution arguments
     * @return the localized message, or {@code key} if missing
     */
    public String get(String key, Object... args) {
        return get(key, LocaleContextHolder.getLocale(), args);
    }

    /** Resolve {@code key} using an explicit {@link Locale}. */
    public String get(String key, Locale locale, Object... args) {
        if (key == null) return "";
        try {
            return messageSource.getMessage(key, args, locale);
        } catch (NoSuchMessageException ex) {
            return key;
        }
    }

    /** Resolve a message with a programmatic fallback when the key is absent. */
    public String getOrDefault(String key, String defaultMessage, Object... args) {
        return messageSource.getMessage(key, args, defaultMessage, LocaleContextHolder.getLocale());
    }
}
