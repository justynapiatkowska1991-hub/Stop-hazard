package pl.stophazard.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrafficFilterEngineFactoryTest {

    @Test
    fun factoryReturnsDisabledEngineByDefault() {
        val engine = TrafficFilterEngineFactory.create()

        assertTrue(engine is DisabledTrafficFilterEngine)
        assertFalse(engine.isRunning())
        assertFalse(engine.start())
        assertFalse(engine.isRunning())
    }
}
