package com.marzouki.shop.seo;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.marzouki.shop.catalog.ProductRepository;
import java.time.Duration;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/** robots.txt and sitemap.xml. */
@Controller
@RequiredArgsConstructor
public class SeoController {

    /** The most URLs one sitemap file may list. */
    static final int MAX_URLS = 50_000;

    private final SiteProperties site;
    private final ProductRepository products;
    private final Cache<String, String> cache = Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(1)).maximumSize(2).build();

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public ResponseEntity<String> robots() {
        String body = """
                User-agent: *
                Disallow: /manage/
                Disallow: /admin/
                Disallow: /account/
                Disallow: /api/
                Disallow: /cart
                Disallow: /checkout
                Disallow: /login
                Disallow: /register
                Disallow: /forgot-password
                Disallow: /reset-password
                Disallow: /verify-email
                Disallow: /2fa/
                Disallow: /geo/

                Sitemap: %s/sitemap.xml
                """.formatted(site.url());
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic()).body(body);
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public ResponseEntity<String> sitemap() {
        String xml = cache.get("sitemap", k -> build());
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic()).body(xml);
    }

    private String build() {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        entry(xml, site.url() + "/", null);
        entry(xml, site.url() + "/products", null);
        for (Object[] row : products.sitemapEntries(PageRequest.of(0, MAX_URLS - 2))) {
            entry(xml, site.url() + "/products/" + row[0], ((java.time.Instant) row[1]).atZone(ZoneOffset.UTC).toLocalDate().toString());
        }
        return xml.append("</urlset>\n").toString();
    }

    private static void entry(StringBuilder xml, String url, String lastModified) {
        xml.append("  <url><loc>").append(url).append("</loc>");
        if (lastModified != null) {
            xml.append("<lastmod>").append(lastModified).append("</lastmod>");
        }
        xml.append("</url>\n");
    }
}