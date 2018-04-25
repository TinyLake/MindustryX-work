package io.anuke.mindustry.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.Pools;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.SyncEntity;
import io.anuke.mindustry.entities.effect.BelowLiquidEffect;
import io.anuke.mindustry.entities.effect.GroundEffectEntity;
import io.anuke.mindustry.entities.effect.GroundEffectEntity.GroundEffect;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.graphics.BlockRenderer;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.graphics.MinimapRenderer;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.input.InputHandler;
import io.anuke.mindustry.input.PlaceMode;
import io.anuke.mindustry.ui.fragments.ToolFragment;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.BlockBar;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.*;
import io.anuke.ucore.entities.EffectEntity;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.function.Callable;
import io.anuke.ucore.graphics.*;
import io.anuke.ucore.modules.RendererModule;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Tmp;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.ucore.core.Core.batch;
import static io.anuke.ucore.core.Core.camera;

public class Renderer extends RendererModule{
	private final static float shieldHitDuration = 18f;
	
	public Surface shadowSurface, shieldSurface, waterSurface;
	
	private int targetscale = baseCameraScale;
	private Texture background = new Texture("sprites/background.png");
	private FloatArray shieldHits = new FloatArray();
	private Array<Callable> shieldDraws = new Array<>();
	private Rectangle rect = new Rectangle(), rect2 = new Rectangle();
	private BlockRenderer blocks = new BlockRenderer();
	private MinimapRenderer minimap = new MinimapRenderer();

	public Renderer() {
		Lines.setCircleVertices(14);

		Core.cameraScale = baseCameraScale;
		Effects.setEffectProvider((effect, color, x, y, rotation, data) -> {
			if(effect == Fx.none) return;
			if(Settings.getBool("effects")){
				Rectangle view = rect.setSize(camera.viewportWidth, camera.viewportHeight)
						.setCenter(camera.position.x, camera.position.y);
				Rectangle pos = rect2.setSize(effect.size).setCenter(x, y);

				if(view.overlaps(pos)){
					int id = 0;

					if(!(effect instanceof GroundEffect) || ((GroundEffect)effect).isStatic) {
						EffectEntity entity = Pools.obtain(EffectEntity.class);
						entity.effect = effect;
						entity.color = color;
						entity.rotation = rotation;
						entity.lifetime = effect.lifetime;
						id = entity.set(x, y).add(effectGroup).id;

						if(data instanceof Entity){
							entity.setParent((Entity)data);
						}
					}

					if(effect instanceof GroundEffect){
						GroundEffectEntity entity = Pools.obtain(GroundEffectEntity.class);
						entity.effect = effect;
						entity.color = color;
						entity.rotation = rotation;
						entity.lifetime = effect.lifetime;
						entity.set(x, y).add(groundEffectGroup);

						if(((GroundEffect)effect).isStatic){
							entity.id = id;
						}
					}
				}
			}
		});

		Cursors.cursorScaling = 3;
		Cursors.outlineColor = Color.valueOf("444444");
		Cursors.arrow = Cursors.loadCursor("cursor");
		Cursors.hand = Cursors.loadCursor("hand");
		Cursors.ibeam = Cursors.loadCursor("ibar");

		clearColor = Hue.lightness(0.4f);
		clearColor.a = 1f;

		background.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);

