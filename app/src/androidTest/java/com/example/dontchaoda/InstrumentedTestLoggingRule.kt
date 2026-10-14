package com.example.dontchaoda

import android.util.Log
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class InstrumentedTestLoggingRule(
    private val descriptions: Map<String, List<String>>,
    private val tag: String = "GradingInstrumentedTest"
) : TestWatcher() {

    override fun starting(description: Description) {
        logTestHeader(description, "Running")
    }

    override fun succeeded(description: Description) {
        Log.i(tag, "- test PASSED.")
    }

    override fun failed(e: Throwable, description: Description) {
        Log.e(tag, "- test FAILED: ${e.message ?: e::class.java.simpleName}")
    }

    private fun logTestHeader(description: Description, prefix: String) {
        val methodName = description.methodName ?: return
        val steps = descriptions[methodName].orEmpty()
        val title = methodName.replace('_', ' ')
        Log.d(tag, "### $prefix $title")
        steps.forEach { Log.d(tag, "- $it") }
    }
}
