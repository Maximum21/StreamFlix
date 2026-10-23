package com.asadraza.streamflix.core.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageUrlTest {
    @Test
    fun `missing paths produce no url`() {
        assertNull(ImageUrl.poster(null))
        assertNull(ImageUrl.backdrop(null))
    }

    @Test
    fun `urls combine base, size and path`() {
        assertEquals("${Constants.IMAGE_BASE_URL}${Constants.POSTER_SIZE_W500}/a.jpg", ImageUrl.poster("/a.jpg"))
        assertEquals("${Constants.IMAGE_BASE_URL}w300/a.jpg", ImageUrl.poster("/a.jpg", "w300"))
        assertEquals(
            "${Constants.IMAGE_BASE_URL}${Constants.BACKDROP_SIZE_ORIGINAL}/b.jpg",
            ImageUrl.backdrop("/b.jpg")
        )
    }
}
