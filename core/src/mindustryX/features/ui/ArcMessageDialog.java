package mindustryX.features.ui;

import arc.*;
import arc.graphics.*;
import arc.math.geom.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.blocks.storage.*;
import mindustryX.features.*;

import java.text.*;
import java.util.*;

import static mindustry.Vars.*;

//move from mindustry.arcModule.ui.dialogs.MessageDialog
public class ArcMessageDialog extends BaseDialog{
    private static boolean ignoreMark = false;
    public static final Seq<advanceMsg> msgList = new Seq<>();

    private int maxMsgRecorded = Math.max(Core.settings.getInt("maxMsgRecorded"), 20);
    private Table historyTable;
    private Boolean fieldMode = false;

    public ArcMessageDialog(){
        super("ARC-中央监控室");

        //voiceControl.voiceControlDialog();
        cont.pane(t -> historyTable = t).growX().scrollX(false);

        addCloseButton();
        buttons.button("设置", Icon.settings, this::arcMsgSettingTable);
        buttons.button("导出", Icon.upload, this::exportMsg).name("导出聊天记录");

        buttons.row();
        buttons.button("清空", Icon.trash, () -> {
            clearMsg();
            build();
        });

        shown(this::build);
        onResize(this::build);

        Events.on(EventType.WorldLoadEvent.class, e -> {
            addMsg(new ArcMessageDialog.advanceMsg(arcMsgType.eventWorldLoad, "载入地图： " + state.map.name()));
            addMsg(new ArcMessageDialog.advanceMsg(arcMsgType.eventWorldLoad, "简介： " + state.map.description()));
            limitMsg(maxMsgRecorded);
        });

        Events.on(EventType.WaveEvent.class, e -> {
            if(state.wavetime < 60f) return;
            addMsg(new ArcMessageDialog.advanceMsg(arcMsgType.eventWave, "波次： " + state.wave + " | " + getWaveInfo(state.wave - 1)));
        });

        Events.on(EventType.BlockDestroyEvent.class, e -> {
            if(e.tile.build instanceof CoreBlock.CoreBuild)
                addMsg(new ArcMessageDialog.advanceMsg(arcMsgType.eventCoreDestory, "核心摧毁： " + "(" + (int)e.tile.x + "," + (int)e.tile.y + ")", new Vec2(e.tile.x * 8, e.tile.y * 8)));
        });
    }

    public static void share(String type, String content){
        UIExt.sendChatMessage("<ARCxMDTX><" + type + ">" + content);
    }

    public static void shareWaveInfo(int waves){
        if(!state.rules.waves) return;
        StringBuilder builder = new StringBuilder();
        builder.append("标记了第").append(waves).append("波");
        if(waves < state.wave){
            builder.append("。");
        }else{
            if(waves > state.wave){
                builder.append("，还有").append(waves - state.wave).append("波");
            }
            int timer = (int)(state.wavetime + (waves - state.wave) * state.rules.waveSpacing);
            builder.append("[[").append(FormatDefault.duration((float)timer / 60)).append("]。");
        }

        builder.append(getWaveInfo(waves));
        share("Wave", builder.toString());
    }

    public static void shareContent(UnlockableContent content, boolean description){
        StringBuilder builder = new StringBuilder();
        builder.append("标记了").append(content.localizedName).append(content.emoji());
        builder.append("(").append(content.name).append(")");
        if(content.description != null && description){
            builder.append("。介绍: ").append(content.description);
        }
        ArcMessageDialog.share("Content", builder.toString());
    }

    public static String getWaveInfo(int waves){
        StringBuilder builder = new StringBuilder();
        if(state.rules.attackMode){
            int sum = Math.max(state.teams.present.sum(t -> t.team != player.team() ? t.cores.size : 0), 1) + Vars.spawner.countSpawns();
            builder.append("包含(×").append(sum).append(")");
        }else{
            builder.append("包含(×").append(Vars.spawner.countSpawns()).append("):");
        }
        for(SpawnGroup group : state.rules.spawns){
            if(group.getSpawned(waves - 1) > 0){
                builder.append((char)Fonts.getUnicode(group.type.name)).append("(");
                if(group.effect != StatusEffects.invincible && group.effect != StatusEffects.none && group.effect != null){
                    builder.append((char)Fonts.getUnicode(group.effect.name)).append("|");
                }
                if(group.getShield(waves - 1) > 0){
                    builder.append(FormatDefault.format(group.getShield(waves - 1))).append("|");
                }
                builder.append(group.getSpawned(waves - 1)).append(")");
            }
        }
        return builder.toString();
    }

