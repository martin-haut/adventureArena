package adventureArena.tasks;

import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import adventureArena.TerrainHelper;
import adventureArena.miniGameComponents.MiniGame;


public class PeriodicEntityTask extends AbstractPeriodicTask {

	private final World			world;
	private final Location		airBlockAboveAttachedBlockTelePos;
	private final EntityType	entityType;
	private final double		hp;
	private final float			explosionPower;
	private final double		lifeTime;
	private final MiniGame		miniGame;
	private final List<Integer>	runningTasks;



	public PeriodicEntityTask(World w, Vector attachedBlockPosition, EntityType entityType, double hp, float explosionPower, double lifeTime, MiniGame mg, List<Integer> runningTasks) {
		super();
		world = w;
		airBlockAboveAttachedBlockTelePos = TerrainHelper.getAirBlockAboveGroundTelePos(attachedBlockPosition.toLocation(w), true, mg);
		this.entityType = entityType;
		this.hp = hp;
		this.explosionPower = explosionPower;
		this.lifeTime = lifeTime;
		miniGame = mg;
		this.runningTasks = runningTasks;
	}

	@Override
	public void tick() {
		final Entity entity = world.spawnEntity(airBlockAboveAttachedBlockTelePos, entityType);
		if (entity instanceof Creature && hp > 0) {
			Creature creature = (Creature) entity;
			creature.setMaxHealth(hp);
			creature.setHealth(hp);
			creature.setTarget(miniGame.getRandomPlayer());
			if (explosionPower > 0f) {
				EntityEquipment ee = creature.getEquipment();
				ee.setHelmet(new ItemStack(Material.PUMPKIN));
			}
		}
		if (lifeTime > 0) {
			entity.setCustomNameVisible(true);
			AbstractPeriodicTask explosionTimerTask = new AbstractPeriodicTask() {

				@Override
				public void tick() {
					if (explosionPower > 0f) {
						ChatColor cc = getNumberOfExecutionsLeft() < 3 ? ChatColor.RED : ChatColor.BLUE;
						entity.setCustomName(cc.toString() + getNumberOfExecutionsLeft());
						//AA_MessageSystem.consoleDebug("EXPLODE: " + cc.toString() + getNumberOfExecutionsLeft());
					}
				}

				@Override
				public void lastTick() {
					if (entity.isValid()) {
						if (explosionPower > 0f) {
							entity.setCustomName("Exploding " + entity.getName());
							entity.getWorld().createExplosion(entity.getLocation(), explosionPower);
						}
						entity.remove();
					}
				}
			};

			explosionTimerTask.schedule(0, 1, (int) Math.round(lifeTime) + 1);
			runningTasks.add(explosionTimerTask.getTaskId());
		}
	}

	@Override
	public void lastTick() {
		tick();
	}
}
