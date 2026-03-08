package com.smartpet.todo.tui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandAutoCompleterTest {

    @Test
    fun `completes root command prefix`() {
        val result = CommandAutoCompleter.complete(":ag")

        assertEquals(listOf(":agents"), result.candidates)
        assertEquals(0, result.replacementStart)
        assertEquals(3, result.replacementEnd)
    }

    @Test
    fun `completes mode argument`() {
        val result = CommandAutoCompleter.complete(":mode c")

        assertEquals(listOf("compare"), result.candidates)
        assertEquals(6, result.replacementStart)
        assertEquals(7, result.replacementEnd)
        assertEquals(":mode <auto|compare|single>", result.hint)
    }

    @Test
    fun `completes use argument from registered agents`() {
        val result = CommandAutoCompleter.complete(
            input = ":use co",
            registeredAgents = listOf("codex", "gemini")
        )

        assertEquals(listOf("codex"), result.candidates)
        assertEquals(":use <agent>", result.hint)
    }

    @Test
    fun `completes include argument token by token and excludes already selected`() {
        val result = CommandAutoCompleter.complete(
            input = ":include codex,ge",
            registeredAgents = listOf("codex", "gemini", "claude")
        )

        assertEquals(listOf("gemini"), result.candidates)
    }

    @Test
    fun `completes at cursor position inside argument token`() {
        val input = ":mode compare"
        val cursor = input.indexOf("par")

        val result = CommandAutoCompleter.complete(input = input, cursor = cursor)

        assertEquals(listOf("compare"), result.candidates)
        assertEquals(6, result.replacementStart)
        assertEquals(input.length, result.replacementEnd)
    }

    @Test
    fun `returns empty candidates for use when no agents are registered`() {
        val result = CommandAutoCompleter.complete(":use c", registeredAgents = emptyList())

        assertTrue(result.candidates.isEmpty())
        assertEquals(":use <agent>", result.hint)
    }

    @Test
    fun `returns did you mean for unknown command`() {
        val result = CommandAutoCompleter.complete(":agnts")

        assertEquals(":agents", result.didYouMean)
    }

    @Test
    fun `returns empty candidates for non command input`() {
        val result = CommandAutoCompleter.complete("hello")

        assertTrue(result.candidates.isEmpty())
        assertNull(result.didYouMean)
    }
}
