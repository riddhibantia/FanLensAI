package com.fanlens.ai.data.prefs

/**
 * Freshness display rules (PRD FR-09). The server is authoritative for the
 * freshness value; this only normalizes unknown input and builds the label
 * so every score display shows a status plus a timestamp.
 */
object Freshness {
    val KNOWN = setOf("LIVE", "DELAYED", "STALE", "DEMO")

    fun normalize(raw: String?): String {
        val upper = raw?.uppercase().orEmpty()
        return if (upper in KNOWN) upper else "NO DATA"
    }

    fun label(freshness: String, updatedAt: String?): String {
        val status = normalize(freshness)
        val stamp = updatedAt?.takeIf { it.isNotBlank() } ?: return "$status · no timestamp yet"
        return "$status · updated $stamp"
    }
}
