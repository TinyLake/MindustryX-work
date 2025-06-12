package mindustry.desktop.steam

import com.codedisaster.steamworks.SteamUser
import com.codedisaster.steamworks.SteamUserCallback

class SUser : SteamUserCallback {
    val user: SteamUser = SteamUser(this)
}