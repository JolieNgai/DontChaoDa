package com.example.dontchaoda

import org.junit.rules.TestWatcher
import org.junit.runner.Description

class TestLoggingRule(
    private val descriptions: Map<String, List<String>>
) : TestWatcher() {

    override fun starting(description: Description) {
        logTestHeader(description, "Running")
    }

    override fun succeeded(description: Description) {
        println("- test PASSED.")
    }

    override fun failed(e: Throwable, description: Description) {
        println("- test FAILED: ${e.message ?: e::class.java.simpleName}")
    }

    private fun logTestHeader(description: Description, prefix: String) {
        val methodName = description.methodName ?: return
        val steps = descriptions[methodName].orEmpty()
        val title = methodName.replace('_', ' ')
        println("### $prefix $title")
        steps.forEach { println("- $it") }
    }
}