		for(Block block : Block.getAllBlocks()){
			block.load();
		}
	}

	@Override
	public void init(){
		pixelate = Settings.getBool("pixelate");
		int scale = Settings.getBool("pixelate") ? Core.cameraScale : 1;
		
		shadowSurface = Graphics.createSurface(scale);
		shieldSurface = Graphics.createSurface(scale);
		pixelSurface = Graphics.createSurface(scale);
		waterSurface = Graphics.createSurface(scale);
	}

	public void setPixelate(boolean pixelate){
		this.pixelate = pixelate;
	}

	@Override
	public void update(){

		if(Core.cameraScale != targetscale){
			float targetzoom = (float) Core.cameraScale / targetscale;
			camera.zoom = Mathf.lerpDelta(camera.zoom, targetzoom, 0.2f);

			if(Mathf.in(camera.zoom, targetzoom, 0.005f)){
				camera.zoom = 1f;
				Graphics.setCameraScale(targetscale);
				control.input().resetCursor();
			}
		}else{
			camera.zoom = Mathf.lerpDelta(camera.zoom, 1f, 0.2f);
		}

		if(state.is(State.menu)){
			Graphics.clear(Color.BLACK);
		}else{
			boolean smoothcam = Settings.getBool("smoothcam");


			if(!smoothcam){
				setCamera(player.x, player.y);
			}else{
				smoothCamera(player.x, player.y, android ? 0.3f : 0.14f);
			}

			if(Settings.getBool("pixelate"))
				limitCamera(4f, player.x, player.y);

			float prex = camera.position.x, prey = camera.position.y;
			updateShake(0.75f);
			float prevx = camera.position.x, prevy = camera.position.y;
			clampCamera(-tilesize / 2f, -tilesize / 2f + 1, world.width() * tilesize - tilesize / 2f, world.height() * tilesize - tilesize / 2f);

			float deltax = camera.position.x - prex, deltay = camera.position.y - prey;

			if(android){
				player.x += camera.position.x - prevx;
				player.y += camera.position.y - prevy;
			}

			float lastx = camera.position.x, lasty = camera.position.y;
			
			if(snapCamera && smoothcam && Settings.getBool("pixelate")){
				camera.position.set((int) camera.position.x, (int) camera.position.y, 0);
			}
			
			if(Gdx.graphics.getHeight() / Core.cameraScale % 2 == 1){
				camera.position.add(0, -0.5f, 0);
			}

			if(Gdx.graphics.getWidth() / Core.cameraScale % 2 == 1){
				camera.position.add(-0.5f, 0, 0);
			}
			
			draw();

			camera.position.set(lastx - deltax, lasty - deltay, 0);
		}
	}

	@Override
	public void draw(){
		camera.update();

		Graphics.clear(clearColor);
		
		batch.setProjectionMatrix(camera.combined);
		
		if(pixelate) 
			Graphics.surface(pixelSurface, false);
		else
			batch.begin();

		drawPadding();
		
		blocks.drawFloor();

		Entities.draw(groundEffectGroup, e -> e instanceof BelowLiquidEffect);
		Entities.draw(puddleGroup);
		Entities.draw(groundEffectGroup, e -> !(e instanceof BelowLiquidEffect));

		blocks.processBlocks();
		blocks.drawBlocks(Layer.overlay);

		drawAllTeams(false);

		blocks.skipLayer(Layer.turret);
		blocks.drawBlocks(Layer.laser);

		drawAllTeams(true);

		Entities.draw(bulletGroup);
		Entities.draw(airItemGroup);
        Entities.draw(effectGroup);

		//drawShield();

		drawOverlay();

		if(pixelate)
			Graphics.flushSurface();

		drawPlayerNames();
		
		batch.end();
	}

	private void drawAllTeams(boolean flying){
		for(Team team : Team.values()){
			EntityGroup<BaseUnit> group = unitGroups[team.ordinal()];
			if(group.count(p -> p.isFlying() == flying) +
					playerGroup.count(p -> p.isFlying() == flying && p.team == team) == 0 && flying) continue;

			Shaders.outline.color.set(team.color);

			Graphics.beginShaders(Shaders.outline);
			Graphics.shader(Shaders.hit, false);
			drawTeam(team, flying);
			Graphics.shader();
			blocks.drawTeamBlocks(Layer.turret, team);
			Graphics.endShaders();
		}
	}

	private void drawTeam(Team team, boolean flying){
		Entities.draw(unitGroups[team.ordinal()], u -> u.isFlying() == flying);
		Entities.draw(playerGroup, p -> p.isFlying() == flying && p.team == team);
	}

	@Override
	public void resize(int width, int height){
		super.resize(width, height);
		control.input().resetCursor();
		camera.position.set(player.x, player.y, 0);
	}

	@Override
	public void dispose() {
		background.dispose();
	}

	public MinimapRenderer minimap() {
		return minimap;
	}

	void drawPadding(){
		float vw = world.width() * tilesize;
		float cw = camera.viewportWidth * camera.zoom;
		float ch = camera.viewportHeight * camera.zoom;
		if(vw < cw){
			batch.draw(background,
					camera.position.x + vw/2,
					Mathf.round(camera.position.y - ch/2, tilesize),
					(cw - vw) /2,
					ch + tilesize,
					0, 0,
					((cw - vw) / 2 / tilesize), -ch / tilesize + 1);

			batch.draw(background,
					camera.position.x - vw/2,
					Mathf.round(camera.position.y - ch/2, tilesize),
					-(cw - vw) /2,
					ch + tilesize,
					0, 0,
					-((cw - vw) / 2 / tilesize), -ch / tilesize + 1);
		}
	}

	void drawPlayerNames(){
		GlyphLayout layout = Pools.obtain(GlyphLayout.class);

        Draw.tscl(0.25f/2);
	    for(Player player : playerGroup.all()){
	       if(!player.isLocal && !player.isDead()){
	        	layout.setText(Core.font, player.name);
				Draw.color(0f, 0f, 0f, 0.3f);
				Draw.rect("blank", player.x, player.y + 8 - layout.height/2, layout.width + 2, layout.height + 2);
				Draw.color();
				Draw.tcolor(player.getColor());
	            Draw.text(player.name, player.x, player.y + 8);

	            if(player.isAdmin){
	            	Draw.color(player.getColor());
	            	float s = 3f;
					Draw.rect("icon-admin-small", player.x + layout.width/2f + 2 + 1, player.y + 7f, s, s);
				}
				Draw.reset();
           }
        }
		Pools.free(layout);
        Draw.tscl(fontscale);
    }

	void drawShield(){
		if(shieldGroup.size() == 0 && shieldDraws.size == 0) return;
		
		Graphics.surface(renderer.shieldSurface, false);
		Draw.color(Color.ROYAL);
		Entities.draw(shieldGroup);
		for(Callable c : shieldDraws){
			c.run();
		}
		Draw.reset();
		Graphics.surface();
		
		for(int i = 0; i < shieldHits.size / 3; i++){
			float time = shieldHits.get(i * 3 + 2);

			time += Timers.delta() / shieldHitDuration;
			shieldHits.set(i * 3 + 2, time);

			if(time >= 1f){
				shieldHits.removeRange(i * 3, i * 3 + 2);
				i--;
			}
		}

		Texture texture = shieldSurface.texture();
		Shaders.shield.color.set(Color.SKY);

		Tmp.tr2.setRegion(texture);
		Shaders.shield.region = Tmp.tr2;
		Shaders.shield.hits = shieldHits;
		
		if(Shaders.shield.isFallback){
			Draw.color(1f, 1f, 1f, 0.3f);
			Shaders.outline.color = Color.SKY;
			Shaders.outline.region = Tmp.tr2;
		}

		Graphics.end();
		Graphics.shader(Shaders.shield.isFallback ? Shaders.outline : Shaders.shield);
		Graphics.setScreen();

		Core.batch.draw(texture, 0, Gdx.graphics.getHeight(), Gdx.graphics.getWidth(), -Gdx.graphics.getHeight());

		Graphics.shader();
		Graphics.end();
		Graphics.beginCam();
		
		Draw.color();
		shieldDraws.clear();
	}

	public BlockRenderer getBlocks() {
		return blocks;
	}

	public void addShieldHit(float x, float y){
		shieldHits.addAll(x, y, 0f);
	}

	public void addShield(Callable call){
		shieldDraws.add(call);
	}

	void drawOverlay(){

		//draw tutorial placement point
		if(world.getMap().name.equals("tutorial") && control.tutorial().showBlock()){
			//TODO draw placement point for tutorial
			/*
			int x = world.getCore().x + control.tutorial().getPlacePoint().x;
			int y = world.getCore().y + control.tutorial().getPlacePoint().y;
			int rot = control.tutorial().getPlaceRotation();

			Lines.stroke(1f);
			Draw.color(Color.YELLOW);
			Lines.square(x * tilesize, y * tilesize, tilesize / 2f + Mathf.sin(Timers.time(), 4f, 1f));

			Draw.color(Color.ORANGE);
			Lines.stroke(2f);
			if(rot != -1){
				Lines.lineAngle(x * tilesize, y * tilesize, rot * 90, 6);
			}
			Draw.reset();*/
		}

		//draw config selected block
		if(ui.configfrag.isShown()){
			Tile tile = ui.configfrag.getSelectedTile();
			tile.block().drawConfigure(tile);
		}
		
		int tilex = control.input().getBlockX();
		int tiley = control.input().getBlockY();
		
		if(android){
			Vector2 vec = Graphics.world(Gdx.input.getX(0), Gdx.input.getY(0));
			tilex = Mathf.scl2(vec.x, tilesize);
			tiley = Mathf.scl2(vec.y, tilesize);
		}

		InputHandler input = control.input();

		//draw placement box
		if((input.recipe != null && state.inventory.hasItems(input.recipe.requirements) && (!ui.hasMouse() || android)
				&& control.input().drawPlace())){

			input.placeMode.draw(control.input().getBlockX(), control.input().getBlockY(),
					control.input().getBlockEndX(), control.input().getBlockEndY());
			
			if(input.breakMode == PlaceMode.holdDelete)
				input.breakMode.draw(tilex, tiley, 0, 0);
			
		}else if(input.breakMode.delete && control.input().drawPlace()
				&& (input.recipe == null || !state.inventory.hasItems(input.recipe.requirements))
				&& (input.placeMode.delete || input.breakMode.both || !android)){

            if(input.breakMode == PlaceMode.holdDelete)
                input.breakMode.draw(tilex, tiley, 0, 0);
            else
				input.breakMode.draw(control.input().getBlockX(), control.input().getBlockY(),
						control.input().getBlockEndX(), control.input().getBlockEndY());
		}

		if(ui.toolfrag.confirming){
			ToolFragment t = ui.toolfrag;
			PlaceMode.areaDelete.draw(t.px, t.py, t.px2, t.py2);
		}
		
		Draw.reset();

		//draw selected block bars and info
		if(input.recipe == null && !ui.hasMouse() && !ui.configfrag.isShown()){
			Tile tile = world.tileWorld(Graphics.mouseWorld().x, Graphics.mouseWorld().y);

			if(tile != null && tile.block() != Blocks.air){
				Tile target = tile;
				if(tile.isLinked())
					target = tile.getLinked();

				if(showBlockDebug && target.entity != null){
					Draw.color(Color.RED);
					Lines.crect(target.drawx(), target.drawy(), target.block().size * tilesize, target.block().size * tilesize);
					Vector2 v = new Vector2();



					Draw.tcolor(Color.YELLOW);
					Draw.tscl(0.25f);
					Array<Object> arr = target.block().getDebugInfo(target);
					StringBuilder result = new StringBuilder();
					for(int i = 0; i < arr.size/2; i ++){
						result.append(arr.get(i*2));
						result.append(": ");
						result.append(arr.get(i*2 + 1));
						result.append("\n");
					}
					Draw.textc(result.toString(), target.drawx(), target.drawy(), v);
					Draw.color(0f, 0f, 0f, 0.5f);
					Fill.rect(target.drawx(), target.drawy(), v.x, v.y);
					Draw.textc(result.toString(), target.drawx(), target.drawy(), v);
					Draw.tscl(fontscale);
					Draw.reset();
				}

				if(Inputs.keyDown("block_info") && target.block().isAccessible()){
					Draw.color(Colors.get("accent"));
					Lines.crect(target.drawx(), target.drawy(), target.block().size * tilesize, target.block().size * tilesize);
					Draw.color();
				}

				if(target.entity != null) {
					int bot = 0, top = 0;
					for (BlockBar bar : target.block().bars.list()) {
						float offset = Mathf.sign(bar.top) * (target.block().size / 2f * tilesize + 3f + 4f * ((bar.top ? top : bot))) +
								(bar.top ? -1f : 0f);

						float value = bar.value.get(target);

						if(MathUtils.isEqual(value, -1f)) continue;

						drawBar(bar.type.color, target.drawx(), target.drawy() + offset, value);

						if (bar.top)
							top++;
						else
							bot++;
					}
				}

				target.block().drawSelect(target);
			}
		}

		if(control.input().isDroppingItem()){
			Vector2 v = Graphics.mouseWorld();
			float size = 8;
			Draw.rect(player.inventory.getItem().item.region, v.x, v.y, size, size);
			Draw.color("accent");
			Lines.circle(v.x, v.y, 6 + Mathf.absin(Timers.time(), 5f, 1f));
			Draw.reset();

			Tile tile = world.tileWorld(v.x, v.y);
			if(tile != null) tile = tile.target();
			if(tile != null && tile.block().acceptStack(player.inventory.getItem().item, player.inventory.getItem().amount, tile, player) > 0){
				Draw.color("place");
				Lines.square(tile.drawx(), tile.drawy(), tile.block().size*tilesize/2f + 1 + Mathf.absin(Timers.time(), 5f, 1f));
				Draw.color();
			}
		}

		//TODO draw health bars

		/*
		if((!debug || showUI) && Settings.getBool("healthbars")){

			//draw entity health bars
			for(BaseUnit entity : enemyGroup.all()){
				drawHealth(entity);
			}

			for(Player player : playerGroup.all()){
				if(!player.isDead()) drawHealth(player);
			}
		}*/
	}

	void drawHealth(SyncEntity dest){
		float x = dest.getDrawPosition().x;
		float y = dest.getDrawPosition().y;
		if(dest instanceof Player && snapCamera && Settings.getBool("smoothcam") && Settings.getBool("pixelate")){
			drawHealth((int) x, (int) y - 7f, dest.health, dest.maxhealth);
		}else{
			drawHealth(x, y - 7f, dest.health, dest.maxhealth);
		}
	}

	void drawHealth(float x, float y, float health, float maxhealth){
		drawBar(Color.RED, x, y, health / maxhealth);
	}
	
	//TODO optimize!
	public void drawBar(Color color, float x, float y, float finion){
		finion = Mathf.clamp(finion);

		if(finion > 0) finion = Mathf.clamp(finion + 0.2f, 0.24f, 1f);

		float len = 3;

		float w = (int) (len * 2 * finion) + 0.5f;

		x -= 0.5f;
		y += 0.5f;

		Lines.stroke(3f);
		Draw.color(Color.SLATE);
		Lines.line(x - len + 1, y, x + len + 1.5f, y);
		Lines.stroke(1f);
		Draw.color(Color.BLACK);
		Lines.line(x - len + 1, y, x + len + 0.5f, y);
		Draw.color(color);
		if(w >= 1)
			Lines.line(x - len + 1, y, x - len + w, y);
		Draw.reset();
	}

	public void setCameraScale(int amount){
		targetscale = amount;
		clampScale();
		//scale up all surfaces in preparation for the zoom
		if(Settings.getBool("pixelate")){
			for(Surface surface : Graphics.getSurfaces()){
				surface.setScale(targetscale);
			}
		}
	}

	public void scaleCamera(int amount){
		setCameraScale(targetscale + amount);
	}

	public void clampScale(){
		targetscale = Mathf.clamp(targetscale, Math.round(Unit.dp.scl(2)), Math.round(Unit.dp.scl((5))));
	}

}
