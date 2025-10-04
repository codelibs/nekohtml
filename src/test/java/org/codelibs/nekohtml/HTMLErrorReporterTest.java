/*
 * Copyright 2012-2025 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.nekohtml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Test class for {@link HTMLErrorReporter} interface.
 *
 * @author CodeLibs Project
 */
public class HTMLErrorReporterTest {

    /**
     * Test basic mock implementation of HTMLErrorReporter.
     */
    @Test
    public void testMockErrorReporter() {
        final HTMLErrorReporter reporter = mock(HTMLErrorReporter.class);
        assertNotNull(reporter, "Mock error reporter should not be null");

        // Configure mock behavior
        when(reporter.formatMessage("test.error", new Object[] { "arg1" })).thenReturn("Test error: arg1");

        final String message = reporter.formatMessage("test.error", new Object[] { "arg1" });
        assertEquals("Test error: arg1", message, "Mock should return configured message");

        verify(reporter, times(1)).formatMessage(eq("test.error"), any(Object[].class));
    }

    /**
     * Test reporting warnings with mock.
     */
    @Test
    public void testReportWarningWithMock() {
        final HTMLErrorReporter reporter = mock(HTMLErrorReporter.class);

        reporter.reportWarning("warning.key", new Object[] { "value1", "value2" });

        verify(reporter, times(1)).reportWarning(eq("warning.key"), any(Object[].class));
    }

    /**
     * Test reporting errors with mock.
     */
    @Test
    public void testReportErrorWithMock() {
        final HTMLErrorReporter reporter = mock(HTMLErrorReporter.class);

        reporter.reportError("error.key", new Object[] { "error1" });

        verify(reporter, times(1)).reportError(eq("error.key"), any(Object[].class));
    }

    /**
     * Test custom implementation that collects error messages.
     */
    @Test
    public void testCustomImplementationCollectingErrors() {
        final CollectingErrorReporter reporter = new CollectingErrorReporter();

        reporter.reportWarning("warning1", new Object[] { "arg1" });
        reporter.reportWarning("warning2", new Object[] { "arg2", "arg3" });
        reporter.reportError("error1", new Object[] { "error" });

        assertEquals(2, reporter.getWarnings().size(), "Should have collected 2 warnings");
        assertEquals(1, reporter.getErrors().size(), "Should have collected 1 error");

        assertEquals("warning1: [arg1]", reporter.getWarnings().get(0), "First warning message should match");
        assertEquals("warning2: [arg2, arg3]", reporter.getWarnings().get(1), "Second warning message should match");
        assertEquals("error1: [error]", reporter.getErrors().get(0), "Error message should match");
    }

    /**
     * Test formatMessage with custom implementation.
     */
    @Test
    public void testFormatMessageCustomImplementation() {
        final SimpleErrorReporter reporter = new SimpleErrorReporter();

        final String message1 = reporter.formatMessage("test.message", new Object[] { "value" });
        assertEquals("test.message: [value]", message1, "Formatted message should include key and args");

        final String message2 = reporter.formatMessage("another.key", new Object[] { "a", "b", "c" });
        assertEquals("another.key: [a, b, c]", message2, "Formatted message should include all arguments");
    }

    /**
     * Test formatMessage with null arguments.
     */
    @Test
    public void testFormatMessageWithNullArguments() {
        final SimpleErrorReporter reporter = new SimpleErrorReporter();

        final String message = reporter.formatMessage("test.key", null);
        assertEquals("test.key: null", message, "Should handle null arguments array");
    }

    /**
     * Test formatMessage with empty arguments.
     */
    @Test
    public void testFormatMessageWithEmptyArguments() {
        final SimpleErrorReporter reporter = new SimpleErrorReporter();

        final String message = reporter.formatMessage("test.key", new Object[0]);
        assertEquals("test.key: []", message, "Should handle empty arguments array");
    }

