package pl.stophazard.app.prototype

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PrototypeTrafficEngineTest {

    @Test
    fun engineIsStoppedByDefault() {
        val engine = PrototypeTrafficEngine()

        assertEquals(PrototypeTrafficEngine.State.STOPPED, engine.state)
        assertFalse(engine.start())
        assertEquals(PrototypeTrafficEngine.State.STOPPED, engine.state)
    }

    @Test
    fun deviceTestStartFailsSafelyUntilRealTransportExists() {
        val engine = PrototypeTrafficEngine()

        assertFalse(engine.start(allowStartForDeviceTest = true))
        assertEquals(PrototypeTrafficEngine.State.FAILED, engine.state)
    }

    @Test
    fun stopReturnsEngineToStoppedState() {
        val engine = PrototypeTrafficEngine()

        engine.start(allowStartForDeviceTest = true)
        engine.stop()

        assertEquals(PrototypeTrafficEngine.State.STOPPED, engine.state)
    }
}
