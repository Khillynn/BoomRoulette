package com.khillynn;

import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BaddaBoomRoulette extends JavaPlugin implements Listener, PluginMessageListener{
    final String world = "Witness_Me";
    final int plateRadius = 8, plateAmt = 10, plateXZsTotal = (plateAmt * 2);
    int platesOutOfPlay = 0, CntDownTaskID;
    boolean hasPlayed = false;
    final ArrayList<Location> arenaLocations = new ArrayList<>();
    final ArrayList<Location> plateLocations = new ArrayList<>();
    final ArrayList<DyeColor> woolColors = new ArrayList<>();
    final ArrayList<org.bukkit.block.Block> origBlocks = new ArrayList<>();
    final ArrayList<Material> origMats = new ArrayList<>();
    final List<Double> createXZs = new ArrayList<>();
    final BukkitScheduler roundStartCounter = Bukkit.getServer().getScheduler();

    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("BaddaBoomRoulette is Enabled! =D");


        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        Bukkit.getMessenger().registerIncomingPluginChannel(this, "BugeeCord", this);

        final Location spawnPoint = Bukkit.getWorld(world).getSpawnLocation();

        woolColors.add(DyeColor.PINK);
        woolColors.add(DyeColor.RED);
        //woolColors.add(DyeColor.ORANGE);
        woolColors.add(DyeColor.GREEN);
        woolColors.add(DyeColor.LIGHT_BLUE);
        woolColors.add(DyeColor.BLUE);
        woolColors.add(DyeColor.PURPLE);

        for (int theX = (0 - plateRadius); theX <= plateRadius; theX++) {
            for (int theZ = (0 - plateRadius); theZ <= plateRadius; theZ++) {
                arenaLocations.add(new Location(spawnPoint.getWorld(), spawnPoint.getX() + theX, spawnPoint.getY(), spawnPoint.getZ() + theZ));
            }
        }
    }

    private void checkPlayersAndStart() {
        final Location spawnPoint = Bukkit.getWorld(world).getSpawnLocation();

        if (Bukkit.getServer().getOnlinePlayers().length > 1) {
            //counts down and begins the round
            CntDownTaskID = roundStartCounter.scheduleSyncRepeatingTask(this, new Runnable() {
                int seconds = 0;

                public void run() {
                    seconds++;
                    if(Bukkit.getServer().getOnlinePlayers().length == 1){
                        Bukkit.broadcastMessage("[" + ChatColor.GOLD + "Server" + ChatColor.WHITE + "]: " + ChatColor.YELLOW + "waiting for more players");
                        seconds = 0;
                        Bukkit.getServer().getScheduler().cancelTask(CntDownTaskID);
                    }
                    if (seconds == 15)
                        Bukkit.broadcastMessage("[" + ChatColor.GOLD + "Server" + ChatColor.WHITE + "]: " + ChatColor.YELLOW + "15 seconds until the round begins");
                    else if (seconds == 25) {
                        Bukkit.broadcastMessage("[" + ChatColor.GOLD + "Server" + ChatColor.WHITE + "]: " + ChatColor.YELLOW + "5");
                        Bukkit.getServer().getWorld(world).playSound(spawnPoint, Sound.CLICK, 1, 1);
                    }
                    else if (seconds == 26) {
                        Bukkit.broadcastMessage("[" + ChatColor.GOLD + "Server" + ChatColor.WHITE + "]: " + ChatColor.YELLOW + "4");
                        Bukkit.getServer().getWorld(world).playSound(spawnPoint, Sound.CLICK, 1, 1);
                    }
                    else if (seconds == 27) {
                        Bukkit.broadcastMessage("[" + ChatColor.GOLD + "Server" + ChatColor.WHITE + "]: " + ChatColor.YELLOW + "3");
                        Bukkit.getServer().getWorld(world).playSound(spawnPoint, Sound.CLICK, 1, 1);
                    }
                    else if (seconds == 28) {
                        Bukkit.broadcastMessage("[" + ChatColor.GOLD + "Server" + ChatColor.WHITE + "]: " + ChatColor.YELLOW + "2");
                        Bukkit.getServer().getWorld(world).playSound(spawnPoint, Sound.CLICK, 1, 1);
                    }
                    else if (seconds == 29) {
                        Bukkit.broadcastMessage("[" + ChatColor.GOLD + "Server" + ChatColor.WHITE + "]: " + ChatColor.YELLOW + "1");
                        Bukkit.getServer().getWorld(world).playSound(spawnPoint, Sound.CLICK, 1, 1);
                    }
                    else if (seconds == 30) {
                        Bukkit.broadcastMessage("[" + ChatColor.GOLD + "Server" + ChatColor.WHITE + "]: " + ChatColor.YELLOW + "Go forth for the Glory of ValHalla!");
                        Bukkit.getServer().getWorld(world).playSound(spawnPoint, Sound.EXPLODE, 1, 2);
                        createPlates();
                        Location spawnPoint = Bukkit.getWorld(world).getSpawnLocation();
                        Bukkit.getServer().getWorld(world).playEffect(spawnPoint, Effect.RECORD_PLAY, Material.GREEN_RECORD);
                    } else if (seconds == 90) {
                        Bukkit.getServer().getWorld(world).playEffect(spawnPoint, Effect.RECORD_PLAY, 0);
                        resetBlocks();
                        comparePlayerInv();
                    }
                    else if(seconds > 90)
                        Bukkit.getServer().getScheduler().cancelTask(CntDownTaskID);
                }
            }, 0l, 20L);
        }
    }

    //at the end of the round all players' inventories are compared to find the winner
    private void comparePlayerInv() {
        final Player[] onlinePlayers = Bukkit.getOnlinePlayers();
        ArrayList<Player> players = new ArrayList<>();
        ArrayList<Integer> diamondAmt = new ArrayList<>();
        int playerAmt = 0;

        for(Player all : onlinePlayers){
            ItemStack hand = all.getItemInHand();

            if(hand.getType() == Material.DIAMOND){
                diamondAmt.add(hand.getAmount());
            }
            else{
                diamondAmt.add(0);
            }
            players.add(all);
            playerAmt++;
        }

        int winningAmt = diamondAmt.get(0), playerTies = 0, tiedDiamondAmt = 0;
        boolean noWin = false;
        Player winner = players.get(0);

        //Collections.sort(diamondAmt);

        for(int playerNum = 1; playerNum < onlinePlayers.length; playerNum++){
            if(diamondAmt.get(playerNum) > winningAmt) {
                winningAmt = diamondAmt.get(playerNum);
                winner = players.get(playerNum);
            }
            else if(diamondAmt.get(playerNum) == winningAmt) {
                playerTies++;
                tiedDiamondAmt = diamondAmt.get(playerNum);
            }
        }

        if((playerTies == playerAmt) || (tiedDiamondAmt == winningAmt))
            noWin = true;

        if(playerAmt == 1)
            noWin = false;

        if(!noWin) {
            Firework fw = (Firework) winner.getWorld().spawnEntity(winner.getLocation(), EntityType.FIREWORK);
            FireworkMeta fm = fw.getFireworkMeta();
            fm.addEffect(FireworkEffect.builder().flicker(true).withColor(Color.PURPLE).withFade(Color.BLUE).with(FireworkEffect.Type.STAR).trail(true).build());
            fm.setPower(0);

            for (int fwNum = 1; fwNum <= 5; fwNum++) {
                fw.setFireworkMeta(fm);
            }
            Bukkit.broadcastMessage("[" + ChatColor.GOLD + "Server" + ChatColor.WHITE + "]: " + ChatColor.YELLOW + "The Winner is " + ChatColor.WHITE + winner.getName() + ChatColor.YELLOW + " with " + ChatColor.AQUA + winningAmt + " diamonds" + ChatColor.YELLOW + "!");
        }
        else
            Bukkit.broadcastMessage("[" + ChatColor.GOLD + "Server" + ChatColor.WHITE + "]: " + ChatColor.YELLOW + "The is no winner.");

        incPoints(winner, players, winningAmt);
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            public void run() {
                tpPlayersToHub(onlinePlayers);
            }
        }, 100L);
    }

    public void incPoints(Player winner, ArrayList<Player> players, int winningAmt){
        MongoDB mdb = new MongoDB(MongoDBD.username, MongoDBD.password, MongoDBD.database, MongoDBD.host, MongoDBD.port);

        for (Player all : players){
            if (winner == all){
                mdb.incUserPoints(all, 10 + winningAmt);
                System.out.println(" ++++++++++ increasing " + all.getName() + "'s points by " + (10 + winningAmt));
            }
            else {
                mdb.incUserPoints(all, 5);
                System.out.println(" ++++++++++ increasing " + all.getName() + "'s points by 5");
            }

        }

        mdb.closeConnection();
    }

    private void tpPlayersToHub(Player[] onlinePlayers) {
        for(Player all : onlinePlayers){
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            try{
                out.writeUTF("Connect");
                out.writeUTF("hub");
            } catch(Exception exception){
                exception.printStackTrace();
            }
            all.sendPluginMessage(this, "BungeeCord", b.toByteArray());
        }
    }

    @EventHandler
    public void plateActivated (final PlayerInteractEvent e) { //this needs to be fixed so it is only activated once
        BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
        final Player player = e.getPlayer();

        scheduler.scheduleSyncDelayedTask(this, new Runnable() {
            @Override
            public void run() {

                if ((e.getAction().equals(Action.PHYSICAL) && e.getClickedBlock().getType() == Material.GOLD_PLATE)) {
                    final Location thePlateLoc = new Location(Bukkit.getServer().getWorld(world), e.getClickedBlock().getX(), e.getClickedBlock().getY(), e.getClickedBlock().getZ());

                    final int shinyAndChrome = new Random().nextInt(100) + 1; /*provides a random chance for the player to receive a diamond, have a firework
                                                                go off, or to have an explosion created and to loose their inventory*/
                    final Player player = e.getPlayer();
                    final Location loc = player.getLocation();
                    final Inventory inventory = player.getInventory();

                    removePlate(thePlateLoc);
                    //these three if and else-if statements determine what the player is rewarded with when they push a pressure plate
                    if (shinyAndChrome >= 1 && shinyAndChrome <= 20) {
                        Firework fw = (Firework) player.getWorld().spawnEntity(loc, EntityType.FIREWORK);
                        FireworkMeta fm = fw.getFireworkMeta();
                        fm.addEffect(FireworkEffect.builder().flicker(true).withColor(Color.YELLOW).withFade(Color.RED).with(FireworkEffect.Type.BALL_LARGE).trail(true).build());
                        fm.setPower(0);
                        fw.setFireworkMeta(fm);
                        player.setVelocity(player.getEyeLocation().getDirection().multiply(2));
                    } else if (shinyAndChrome > 20 && shinyAndChrome <= 80) {
                        inventory.addItem(new ItemStack(Material.DIAMOND, 1));
                        player.setVelocity(player.getEyeLocation().getDirection().multiply(2));
                    } else if (shinyAndChrome > 80 && shinyAndChrome <= 100) {
                        Bukkit.getServer().getWorld(world).createExplosion(loc.getX(), loc.getY(), loc.getZ(), 0.0F, false, false);
                        inventory.clear();
                        createPlates();
                    }
                }

            }
        }, 10L);
    }

    //removes an individual plate if the player's plate didn't cause an explosion
    private void removePlate(Location thePlateLoc) {
        platesOutOfPlay++;

        for(int plateLocToRemove = 0; plateLocToRemove < plateLocations.size(); plateLocToRemove+=2){
            if((plateLocations.get(plateLocToRemove).getX() == thePlateLoc.getX()) && (plateLocations.get(plateLocToRemove).getZ() == thePlateLoc.getZ())) {
                plateLocations.get(plateLocToRemove).getBlock().setType(origMats.get(plateLocToRemove));
                plateLocations.get(plateLocToRemove + 1).getBlock().setType(origMats.get(plateLocToRemove + 1));
            }
        }
        if(platesOutOfPlay == 10) //add some sort of affect or message to let the players know they might not have been penalized (only added 10 more plates)
            createPlates();
    }

    //create the pressure plates/colored wool
    private void createPlates() {
        Location spawnPoint = Bukkit.getWorld(world).getSpawnLocation();
        platesOutOfPlay = 0;

        for(Player all : Bukkit.getOnlinePlayers()){
            all.teleport(spawnPoint);
        }

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

            plateLocations.add(new Location(getServer().getWorld(world), createXZs.get(plateNum), spawnPoint.getY(), createXZs.get(plateNum + 1)));
            plateLocations.add(new Location(getServer().getWorld(world), createXZs.get(plateNum), spawnPoint.getY() - 1, createXZs.get(plateNum + 1)));
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

    private void resetBlocks() {
        if(origBlocks.get(0) != null)
            for (int changeToOrig = 0; changeToOrig < plateXZsTotal; changeToOrig += 2) {
                origBlocks.get(changeToOrig).setType(origMats.get(changeToOrig));
                origBlocks.get(changeToOrig + 1).setType(origMats.get(changeToOrig + 1));
            }
    }

    @EventHandler
    public  void playerJoin (PlayerJoinEvent e){
        Location spawnPoint = Bukkit.getWorld(world).getSpawnLocation();
        e.getPlayer().teleport(spawnPoint);
        e.getPlayer().getInventory().clear();
        checkPlayersAndStart();
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
    public void itemMove(InventoryClickEvent e){
        e.setCancelled(true);
    }

    @EventHandler
    public void handChange(PlayerItemHeldEvent e){
        e.setCancelled(true);
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
    public void blockDamaged (BlockDamageEvent e){
        e.setCancelled(true);
    }

    //prevents weather changes
    @EventHandler
    public void weatherChange(WeatherChangeEvent e){
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

    @Override
    public void onPluginMessageReceived(String s, Player player, byte[] bytes) {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        player.sendMessage("recieved");
        try{
            String msg = in.readUTF();
            Bukkit.broadcastMessage(msg);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
