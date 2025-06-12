package mindustry.desktop.steam

import arc.Core
import arc.files.Fi
import arc.func.Cons
import arc.func.Cons2
import arc.struct.ObjectMap
import arc.struct.Seq
import arc.util.Log
import arc.util.Strings
import com.codedisaster.steamworks.*
import com.codedisaster.steamworks.SteamRemoteStorage.PublishedFileVisibility
import com.codedisaster.steamworks.SteamRemoteStorage.WorkshopFileType
import com.codedisaster.steamworks.SteamUGC.*
import mindustry.Vars
import mindustry.game.Schematic
import mindustry.gen.Icon
import mindustry.maps.Map
import mindustry.mod.Mods.LoadedMod
import mindustry.service.Achievement
import mindustry.type.Publishable
import mindustry.ui.dialogs.BaseDialog
import java.util.*

class SWorkshop : SteamUGCCallback {
    val ugc: SteamUGC = SteamUGC(this)

    private val workshopFiles = ObjectMap<Class<out Publishable>, Seq<Fi>>()
    private val detailHandlers = ObjectMap<SteamUGCQuery, Cons2<Seq<SteamUGCDetails>, SteamResult>>()
    private val itemHandlers = Seq<Cons<SteamPublishedFileID>>()
    private val updatedHandlers = ObjectMap<SteamPublishedFileID, Runnable>()

    init {
        val items = ugc.numSubscribedItems
        val ids = arrayOfNulls<SteamPublishedFileID>(items)
        val info = ItemInstallInfo()
        ugc.getSubscribedItems(ids)

        val folders = Seq.with(*ids)
            .map { f: SteamPublishedFileID? -> if (!ugc.getItemInstallInfo(f, info) || info.folder == null) null else Fi(info.folder) }
            .select { f: Fi? -> f != null && f.list().size > 0 }

        workshopFiles.put(Map::class.java, folders.select { f: Fi? -> f!!.list().size == 1 && f.list()[0].extension() == Vars.mapExtension }.map { f: Fi? ->
            f!!.list()[0]
        })
        workshopFiles.put(Schematic::class.java, folders.select { f: Fi? -> f!!.list().size == 1 && f.list()[0].extension() == Vars.schematicExtension }.map { f: Fi? ->
            f!!.list()[0]
        })
        workshopFiles.put(LoadedMod::class.java, folders.select { f: Fi? -> f!!.child("mod.json").exists() || f.child("mod.hjson").exists() }.`as`())

        if (!workshopFiles[Map::class.java].isEmpty) {
            Achievement.downloadMapWorkshop.complete()
        }

        workshopFiles.each { type: Class<out Publishable>, list: Seq<Fi> ->
            Log.info("Fetched content (@): @", type.simpleName, list.size)
        }
    }

    fun getWorkshopFiles(type: Class<out Publishable>): Seq<Fi> {
        return workshopFiles[type, { Seq(0) }]
    }

    /** Publish a new item and submit an update for it.
     * If it is already published, redirects to its page. */
    fun publish(p: Publishable) {
        Log.info("publish(): " + p.steamTitle())
        if (p.hasSteamID()) {
            Log.info("Content already published, redirecting to ID.")
            viewListing(p)
            return
        }

        if (!p.prePublish()) {
            Log.info("Rejecting due to pre-publish.")
            return
        }

        showPublish { id: SteamPublishedFileID -> update(p, id, null, true) }
    }

