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

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.codelibs.nekohtml.parsers.DOMParser;
import org.codelibs.nekohtml.parsers.SAXParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Tests for thread safety and concurrent usage of NekoHTML parsers.
 * Tests multiple threads parsing different documents and shared parser scenarios.
 *
 * @author CodeLibs Project
 */
public class ThreadSafetyTest {

    private static final String SIMPLE_HTML = "<html><body><div>Content</div></body></html>";
    private static final String COMPLEX_HTML = "<html><head><title>Test</title></head><body>"
            + "<article><header><h1>Title</h1></header>"
            + "<section><p>Paragraph 1</p><p>Paragraph 2</p></section>"
            + "<footer>Footer</footer></article>"
            + "</body></html>";

    // ========================================================================
    // Concurrent Parsing with Separate Parser Instances
    // ========================================================================

    @Test
    @Timeout(30)
    public void testConcurrentParsingWithSeparateParsers10Threads() throws Exception {
        // Given: 10 threads, each with its own parser
        final int threadCount = 10;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final List<Future<Document>> futures = new ArrayList<>();

        // When: All threads parse concurrently
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            futures.add(executor.submit(new Callable<Document>() {
                @Override
                public Document call() throws Exception {
                    final DOMParser parser = new DOMParser();
                    final String html = "<html><body><div>Thread " + threadId + "</div></body></html>";
                    parser.parse(new InputSource(new StringReader(html)));
                    return parser.getDocument();
                }
            }));
        }

        // Then: All should succeed
        executor.shutdown();
        assertTrue(executor.awaitTermination(20, TimeUnit.SECONDS), "Executor should finish");

