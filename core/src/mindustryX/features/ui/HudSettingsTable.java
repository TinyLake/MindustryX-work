package mindustryX.features.ui;

import arc.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustryX.features.Settings;
import mindustryX.features.*;

import static arc.Core.*;
import static mindustry.Vars.*;

//moved from mindustry.arcModule.ui.quickTool.HudSettingsTable
public class HudSettingsTable extends ToolTableBase{
    private final Table cont = new Table();

    public HudSettingsTable(){
        icon = String.valueOf(Iconc.settings);
        rebuild();
        Events.on(EventType.WorldLoadEvent.class, e -> Core.settings.put("removeLogicLock", false));
    }

    @Override
    protected void buildTable(){
        cont.clearChildren();

        cont.background(Styles.black6);
        cont.table(t -> {
            t.button("[cyan]S", Styles.flatBordert, () -> Call.sendChatMessage("/sync")).size(30).tooltip("同步一波");
            t.button("[cyan]观", Styles.flatBordert, () -> Call.sendChatMessage("/ob")).size(30).tooltip("观察者模式");
            t.button("[cyan]技", Styles.flatBordert, () -> Call.sendChatMessage("/skill")).size(30).tooltip("技能！");
            t.button("[cyan]版", Styles.flatBordert, () -> Call.sendChatMessage("/broad")).size(30).tooltip("服务器信息版");
            t.button("[red]版", Styles.flatTogglet, () -> Settings.toggle("ShowInfoPopup")).checked(a -> Core.settings.getBool("ShowInfoPopup")).size(30, 30).tooltip("关闭所有信息版");
            t.button("[white]法", Styles.flatBordert, () -> ui.showConfirm("受不了，直接投降？", () -> Call.sendChatMessage("/vote gameover"))).size(30, 30).tooltip("法国军礼");
            if(settings.getInt("arcQuickMsg", 0) == 0)
                t.button("\uE87C", Styles.flatBordert, this::arcQuickMsgTable).size(30, 30).tooltip("快捷消息");
        }).left().row();

        if(settings.getInt("arcQuickMsg") > 0){
            cont.table(t -> {
                for(int i = 0; i < settings.getInt("arcQuickMsg"); i++){
                    if(i % settings.getInt("arcQuickMsgKey", 8) == 0) t.row();
                    int finalI = i;
                    t.button(settings.getString(getArcQuickMsgShortName(i)), Styles.flatBordert, () -> {
                        if(settings.getBool(getArcQuickMsgJs(finalI))) mods.getScripts().runConsole(settings.getString(getArcQuickMsgName(finalI)));
                        else Call.sendChatMessage(settings.getString(getArcQuickMsgName(finalI)));
                    }
                    ).size(30);
                }
                t.button("\uE87C", Styles.flatBordert, this::arcQuickMsgTable).size(30, 30).tooltip("快捷消息");
            }).left().row();
        }
        cont.table(t -> {
            t.button("[cyan]块", Styles.flatTogglet, () -> Settings.cycle("blockRenderLevel", 3))
            .checked((a) -> RenderExt.blockRenderLevel > 0).size(30, 30).tooltip("建筑显示");
            t.button("[cyan]兵", Styles.flatTogglet, () -> RenderExt.unitHide = !RenderExt.unitHide)
            .checked(a -> !RenderExt.unitHide).size(30, 30).tooltip("兵种显示");
            t.button("[cyan]箱", Styles.flatTogglet, () -> Settings.toggle("unithitbox"))
            .checked(a -> Core.settings.getBool("unithitbox")).size(30, 30).tooltip("碰撞箱显示");
            t.button("[cyan]弹", Styles.flatTogglet, () -> Settings.toggle("bulletShow"))
            .checked(a -> Core.settings.getBool("bulletShow")).size(30, 30).tooltip("子弹显示");
            t.button("[cyan]" + Iconc.map, Styles.flatTogglet, () -> Settings.toggle("minimap"))
            .checked(a -> Core.settings.getBool("minimap")).size(30, 30).tooltip("小地图显示");
            t.button("[violet]锁", Styles.flatTogglet, () -> {
                Settings.toggle("removeLogicLock");
                control.input.logicCutscene = false;
                ui.announce("已移除逻辑视角锁定");
            }).checked(a -> Core.settings.getBool("removeLogicLock")).size(30, 30).tooltip("逻辑锁定");
            t.button("[cyan]雾", Styles.flatTogglet, () -> {
                if(!state.rules.pvp || player.team().id == 255) renderer.fogEnabled = !renderer.fogEnabled;
            }).checked(a -> renderer.fogEnabled).size(30, 30).tooltip("战争迷雾").visible(() -> !state.rules.pvp || player.team().id == 255);
        }).left().row();
        cont.table(t -> {
            t.button("[red]灯", Styles.flatTogglet, () -> Settings.toggle("drawlight"))
            .checked(a -> state.rules.lighting).size(30, 30).name("灯光").tooltip("[cyan]开灯啊！");
            t.button("[acid]效", Styles.flatTogglet, () -> Settings.toggle("effects"))
            .checked(a -> Core.settings.getBool("effects")).size(30, 30).tooltip("特效显示");
            t.button("[acid]光", Styles.flatTogglet, () -> {
                Settings.toggle("bloom");
                renderer.toggleBloom(settings.getBool("bloom"));
            }).checked(a -> Core.settings.getBool("bloom")).size(30, 30).tooltip("光效显示");
            t.button("[acid]墙", Styles.flatTogglet, () -> enableDarkness ^= true)
            .checked(a -> enableDarkness).size(30, 30).tooltip("墙体阴影显示");
            t.button("[acid]天", Styles.flatTogglet, () -> Settings.toggle("showweather"))
            .checked(a -> Core.settings.getBool("showweather")).size(30, 30).tooltip("天气显示");
            t.button("[cyan]扫", Styles.flatTogglet, () -> ArcScanMode.enabled = !ArcScanMode.enabled)
            .checked(a -> ArcScanMode.enabled).size(30, 30).tooltip("扫描模式");

        }).left().row();

        sliderPref("turretShowRange", 0, 3, 1, s -> switch(s){
            case 0 -> "关闭";
            case 1 -> "仅对地";
            case 2 -> "仅对空";
            case 3 -> "全部";
            default -> s + "";
        });
        sliderPref("chatValidType", 0, 3, 1, s -> switch(s){
            case 0 -> "原版模式";
            case 1 -> "纯净聊天";
            case 2 -> "服务器记录";
            case 3 -> "全部记录";
            default -> s + "";
        });
        checkPref("unitHealthBar");
        sliderPref("unitTransparency", 0, 100, 5, i -> i > 0 ? i + "%" : "关闭");
        sliderPref("unitDrawMinHealth", 0, 2500, 50, i -> i + "[red]HP");
        sliderPref("unitBarDrawMinHealth", 0, 2500, 100, i -> i + "[red]HP");
        sliderPref("unitWeaponRange", 0, 100, 1, i -> i > 0 ? i + "%" : "关闭");

        checkPref("alwaysShowUnitRTSAi");
        checkPref("unitLogicMoveLine");
        checkPref("unitWeaponTargetLine");

        checkPref("blockWeaponTargetLine");
        checkPref("unitbuildplan");
        sliderPref("minimapSize", 40, 400, 10, i -> i + "");

        ScrollPane pane = pane(cont).maxSize(800f, 300f).get();
        pane.update(() -> {
            Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
            if(e != null && e.isDescendantOf(pane)){
                pane.requestScroll();
            }else if(pane.hasScroll()){
                Core.scene.setScrollFocus(null);
            }
        });
    }

