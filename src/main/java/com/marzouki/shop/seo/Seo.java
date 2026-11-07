package com.marzouki.shop.seo;

/**
 * What a page tells search engines and social networks.
 *
 * @param description  the summary shown in search results
 * @param canonical    the one address this content should be known by
 * @param image        an absolute image address for link previews, or null
 * @param type         the Open Graph type, "website" or "product"
 * @param noindex      keep this view out of search results (filtered and paged lists)
 * @param jsonLd       structured data as JSON that is already safe to place inside a script tag, or null
 */
public record Seo(String description, String canonical, String image, String type, boolean noindex, String jsonLd) {
}