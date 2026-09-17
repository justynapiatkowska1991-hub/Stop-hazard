package pl.stophazard.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrafficFilterEngineTest {

    @Test
    fun disabledEngineNeverStarts() {
        val engine = DisabledTrafficFilterEngine()

        assertFalse(engine.start())
        assertFalse(engine.isRunning())
    }

    @Test
    fun factoryReturnsDisabledEngineUntilProductionEngineIsReady() {
        val engine = TrafficFilterEngineFactory.create()

        assertTrue(engine is DisabledTrafficFilterEngine)
        assertFalse(engine.start())
        assertFalse(engine.isRunning())
    }
}
