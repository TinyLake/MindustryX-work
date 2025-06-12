package mindustry.desktop

import arc.ApplicationListener
import arc.Core
import arc.Events
import arc.Files
import arc.backend.sdl.SdlApplication
import arc.backend.sdl.SdlConfig
import arc.backend.sdl.jni.SDL
import arc.discord.DiscordRPC
import arc.discord.DiscordRPC.NoDiscordClientException
import arc.discord.DiscordRPC.RichPresence
import arc.files.Fi
import arc.func.Cons
import arc.math.Rand
import arc.struct.Seq
import arc.util.Log
import arc.util.OS
import arc.util.Strings
import arc.util.serialization.Base64Coder
import com.codedisaster.steamworks.SteamAPI
import mindustry.ClientLauncher
import mindustry.Vars
import mindustry.core.Version
import mindustry.desktop.steam.*
import mindustry.game.EventType.ClientLoadEvent
import mindustry.game.EventType.DisposeEvent
import mindustry.gen.*
import mindustry.net.ArcNetProvider
import mindustry.net.CrashSender
import mindustry.net.Net.NetProvider
import mindustry.service.GameService
import mindustry.type.Publishable
import java.io.File
import java.io.FileOutputStream
import java.util.*

class DesktopLauncher(args: Array<String>) : ClientLauncher() {
    var useDiscord: Boolean = !OS.hasProp("nodiscord")
    var loadError: Boolean = false
    var steamError: Throwable? = null

    init {
        SDL.SDL_SetHint("SDL_WINDOWS_DPI_SCALING", "1")
        Version.init()
        val useSteam = Version.modifier.contains("steam")
        Vars.testMobile = Seq.with(*args).contains("-testMobile")

        if (useDiscord) {
            try {
                DiscordRPC.connect(discordID)
                Log.info("Initialized Discord rich presence.")
                Runtime.getRuntime().addShutdownHook(Thread { DiscordRPC.close() })
            } catch (none: NoDiscordClientException) {
                //don't log if no client is found
                useDiscord = false
            } catch (t: Throwable) {
                useDiscord = false
                Log.warn("Failed to initialize Discord RPC - you are likely using a JVM <16.")
            }
        }

        if (useSteam) {
            Events.on(ClientLoadEvent::class.java) { event: ClientLoadEvent? ->
                if (steamError != null) {
                    Core.app.post {
                        Core.app.post {
                            Core.app.post {
                                Vars.ui.showErrorMessage(Core.bundle.format("steam.error", if ((steamError!!.message == null)) steamError!!.javaClass.simpleName else steamError!!.javaClass.simpleName + ": " + steamError!!.message))
                            }
                        }
                    }
                }
            }

            try {
                SteamAPI.loadLibraries()

                if (!SteamAPI.init()) {
                    loadError = true
                    Log.err("Steam client not running.")
                } else {
                    initSteam(args)
                    Vars.steam = true
                }

                if (SteamAPI.restartAppIfNecessary(SVars.steamID)) {
                    System.exit(0)
                }
            } catch (e: Throwable) {
                Vars.steam = false
                Log.err("Failed to load Steam native libraries.")
                logSteamError(e)
            }
        }
    }

    fun logSteamError(e: Throwable?) {
        steamError = e
        loadError = true
        Log.err(e)
        try {
            FileOutputStream("steam-error-log-" + System.nanoTime() + ".txt").use { s ->
                val log = Strings.neatError(e)
                s.write(log.toByteArray())
            }
        } catch (e2: Exception) {
            Log.err(e2)
        }
    }

