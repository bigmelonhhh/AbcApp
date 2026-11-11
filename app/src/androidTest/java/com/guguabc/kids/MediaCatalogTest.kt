package com.guguabc.kids

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.test.core.app.ApplicationProvider
import com.guguabc.kids.data.Words
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Validate the actual packaged resources with Android decoders, not just filenames. */
class MediaCatalogTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun everyWordHasItsOwnDecodablePictureAndOfflinePronunciation() {
        Words.all.forEach { word ->
            assertEquals("img_${word.id}", context.resources.getResourceEntryName(word.imageRes))
            assertEquals("word_${word.id}", context.resources.getResourceEntryName(word.audioRes))
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(context.resources, word.imageRes, options)
            assertTrue("${word.id} image cannot decode", options.outWidth >= 512 && options.outHeight >= 512)
            val media = MediaMetadataRetriever()
            try {
                context.resources.openRawResourceFd(word.audioRes).use { file ->
                    media.setDataSource(file.fileDescriptor, file.startOffset, file.length)
                }
                val duration = media.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
                assertTrue("${word.id} invalid speech duration: $duration", duration in 300..5000)
            } finally {
                media.release()
            }
        }
    }
}