    void build(){
        historyTable.clear();
        historyTable.setWidth(800f);
        if(msgList.size == 0) return;
        for(int i = msgList.size - 1; i >= 0; i--){
            int finalI = i;
            if(!msgList.get(finalI).msgType.show) continue;
            historyTable.table(Tex.whiteui, t -> {
                advanceMsg thisMsg = msgList.get(finalI);
                t.background(Tex.whitePane);
                t.setColor(thisMsg.msgType.color);
                t.marginTop(5);

                t.table(Tex.whiteui, tt -> {
                    tt.color.set(thisMsg.msgType.color);

                    if(msgList.get(finalI).msgType == arcMsgType.chat)
                        tt.add(getPlayerName(thisMsg)).style(Styles.outlineLabel).left().width(300f);
                    else
                        tt.add(thisMsg.msgType.name).style(Styles.outlineLabel).color(thisMsg.msgType.color).left().width(300f);

                    tt.add(formatTime(thisMsg.time)).style(Styles.outlineLabel).color(thisMsg.msgType.color).left().padLeft(20f).width(100f);

                    if(thisMsg.msgLoc.x != -1){
                        tt.button("♐： " + (int)(thisMsg.msgLoc.x / tilesize) + "," + (int)(thisMsg.msgLoc.y / tilesize), Styles.logict, () -> {
                            control.input.panCamera(thisMsg.msgLoc);
                            MarkerType.mark.at(Tmp.v1.scl(thisMsg.msgLoc.x, thisMsg.msgLoc.y)).color = color;
                            hide();
                        }).padLeft(50f).height(24f).width(150f);
                    }

                    tt.add().growX();
                    tt.add("    " + (msgList.size - finalI)).style(Styles.outlineLabel).color(thisMsg.msgType.color).padRight(10);

                    tt.button(Icon.copy, Styles.logici, () -> {
                        Core.app.setClipboardText(thisMsg.message);
                        ui.announce("已导出本条聊天记录");
                    }).size(24f).padRight(6);
                    tt.button(Icon.cancel, Styles.logici, () -> {
                        msgList.remove(finalI);
                        build();
                    }).size(24f);

                }).growX().height(30);

                t.row();

                t.table(tt -> {
                    tt.left();
                    tt.marginLeft(4);
                    tt.setColor(thisMsg.msgType.color);
                    if(fieldMode) tt.field(thisMsg.message, Styles.nodeArea, text -> {
                    }).growX();
                    else tt.labelWrap(getPlayerMsg(thisMsg)).growX();
                }).pad(4).padTop(2).growX().grow();

                t.marginBottom(7);
            }).growX().maxWidth(1000f).padBottom(15f).row();
        }
    }

    private String getPlayerName(advanceMsg msgElement){
        int typeStart = msgElement.message.indexOf("[coral][");
        int typeEnd = msgElement.message.indexOf("[coral]]");
        if(typeStart == -1 || typeEnd == -1 || typeEnd <= typeStart){
            return msgElement.msgType.name;
        }

        return msgElement.message.substring(typeStart + 20, typeEnd);
    }

    private String getPlayerMsg(advanceMsg msgElement){
        if(msgElement.msgType != arcMsgType.normal) return msgElement.message;
        int typeStart = msgElement.message.indexOf("[coral][");
        int typeEnd = msgElement.message.indexOf("[coral]]");
        if(typeStart == -1 || typeEnd == -1 || typeEnd <= typeStart){
            return msgElement.message;
        }
        return msgElement.message.substring(typeEnd + 9);
    }

