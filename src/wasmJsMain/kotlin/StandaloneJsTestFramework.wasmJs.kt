import kotlin.js.Promise

internal actual val standaloneJsFlatTestFramework: JsTestFramework = object : StandaloneJsFlatTestFramework() {
    override fun toString(): String = "standalone/Wasm/JS"

    override fun suite(name: String, ignored: Boolean, suiteFn: () -> Unit) {
        if (ignored) return // TODO

        suiteFn()

        val suiteFlowId = report.addSuiteStart(name)

        if (tests.isEmpty()) {
            // Finish a suite with no tests synchronously.
            report.addSuiteFinish(name, suiteFlowId)
        } else {
            // Run tests and finish the suite asynchronously.
            fun Test.runWithRemainingTests(nextIndex: Int) {
                startedPromise().then {
                    val nextTest = tests.getOrNull(nextIndex)
                    if (nextTest != null) {
                        nextTest.runWithRemainingTests(nextIndex + 1)
                    } else {
                        report.addSuiteFinish(name, suiteFlowId)
                    }
                    null
                }
            }

            tests.first().runWithRemainingTests(1)
        }
    }

    private fun Test.startedPromise() = Promise { resolve, _ ->
        val flowId = report.addTestStart(name)

        try {
            val promise = testFn() as? Promise<*>
            if (promise != null) {
                promise.then {
                    report.addTestFinish(name, flowId)
                    resolve(null)
                    null
                }.catch { exception ->
                    report.addTestFailureAndFinish(name, Throwable("$exception"), flowId)
                    resolve(null)
                    null
                }
            } else {
                report.addTestFinish(name, flowId)
                resolve(null)
            }
        } catch (exception: Throwable) {
            report.addTestFailureAndFinish(name, exception, flowId)
            resolve(null)
        }
    }
}
