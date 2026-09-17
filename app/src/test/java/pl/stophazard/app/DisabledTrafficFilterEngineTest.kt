package pl.stophazard.app

import org.junit.Assert.assertFalse
import org.junit.Test

class DisabledTrafficFilterEngineTest {

    @Test
    fun disabledEngineNeverStartsOrReportsRunning() {
        val engine = DisabledTrafficFilterEngine()

        assertFalse(engine.isRunning())
        assertFalse(engine.start())
        assertFalse(engine.isRunning())

        engine.stop()
        assertFalse(engine.isRunning())
    }

    @Test
    fun repeatedStopIsSafe() {
        val engine = DisabledTrafficFilterEngine()

        engine.stop()
        engine.stop()

        assertFalse(engine.isRunning())
    }
}
