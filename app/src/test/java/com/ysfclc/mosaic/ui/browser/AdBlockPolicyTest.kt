package com.ysfclc.mosaic.ui.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdBlockPolicyTest {
    @Test
    fun blocksKnownThirdPartyAdDomainsOnOrdinaryPages() {
        assertTrue(AdBlockPolicy.shouldBlock("https://cdn.taboola.com/lib.js", "news.example"))
        assertTrue(AdBlockPolicy.shouldBlock("https://sub.criteo.com/track", "news.example"))
    }

    @Test
    fun blocksKnownThirdPartyAdDomainsOnYoutubeAndSpotifyPages() {
        assertTrue(AdBlockPolicy.shouldBlock("https://cdn.taboola.com/ad.js", "m.youtube.com"))
        assertTrue(AdBlockPolicy.shouldBlock("https://cdn.criteo.com/ad.js", "open.spotify.com"))
    }

    @Test
    fun blocksGoogleAdInfrastructureOnOrdinaryPages() {
        assertTrue(AdBlockPolicy.shouldBlock("https://securepubads.g.doubleclick.net/tag", "news.example"))
        assertTrue(AdBlockPolicy.shouldBlock("https://pagead2.googlesyndication.com/pagead/js", "news.example"))
    }

    @Test
    fun allowsGooglePlayerCompatibilityHostsOnlyInYoutubeContext() {
        assertFalse(AdBlockPolicy.shouldBlock("https://securepubads.g.doubleclick.net/tag", "www.youtube.com"))
        assertFalse(AdBlockPolicy.shouldBlock("https://pagead2.googlesyndication.com/pagead/js", "m.youtube.com"))
        assertFalse(
            AdBlockPolicy.shouldBlock(
                "https://securepubads.g.doubleclick.net/tag",
                "news.example",
                "https://www.youtube.com/embed/abc",
            ),
        )
        assertTrue(AdBlockPolicy.shouldBlock("https://securepubads.g.doubleclick.net/tag", "open.spotify.com"))
        assertTrue(AdBlockPolicy.shouldBlock("https://securepubads.g.doubleclick.net/tag", "news.example"))
    }

    @Test
    fun recognizesProviderPagesWithoutExemptingTheirThirdPartyRequests() {
        assertTrue(AdBlockPolicy.isProviderPage("www.youtube.com"))
        assertTrue(AdBlockPolicy.isProviderPage("open.spotify.com"))
        assertFalse(AdBlockPolicy.isProviderPage("news.example"))
        assertTrue(AdBlockPolicy.shouldBlock("https://cdn.taboola.com/ad.js", "www.youtube.com"))
    }

    @Test
    fun preservesProviderContentAndMediaHosts() {
        assertFalse(AdBlockPolicy.shouldBlock("https://www.youtube.com/watch?v=abc", "news.example"))
        assertFalse(AdBlockPolicy.shouldBlock("https://r1---sn.googlevideo.com/videoplayback", "www.youtube.com"))
        assertFalse(AdBlockPolicy.shouldBlock("https://i.scdn.co/image/cover", "open.spotify.com"))
        assertFalse(AdBlockPolicy.shouldBlock("https://open.spotify.com/track/abc", "news.example"))
    }

    @Test
    fun allowsOrdinaryContentAndUnknownHosts() {
        assertFalse(AdBlockPolicy.shouldBlock("https://cdn.example.com/app.js", "news.example"))
    }

    @Test
    fun handlesMalformedUrlsWithoutBlocking() {
        assertFalse(AdBlockPolicy.shouldBlock("not a url", "news.example"))
    }
}
