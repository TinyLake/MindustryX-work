package mindustry.desktop.steam

import arc.ApplicationListener
import arc.Core
import arc.Events
import arc.func.Cons
import arc.struct.IntMap
import arc.struct.Seq
import arc.util.Log
import arc.util.Strings
import arc.util.Structs
import com.codedisaster.steamworks.*
import com.codedisaster.steamworks.SteamMatchmaking.*
import com.codedisaster.steamworks.SteamNetworking.P2PSend
import com.codedisaster.steamworks.SteamNetworking.P2PSessionError
import mindustry.Vars
import mindustry.core.Version
import mindustry.game.EventType
import mindustry.game.EventType.ClientLoadEvent
import mindustry.game.EventType.WaveEvent
import mindustry.game.Gamemode
import mindustry.net.ArcNetProvider.PacketSerializer
import mindustry.net.Host
import mindustry.net.Net.NetProvider
import mindustry.net.NetConnection
import mindustry.net.Packet
import mindustry.net.Packets.*
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.CopyOnWriteArrayList

class SNet(val provider: NetProvider) : SteamNetworkingCallback, SteamMatchmakingCallback, SteamFriendsCallback, NetProvider {
    val snet: SteamNetworking = SteamNetworking(this)
    val smat: SteamMatchmaking = SteamMatchmaking(this)
    val friends: SteamFriends = SteamFriends(this)

    val serializer: PacketSerializer = PacketSerializer()
    val writeBuffer: ByteBuffer = ByteBuffer.allocateDirect(16384)
    val readBuffer: ByteBuffer = ByteBuffer.allocateDirect(16384)
    val readCopyBuffer: ByteBuffer = ByteBuffer.allocate(writeBuffer.capacity())

    val connections: CopyOnWriteArrayList<SteamConnection> = CopyOnWriteArrayList()
    val steamConnections: IntMap<SteamConnection> = IntMap() //maps steam ID -> valid net connection

    var currentLobby: SteamID? = null
    var currentServer: SteamID? = null
    var lobbyCallback: Cons<Host>? = null
    var lobbyDoneCallback: Runnable? = null
    var joinCallback: Runnable? = null

    init {
        Events.on(ClientLoadEvent::class.java) { e: ClientLoadEvent? ->
            Core.app.addListener(object : ApplicationListener {
                //read packets
                var length: Int = 0
                var from: SteamID = SteamID()

                override fun update() {
                    while ((snet.isP2PPacketAvailable(0).also { length = it }) != 0) {
                        try {
                            readBuffer.position(0).limit(readBuffer.capacity())
                            //lz4 chokes on direct buffers, so copy the bytes over
                            val len: Int = snet.readP2PPacket(from, readBuffer, 0)
                            readBuffer.limit(len)
                            readCopyBuffer.position(0)
                            readCopyBuffer.put(readBuffer)
                            readCopyBuffer.position(0)
                            val fromID: Int = from.accountID
                            val output: Any = serializer.read(readCopyBuffer)

                            //it may be theoretically possible for this to be a framework message, if the packet is malicious or corrupted
                            if (output !is Packet) return

                            val pack: Packet = output as Packet

                            if (Vars.net.server()) {
                                var con: SteamConnection? = steamConnections.get(fromID)
                                try {
                                    //accept users on request
                                    if (con == null) {
                                        con = SteamConnection(SteamID.createFromNativeHandle(from.handle()))
                                        val c: Connect = Connect()
                                        c.addressTCP = "steam:" + from.getAccountID()

                                        Log.info("&bReceived STEAM connection: @", c.addressTCP)

                                        steamConnections.put(from.getAccountID(), con)
                                        connections.add(con)
                                        Vars.net.handleServerReceived(con, c)
                                    }

                                    Vars.net.handleServerReceived(con, pack)
                                } catch (e: Throwable) {
                                    Log.err(e)
                                }
                            } else if (currentServer != null && fromID == currentServer!!.getAccountID()) {
                                try {
                                    Vars.net.handleClientReceived(pack)
                                } catch (t: Throwable) {
                                    Vars.net.handleException(t)
                                }
                            }
                        } catch (e: Exception) {
                            if (Vars.net.server()) {
                                Log.err(e)
                            } else {
                                Vars.net.showError(e)
                            }
                        }
                    }
                }
            })
        }

        Events.on(WaveEvent::class.java, { e: WaveEvent? -> updateWave() })
        Events.run(EventType.Trigger.newGame, { this.updateWave() })
    }