    private void arcMsgSettingTable(){
        BaseDialog setDialog = new BaseDialog("中央监控室-设置");
        if(Core.settings.getInt("maxMsgRecorded") == 0) Core.settings.put("maxMsgRecorded", 500);

        setDialog.cont.table(t -> {

            t.check("停止识别标记等交互信息", ignoreMark, a -> ignoreMark = !ignoreMark).left().width(300f);
            t.row();

            t.check("信息编辑模式", fieldMode, a -> {
                fieldMode = a;
                build();
            }).left().width(200f);
            t.row();

            t.add("调整显示的信息").height(50f);
            t.row();
            t.table(tt -> {
                tt.button("关闭全部", Styles.cleart, () -> {
                    for(arcMsgType type : arcMsgType.values()) type.show = false;
                }).width(200f).height(50f);
                tt.button("默认", Styles.cleart, () -> {
                    for(arcMsgType type : arcMsgType.values()) type.show = true;
                    arcMsgType.serverTips.show = false;
                }).width(200f).height(50f);
            });
            t.row();
            t.table(Tex.button, tt -> tt.pane(tp -> {
                for(arcMsgType type : arcMsgType.values()){

                    CheckBox box = new CheckBox("[#" + type.color.toString() + "]" + type.name);

                    box.update(() -> box.setChecked(type.show));
                    box.changed(() -> {
                        type.show = !type.show;
                        build();
                    });

                    box.left();
                    tp.add(box).left().padTop(3f).row();
                }
            }).maxHeight(500).width(400f));
        });

        setDialog.cont.row();

        setDialog.cont.table(t -> {
            t.add("最大储存聊天记录(过高可能导致卡顿)：");
            t.field(maxMsgRecorded + "", text -> {
                int record = Math.min(Math.max(Integer.parseInt(text), 1), 9999);
                maxMsgRecorded = record;
                Core.settings.put("maxMsgRecorded", record);
            }).valid(Strings::canParsePositiveInt).width(200f).get();
            t.row();
            t.add("超出限制的聊天记录将在载入地图时清除");
        });

        setDialog.addCloseButton();
        setDialog.button("刷新", Icon.refresh, this::build);

        setDialog.show();
    }

    public String formatTime(Date time){
        return new SimpleDateFormat("HH:mm:ss", Locale.US).format(time);
    }

    public static void resolveMsg(String message, @Nullable Player playersender){
        if(!ignoreMark){
            if(resolveMarkMsg(message, playersender)) return;
            if(ui.schematics.resolveSchematic(message, playersender)) return;

            if(playersender != null){
                addMsg(new ArcMessageDialog.advanceMsg(ArcMessageDialog.arcMsgType.chat, message, playersender.name(), new Vec2(playersender.x, playersender.y)));
                return;
            }
            if(resolveServerMsg(message)) return;
        }
        addMsg(new ArcMessageDialog.advanceMsg(ArcMessageDialog.arcMsgType.normal, message));
    }

    public static boolean resolveMarkMsg(String message, @Nullable Player playersender){
        //除了markType以外的内容
        if(message.contains("<ARC")){
            if(message.contains("标记了") && message.contains("Wave")){
                addMsg(new ArcMessageDialog.advanceMsg(arcMsgType.markWave, message));
                return true;
            }else if(message.contains("标记了") && message.contains("Content")){
                addMsg(new ArcMessageDialog.advanceMsg(arcMsgType.markContent, message));
                return true;
            }else if(message.contains("<AT>")){
                addMsg(new ArcMessageDialog.advanceMsg(arcMsgType.markPlayer, message));
                if(message.substring(message.indexOf("AT")).contains(player.name)){
                    if(playersender != null)
                        ui.announce("[gold]你被[white] " + playersender.name + " [gold]戳了一下，请注意查看信息框哦~", 10);
                    else ui.announce("[orange]你被戳了一下，请注意查看信息框哦~", 10);
                }
                return true;
            }
        }
        return false;
    }

    public static boolean resolveServerMsg(String message){
        Seq<String> serverMsg = Seq.with("加入了服务器", "离开了服务器", "自动存档完成", "登录成功", "经验+", "[YELLOW]本局游戏时长:", "[YELLOW]单人快速投票", "[GREEN]回档成功",
        "[YELLOW]PVP保护时间, 全力进攻吧", "[YELLOW]发起", "[YELLOW]你可以在投票结束前使用", "[GREEN]投票成功", "[GREEN]换图成功,当前地图",
        "[RED]本地图禁用单位", "[RED]该地图限制空军,禁止进入敌方领空", "[yellow]本地图限制空军", "[YELLOW]火焰过多造成服务器卡顿,自动关闭火焰",
        " [GREEN]====", "[RED]无效指令", "[RED]该技能", "切换成功",
        "[violet][投票系统][]", "[coral][-]野生的", "[CYAN][+]野生的"   // xem相关
        );
        for(int i = 0; i < serverMsg.size; i++){
            if(message.contains(serverMsg.get(i))){
                addMsg(new ArcMessageDialog.advanceMsg(arcMsgType.serverMsg, message));
                return true;
            }
        }

        if(message.contains("小贴士")){
            addMsg(new ArcMessageDialog.advanceMsg(arcMsgType.serverTips, message));
            return true;
        }
        if(message.contains("[acid][公屏][white]")){
            addMsg(new ArcMessageDialog.advanceMsg(arcMsgType.serverToast, message));
            return true;
        }
        if(message.contains("[YELLOW][技能]")){
            addMsg(new ArcMessageDialog.advanceMsg(arcMsgType.serverSkill, message));
            return true;
        }

        return false;
    }

