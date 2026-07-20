package com.beeper.lightos
import org.junit.Test
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.StateEvent
class BeeperTest {
    @Test
    fun testUnsigned() {
        val event: StateEvent<*>? = null
        val u = event?.unsigned
        println(u?.let { it::class.members.map { m -> m.name } })
    }
}
