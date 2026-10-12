package com.fanlens.ai.data.network

import com.fanlens.ai.di.TokenStore
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DtoSerializationTest {

    private val api = FanLensApi("http://127.0.0.1:8000/") { TokenStore }

    @Test
    fun preferencesPatch_omitsNulls() {
        val raw = api.json.encodeToString(
            PreferencesRequest.serializer(),
            PreferencesRequest(overlayEnabled = true),
        )
        assertTrue(raw.contains("overlay_enabled"))
        assertFalse(
            "partial PATCH must not contain explicit nulls: $raw",
            raw.contains("null"),
        )
    }

    @Test
    fun explainRequest_omitsUnsetMatchId() {
        val raw = api.json.encodeToString(
            ExplainRequest.serializer(),
            ExplainRequest(question = "why?"),
        )
        assertTrue(raw.contains("why?"))
        assertFalse(raw.contains("null"))
    }
}