    /**
     * Test reportWarning with null arguments.
     */
    @Test
    public void testReportWarningWithNullArguments() {
        final CollectingErrorReporter reporter = new CollectingErrorReporter();

        reporter.reportWarning("warning.key", null);

        assertEquals(1, reporter.getWarnings().size(), "Should have collected 1 warning");
        assertEquals("warning.key: null", reporter.getWarnings().get(0), "Warning should handle null args");
    }

    /**
     * Test reportError with null arguments.
     */
    @Test
    public void testReportErrorWithNullArguments() {
        final CollectingErrorReporter reporter = new CollectingErrorReporter();

        reporter.reportError("error.key", null);

        assertEquals(1, reporter.getErrors().size(), "Should have collected 1 error");
        assertEquals("error.key: null", reporter.getErrors().get(0), "Error should handle null args");
    }

    /**
     * Test multiple warnings and errors in sequence.
     */
    @Test
    public void testMultipleReportsInSequence() {
        final CollectingErrorReporter reporter = new CollectingErrorReporter();

        // Report multiple warnings
        for (int i = 0; i < 5; i++) {
            reporter.reportWarning("warning" + i, new Object[] { "arg" + i });
        }

        // Report multiple errors
        for (int i = 0; i < 3; i++) {
            reporter.reportError("error" + i, new Object[] { "err" + i });
        }

        assertEquals(5, reporter.getWarnings().size(), "Should have collected 5 warnings");
        assertEquals(3, reporter.getErrors().size(), "Should have collected 3 errors");
    }

    /**
     * Test that HTMLErrorReporter can be used polymorphically.
     */
    @Test
    public void testPolymorphicUsage() {
        final HTMLErrorReporter reporter = new SimpleErrorReporter();

        final String formatted = reporter.formatMessage("key", new Object[] { "value" });
        assertNotNull(formatted, "Formatted message should not be null");

        // Should be able to call all interface methods
        reporter.reportWarning("warn", new Object[] {});
        reporter.reportError("err", new Object[] {});
    }

    /**
     * Test custom implementation with specific message format.
     */
    @Test
    public void testCustomMessageFormat() {
        final HTMLErrorReporter reporter = new HTMLErrorReporter() {
            @Override
            public String formatMessage(final String key, final Object[] args) {
                final StringBuilder sb = new StringBuilder();
                sb.append("[").append(key).append("]");
                if (args != null && args.length > 0) {
                    sb.append(" ");
                    for (int i = 0; i < args.length; i++) {
                        if (i > 0) {
                            sb.append(", ");
                        }
                        sb.append(args[i]);
                    }
                }
                return sb.toString();
            }

            @Override
            public void reportWarning(final String key, final Object[] args) {
                // No-op
            }

            @Override
            public void reportError(final String key, final Object[] args) {
                // No-op
            }
        };

        assertEquals("[test]", reporter.formatMessage("test", null), "Should format key only");
        assertEquals("[test] arg1", reporter.formatMessage("test", new Object[] { "arg1" }), "Should format with single arg");
        assertEquals("[test] a, b, c", reporter.formatMessage("test", new Object[] { "a", "b", "c" }), "Should format with multiple args");
    }

    /**
     * Test no-op implementation (silent error reporter).
     */
    @Test
    public void testNoOpImplementation() {
        final HTMLErrorReporter reporter = new HTMLErrorReporter() {
            @Override
            public String formatMessage(final String key, final Object[] args) {
                return null;
            }

            @Override
            public void reportWarning(final String key, final Object[] args) {
                // Silently ignore
            }

            @Override
            public void reportError(final String key, final Object[] args) {
                // Silently ignore
            }
        };

        // Should not throw exceptions
        assertNull(reporter.formatMessage("test", new Object[] {}), "No-op implementation can return null");
        reporter.reportWarning("warn", new Object[] {});
        reporter.reportError("error", new Object[] {});
    }

