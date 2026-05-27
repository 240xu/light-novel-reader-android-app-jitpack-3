package io.legado.engine.analyzer

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import org.xml.sax.InputSource

/**
 * XPath-based analyzer.
 * Extracted from Legado AnalyzeByXPath.
 * Uses Jsoup for HTML parsing, then converts to W3C DOM for XPath evaluation.
 */
class AnalyzeByXPath(private val doc: Document) {

    companion object {
        private val xpathFactory: XPathFactory = XPathFactory.newInstance()

        fun parse(html: String, baseUrl: String = ""): AnalyzeByXPath {
            val doc = Jsoup.parse(html, baseUrl)
            return AnalyzeByXPath(doc)
        }
    }

    /**
     * Get the first node matching the XPath expression.
     */
    fun first(xpathStr: String): Node? {
        if (xpathStr.isBlank()) return null
        return try {
            val xpathExpr = xpathStr.removePrefix("@XPath:")
            val domDoc = toW3CDocument(doc)
            val xpath: XPath = xpathFactory.newXPath()
            xpath.evaluate(xpathExpr, domDoc, XPathConstants.NODE) as? Node
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get all nodes matching the XPath expression.
     */
    fun nodeList(xpathStr: String): List<Node> {
        if (xpathStr.isBlank()) return emptyList()
        return try {
            val xpathExpr = xpathStr.removePrefix("@XPath:")
            val domDoc = toW3CDocument(doc)
            val xpath: XPath = xpathFactory.newXPath()
            val nodeList = xpath.evaluate(xpathExpr, domDoc, XPathConstants.NODESET) as? NodeList
                ?: return emptyList()
            (0 until nodeList.length).mapNotNull { nodeList.item(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get text content of the first matching node.
     */
    fun text(xpathStr: String): String {
        if (xpathStr.isBlank()) return ""
        return try {
            val node = first(xpathStr) ?: return ""
            getNodeText(node)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Get text list from matching nodes.
     */
    fun textList(xpathStr: String): List<String> {
        if (xpathStr.isBlank()) return emptyList()
        return try {
            nodeList(xpathStr).map { getNodeText(it) }.filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get attribute value from first matching node.
     */
    fun attr(xpathStr: String, attrName: String): String {
        if (xpathStr.isBlank()) return ""
        return try {
            val node = first(xpathStr) ?: return ""
            node.attributes?.getNamedItem(attrName)?.nodeValue ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun getNodeText(node: Node): String {
        return when (node.nodeType) {
            Node.TEXT_NODE -> node.textContent?.trim() ?: ""
            Node.ELEMENT_NODE -> node.textContent?.trim() ?: ""
            else -> node.textContent?.trim() ?: ""
        }
    }

    private fun toW3CDocument(jsoupDoc: Document): org.w3c.dom.Document {
        val html = jsoupDoc.outerHtml()
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false
        val builder = factory.newDocumentBuilder()
        return builder.parse(InputSource(StringReader(html)))
    }
}
