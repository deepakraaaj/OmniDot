package com.omnidot.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkDetectorTest {
    @Test
    fun `finds links in messages`() {
        assertEquals("https://meet.google.com/abc-defg-hij", LinkDetector.firstLink("Join here https://meet.google.com/abc-defg-hij now"))
        assertEquals("www.zoom.us/j/123", LinkDetector.firstLink("www.zoom.us/j/123"))
        assertEquals("bit.ly/3xyz", LinkDetector.firstLink("check bit.ly/3xyz"))
    }

    @Test
    fun `ignores plain text and trailing punctuation`() {
        assertNull(LinkDetector.firstLink("see you at 5. ok?"))
        assertEquals("https://example.com/a", LinkDetector.firstLink("(https://example.com/a)."))
    }
}
