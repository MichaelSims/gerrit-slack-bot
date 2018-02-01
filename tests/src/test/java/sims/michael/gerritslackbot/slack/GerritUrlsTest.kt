package sims.michael.gerritslackbot.slack

import org.junit.Assert.assertEquals
import org.junit.Test

class GerritUrlsTest {
    @Test
    fun formatGerritBaseUrl_returns_change_url_with_change_number_removed_if_change_number_exists() {
        assertEquals("http://server:8976/node1/gerrit", formatGerritBaseUrl("http://server:8976/node1/gerrit/4578"))
    }

    @Test
    fun formatGerritBaseUrl_falls_back_to_HttpUrl_parse_if_change_number_does_not_exist() {
        assertEquals("http://example.com", formatGerritBaseUrl("http://example.com/foo"))
    }

    @Test
    fun formatGerritBaseUrl_fallback_parsing_preserves_port_number_if_non_default() {
        assertEquals("http://example.com:12345", formatGerritBaseUrl("http://example.com:12345/foo"))
    }

    @Test
    fun formatGerritChangeUrl_works_as_expected() {
        assertEquals("http://server:8976/1/2", formatGerritChangeUrl("http://server:8976", 1, 2))
        assertEquals("http://server:8976/1", formatGerritChangeUrl("http://server:8976", 1, null))
    }
}