    fun initSteam(args: Array<String>) {
        SVars.net = SNet(ArcNetProvider())
        SVars.stats = SStats()
        SVars.workshop = SWorkshop()
        SVars.user = SUser()
        val isShutdown = booleanArrayOf(false)

        Vars.service = object : GameService() {
            override fun enabled(): Boolean {
                return true
            }

            override fun completeAchievement(name: String) {
                SVars.stats.stats.setAchievement(name)
                SVars.stats.stats.storeStats()
            }

            override fun clearAchievement(name: String) {
                SVars.stats.stats.clearAchievement(name)
                SVars.stats.stats.storeStats()
            }

            override fun isAchieved(name: String): Boolean {
                return SVars.stats.stats.isAchieved(name, false)
            }

            override fun getStat(name: String, def: Int): Int {
                return SVars.stats.stats.getStatI(name, def)
            }

            override fun setStat(name: String, amount: Int) {
                SVars.stats.stats.setStatI(name, amount)
            }

            override fun storeStats() {
                SVars.stats.onUpdate()
            }
        }

        Events.on<ClientLoadEvent>(ClientLoadEvent::class.java) { event: ClientLoadEvent? ->
            Core.settings.defaults("name", SVars.net.friends.personaName)
            if (Vars.player.name.isEmpty()) {
                Vars.player.name = SVars.net.friends.personaName
                Core.settings.put("name", Vars.player.name)
            }
            Vars.steamPlayerName = SVars.net.friends.personaName
            //update callbacks
            Core.app.addListener(object : ApplicationListener {
                override fun update() {
                    if (SteamAPI.isSteamRunning()) {
                        SteamAPI.runCallbacks()
                    }
                }
            })
            Core.app.post {
                if (args.size >= 2 && args[0] == "+connect_lobby") {
                    try {
                        val id = args[1].toLong()
                        Vars.ui.join.connect("steam:$id", Vars.port)
                    } catch (e: Exception) {
                        Log.err("Failed to parse steam lobby ID: @", e.message)
                        e.printStackTrace()
                    }
                }
            }
        }

        Events.on(DisposeEvent::class.java) { event: DisposeEvent? ->
            SteamAPI.shutdown()
            isShutdown[0] = true
        }

        //steam shutdown hook
        Runtime.getRuntime().addShutdownHook(Thread {
            if (!isShutdown[0]) {
                SteamAPI.shutdown()
            }
        })
    }

    override fun getWorkshopContent(type: Class<out Publishable>): Seq<Fi> {
        return if (!Vars.steam) super.getWorkshopContent(type) else SVars.workshop.getWorkshopFiles(type)
    }

    override fun viewListing(pub: Publishable) {
        SVars.workshop.viewListing(pub)
    }

    override fun viewListingID(id: String) {
        SVars.net.friends.activateGameOverlayToWebPage("steam://url/CommunityFilePage/$id")
    }

    override fun getNet(): NetProvider {
        return if (Vars.steam) SVars.net else ArcNetProvider()
    }

    override fun openWorkshop() {
        SVars.net.friends.activateGameOverlayToWebPage("https://steamcommunity.com/app/1127400/workshop/")
    }

    override fun publish(pub: Publishable) {
        SVars.workshop.publish(pub)
    }

    override fun inviteFriends() {
        SVars.net.showFriendInvites()
    }

    override fun updateLobby() {
        if (SVars.net != null) {
            SVars.net.updateLobby()
        }
    }

    override fun updateRPC() {
        //if we're using neither discord nor steam, do no work
        if (!useDiscord && !Vars.steam) return

        //common elements they each share
        val inGame = Vars.state.isGame
        var gameMapWithWave: String? = "Unknown Map"
        var gameMode = ""
        var gamePlayersSuffix = ""
        var uiState = ""

        if (inGame) {
            gameMapWithWave = Strings.capitalize(Strings.stripColors(Vars.state.map.name()))

            if (Vars.state.rules.waves) {
                gameMapWithWave += " | Wave " + Vars.state.wave
            }
            gameMode = if (Vars.state.rules.pvp) "PvP" else if (Vars.state.rules.attackMode) "Attack" else if (Vars.state.rules.infiniteResources) "Sandbox" else "Survival"
            if (Vars.net.active() && Groups.player.size() > 1) {
                gamePlayersSuffix = (" | " + Groups.player.size()).toString() + " Players"
            }
        } else {
            uiState = if (Vars.ui.editor != null && Vars.ui.editor.isShown) {
                "In Editor"
            } else if (Vars.ui.planet != null && Vars.ui.planet.isShown) {
                "In Launch Selection"
            } else {
                "In Menu"
            }
        }

        if (useDiscord) {
            val presence = RichPresence()

            if (inGame) {
                presence.state = gameMode + gamePlayersSuffix
                presence.details = gameMapWithWave
                if (Vars.state.rules.waves) {
                    presence.largeImageText = "Wave " + Vars.state.wave
                }
            } else {
                presence.state = uiState
            }

            presence.largeImageKey = "logo"

            try {
                DiscordRPC.send(presence)
            } catch (ignored: Exception) {
            }
        }

        if (Vars.steam) {
            //Steam mostly just expects us to give it a nice string, but it apparently expects "steam_display" to always be a loc token, so I've uploaded this one which just passes through 'steam_status' raw.
            SVars.net.friends.setRichPresence("steam_display", "#steam_status_raw")

            if (inGame) {
                SVars.net.friends.setRichPresence("steam_status", gameMapWithWave)
            } else {
                SVars.net.friends.setRichPresence("steam_status", uiState)
            }
        }
    }

