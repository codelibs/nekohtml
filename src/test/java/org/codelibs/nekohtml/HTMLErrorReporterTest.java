package org.codelibs.nekohtml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.xerces.xni.parser.XMLErrorHandler;
import org.apache.xerces.xni.parser.XMLParseException;
import org.junit.jupiter.api.Test;

public class HTMLErrorReporterTest {

    // Utility to fetch expected localized text via resource bundle
    private static String expectedMessage(Locale locale, String key, Object[] args) {
        ResourceBundle bundle = ResourceBundle.getBundle("org/codelibs/nekohtml/res/ErrorMessages", locale);
        String pattern = bundle.getString(key);
        return MessageFormat.format(pattern, args);
    }

    @Test
    void formatMessage_localized_usesResourceBundle() {
        HTMLConfiguration cfg = new HTMLConfiguration();
        HTMLErrorReporter reporter = cfg.fErrorReporter;

        // Ensure complex/ResourceBundle format is used
        cfg.setFeature(HTMLConfiguration.SIMPLE_ERROR_FORMAT, false);

        String key = "HTML1005"; // Invalid character entity "{0}".
        Object[] args = new Object[] { "amp" };

        String expected = expectedMessage(cfg.fLocale, key, args);
        String actual = reporter.formatMessage(key, args);

        assertEquals(expected, actual, "Should format message via ResourceBundle");
    }

    @Test
    void formatMessage_simpleFormatWhenEnabled() {
        HTMLConfiguration cfg = new HTMLConfiguration();
        HTMLErrorReporter reporter = cfg.fErrorReporter;

        // Enable simple formatting fallback
        cfg.setFeature(HTMLConfiguration.SIMPLE_ERROR_FORMAT, true);

        String key = "HTML1005";
        Object[] args = new Object[] { "amp" };

        String expected = HTMLConfiguration.ERROR_DOMAIN + '#' + key + "\tamp";
        String actual = reporter.formatMessage(key, args);

        assertEquals(expected, actual, "Should format in simple domain#key\\targs style");
    }

    @Test
    void formatMessage_unknownKeyFallsBackToSimple() {
        HTMLConfiguration cfg = new HTMLConfiguration();
        HTMLErrorReporter reporter = cfg.fErrorReporter;

        // Keep complex mode but use an unknown key -> fallback to simple
        cfg.setFeature(HTMLConfiguration.SIMPLE_ERROR_FORMAT, false);

        String key = "HTML9999X";
        Object[] args = new Object[] { "one", 2 };

        String expected = HTMLConfiguration.ERROR_DOMAIN + '#' + key + "\tone\t2";
        String actual = reporter.formatMessage(key, args);

        assertEquals(expected, actual, "Unknown key should fall back to simple format");
    }

    @Test
    void formatMessage_nullArgs() {
        HTMLConfiguration cfg = new HTMLConfiguration();
        HTMLErrorReporter reporter = cfg.fErrorReporter;

        cfg.setFeature(HTMLConfiguration.SIMPLE_ERROR_FORMAT, false);

        String key = "HTML2000"; // "Empty document."
        Object[] args = null;

        String expected = expectedMessage(cfg.fLocale, key, new Object[0]);
        String actual = reporter.formatMessage(key, args);

        assertEquals(expected, actual, "Null args should be handled gracefully");
    }

    @Test
    void reportWarning_invokesErrorHandlerWithXMLParseException() {
        HTMLConfiguration cfg = new HTMLConfiguration();
        HTMLErrorReporter reporter = cfg.fErrorReporter;

        XMLErrorHandler handler = mock(XMLErrorHandler.class);
        cfg.setErrorHandler(handler);

        String key = "HTML1004"; // Bare ampersand found.
        Object[] args = null;

        // Use reporter to compute expected message to match its internal behavior
        String expectedMessage = reporter.formatMessage(key, args);

        reporter.reportWarning(key, args);

        verify(handler, times(1)).warning(eq(HTMLConfiguration.ERROR_DOMAIN), eq(key),
                org.mockito.ArgumentMatchers.argThat(ex -> ex instanceof XMLParseException && expectedMessage.equals(ex.getMessage())));
    }

    @Test
    void reportError_invokesErrorHandlerWithXMLParseException() {
        HTMLConfiguration cfg = new HTMLConfiguration();
        HTMLErrorReporter reporter = cfg.fErrorReporter;

        XMLErrorHandler handler = mock(XMLErrorHandler.class);
        cfg.setErrorHandler(handler);

        String key = "HTML1011"; // Missing attribute name.
        Object[] args = null;

        String expectedMessage = reporter.formatMessage(key, args);
        reporter.reportError(key, args);

        verify(handler, times(1)).error(eq(HTMLConfiguration.ERROR_DOMAIN), eq(key),
                org.mockito.ArgumentMatchers.argThat(ex -> ex instanceof XMLParseException && expectedMessage.equals(ex.getMessage())));
    }

    @Test
    void formatMessage_localeChangeReloadsBundle() {
        HTMLConfiguration cfg = new HTMLConfiguration();
        HTMLErrorReporter reporter = cfg.fErrorReporter;

        cfg.setFeature(HTMLConfiguration.SIMPLE_ERROR_FORMAT, false);

        // Prime with default locale
        reporter.formatMessage("HTML2001", new Object[] { "div" });

        // Switch locale to Japanese and verify message is resolved again
        cfg.fLocale = Locale.JAPANESE;
        String key = "HTML2001"; // Present in _ja.properties
        Object[] args = new Object[] { "div" };

        String expected = expectedMessage(Locale.JAPANESE, key, args);
        String actual = reporter.formatMessage(key, args);

        assertEquals(expected, actual, "Changing locale should reload or update bundle usage");
    }
}