    /**
     * Test error reporter that counts invocations.
     */
    @Test
    public void testCountingErrorReporter() {
        final CountingErrorReporter reporter = new CountingErrorReporter();

        reporter.reportWarning("w1", new Object[] {});
        reporter.reportWarning("w2", new Object[] {});
        reporter.reportError("e1", new Object[] {});
        reporter.reportWarning("w3", new Object[] {});
        reporter.reportError("e2", new Object[] {});
        reporter.reportError("e3", new Object[] {});

        assertEquals(3, reporter.getWarningCount(), "Should have counted 3 warnings");
        assertEquals(3, reporter.getErrorCount(), "Should have counted 3 errors");
        assertEquals(6, reporter.getTotalCount(), "Should have counted 6 total reports");
    }

    /**
     * Test error reporter with special characters in keys and arguments.
     */
    @Test
    public void testSpecialCharactersInKeysAndArgs() {
        final SimpleErrorReporter reporter = new SimpleErrorReporter();

        final String message1 = reporter.formatMessage("key.with.dots", new Object[] { "value" });
        assertTrue(message1.contains("key.with.dots"), "Should handle dots in key");

        final String message2 = reporter.formatMessage("key", new Object[] { "value with spaces" });
        assertTrue(message2.contains("value with spaces"), "Should handle spaces in arguments");

        final String message3 = reporter.formatMessage("key", new Object[] { "special<>chars" });
        assertTrue(message3.contains("special<>chars"), "Should handle special characters in arguments");
    }

    /**
     * Test interface implementation can be assigned to interface type.
     */
    @Test
    public void testInterfaceTypeAssignment() {
        final HTMLErrorReporter reporter1 = new SimpleErrorReporter();
        final HTMLErrorReporter reporter2 = new CollectingErrorReporter();
        final HTMLErrorReporter reporter3 = new CountingErrorReporter();

        assertNotNull(reporter1, "SimpleErrorReporter should be assignable to HTMLErrorReporter");
        assertNotNull(reporter2, "CollectingErrorReporter should be assignable to HTMLErrorReporter");
        assertNotNull(reporter3, "CountingErrorReporter should be assignable to HTMLErrorReporter");
    }

    // Helper classes for testing

    /**
     * Simple implementation that formats messages with key and args.
     */
    static class SimpleErrorReporter implements HTMLErrorReporter {
        @Override
        public String formatMessage(final String key, final Object[] args) {
            if (args == null) {
                return key + ": null";
            }
            final StringBuilder sb = new StringBuilder(key).append(": [");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(args[i]);
            }
            sb.append("]");
            return sb.toString();
        }

        @Override
        public void reportWarning(final String key, final Object[] args) {
            // No-op for simple implementation
        }

        @Override
        public void reportError(final String key, final Object[] args) {
            // No-op for simple implementation
        }
    }

    /**
     * Implementation that collects all warnings and errors.
     */
    static class CollectingErrorReporter implements HTMLErrorReporter {
        private final List<String> warnings = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();

        @Override
        public String formatMessage(final String key, final Object[] args) {
            return new SimpleErrorReporter().formatMessage(key, args);
        }

        @Override
        public void reportWarning(final String key, final Object[] args) {
            warnings.add(formatMessage(key, args));
        }

        @Override
        public void reportError(final String key, final Object[] args) {
            errors.add(formatMessage(key, args));
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    /**
     * Implementation that counts warnings and errors.
     */
    static class CountingErrorReporter implements HTMLErrorReporter {
        private int warningCount = 0;
        private int errorCount = 0;

        @Override
        public String formatMessage(final String key, final Object[] args) {
            return key;
        }

        @Override
        public void reportWarning(final String key, final Object[] args) {
            warningCount++;
        }

        @Override
        public void reportError(final String key, final Object[] args) {
            errorCount++;
        }

        public int getWarningCount() {
            return warningCount;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public int getTotalCount() {
            return warningCount + errorCount;
        }
    }

} // class HTMLErrorReporterTest
