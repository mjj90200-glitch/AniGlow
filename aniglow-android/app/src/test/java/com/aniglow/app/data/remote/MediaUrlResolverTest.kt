package com.aniglow.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaUrlResolverTest {
    @Test
    fun `relative api image uses current server root`() {
        assertEquals(
            "http://10.0.2.2:8081/api/images/2026/example.jpg",
            resolveMediaUrl("/api/images/2026/example.jpg"),
        )
    }

    @Test
    fun `absolute image url stays unchanged`() {
        val url = "https://cdn.example.com/post.jpg"
        assertEquals(url, resolveMediaUrl(url))
    }

    @Test
    fun `blank image url is ignored`() {
        assertNull(resolveMediaUrl("  "))
        assertNull(resolveMediaUrl(null))
    }
}
