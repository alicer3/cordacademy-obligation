import io.cordacademy.test.IntegrationTest

class NodeDriver : IntegrationTest(
    "io.cordacademy.obligation.v1.contract",
    "io.cordacademy.obligation.workflow"
) {
    companion object {
        @JvmStatic
        fun main(vararg args: String) = NodeDriver().start {}
    }
}