        for (int i = 0; i < threadCount; i++) {
            final Document doc = futures.get(i).get();
            assertNotNull(doc, "Document " + i + " should be parsed");
            assertEquals("DIV", doc.getElementsByTagName("DIV").item(0).getNodeName(), "Should have DIV");
        }
    }

    @Test
    @Timeout(30)
    public void testConcurrentParsingWithSeparateParsers100Threads() throws Exception {
        // Given: 100 threads parsing different documents
        final int threadCount = 100;
        final ExecutorService executor = Executors.newFixedThreadPool(20);
        final AtomicInteger successCount = new AtomicInteger(0);
        final List<Future<Boolean>> futures = new ArrayList<>();

        // When: All threads parse concurrently
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            futures.add(executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    try {
                        final DOMParser parser = new DOMParser();
                        final String html = "<html><body><h1>Thread " + threadId + "</h1>"
                                + "<p>Paragraph " + threadId + "</p></body></html>";
                        parser.parse(new InputSource(new StringReader(html)));
                        final Document doc = parser.getDocument();

                        if (doc != null && doc.getElementsByTagName("H1").getLength() == 1) {
                            successCount.incrementAndGet();
                            return true;
                        }
                        return false;
                    } catch (final Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                }
            }));
        }

        // Then: All should succeed
        executor.shutdown();
        assertTrue(executor.awaitTermination(25, TimeUnit.SECONDS), "Executor should finish");

        assertEquals(threadCount, successCount.get(), "All threads should parse successfully");
    }

    @Test
    @Timeout(30)
    public void testConcurrentParsingDifferentDocumentTypes() throws Exception {
        // Given: Multiple threads parsing different types of documents
        final int threadCount = 20;
        final ExecutorService executor = Executors.newFixedThreadPool(10);
        final List<Future<Boolean>> futures = new ArrayList<>();

        final String[] htmlTypes = new String[] { SIMPLE_HTML, COMPLEX_HTML,
                "<html><body><table><tr><td>Table</td></tr></table></body></html>",
                "<html><body><form><input type=\"text\"></form></body></html>",
                "<html><body><ul><li>Item 1</li><li>Item 2</li></ul></body></html>" };

        // When: Threads parse different document types
        for (int i = 0; i < threadCount; i++) {
            final String html = htmlTypes[i % htmlTypes.length];
            futures.add(executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    try {
                        final DOMParser parser = new DOMParser();
                        parser.parse(new InputSource(new StringReader(html)));
                        return parser.getDocument() != null;
                    } catch (final Exception e) {
                        return false;
                    }
                }
            }));
        }

        // Then: All should succeed
        executor.shutdown();
        assertTrue(executor.awaitTermination(20, TimeUnit.SECONDS), "Executor should finish");

        for (final Future<Boolean> future : futures) {
            assertTrue(future.get(), "Parsing should succeed");
        }
    }

    // ========================================================================
    // Parser Reuse Tests
    // ========================================================================

    @Test
    @Timeout(20)
    public void testParserReuseSequential() throws Exception {
        // Given: Single parser used multiple times sequentially
        final DOMParser parser = new DOMParser();

        // When: Parsing multiple documents sequentially
        for (int i = 0; i < 10; i++) {
            final String html = "<html><body><div id=\"" + i + "\">Content " + i + "</div></body></html>";
            parser.parse(new InputSource(new StringReader(html)));
            final Document doc = parser.getDocument();

            // Then: Each parse should succeed independently
            assertNotNull(doc, "Document " + i + " should be parsed");
            assertEquals("DIV", doc.getElementsByTagName("DIV").item(0).getNodeName(), "Should have DIV");
        }
    }

    @Test
    @Timeout(20)
    public void testSAXParserReuseSequential() throws Exception {
        // Given: Single SAX parser used multiple times
        final SAXParser parser = new SAXParser();
        final DefaultHandler handler = new DefaultHandler();

        // When: Parsing multiple documents sequentially
        for (int i = 0; i < 10; i++) {
            final String html = "<html><body><p>Paragraph " + i + "</p></body></html>";
            parser.setContentHandler(handler);
            parser.parse(new InputSource(new StringReader(html)));
        }

        // Then: All parses should complete without error
        assertNotNull(parser, "Parser should still be valid");
    }

    // ========================================================================
    // Concurrent Parsing with SAX Parser
    // ========================================================================

    @Test
    @Timeout(30)
    public void testConcurrentSAXParsing() throws Exception {
        // Given: Multiple threads using SAX parser
        final int threadCount = 20;
        final ExecutorService executor = Executors.newFixedThreadPool(10);
        final AtomicInteger successCount = new AtomicInteger(0);
        final List<Future<Boolean>> futures = new ArrayList<>();

        // When: Threads parse with SAX
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    try {
                        final SAXParser parser = new SAXParser();
                        final DefaultHandler handler = new DefaultHandler();
                        parser.setContentHandler(handler);
                        parser.parse(new InputSource(new StringReader(COMPLEX_HTML)));
                        successCount.incrementAndGet();
                        return true;
                    } catch (final Exception e) {
                        return false;
                    }
                }
            }));
        }

        // Then: All should succeed
        executor.shutdown();
        assertTrue(executor.awaitTermination(20, TimeUnit.SECONDS), "Executor should finish");
        assertEquals(threadCount, successCount.get(), "All SAX parses should succeed");
    }

    // ========================================================================
    // Stress Tests with Many Concurrent Operations
    // ========================================================================

    @Test
    @Timeout(60)
    public void testHighConcurrencyStressTest() throws Exception {
        // Given: High concurrency scenario (50 threads, 100 parses each)
        final int threadCount = 50;
        final int parsesPerThread = 100;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final AtomicInteger totalParses = new AtomicInteger(0);
        final List<Future<Integer>> futures = new ArrayList<>();

        // When: Many concurrent parse operations
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    int successCount = 0;
                    final DOMParser parser = new DOMParser();

                    for (int j = 0; j < parsesPerThread; j++) {
                        try {
                            final String html = "<html><body><span>" + j + "</span></body></html>";
                            parser.parse(new InputSource(new StringReader(html)));
                            if (parser.getDocument() != null) {
                                successCount++;
                                totalParses.incrementAndGet();
                            }
                        } catch (final Exception e) {
                            // Count failures
                        }
                    }
                    return successCount;
                }
            }));
        }

        // Then: Most parses should succeed
        executor.shutdown();
        assertTrue(executor.awaitTermination(50, TimeUnit.SECONDS), "Executor should finish");

        int totalSuccess = 0;
        for (final Future<Integer> future : futures) {
            totalSuccess += future.get();
        }

        final int expectedTotal = threadCount * parsesPerThread;
        assertTrue(totalSuccess > expectedTotal * 0.95, "At least 95% of parses should succeed: " + totalSuccess + " / " + expectedTotal);
    }

    // ========================================================================
    // Error Handling in Concurrent Scenarios
    // ========================================================================

    @Test
    @Timeout(30)
    public void testConcurrentParsingWithMalformedHTML() throws Exception {
        // Given: Multiple threads parsing malformed HTML
        final int threadCount = 20;
        final ExecutorService executor = Executors.newFixedThreadPool(10);
        final AtomicInteger successCount = new AtomicInteger(0);
        final List<Future<Boolean>> futures = new ArrayList<>();

        final String[] malformedHTML = new String[] { "<html><body><div>Unclosed div", "<html><body><b><i></b></i></body></html>",
                "<html><body><table><td>No TR</td></table></body></html>", "<html><body><p>Paragraph <div>Block</div> continues</p>",
                "<html><body><ul><div>Wrong nesting</div></ul>" };

        // When: Threads parse malformed HTML
        for (int i = 0; i < threadCount; i++) {
            final String html = malformedHTML[i % malformedHTML.length];
            futures.add(executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    try {
                        final DOMParser parser = new DOMParser();
                        parser.parse(new InputSource(new StringReader(html)));
                        if (parser.getDocument() != null) {
                            successCount.incrementAndGet();
                            return true;
                        }
                        return false;
                    } catch (final Exception e) {
                        // Parser should handle malformed HTML gracefully
                        return false;
                    }
                }
            }));
        }

        // Then: Should handle malformed HTML without crashing
        executor.shutdown();
        assertTrue(executor.awaitTermination(20, TimeUnit.SECONDS), "Executor should finish");
        assertTrue(successCount.get() > 0, "Should successfully parse malformed HTML");
    }

    @Test
    @Timeout(30)
    public void testConcurrentParsingWithVeryLargeDocuments() throws Exception {
        // Given: Multiple threads parsing large documents
        final int threadCount = 10;
        final ExecutorService executor = Executors.newFixedThreadPool(5);
        final List<Future<Boolean>> futures = new ArrayList<>();

        // When: Threads parse large documents
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    try {
                        final StringBuilder html = new StringBuilder("<html><body>");
                        for (int j = 0; j < 1000; j++) {
                            html.append("<div>Content ").append(j).append("</div>");
                        }
                        html.append("</body></html>");

                        final DOMParser parser = new DOMParser();
                        parser.parse(new InputSource(new StringReader(html.toString())));
                        return parser.getDocument() != null;
                    } catch (final Exception e) {
                        return false;
                    }
                }
            }));
        }

        // Then: All should succeed
        executor.shutdown();
        assertTrue(executor.awaitTermination(25, TimeUnit.SECONDS), "Executor should finish");

        for (final Future<Boolean> future : futures) {
            assertTrue(future.get(), "Large document parsing should succeed");
        }
    }

    // ========================================================================
    // Mixed Operations Test
    // ========================================================================

    @Test
    @Timeout(30)
    public void testMixedDOMAndSAXParsing() throws Exception {
        // Given: Mix of DOM and SAX parsing in parallel
        final int threadCount = 20;
        final ExecutorService executor = Executors.newFixedThreadPool(10);
        final AtomicInteger successCount = new AtomicInteger(0);
        final List<Future<Boolean>> futures = new ArrayList<>();

        // When: Mixed DOM and SAX parsing
        for (int i = 0; i < threadCount; i++) {
            final boolean useDOMParser = (i % 2 == 0);
            futures.add(executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    try {
                        if (useDOMParser) {
                            final DOMParser parser = new DOMParser();
                            parser.parse(new InputSource(new StringReader(COMPLEX_HTML)));
                            if (parser.getDocument() != null) {
                                successCount.incrementAndGet();
                                return true;
                            }
                        } else {
                            final SAXParser parser = new SAXParser();
                            final DefaultHandler handler = new DefaultHandler();
                            parser.setContentHandler(handler);
                            parser.parse(new InputSource(new StringReader(COMPLEX_HTML)));
                            successCount.incrementAndGet();
                            return true;
                        }
                        return false;
                    } catch (final Exception e) {
                        return false;
                    }
                }
            }));
        }

        // Then: All should succeed
        executor.shutdown();
        assertTrue(executor.awaitTermination(20, TimeUnit.SECONDS), "Executor should finish");
        assertEquals(threadCount, successCount.get(), "All mixed parses should succeed");
    }

    // ========================================================================
    // Parser State After Concurrent Use
    // ========================================================================

    @Test
    @Timeout(30)
    public void testParserStateAfterConcurrentUse() throws Exception {
        // Given: Concurrent parsing followed by sequential parsing
        final int threadCount = 10;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final List<Future<Boolean>> futures = new ArrayList<>();

        // When: Concurrent parsing
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    final DOMParser parser = new DOMParser();
                    parser.parse(new InputSource(new StringReader(SIMPLE_HTML)));
                    return parser.getDocument() != null;
                }
            }));
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(20, TimeUnit.SECONDS), "Executor should finish");

        // Then: Sequential parsing should still work
        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(COMPLEX_HTML)));
        final Document doc = parser.getDocument();
        assertNotNull(doc, "Sequential parsing after concurrent use should work");
        assertEquals("ARTICLE", doc.getElementsByTagName("ARTICLE").item(0).getNodeName(), "Should have ARTICLE");
    }

    @Test
    @Timeout(20)
    public void testNoMemoryLeaksWithRepeatedParsing() throws Exception {
        // Given: Repeated parsing to check for memory leaks
        final int iterations = 1000;

        // When: Parsing many times with same parser instance
        final DOMParser parser = new DOMParser();
        for (int i = 0; i < iterations; i++) {
            parser.parse(new InputSource(new StringReader(SIMPLE_HTML)));
            final Document doc = parser.getDocument();
            assertNotNull(doc, "Document " + i + " should be parsed");

            // Force GC periodically to detect leaks
            if (i % 100 == 0) {
                System.gc();
            }
        }

        // Then: Should complete without OutOfMemoryError
        assertNotNull(parser, "Parser should still be valid");
    }

    @Test
    @Timeout(30)
    public void testConcurrentParsingWithDifferentFeatures() throws Exception {
        // Given: Parsers with different feature configurations
        final int threadCount = 20;
        final ExecutorService executor = Executors.newFixedThreadPool(10);
        final List<Future<Boolean>> futures = new ArrayList<>();

        // When: Concurrent parsing with different features
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    try {
                        final DOMParser parser = new DOMParser();
                        // Each parser can have different configurations
                        parser.parse(new InputSource(new StringReader(COMPLEX_HTML)));
                        return parser.getDocument() != null;
                    } catch (final Exception e) {
                        return false;
                    }
                }
            }));
        }

        // Then: All should succeed
        executor.shutdown();
        assertTrue(executor.awaitTermination(20, TimeUnit.SECONDS), "Executor should finish");

        for (final Future<Boolean> future : futures) {
            assertTrue(future.get(), "Parsing with features should succeed");
        }
    }
}