    val isSteamClient: Boolean
        get() = currentServer != null

    @Throws(IOException::class)
    override fun connectClient(ip: String, port: Int, success: Runnable) {
        if (ip.startsWith("steam:")) {
            val lobbyname = ip.substring("steam:".length)
            try {
                val lobby = SteamID.createFromNativeHandle(lobbyname.toLong())
                joinCallback = success
                smat.joinLobby(lobby)
            } catch (e: NumberFormatException) {
                throw IOException("Invalid Steam ID: $lobbyname")
            }
        } else {
            provider.connectClient(ip, port, success)
        }
    }

    override fun sendClient(`object`: Any, reliable: Boolean) {
        if (isSteamClient) {
            if (currentServer == null) {
                Log.info("Not connected, quitting.")
                return
            }

            try {
                writeBuffer.limit(writeBuffer.capacity())
                writeBuffer.position(0)
                serializer.write(writeBuffer, `object`)
                val length = writeBuffer.position()
                writeBuffer.flip()

                snet.sendP2PPacket(currentServer, writeBuffer, if (reliable || length >= 1000) P2PSend.Reliable else P2PSend.UnreliableNoDelay, 0)
            } catch (e: Exception) {
                Vars.net.showError(e)
            }
        } else {
            provider.sendClient(`object`, reliable)
        }
    }

    override fun disconnectClient() {
        if (isSteamClient) {
            if (currentLobby != null) {
                smat.leaveLobby(currentLobby)
                snet.closeP2PSessionWithUser(currentServer)
                currentServer = null
                currentLobby = null
                Vars.net.handleClientReceived(Disconnect())
            }
        } else {
            provider.disconnectClient()
        }
    }

    override fun discoverServers(callback: Cons<Host>, done: Runnable) {
        smat.addRequestLobbyListResultCountFilter(32)
        smat.addRequestLobbyListDistanceFilter(LobbyDistanceFilter.Worldwide)
        smat.requestLobbyList()
        lobbyCallback = callback

        //after the steam lobby is done discovering, look for local network servers.
        lobbyDoneCallback = Runnable { provider.discoverServers(callback, done) }
    }

    override fun pingHost(address: String, port: Int, valid: Cons<Host>, failed: Cons<Exception>) {
        provider.pingHost(address, port, valid, failed)
    }

    @Throws(IOException::class)
    override fun hostServer(port: Int) {
        provider.hostServer(port)
        smat.createLobby(if (Core.settings.getBool("steampublichost")) LobbyType.Public else LobbyType.FriendsOnly, Core.settings.getInt("playerlimit"))

        Core.app.post {
            Core.app.post {
                Core.app.post {
                    Log.info(
                        "Server: @\nClient: @\nActive: @",
                        Vars.net.server(),
                        Vars.net.client(),
                        Vars.net.active()
                    )
                }
            }
        }
    }

    fun updateLobby() {
        if (currentLobby != null && Vars.net.server()) {
            smat.setLobbyType(currentLobby, if (Core.settings.getBool("steampublichost")) LobbyType.Public else LobbyType.FriendsOnly)
            smat.setLobbyMemberLimit(currentLobby, Core.settings.getInt("playerlimit"))
        }
    }

    fun updateWave() {
        if (currentLobby != null && Vars.net.server()) {
            smat.setLobbyData(currentLobby, "mapname", Vars.state.map.name())
            smat.setLobbyData(currentLobby, "wave", Vars.state.wave.toString() + "")
            smat.setLobbyData(currentLobby, "gamemode", Vars.state.rules.mode().name + "")
        }
    }

    override fun closeServer() {
        provider.closeServer()

        if (currentLobby != null) {
            smat.leaveLobby(currentLobby)
            for (con: SteamConnection in steamConnections.values()) {
                con.close()
            }
            currentLobby = null
        }

        steamConnections.clear()
    }

