package com.khillynn;

import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BaddaBoomRoulette extends JavaPlugin implements Listener {
    final String world = "Witness_Me";
    final int plateRadius = 8, plateAmt = 10, plateXZsTotal = (plateAmt * 2);
    boolean hasPlayed = false;
    final ArrayList<Location> arenaLocations = new ArrayList<>();
    final ArrayList<Location> plateLocations = new ArrayList<>();
    final ArrayList<DyeColor> woolColors = new ArrayList<>();
    final ArrayList<org.bukkit.block.Block> origBlocks = new ArrayList<>();
    final ArrayList<Material> origMats = new ArrayList<>();
    final List<Double> createXZs = new ArrayList<>();


    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("BaddaBoomRoulette is Enabled! =D");

        final Location spawnPoint = Bukkit.getWorld(world).getSpawnLocation();

        woolColors.add(DyeColor.PINK);
        woolColors.add(DyeColor.RED);
        woolColors.add(DyeColor.ORANGE);
        woolColors.add(DyeColor.GREEN);
        woolColors.add(DyeColor.LIGHT_BLUE);
        woolColors.add(DyeColor.BLUE);
        woolColors.add(DyeColor.PURPLE);

        for (int theX = (0 - plateRadius); theX <= plateRadius; theX++) {
            for (int theZ = (0 - plateRadius); theZ <= plateRadius; theZ++) {
                arenaLocations.add(new Location(spawnPoint.getWorld(), spawnPoint.getX() + theX, spawnPoint.getY(), spawnPoint.getZ() + theZ));
            }
        }

        createPlates();
    }

    @EventHandler
    public void plateActivated (PlayerInteractEvent e) { //this needs to be fixed so it is only activated once
        if((e.getAction().equals(Action.PHYSICAL) && e.getClickedBlock().getType() == Material.GOLD_PLATE))
        {
            final Location spawnPoint = Bukkit.getWorld(world).getSpawnLocation();
            Location thePlate = new Location(Bukkit.getServer().getWorld(world), e.getClickedBlock().getX(), e.getClickedBlock().getY(), e.getClickedBlock().getZ());

            removePlate(thePlate);
            int shinyAndChrome = new Random().nextInt(100) + 1; /*provides a random chance for the player to receive a diamond, have a firework
                                                                go off, or to have an explosion created and to lose their inventory*/
            final Player player = e.getPlayer();
            final Location loc = player.getLocation();
            final Inventory inventory = player.getInventory();

            //these three if and else-if statements determine what the player is rewarded with when they push a pressure plate
            if (shinyAndChrome >= 1 && shinyAndChrome <= 20) {
                Firework fw = (Firework) player.getWorld().spawnEntity(loc, EntityType.FIREWORK);
                FireworkMeta fm = fw.getFireworkMeta();
                fm.addEffect(FireworkEffect.builder().flicker(true).withColor(Color.YELLOW).withFade(Color.RED).with(FireworkEffect.Type.BALL_LARGE).trail(true).build());
                fm.setPower(1);
                fw.setFireworkMeta(fm);
                player.sendMessage("\"I live, I die. I LIVE AGAIN!\" ~ " + e.getPlayer().getName());
            } else if (shinyAndChrome > 20 && shinyAndChrome <= 70 && !e.getPlayer().isDead()) {
                inventory.addItem(new ItemStack(Material.DIAMOND, 1));
            } else if (shinyAndChrome > 70 && shinyAndChrome <= 100) {
                Bukkit.getServer().getWorld(world).createExplosion(loc.getX(), loc.getY(), loc.getZ(), 0.0F, false, false);
                inventory.clear();
                player.teleport(spawnPoint);
                createPlates();
            }
        }
    }

    //removes an individual plate if the player's plate didn't cause an explosion
    private void removePlate(Location thePlate) {
        for(int plateLocToRemove = 0; plateLocToRemove < plateLocations.size(); plateLocToRemove+=2){
            if((plateLocations.get(plateLocToRemove).getX() == thePlate.getX()) && (plateLocations.get(plateLocToRemove).getZ() == thePlate.getZ())) {
                //thePlate.getBlock().setType(Material.AIR);
                thePlate.getBlock().setType(origMats.get(plateLocToRemove));
                thePlate.getBlock().getRelative(BlockFace.DOWN).setType(origMats.get(plateLocToRemove + 1));
            }
        }
    }

    //create the pressure plates/colored wool
    private void createPlates() {
        BukkitScheduler forGlory = Bukkit.getServer().getScheduler();

        forGlory.scheduleSyncDelayedTask(this, new Runnable() {
            public void run() {
                final Location spawnPoint = Bukkit.getWorld(world).getSpawnLocation();

                if (hasPlayed)
                    resetBlocks();

                //gets 10 random locations in the arena
                for(int locLoop = 1; locLoop <= plateAmt; locLoop++){
                    int randomX = new Random().nextInt(arenaLocations.size());
                    int randomZ = new Random().nextInt(arenaLocations.size());
                    if((randomX == spawnPoint.getBlockX()) && (randomZ == spawnPoint.getBlockZ())) {
                        locLoop--;
                        continue;
                    }
                    double xCreate = arenaLocations.get(randomX).getX();
                    double zCreate = arenaLocations.get(randomZ).getZ();

                    if(locLoop == 1)
                        createXZs.clear();

                    createXZs.add(xCreate);
                    createXZs.add(zCreate);
                }

                //gets all the locations of the blocks being changed
                for(int plateNum = 0; plateNum < plateXZsTotal; plateNum+=2){
                    if(plateNum == 0)
                        plateLocations.clear();

                    plateLocations.add(new Location(getServer().getWorld(world), createXZs.get(plateNum), 64, createXZs.get(plateNum + 1)));
                    plateLocations.add(new Location(getServer().getWorld(world), createXZs.get(plateNum), 63, createXZs.get(plateNum + 1)));
                }

                //gets the blocks being changed (to get their original material and to change them later)
                for(int eachBlock = 0; eachBlock < plateXZsTotal; eachBlock++){
                    if(eachBlock == 0)
                        origBlocks.clear();

                    origBlocks.add(Bukkit.getServer().getWorld(world).getBlockAt(plateLocations.get(eachBlock)));
                }

                //gets the original material type of the blocks
                for(int eachMat = 0; eachMat < plateXZsTotal; eachMat++){
                    if(eachMat == 0)
                        origMats.clear();

                    origMats.add(origBlocks.get(eachMat).getType());
                }

                //changes all of the locations and the block below them to a pressure plate and a colored wool
                for(int changedBlock = 0; changedBlock < plateXZsTotal; changedBlock+=2){
                    int randColor = new Random().nextInt(woolColors.size());

                    origBlocks.get(changedBlock).setType(Material.GOLD_PLATE);
                    origBlocks.get(changedBlock + 1).setType(Material.WOOL);
                    origBlocks.get(changedBlock + 1).setData(woolColors.get(randColor).getWoolData());

                    if(!hasPlayed)
                        hasPlayed = true;
                }
            }
        }, 40L);
    }

    private void resetBlocks() {
        for (int changeToOrig = 0; changeToOrig < plateXZsTotal; changeToOrig += 2) {
            origBlocks.get(changeToOrig).setType(origMats.get(changeToOrig));
            origBlocks.get(changeToOrig + 1).setType(origMats.get(changeToOrig + 1));
        }
    }

    @EventHandler
    public  void playerJoin (PlayerJoinEvent e){
        final Location spawnPoint = Bukkit.getWorld(world).getSpawnLocation();
        e.getPlayer().teleport(spawnPoint);
    }

    @EventHandler
    public void playerLeave (PlayerQuitEvent e){
        e.getPlayer().getInventory().clear();
    }

    @EventHandler
    public void playerRespawn (PlayerRespawnEvent e){
        e.getPlayer().getInventory().clear();
    }

    @EventHandler
    public void itemDrop (PlayerDropItemEvent e){
        e.setCancelled(true);
    }

    @EventHandler
    public void playerDied (PlayerDeathEvent e){
        e.getDrops().clear();
    }

    //prevents blocks from breaking
    @EventHandler
    public void blockBreak (BlockBreakEvent e){
        e.setCancelled(true);
    }

    //prevents players from regaining health
    @EventHandler
    public void playerHPRegen (EntityRegainHealthEvent e){
        e.setCancelled(true);
    }

    //prevents players from becoming hungry
    @EventHandler
    public void playerHungerStop (FoodLevelChangeEvent e){
        e.setCancelled(true);
    }

    //prevents players from picking up items
    @EventHandler
    public void itemPickUp (PlayerPickupItemEvent e){
        e.setCancelled(true);
    }

    @Override
    public void onDisable() {
        resetBlocks();
    }
}
