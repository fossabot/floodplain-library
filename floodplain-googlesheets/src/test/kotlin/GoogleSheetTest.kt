import io.floodplain.kotlindsl.each
import io.floodplain.kotlindsl.message.empty
import io.floodplain.kotlindsl.set
import io.floodplain.kotlindsl.source
import io.floodplain.kotlindsl.stream
import io.floodplain.sink.sheet.GoogleSheetConfiguration
import io.floodplain.sink.sheet.googleSheetConfig
import io.floodplain.sink.sheet.googleSheetsSink
import kotlinx.coroutines.delay
import org.junit.Test

private val logger = mu.KotlinLogging.logger {}

public class GoogleSheetTest {

    val spreadsheetId = "1MTAn1d13M8ptb2MkBHOSNK1gbJOOW1sFQoSfqa1JbXU"
    // var spreadsheetId = "1COkG3-Y0phnHKvwNiFpYewKhT3weEC5CmzmKkXUpPA4"

    @Test
    fun testGoogleSheet() {
        // sanity check
        GoogleSheetConfiguration("connectorName")
    }

    @Test
    fun testSheetWithTopology() {
        stream {
            val config = googleSheetConfig("somename")
            source("topic") {
                each { _, msg, _ -> logger.info("MSG: $msg") }
                set { _, msg, _ ->
                    msg["_row"] = msg.integer("id").toLong()
                    msg
                }
                googleSheetsSink(
                    "outputtopic", spreadsheetId, listOf("column1", "column2"), "A", 1, config)
            }
        }.renderAndTest {
            input("topic", "k1", empty().set("column1", "kol1").set("column2", "otherkol1").set("id", 1))
            input("topic", "k2", empty().set("column1", "kol2").set("column2", "otherkol2").set("id", 2))
            // delay(1000)
            delay(1000)
            input("topic", "k3", empty().set("column1", "kol3").set("column2", "otherkol3").set("id", 3))
            delay(1000)
            val out = outputSize("outputtopic")
            println("output: $out")
            val msg = output("outputtopic")
            logger.info("info: $msg")
            // TODO improve testing
        }
        // Thread.sleep(200000)
    }
}