    override fun getConnections(): Iterable<NetConnection> {
        //merge provider connections
        val connectionsOut = CopyOnWriteArrayList<NetConnection>(connections)
        for (c: NetConnection in provider.connections) connectionsOut.add(c)
        return connectionsOut
    }

    fun disconnectSteamUser(steamid: SteamID) {
        //a client left
        val sid = steamid.accountID
        snet.closeP2PSessionWithUser(steamid)

        if (steamConnections.containsKey(sid)) {
            val con = steamConnections[sid]
            Vars.net.handleServerReceived(con, Disconnect())
            steamConnections.remove(sid)
            connections.remove(con)
        }
    }

    override fun onLobbyInvite(steamIDUser: SteamID, steamIDLobby: SteamID, gameID: Long) {
        Log.info("onLobbyInvite @ @ @", steamIDLobby.accountID, steamIDUser.accountID, gameID)
    }

    override fun onLobbyEnter(steamIDLobby: SteamID, chatPermissions: Int, blocked: Boolean, response: ChatRoomEnterResponse) {
        Log.info("onLobbyEnter @ @", steamIDLobby.accountID, response)

        if (response != ChatRoomEnterResponse.Success) {
            Vars.ui.loadfrag.hide()
            Vars.ui.showErrorMessage(Core.bundle.format("cantconnect", response.toString()))
            return
        }

        val version = Strings.parseInt(smat.getLobbyData(steamIDLobby, "version"), -1)

        //check version
        if (version != Version.build) {
            Vars.ui.loadfrag.hide()
            Vars.ui.showInfo(
                "[scarlet]" + (if (version > Version.build) KickReason.clientOutdated else KickReason.serverOutdated).toString() + "\n[]" +
                        Core.bundle.format("server.versions", Version.build, version)
            )
            smat.leaveLobby(steamIDLobby)
            return
        }

        Vars.logic.reset()
        Vars.net.reset()

        currentLobby = steamIDLobby
        currentServer = smat.getLobbyOwner(steamIDLobby)

        Log.info("Connect to owner @: @", (currentServer as SteamID).getAccountID(), friends.getFriendPersonaName(currentServer))

        if (joinCallback != null) {
            joinCallback!!.run()
            joinCallback = null
        }

        val con = Connect()
        con.addressTCP = "steam:" + (currentServer as SteamID).getAccountID()

        Vars.net.setClientConnected()
        Vars.net.handleClientReceived(con)

        Core.app.post {
            Core.app.post {
                Core.app.post {
                    Log.info(
                        "Server: @\nClient: @\nActive: @",
                        Vars.net.server(),
                        Vars.net.client(),
                        Vars.net.active()
                    )
                }
            }
        }
    }

    override fun onLobbyChatUpdate(lobby: SteamID, who: SteamID, changer: SteamID, change: ChatMemberStateChange) {
        Log.info("lobby @: @ caused @'s change: @", lobby.accountID, who.accountID, changer.accountID, change)
        if (change == ChatMemberStateChange.Disconnected || change == ChatMemberStateChange.Left) {
            if (Vars.net.client()) {
                //host left, leave as well
                if ((who == currentServer) || (who == currentLobby)) {
                    Vars.net.disconnect()
                    Log.info("Current host left.")
                }
            } else {
                //a client left
                disconnectSteamUser(who)
            }
        }
    }

