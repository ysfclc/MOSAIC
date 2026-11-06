package com.ysfclc.mosaic.core.media

import java.io.StringReader
import java.net.URI
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.xml.sax.InputSource

data class PodcastFeed(
    val title: String,
    val episodes: List<MediaItemRecord>,
)

/** Secure, bounded RSS 2.0 / Atom parser. Only HTTPS episode media is admitted. */
object PodcastFeedParser {
    private const val MAX_XML_CHARS = 2 * 1024 * 1024
    private const val ACCESS_EXTERNAL_DTD = "http://javax.xml.XMLConstants/property/accessExternalDTD"
    private const val ACCESS_EXTERNAL_SCHEMA = "http://javax.xml.XMLConstants/property/accessExternalSchema"

    fun parse(xml: String, feedAddress: String): PodcastFeed {
        require(xml.length <= MAX_XML_CHARS) { "Podcast feed is larger than 2 MB." }
        val safeFeedAddress = PlayableStreamAddress.normalize(feedAddress)
            ?: throw IllegalArgumentException("Podcast feed must use a valid HTTPS address.")

        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            isXIncludeAware = false
            isExpandEntityReferences = false
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            // These JAXP properties are not implemented by every Android XML provider.
            // The mandatory parser features above still fail closed if unsupported.
            runCatching { setAttribute(ACCESS_EXTERNAL_DTD, "") }
            runCatching { setAttribute(ACCESS_EXTERNAL_SCHEMA, "") }
        }
        val document = factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
        val root = document.documentElement ?: throw IllegalArgumentException("Podcast feed is empty.")
        val channel = root.directChild("channel") ?: root
        val feedTitle = channel.directChildText("title").ifBlank { "Podcast feed" }
        val itemNodes = channel.directChildren("item") + channel.directChildren("entry")

        val episodes = itemNodes.mapNotNull { item ->
            val enclosure = item.directChild("enclosure")
            val atomLink = item.directChildren("link").firstOrNull {
                it.getAttribute("rel").equals("enclosure", ignoreCase = true)
            }
            val rawMediaAddress = enclosure?.getAttribute("url")
                ?.takeIf { it.isNotBlank() }
                ?: atomLink?.getAttribute("href")?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            val mediaAddress = runCatching {
                URI(safeFeedAddress).resolve(rawMediaAddress).toString()
            }.getOrNull()?.let(PlayableStreamAddress::normalize) ?: return@mapNotNull null

            val title = item.directChildText("title").ifBlank { "Untitled episode" }
            val creator = item.directChildText("author")
                .ifBlank { item.directChildText("creator") }
                .takeIf { it.isNotBlank() }
            val mimeType = enclosure?.getAttribute("type")
                ?.takeIf { it.isNotBlank() }
                ?: atomLink?.getAttribute("type")?.takeIf { it.isNotBlank() }

            MediaItemRecord(
                id = mediaAddress,
                sourceId = "rss:$safeFeedAddress",
                title = title,
                creator = creator,
                collection = feedTitle,
                uri = mediaAddress,
                mimeType = mimeType,
                kind = MediaKind.PODCAST_EPISODE,
            )
        }.distinctBy { it.uri }

        return PodcastFeed(feedTitle, episodes)
    }

    private fun Element.directChild(name: String): Element? =
        directChildren(name).firstOrNull()

    private fun Element.directChildren(name: String): List<Element> {
        val matches = ArrayList<Element>()
        val nodes = childNodes
        for (index in 0 until nodes.length) {
            val child = nodes.item(index)
            if (child is Element && child.tagName.substringAfter(':').equals(name, ignoreCase = true)) {
                matches += child
            }
        }
        return matches
    }

    private fun Element.directChildText(name: String): String =
        directChild(name)?.textContent?.trim().orEmpty()
}