    /** Fetches info for an item, checking to make sure that it exists. */
    fun viewListing(p: Publishable) {
        val handle = Strings.parseLong(p.steamID, -1)
        val id = SteamPublishedFileID(handle)
        Log.info("Handle = $handle")

        Vars.ui.loadfrag.show()
        query(ugc.createQueryUGCDetailsRequest(id)) { detailsList: Seq<SteamUGCDetails>, result: SteamResult ->
            Vars.ui.loadfrag.hide()
            Log.info("Fetch result = $result")
            if (result == SteamResult.OK) {
                val details = detailsList.first()
                Log.info("Details result = " + details.result)
                if (details.result == SteamResult.OK) {
                    if (details.ownerID == SVars.user!!.user.steamID) {
                        val dialog = BaseDialog("@workshop.info")
                        dialog.setFillParent(false)
                        dialog.cont.add("@workshop.menu").pad(20f)
                        dialog.addCloseButton()

                        dialog.buttons.button("@view.workshop", Icon.link) {
                            viewListingID(id)
                            dialog.hide()
                        }.size(210f, 64f)

                        dialog.buttons.button("@workshop.update", Icon.up) {
                            object : BaseDialog("@workshop.update") {
                                init {
                                    setFillParent(false)
                                    cont.margin(10f).add("@changelog").padRight(6f)
                                    cont.row()
                                    val field = cont.area("") { t: String? -> }.size(500f, 160f).get()
                                    field.maxLength = 400
                                    cont.row()

                                    val updatedesc = booleanArrayOf(false)

                                    cont.check("@updatedesc") { b: Boolean -> updatedesc[0] = b }.pad(4f)

                                    buttons.defaults().size(120f, 54f).pad(4f)
                                    buttons.button("@ok") {
                                        if (!p.prePublish()) {
                                            Log.info("Rejecting due to pre-publish.")
                                            return@button
                                        }
                                        Vars.ui.loadfrag.show("@publishing")
                                        this@SWorkshop.update(p, SteamPublishedFileID(Strings.parseLong(p.steamID, -1)), field.text.replace("\r", "\n"), updatedesc[0])
                                        dialog.hide()
                                        hide()
                                    }
                                    buttons.button("@cancel") { this.hide() }
                                }
                            }.show()
                        }.size(210f, 64f)
                        dialog.show()
                    } else {
                        SVars.net!!.friends.activateGameOverlayToWebPage("steam://url/CommunityFilePage/" + details.publishedFileID.handle())
                    }
                } else if (details.result == SteamResult.FileNotFound) {
                    p.removeSteamID()
                    Vars.ui.showErrorMessage("@missing")
                } else {
                    Vars.ui.showErrorMessage(Core.bundle.format("workshop.error", details.result.name))
                }
            } else {
                Vars.ui.showErrorMessage(Core.bundle.format("workshop.error", result.name))
            }
        }
    }

    fun viewListingID(id: SteamPublishedFileID) {
        SVars.net!!.friends.activateGameOverlayToWebPage("steam://url/CommunityFilePage/" + id.handle())
    }

    fun update(p: Publishable, id: SteamPublishedFileID, changelog: String?, updateDescription: Boolean) {
        Log.info("Calling update(@) @", p.steamTitle(), id.handle())
        val sid = id.handle().toString() + ""

        updateItem(id, { h: SteamUGCUpdateHandle? ->
            if (updateDescription) {
                ugc.setItemTitle(h, p.steamTitle())
                if (p.steamDescription() != null) {
                    ugc.setItemDescription(h, p.steamDescription())
                }
            }
            val tags = p.extraTags()
            tags.add(p.steamTag())

            ugc.setItemTags(h, tags.toArray(String::class.java))
            val path = p.createSteamPreview(sid).absolutePath()

            Log.info("PREVIEW @ @ @", ugc.setItemPreview(h, path), path, Fi.get(path).exists())

            ugc.setItemContent(h, p.createSteamFolder(sid).absolutePath())
            if (changelog == null) {
                ugc.setItemVisibility(h, PublishedFileVisibility.Private)
            }
            ugc.submitItemUpdate(h, changelog ?: "<Created>")
            if (p is Map) {
                Achievement.publishMap.complete()
            }
        }, { p.addSteamID(sid) })
    }

    fun showPublish(published: Cons<SteamPublishedFileID>) {
        val dialog = BaseDialog("@confirm")
        dialog.setFillParent(false)
        dialog.cont.add("@publish.confirm").width(600f).wrap()
        dialog.addCloseButton()
        dialog.buttons.button(
            "@eula", Icon.link
        ) { SVars.net!!.friends.activateGameOverlayToWebPage("https://steamcommunity.com/sharedfiles/workshoplegalagreement") }
            .size(210f, 64f)

        dialog.buttons.button("@ok", Icon.ok) {
            Log.info("Accepted, publishing item...")
            itemHandlers.add(published)
            ugc.createItem(SVars.steamID, WorkshopFileType.Community)
            Vars.ui.loadfrag.show("@publishing")
            dialog.hide()
        }.size(170f, 64f)
        dialog.show()
    }

    fun query(query: SteamUGCQuery, handler: Cons2<Seq<SteamUGCDetails>, SteamResult>) {
        Log.info("POST QUERY $query")
        detailHandlers.put(query, handler)
        ugc.sendQueryUGCRequest(query)
    }