    public static void addMsg(advanceMsg msg){
        msgList.add(msg);
    }

    private void clearMsg(){
        msgList.clear();
    }

    private void limitMsg(int maxMsg){
        // 限制信息数量
        while(true){
            if(msgList.size < maxMsg) return;
            msgList.remove(0);
        }
    }

    void exportMsg(){
        StringBuilder messageHis = new StringBuilder();
        messageHis.append("下面是[MDTX").append(Version.mdtXBuild).append("] 导出的游戏内聊天记录").append("\n");
        messageHis.append("*** 当前地图名称: ").append(state.map.name()).append("（模式：").append(state.rules.modeName).append("）\n");
        messageHis.append("*** 当前波次: ").append(state.wave).append("\n");

        StringBuilder messageLs = new StringBuilder();
        int messageCount = 0;
        for(int i = 0; i < msgList.size; i++){
            String msg = msgList.get(i).message;
            messageLs.insert(0, msg + "\n");
            messageCount += 1;
        }

        messageHis.append("成功选取共 ").append(messageCount).append(" 条记录，如下：\n");
        messageHis.append(messageLs);
        Core.app.setClipboardText(Strings.stripGlyphs(Strings.stripColors(messageHis.toString())));
    }

    public static class advanceMsg{
        public final arcMsgType msgType;
        public final String message;
        public final Date time;
        public final String sender;
        public boolean selected;
        public final Vec2 msgLoc;

        public advanceMsg(arcMsgType msgType, String message, Date time, String sender, Vec2 msgLoc){
            this.msgType = msgType;
            this.message = message;
            this.time = time;
            this.sender = sender;
            this.msgLoc = new Vec2().set(msgLoc);
        }

        public advanceMsg(arcMsgType msgType, String message, String sender, Vec2 msgLoc){
            this(msgType, message, new Date(), sender, msgLoc);
        }

        public advanceMsg(arcMsgType msgType, String message, Vec2 msgLoc){
            this(msgType, message, "null", msgLoc);
        }

        public advanceMsg(arcMsgType msgType, String message){
            this(msgType, message, new Vec2(-1, -1));
        }


        public advanceMsg sendMessage(){
            ui.chatfrag.addMessage(msgType.arcMsgPreFix() + message);
            return this;
        }
    }

    public enum arcMsgType{
        normal("消息", Color.gray),

        chat("聊天", Color.valueOf("#778899")),
        console("指令", Color.gold),

        markLoc("标记", "坐标", Color.valueOf("#7FFFD4")),
        markWave("标记", "波次", Color.valueOf("#7FFFD4")),
        markContent("标记", "内容", Color.valueOf("#7FFFD4")),
        markPlayer("标记", "玩家", Color.valueOf("#7FFFD4")),
        arcChatPicture("分享", "图片", Color.yellow),
        music("分享", "音乐", Color.pink),
        schematic("分享", "蓝图", Color.blue),
        district("规划区", "", Color.violet),

        serverTips("服务器", "小贴士", Color.valueOf("#98FB98"), false),
        serverMsg("服务器", "信息", Color.valueOf("#cefdce")),
        serverToast("服务器", "通报", Color.valueOf("#00FA9A")),
        serverSkill("服务器", "技能", Color.valueOf("#e6ffcc")),

        logicNotify("逻辑", "通报", Color.valueOf("#ffccff")),
        logicAnnounce("逻辑", "公告", Color.valueOf("#ffccff")),

        eventWorldLoad("事件", "载入地图", Color.valueOf("#ff9999")),
        eventCoreDestory("事件", "核心摧毁", Color.valueOf("#ffcccc")),
        eventWave("事件", "波次", Color.valueOf("#ffcc99"));

        public final String name;
        public final String type;
        public final String subClass;
        public Color color;
        public Boolean show;

        arcMsgType(String type, String subClass, Color color, Boolean show){
            this.name = subClass.isEmpty() ? type : (type + "~" + subClass);
            this.type = type;
            this.subClass = subClass;
            this.color = color;
            this.show = show;
        }

        arcMsgType(String type, String subClass, Color color){
            this(type, subClass, color, true);
        }

        arcMsgType(String type, Color color){
            this(type, "", color);
        }

        public String arcMsgPreFix(){
            return "[#" + color.toString() + "]" + "[" + name + "][]";
        }

    }
}