    private void arcQuickMsgTable(){
        BaseDialog dialog = new BaseDialog("快捷信息");
        dialog.cont.table(t -> {
            t.add("""
            在此编辑快速消息，可在快捷设置面板显示。如设置：
            [white]法 /vote gameover
            这一指令会添加一个“[white]法的按钮，点击会自动输入/vote gameover。
            由于懒得写更新，请修改滑块后[orange]关闭此窗口后再打开一次[white]
            快捷设置面板同样需要[orange]关闭后再打开一次[white]才能生效""").center().fillX().row();
            t.table(tt -> {
                tt.add("快捷消息个数： ");
                Label label = tt.add(String.valueOf(settings.getInt("arcQuickMsg", 0))).get();
                tt.slider(0, 50, 1, settings.getInt("arcQuickMsg", 0), i -> {
                    settings.put("arcQuickMsg", (int)i);
                    label.setText(String.valueOf(settings.getInt("arcQuickMsg")));
                }).width(200f).row();
                tt.add("每行多少个按键： ");
                Label label2 = tt.add(String.valueOf(settings.getInt("arcQuickMsgKey", 0))).get();
                tt.slider(3, 10, 1, settings.getInt("arcQuickMsgKey", 0), i -> {
                    settings.put("arcQuickMsgKey", (int)i);
                    label2.setText(String.valueOf(settings.getInt("arcQuickMsgKey")));
                }).width(200f);
            }).row();
            t.pane(tt -> {
                tt.add("第i个").width(50f);
                tt.add("JS").width(50f);
                tt.add("按钮显示\n(建议单个字符)").width(100f);
                tt.add("              输入信息").width(400f).center().row();

                for(int i = 0; i < settings.getInt("arcQuickMsg", 0); i++){
                    tt.add(i + "  ");
                    int finalI = i;
                    tt.check("", settings.getBool(getArcQuickMsgJs(finalI)), js -> settings.put(getArcQuickMsgJs(finalI), js));
                    tt.field(settings.getString(getArcQuickMsgShortName(finalI), "?"), text -> settings.put(getArcQuickMsgShortName(finalI), text)).maxTextLength(10);
                    tt.field(settings.getString(getArcQuickMsgName(finalI), "未输入指令"), text -> settings.put(getArcQuickMsgName(finalI), text)).maxTextLength(300).width(350f);
                    tt.row();
                }
            });
        });
        dialog.addCloseButton();
        dialog.show();
    }

