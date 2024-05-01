package mindustryX.features.ui;

import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustryX.features.*;

import static mindustry.Vars.*;
import static mindustry.ui.Styles.*;

//moved from mindustry.arcModule.ui.OtherCoreItemDisplay
public class TeamsStatDisplay extends Table{
    private final float fontScl = 0.8f;
    private final SimpleCollapser teamsColl = new SimpleCollapser();

    private final Seq<Teams.TeamData> forceShowTeam = new Seq<>();
    public final Seq<Teams.TeamData> teams = new Seq<>();
    private final Interval timer = new Interval();
    private boolean showStat = true, showItem = true, showUnit = true;


    public TeamsStatDisplay(){
        table(buttons -> {
            buttons.button("[red]+", flatTogglet, teamsColl::toggle).update(t -> {
                t.setChecked(false);
                t.setText(teamsColl.getCollapsed() ? "[red]+" : "[red]×");
            }).size(40).row();
            buttons.collapser(t -> {
                t.button("T", flatTogglet, () -> UIExt.teamSelect.select(team -> teams.contains(team.data()), team -> {
                    if(forceShowTeam.contains(team.data())) forceShowTeam.remove(team.data());
                    else forceShowTeam.add(team.data());
                    teamsRebuild();
                })).checked(gg -> false).size(40).row();
                t.button(Blocks.worldProcessor.emoji(), flatTogglet, () -> {
                    showStat = !showStat;
                    teamsRebuild();
                }).checked(a -> showStat).size(40).row();
                t.button(content.items().get(0).emoji(), flatTogglet, () -> {
                    showItem = !showItem;
                    teamsRebuild();
                }).checked(a -> showItem).size(40).row();
                t.button(UnitTypes.mono.emoji(), flatTogglet, () -> {
                    showUnit = !showUnit;
                    teamsRebuild();
                }).checked(a -> showUnit).size(40).row();
            }, () -> !teamsColl.getCollapsed());
        });
        add(teamsColl).touchable(Touchable.disabled);
        var teamsTable = teamsColl.getTable();
        teamsTable.background(black6);
        teamsTable.update(() -> {
            if(timer.get(120f))
                teamsRebuild();
        });

        Events.on(EventType.ResetEvent.class, e -> {
            forceShowTeam.clear();
            teams.clear();
            teamsTable.clearChildren();
        });
    }

    private void teamsRebuild(){
        teams.clear();
        teams.addAll(Vars.state.teams.getActive());
        if(state.rules.waveTimer) teams.addUnique(state.rules.waveTeam.data());
        forceShowTeam.each(teams::addUnique);
        teams.sort(teamData -> -teamData.cores.size);

        var teamsTable = teamsColl.getTable();
        teamsTable.clear();

        //name + cores + units
        addTeamData(teamsTable, Icon.players.getRegion(), team -> team.team.id < 6 ? team.team.localized() : String.valueOf(team.team.id));
        addTeamData(teamsTable, Blocks.coreNucleus.uiIcon, team -> UI.formatAmount(team.cores.size));
        addTeamData(teamsTable, UnitTypes.mono.uiIcon, team -> UI.formatAmount(team.units.size));
        addTeamData(teamsTable, UnitTypes.gamma.uiIcon, team -> String.valueOf(team.players.size));

        if(showStat){
            teamsTable.image().color(Pal.accent).fillX().height(1).colspan(999).padTop(3).padBottom(3).row();
            addTeamDataCheckB(teamsTable, Blocks.siliconSmelter.uiIcon, team -> team.team.rules().cheat);
            addTeamDataCheck(teamsTable, Blocks.arc.uiIcon, team -> state.rules.blockDamage(team.team));
            addTeamDataCheck(teamsTable, Blocks.titaniumWall.uiIcon, team -> state.rules.blockHealth(team.team));
            addTeamDataCheck(teamsTable, Blocks.buildTower.uiIcon, team -> state.rules.buildSpeed(team.team));
            addTeamDataCheck(teamsTable, UnitTypes.corvus.uiIcon, team -> state.rules.unitDamage(team.team));
            addTeamDataCheck(teamsTable, UnitTypes.oct.uiIcon, team -> state.rules.unitHealth(team.team));
            addTeamDataCheck(teamsTable, UnitTypes.zenith.uiIcon, team -> state.rules.unitCrashDamage(team.team));
            addTeamDataCheck(teamsTable, Blocks.tetrativeReconstructor.uiIcon, team -> state.rules.unitBuildSpeed(team.team));
            addTeamDataCheck(teamsTable, Blocks.basicAssemblerModule.uiIcon, team -> state.rules.unitCost(team.team));
            teamsTable.row();
        }

        if(showItem){
            teamsTable.image().color(Pal.accent).fillX().height(1).colspan(999).padTop(3).padBottom(3).row();
            for(Item item : content.items()){
                boolean show = false;
                for(Teams.TeamData team : teams){
                    if(team.hasCore() && team.core().items.get(item) > 0)
                        show = true;
                }
                if(show){
                    addTeamData(teamsTable, item.uiIcon, team -> (team.hasCore() && team.core().items.get(item) > 0) ? UI.formatAmount(team.core().items.get(item)) : "-");
                }
            }
        }

        if(showUnit){
            teamsTable.image().color(Pal.accent).fillX().height(1).colspan(999).padTop(3).padBottom(3).row();
            for(UnitType unit : content.units()){
                boolean show = false;
                for(Teams.TeamData team : teams){
                    if(team.countType(unit) > 0)
                        show = true;
                }
                if(show){
                    addTeamData(teamsTable, unit.uiIcon, team -> team.countType(unit) > 0 ? String.valueOf(team.countType(unit)) : "-");
                }
            }
        }
    }

    private void addTeamDataCheck(Table table, TextureRegion icon, Floatf<Teams.TeamData> checked){
        if(teams.isEmpty() || teams.allMatch(it -> checked.get(it) == 1f)) return;
        //check allSame
        float value = checked.get(teams.get(0));
        if(teams.allMatch(it -> checked.get(it) == value)){
            addTeamData(table, icon, FormatDefault.format(value));
            return;
        }
        addTeamData(table, icon, team -> FormatDefault.format(checked.get(team)));
    }

    private void addTeamDataCheckB(Table table, TextureRegion icon, Boolf<Teams.TeamData> checked){
        if(teams.isEmpty() || teams.allMatch(it -> !checked.get(it))) return;
        //check allSame
        boolean value = checked.get(teams.get(0));
        if(teams.allMatch(it -> checked.get(it) == value)){
            addTeamData(table, icon, value ? "+" : "x");
            return;
        }
        addTeamData(table, icon, team -> checked.get(team) ? "+" : "×");
    }

    private void addTeamData(Table table, TextureRegion icon, String value){
        // 只显示一个数值
        table.image(icon).size(15, 15).left();
        table.label(() -> "[#" + Pal.accent + "]" + value).align(Align.center).fontScale(fontScl).colspan(table.getColumns() - 1);
        table.row();
    }

    private void addTeamData(Table table, TextureRegion icon, Func<Teams.TeamData,String> teamFunc){
        // 通用情况
        table.image(icon).size(15, 15).left();
        for(Teams.TeamData teamData : teams){
            table.label(() -> "[#" + teamData.team.color + "]" + teamFunc.get(teamData)).fontScale(fontScl);
        }
        table.row();
    }
}