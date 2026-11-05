package com.ysfclc.mosaic.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PodcastFeedParserTest {
    @Test
    fun parsesRssEpisodesAndKeepsOnlyHttpsEnclosures() {
        val xml = """
            <rss version="2.0">
              <channel>
                <title>Builder Radio</title>
                <item>
                  <title>Episode One</title>
                  <author>Yusuf</author>
                  <enclosure url="https://cdn.example/one.mp3" type="audio/mpeg"/>
                </item>
                <item>
                  <title>Insecure Episode</title>
                  <enclosure url="http://cdn.example/two.mp3" type="audio/mpeg"/>
                </item>
                <item>
                  <title>No enclosure</title>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val feed = PodcastFeedParser.parse(xml, "https://feeds.example/show.xml")

        assertEquals("Builder Radio", feed.title)
        assertEquals(1, feed.episodes.size)
        assertEquals("Episode One", feed.episodes.single().title)
        assertEquals("Yusuf", feed.episodes.single().creator)
        assertEquals(MediaKind.PODCAST_EPISODE, feed.episodes.single().kind)
    }

    @Test
    fun parsesAtomEnclosureLinksAndRelativeMediaUrls() {
        val xml = """
            <feed xmlns="http://www.w3.org/2005/Atom">
              <title>Atom Cast</title>
              <entry>
                <title>Episode Two</title>
                <link rel="enclosure" href="/audio/two.mp3" type="audio/mpeg"/>
              </entry>
            </feed>
        """.trimIndent()

        val feed = PodcastFeedParser.parse(xml, "https://feeds.example/podcast/feed.xml")

        assertEquals("Atom Cast", feed.title)
        assertEquals("https://feeds.example/audio/two.mp3", feed.episodes.single().uri)
    }

    @Test
    fun rejectsDoctypeAndExternalEntityPayloads() {
        val xml = """
            <!DOCTYPE rss [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
            <rss><channel><title>&xxe;</title></channel></rss>
        """.trimIndent()

        val result = runCatching { PodcastFeedParser.parse(xml, "https://feeds.example/feed.xml") }
        assertTrue(result.isFailure)
    }

    @Test
    fun rejectsOversizedAndNonHttpsFeeds() {
        assertTrue(
            runCatching {
                PodcastFeedParser.parse("<rss/>", "http://feeds.example/feed.xml")
            }.isFailure,
        )
        assertTrue(
            runCatching {
                PodcastFeedParser.parse(" ".repeat(2 * 1024 * 1024 + 1), "https://feeds.example/feed.xml")
            }.isFailure,
        )
    }
}
