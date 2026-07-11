package com.example.biblelog.simulation

import com.example.biblelog.simulation.config.ciSmokeConfig
import com.example.biblelog.simulation.config.communityLoadConfig
import com.example.biblelog.simulation.report.ReportFormatter
import com.example.biblelog.simulation.runner.SimulationRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import kotlinx.coroutines.runBlocking

class CommunityLoadSimulationTest {

    @Test
    fun `ci smoke simulation converges`() = runBlocking {
        val report = SimulationRunner(ciSmokeConfig()).run()
        println(ReportFormatter.format(report))
        assertTrue(report.violations.isEmpty(), ReportFormatter.format(report))
        assertTrue(report.successCount > 0)
    }

    @Test
    @Tag("load")
    @EnabledIfEnvironmentVariable(named = "BIBLELOG_LOAD_TEST", matches = "true")
    fun `100 users interact for 5 minutes and converge`() = runBlocking {
        val report = SimulationRunner(communityLoadConfig()).run()
        println(ReportFormatter.format(report))
        println(ReportFormatter.formatJson(report))
        assertTrue(report.violations.isEmpty(), ReportFormatter.format(report))
    }
}