    override fun onLobbyMatchList(matches: Int) {
        Log.info("found @ matches", matches)

        if (lobbyDoneCallback != null) {
            val hosts = Seq<Host>()
            for (i in 0 until matches) {
                try {
                    val lobby = smat.getLobbyByIndex(i)
                    if ((smat.getLobbyData(lobby, "hidden") == "true")) continue
                    val mode = smat.getLobbyData(lobby, "gamemode")
                    //make sure versions are equal, don't list incompatible lobbies
                    if ((mode == null) || mode.isEmpty() || (Version.build != -1 && Strings.parseInt(smat.getLobbyData(lobby, "version"), -1) != Version.build)) continue
                    val out = Host(
                        -1,  //invalid ping
                        smat.getLobbyData(lobby, "name"),
                        "steam:" + lobby.handle(),
                        smat.getLobbyData(lobby, "mapname"),
                        Strings.parseInt(smat.getLobbyData(lobby, "wave"), -1),
                        smat.getNumLobbyMembers(lobby),
                        Strings.parseInt(smat.getLobbyData(lobby, "version"), -1),
                        smat.getLobbyData(lobby, "versionType"),
                        Gamemode.valueOf(mode),
                        smat.getLobbyMemberLimit(lobby),
                        "",
                        null
                    )
                    hosts.add(out)
                } catch (e: Exception) {
                    Log.err(e)
                }
            }

            hosts.sort(Structs.comparingInt({ h: Host -> -h.players }))
            hosts.each(lobbyCallback)

            lobbyDoneCallback!!.run()
        }
    }

    override fun onLobbyCreated(result: SteamResult, steamID: SteamID) {
        if (!Vars.net.server()) {
            Log.info("Lobby created on server: @, ignoring.", steamID)
            return
        }

        Log.info("Lobby @ created? @", result, steamID.accountID)
        if (result == SteamResult.OK) {
            currentLobby = steamID

            smat.setLobbyData(steamID, "name", Vars.player.name)
            smat.setLobbyData(steamID, "mapname", Vars.state.map.name())
            smat.setLobbyData(steamID, "version", Version.build.toString() + "")
            smat.setLobbyData(steamID, "versionType", Version.type)
            smat.setLobbyData(steamID, "wave", Vars.state.wave.toString() + "")
            smat.setLobbyData(steamID, "gamemode", Vars.state.rules.mode().name + "")
        }
    }

    fun showFriendInvites() {
        if (currentLobby != null) {
            friends.activateGameOverlayInviteDialog(currentLobby)
            Log.info("Activating overlay dialog")
        }
    }

    override fun onP2PSessionConnectFail(steamIDRemote: SteamID, sessionError: P2PSessionError) {
        if (Vars.net.server()) {
            Log.info("@ has disconnected: @", steamIDRemote.accountID, sessionError)
            disconnectSteamUser(steamIDRemote)
        } else if ((steamIDRemote == currentServer)) {
            Log.info("Disconnected! @: @", steamIDRemote.accountID, sessionError)
            Vars.net.handleClientReceived(Disconnect())
        }
    }

    override fun onP2PSessionRequest(steamIDRemote: SteamID) {
        Log.info("Connection request: @", steamIDRemote.accountID)
        if (Vars.net.server()) {
            Log.info("Am server, accepting request from " + steamIDRemote.accountID)
            snet.acceptP2PSessionWithUser(steamIDRemote)
        }
    }

    override fun onGameLobbyJoinRequested(lobby: SteamID, steamIDFriend: SteamID) {
        Log.info("onGameLobbyJoinRequested @ @", lobby, steamIDFriend)
        smat.joinLobby(lobby)
    }

    inner class SteamConnection(val sid: SteamID) : NetConnection(sid.accountID.toString() + "") {
        init {
            Log.info("Created STEAM connection: @", sid.accountID)
        }

        override fun send(`object`: Any, reliable: Boolean) {
            try {
                writeBuffer.limit(writeBuffer.capacity())
                writeBuffer.position(0)
                serializer.write(writeBuffer, `object`)
                val length = writeBuffer.position()
                writeBuffer.flip()

                snet.sendP2PPacket(sid, writeBuffer, if (reliable || length >= 1000) if (`object` is StreamChunk) P2PSend.ReliableWithBuffering else P2PSend.Reliable else P2PSend.UnreliableNoDelay, 0)
            } catch (e: Exception) {
                Log.err(e)
                Log.info("Error sending packet. Disconnecting invalid client!")
                close()

                val k = steamConnections[sid.accountID]
                if (k != null) steamConnections.remove(sid.accountID)
            }
        }

        override fun isConnected(): Boolean {
            //TODO ???
            //snet.getP2PSessionState(sid, state);
            return true //state.isConnectionActive();
        }

        override fun close() {
            disconnectSteamUser(sid)
        }
    }
}