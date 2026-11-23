package com.example

import com.example.data.AppsScriptSyncService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppsScriptUrlTest {

    @Test
    fun normalizeInputUrl_extractsScriptUrlFromPastedText() {
        val pasted = "Web app URL:\nhttps://script.google.com/macros/s/AKfycbxTest123/exec.\n"
        assertEquals(
            "https://script.google.com/macros/s/AKfycbxTest123/exec",
            AppsScriptSyncService.normalizeInputUrl(pasted)
        )
    }

    @Test
    fun validateAppsScriptUrl_acceptsStandardAndWorkspaceExecUrls() {
        val standard = AppsScriptSyncService.validateAppsScriptUrl(
            "http://script.google.com/macros/s/AKfycbxAbc/exec"
        )
        assertEquals("https://script.google.com/macros/s/AKfycbxAbc/exec", standard)

        val workspace = AppsScriptSyncService.validateAppsScriptUrl(
            "https://script.google.com/a/macros/jslindustries.com/s/AKfycbxAbc/exec"
        )
        assertTrue(workspace.endsWith("/exec"))
    }

    @Test(expected = IllegalStateException::class)
    fun validateAppsScriptUrl_rejectsDevTestUrl() {
        AppsScriptSyncService.validateAppsScriptUrl(
            "https://script.google.com/macros/s/AKfycbxAbc/dev"
        )
    }

    @Test(expected = IllegalStateException::class)
    fun validateAppsScriptUrl_rejectsSpreadsheetUrl() {
        AppsScriptSyncService.validateAppsScriptUrl(
            "https://docs.google.com/spreadsheets/d/123456/edit"
        )
    }
}
