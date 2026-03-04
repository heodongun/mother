package com.smartpet.todo.tui

private val ROOT_COMMANDS = listOf(":help", ":agents", ":clear", ":save", ":exit", ":mode", ":use", ":include", ":model")
private val MODE_VALUES = listOf("auto", "compare", "single")

private val COMMAND_HINTS = mapOf(
    ":mode" to ":mode <auto|compare|single>",
    ":use" to ":use <agent>",
    ":model" to ":model <agent>",
    ":include" to ":include <agent[,agent,...]>"
)

data class CompletionResult(
    val candidates: List<String>,
    val replacementStart: Int,
    val replacementEnd: Int,
    val hint: String? = null,
    val didYouMean: String? = null
)

object CommandAutoCompleter {
    fun complete(
        input: String,
        cursor: Int = input.length,
        registeredAgents: List<String> = emptyList(),
        alreadyIncludedAgents: List<String> = emptyList()
    ): CompletionResult {
        val safeCursor = cursor.coerceIn(0, input.length)
        val head = input.substring(0, safeCursor)

        if (!head.startsWith(':')) {
            return emptyAtCursor(safeCursor)
        }

        val firstSpace = head.indexOf(' ')
        if (firstSpace == -1) {
            val candidates = ROOT_COMMANDS.filter { it.startsWith(head) }
            val didYouMean = if (candidates.isEmpty()) closestCommand(head) else null
            return CompletionResult(
                candidates = candidates,
                replacementStart = 0,
                replacementEnd = safeCursor,
                didYouMean = didYouMean
            )
        }

        val command = head.substring(0, firstSpace)
        val args = input.substring(firstSpace + 1)
        val relativeCursor = (safeCursor - (firstSpace + 1)).coerceAtLeast(0)

        return when (command) {
            ":mode" -> completeSingleToken(
                args = args,
                argsOffset = firstSpace + 1,
                cursorInArgs = relativeCursor,
                candidates = MODE_VALUES,
                hint = COMMAND_HINTS[command]
            )

            ":use", ":model" -> completeSingleToken(
                args = args,
                argsOffset = firstSpace + 1,
                cursorInArgs = relativeCursor,
                candidates = registeredAgents.distinct().sorted(),
                hint = COMMAND_HINTS[command]
            )

            ":include" -> completeCommaSeparatedAgents(
                args = args,
                argsOffset = firstSpace + 1,
                cursorInArgs = relativeCursor,
                registeredAgents = registeredAgents,
                alreadyIncludedAgents = alreadyIncludedAgents,
                hint = COMMAND_HINTS[command]
            )

            else -> emptyAtCursor(safeCursor, didYouMean = closestCommand(command))
        }
    }

    private fun completeSingleToken(
        args: String,
        argsOffset: Int,
        cursorInArgs: Int,
        candidates: List<String>,
        hint: String?
    ): CompletionResult {
        val safeCursor = cursorInArgs.coerceIn(0, args.length)
        val tokenStart = args.lastIndexOf(' ', startIndex = safeCursor - 1).let { if (it == -1) 0 else it + 1 }
        val tokenEnd = args.indexOf(' ', startIndex = safeCursor).let { if (it == -1) args.length else it }
        val token = args.substring(tokenStart, safeCursor)

        return CompletionResult(
            candidates = candidates.filter { it.startsWith(token) },
            replacementStart = argsOffset + tokenStart,
            replacementEnd = argsOffset + tokenEnd,
            hint = hint
        )
    }

    private fun completeCommaSeparatedAgents(
        args: String,
        argsOffset: Int,
        cursorInArgs: Int,
        registeredAgents: List<String>,
        alreadyIncludedAgents: List<String>,
        hint: String?
    ): CompletionResult {
        val safeCursor = cursorInArgs.coerceIn(0, args.length)
        val beforeCursor = args.substring(0, safeCursor)
        val tokenStart = beforeCursor.lastIndexOf(',').let { if (it == -1) 0 else it + 1 }
        val tokenEnd = args.indexOf(',', startIndex = safeCursor).let { if (it == -1) args.length else it }

        val beforeTokenList = args.substring(0, tokenStart)
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val used = (beforeTokenList + alreadyIncludedAgents).toSet()
        val allowedCandidates = registeredAgents.distinct().filterNot { it in used }.sorted()

        val rawToken = args.substring(tokenStart, safeCursor)
        val trimmedToken = rawToken.trimStart()
        val leadingWhitespace = rawToken.length - trimmedToken.length

        return CompletionResult(
            candidates = allowedCandidates.filter { it.startsWith(trimmedToken) },
            replacementStart = argsOffset + tokenStart + leadingWhitespace,
            replacementEnd = argsOffset + tokenEnd,
            hint = hint
        )
    }

    private fun emptyAtCursor(cursor: Int, didYouMean: String? = null): CompletionResult =
        CompletionResult(emptyList(), cursor, cursor, didYouMean = didYouMean)

    private fun closestCommand(rawCommand: String): String? {
        return ROOT_COMMANDS
            .map { candidate -> candidate to levenshtein(rawCommand, candidate) }
            .minByOrNull { it.second }
            ?.takeIf { (_, distance) -> distance <= 3 }
            ?.first
    }

    private fun levenshtein(left: String, right: String): Int {
        if (left == right) return 0
        if (left.isEmpty()) return right.length
        if (right.isEmpty()) return left.length

        val distances = IntArray(right.length + 1) { it }
        for (i in 1..left.length) {
            var previousDiagonal = distances[0]
            distances[0] = i
            for (j in 1..right.length) {
                val temp = distances[j]
                val cost = if (left[i - 1] == right[j - 1]) 0 else 1
                distances[j] = minOf(
                    distances[j] + 1,
                    distances[j - 1] + 1,
                    previousDiagonal + cost
                )
                previousDiagonal = temp
            }
        }
        return distances[right.length]
    }
}