    override fun getUUID(): String {
        if (Vars.steam) {
            try {
                val result = ByteArray(8)
                Rand(SVars.user.user.steamID.accountID.toLong()).nextBytes(result)
                return String(Base64Coder.encode(result))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return super.getUUID()
    }

    companion object {
        const val discordID: Long = 610508934456934412L
        @JvmStatic
        fun main(arg: Array<String>) {
            try {
                Vars.loadLogger()
                SdlApplication(DesktopLauncher(arg), object : SdlConfig() {
                    init {
                        title = "Mindustry"
                        maximized = true
                        width = 900
                        height = 700
                        for (i in arg.indices) {
                            if (arg[i][0] == '-') {
                                val name = arg[i].substring(1)
                                try {
                                    when (name) {
                                        "width" -> width = arg[i + 1].toInt()
                                        "height" -> height = arg[i + 1].toInt()
                                        "gl3" -> gl30 = true
                                        "antialias" -> samples = 16
                                        "debug" -> Log.level = Log.LogLevel.debug
                                        "maximized" -> maximized = arg[i + 1].toBoolean()
                                    }
                                } catch (number: NumberFormatException) {
                                    Log.warn("Invalid parameter number value.")
                                }
                            }
                        }
                        setWindowIcon(Files.FileType.internal, "icons/icon_64.png")
                    }
                })
            } catch (e: Throwable) {
                handleCrash(e)
            }
        }

        fun handleCrash(e: Throwable?) {
            val dialog = Cons { obj: Runnable -> obj.run() }
            var badGPU = false
            val finalMessage = Strings.getFinalMessage(e)
            val total = Strings.getCauses(e).toString()

            if (total.contains("Couldn't create window") || total.contains("OpenGL 2.0 or higher") || total.lowercase(Locale.getDefault()).contains("pixel format") || total.contains("GLEW") || total.contains("unsupported combination of formats")) {
                dialog[Runnable {
                    message(
                        if (total.contains("Couldn't create window")) "A graphics initialization error has occured! Try to update your graphics drivers:\n$finalMessage" else """
     Your graphics card does not support the right OpenGL features.
     Try to update your graphics drivers. If this doesn't work, your computer may not support Mindustry.
     
     Full message: $finalMessage
     """.trimIndent()
                    )
                }]
                badGPU = true
            }

            val fbgp = badGPU

            CrashSender.send(e) { file: File ->
                val fc = Strings.getFinalCause(e)
                if (!fbgp) {
                    dialog[Runnable {
                        message(
                            """
                            A crash has occured. It has been saved in:
                            ${file.absolutePath}
                            ${fc.javaClass.simpleName.replace("Exception", "")}
                            """.trimIndent() + (if (fc.message == null) "" else """
     :
     ${fc.message}
     """.trimIndent())
                        )
                    }]
                }
            }
        }

        private fun message(message: String) {
            SDL.SDL_ShowSimpleMessageBox(SDL.SDL_MESSAGEBOX_ERROR, "oh no", message)
        }
    }
}