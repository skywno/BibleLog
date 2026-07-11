package com.example.biblelog.simulation.report

object ReportFormatter {
    fun format(report: SimulationReport): String = buildString {
        appendLine("=== BibleLog Simulation Report ===")
        appendLine("Seed: ${report.config.seed}")
        appendLine("Users: ${report.config.userCount}")
        appendLine("Duration: ${report.config.duration}")
        appendLine()
        appendLine("Requests: ${report.totalRequests}")
        appendLine("Success: ${report.successCount} (${"%.1f".format(report.successRate * 100)}%)")
        appendLine("Failures: ${report.failureCount}")
        appendLine("Avg latency: ${report.averageLatency}")
        appendLine("P95 latency: ${report.p95Latency}")
        appendLine("WebSocket reconnects: ${report.webSocketReconnectCount}")
        appendLine()
        appendLine("Action breakdown:")
        report.actionBreakdown.forEach { (action, count) ->
            appendLine("  $action: $count")
        }
        if (report.violations.isNotEmpty()) {
            appendLine()
            appendLine("Consistency violations:")
            report.violations.forEach { violation ->
                appendLine("  [${violation.type}] ${violation.message}")
            }
        } else {
            appendLine()
            appendLine("No consistency violations detected.")
        }
    }

    fun formatJson(report: SimulationReport): String = buildString {
        append("{")
        append("\"seed\":${report.config.seed},")
        append("\"userCount\":${report.config.userCount},")
        append("\"totalRequests\":${report.totalRequests},")
        append("\"successCount\":${report.successCount},")
        append("\"failureCount\":${report.failureCount},")
        append("\"webSocketReconnectCount\":${report.webSocketReconnectCount},")
        append("\"violations\":${report.violations.size}")
        append("}")
    }
}
