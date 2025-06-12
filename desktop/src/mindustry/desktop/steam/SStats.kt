package mindustry.desktop.steam

import arc.Events
import arc.util.Log
import arc.util.Timer
import com.codedisaster.steamworks.SteamID
import com.codedisaster.steamworks.SteamResult
import com.codedisaster.steamworks.SteamUserStats
import com.codedisaster.steamworks.SteamUserStatsCallback
import mindustry.Vars
import mindustry.game.EventType.ClientLoadEvent

class SStats : SteamUserStatsCallback {
    val stats: SteamUserStats = SteamUserStats(this)

    private var updated = false
    private val statSavePeriod = 4 //in minutes

    init {
        stats.requestCurrentStats()

        Events.on(ClientLoadEvent::class.java) { e: ClientLoadEvent? ->
            Timer.schedule({
                if (updated) {
                    stats.storeStats()
                }
            }, (statSavePeriod * 60).toFloat(), (statSavePeriod * 60).toFloat())
        }
    }

    fun onUpdate() {
        this.updated = true
    }

    override fun onUserStatsReceived(gameID: Long, steamID: SteamID, result: SteamResult) {
        Vars.service.init()

        if (result != SteamResult.OK) {
            Log.err("Failed to receive steam stats: @", result)
        } else {
            Log.info("Received steam stats.")
        }
    }

    override fun onUserStatsStored(gameID: Long, result: SteamResult) {
        Log.info("Stored stats: @", result)

        if (result == SteamResult.OK) {
            updated = true
        }
    }
}