    private String getArcQuickMsgShortName(int i){
        return "arcQuickMsgShort" + i;
    }

    private String getArcQuickMsgName(int i){
        return "arcQuickMsg" + i;
    }

    private String getArcQuickMsgJs(int i){
        return "arcQuickMsgJs" + i;
    }

    public interface StringProcessor{
        String get(int i);
    }

    public void sliderPref(String name, int min, int max, int step, StringProcessor s){
        Slider slider = new Slider(min, max, step, false);
        Label value = new Label("", Styles.outlineLabel);
        slider.update(() -> {
            slider.setValue(settings.getInt(name));
            value.setText(s.get((int)slider.getValue()));
        });
        slider.changed(() -> settings.put(name, (int)slider.getValue()));

        Table content = new Table();
        content.add(bundle.get("setting." + name + ".name"), Styles.outlineLabel).left().growX().wrap();
        content.add(value).padLeft(10f).right();
        content.margin(3f, 33f, 3f, 33f);
        content.touchable = Touchable.disabled;

        cont.stack(slider, content).width(Math.min(Core.graphics.getWidth() / 1.2f, 300f)).left().padTop(4f).get();
        cont.row();

        if(settings.getDefault(name) == null)
            Log.warn("no default value for " + name);
    }

    public void checkPref(String name){
        CheckBox box = new CheckBox(bundle.get("setting." + name + ".name"));
        box.update(() -> box.setChecked(settings.getBool(name)));
        box.changed(() -> settings.put(name, box.isChecked()));

        box.left();
        cont.add(box).left().padTop(0.5f);
        cont.row();

        if(settings.getDefault(name) == null)
            Log.warn("no default value for " + name);
    }
}