    fun updateItem(publishedFileID: SteamPublishedFileID, tagger: Cons<SteamUGCUpdateHandle?>, updated: Runnable) {
        try {
            val h = ugc.startItemUpdate(SVars.steamID, publishedFileID)
            Log.info("begin updateItem(@)", publishedFileID.handle())

            tagger[h]
            Log.info("Tagged.")

            val info = ItemUpdateInfo()

            Vars.ui.loadfrag.setProgress {
                val status = ugc.getItemUpdateProgress(h, info)
                Vars.ui.loadfrag.setText("@" + status.name.lowercase(Locale.getDefault()))
                if (status == ItemUpdateStatus.Invalid) {
                    Vars.ui.loadfrag.setText("@done")
                    return@setProgress 1f
                }
                status.ordinal.toFloat() / ItemUpdateStatus.entries.size.toFloat()
            }

            updatedHandlers.put(publishedFileID, updated)
        } catch (t: Throwable) {
            Vars.ui.loadfrag.hide()
            Log.err(t)
        }
    }

    override fun onUGCQueryCompleted(query: SteamUGCQuery, numResultsReturned: Int, totalMatchingResults: Int, isCachedData: Boolean, result: SteamResult) {
        Log.info("GET QUERY $query")

        if (detailHandlers.containsKey(query)) {
            Log.info("Query being handled...")
            if (numResultsReturned > 0) {
                Log.info("@ q results", numResultsReturned)
                val details = Seq<SteamUGCDetails>()
                for (i in 0 until numResultsReturned) {
                    details.add(SteamUGCDetails())
                    ugc.getQueryUGCResult(query, i, details[i])
                }
                detailHandlers[query][details, result]
            } else {
                Log.info("Nothing found.")
                detailHandlers[query][Seq(), SteamResult.FileNotFound]
            }

            detailHandlers.remove(query)
        } else {
            Log.info("Query not handled.")
        }
    }

    override fun onSubscribeItem(publishedFileID: SteamPublishedFileID, result: SteamResult) {
        val info = ItemInstallInfo()
        ugc.getItemInstallInfo(publishedFileID, info)
        Log.info("Item subscribed from @", info.folder)
        Achievement.downloadMapWorkshop.complete()
    }

    override fun onUnsubscribeItem(publishedFileID: SteamPublishedFileID, result: SteamResult) {
        val info = ItemInstallInfo()
        ugc.getItemInstallInfo(publishedFileID, info)
        Log.info("Item unsubscribed from @", info.folder)
    }

    override fun onCreateItem(publishedFileID: SteamPublishedFileID, needsToAcceptWLA: Boolean, result: SteamResult) {
        Log.info("onCreateItem($result)")
        if (!itemHandlers.isEmpty) {
            if (result == SteamResult.OK) {
                Log.info("Passing to first handler.")
                itemHandlers.first()[publishedFileID]
            } else {
                Vars.ui.showErrorMessage(Core.bundle.format("publish.error", result.name))
            }

            itemHandlers.remove(0)
        } else {
            Log.err("No handlers for createItem()")
        }
    }

    override fun onSubmitItemUpdate(publishedFileID: SteamPublishedFileID, needsToAcceptWLA: Boolean, result: SteamResult) {
        Vars.ui.loadfrag.hide()
        Log.info("onsubmititemupdate @ @ @", publishedFileID.handle(), needsToAcceptWLA, result)
        if (result == SteamResult.OK) {
            //redirect user to page for further updates
            SVars.net!!.friends.activateGameOverlayToWebPage("steam://url/CommunityFilePage/" + publishedFileID.handle())
            if (needsToAcceptWLA) {
                SVars.net!!.friends.activateGameOverlayToWebPage("https://steamcommunity.com/sharedfiles/workshoplegalagreement")
            }

            if (updatedHandlers.containsKey(publishedFileID)) {
                updatedHandlers[publishedFileID].run()
            }
        } else {
            Vars.ui.showErrorMessage(Core.bundle.format("publish.error", result.name))
        }
    }

    override fun onDownloadItemResult(appID: Int, publishedFileID: SteamPublishedFileID, result: SteamResult) {
        Achievement.downloadMapWorkshop.complete()
        val info = ItemInstallInfo()
        ugc.getItemInstallInfo(publishedFileID, info)
        Log.info("Item downloaded to @", info.folder)
    }

    override fun onDeleteItem(publishedFileID: SteamPublishedFileID, result: SteamResult) {
        val info = ItemInstallInfo()
        ugc.getItemInstallInfo(publishedFileID, info)
        Log.info("Item removed from @", info.folder)
    }
}