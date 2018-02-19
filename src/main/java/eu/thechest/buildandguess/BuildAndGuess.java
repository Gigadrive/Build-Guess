package eu.thechest.buildandguess;

import eu.thechest.buildandguess.user.BuildPlayer;
import eu.thechest.chestapi.ChestAPI;
import eu.thechest.chestapi.game.GameManager;
import eu.thechest.chestapi.mysql.MySQLManager;
import eu.thechest.chestapi.server.GameState;
import eu.thechest.chestapi.server.GameType;
import eu.thechest.chestapi.server.ServerSettingsManager;
import eu.thechest.chestapi.server.ServerUtil;
import eu.thechest.chestapi.user.ChestUser;
import eu.thechest.chestapi.util.BountifulAPI;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by zeryt on 12.03.2017.
 */
public class BuildAndGuess extends JavaPlugin {
    public Location spawnLocation;
    public Location buildSpaceLocation;
    private static BuildAndGuess instance;
    public Player BUILDING_PLAYER;
    public ArrayList<Location> PLACED_BLOCKS;
    public boolean MAY_GUESS = false;

    public GuessWord CURRENT_WORD = null;
    public ArrayList<GuessWord> WORDS_LEFT;

    public ArrayList<String> PLAYERS_LEFT;

    public static BukkitTask GUESSING_TIME;
    public static int GUESS_TIME_LEFT = 90;
    public ArrayList<String> PLAYERS_GUESSED_RIGHT;

    public static final int MIN_PLAYERS = 3;
    public static BukkitTask lobbyCountdown;
    public static int countdown = 40;

    public static ArrayList<Material> DISALLOWED_ITEMS = new ArrayList<Material>();

    public void onEnable(){
        saveDefaultConfig();

        ServerSettingsManager.ENABLE_CHAT = true;
        ServerSettingsManager.ENABLE_NICK = true;
        ServerSettingsManager.updateGameState(GameState.LOBBY);
        ServerSettingsManager.setMaxPlayers(12);
        ServerSettingsManager.PROTECT_ITEM_FRAMES = true;
        ServerSettingsManager.UPDATE_TAB_NAME_WITH_SCOREBOARD = true;
        ServerSettingsManager.VIP_JOIN = true;
        ServerSettingsManager.RUNNING_GAME = GameType.BUILD_AND_GUESS;
        ServerSettingsManager.SHOW_FAME_TITLE_ABOVE_HEAD = true;
        ServerSettingsManager.MIN_PLAYERS = MIN_PLAYERS;
        ServerUtil.updateMapName("JustGuess!");

        spawnLocation = new Location(Bukkit.getWorld(getConfig().getString("locations.spawn.world")),getConfig().getDouble("locations.spawn.x"),getConfig().getDouble("locations.spawn.y"),getConfig().getDouble("locations.spawn.z"),getConfig().getInt("locations.spawn.yaw"),getConfig().getInt("locations.spawn.pitch"));
        buildSpaceLocation = new Location(Bukkit.getWorld(getConfig().getString("locations.buildspace.world")),getConfig().getDouble("locations.buildspace.x"),getConfig().getDouble("locations.buildspace.y"),getConfig().getDouble("locations.buildspace.z"),getConfig().getInt("locations.buildspace.yaw"),getConfig().getInt("locations.buildspace.pitch"));

        Bukkit.getPluginManager().registerEvents(new MainListener(), this);
        MainExecutor exec = new MainExecutor();
        getCommand("setspawn").setExecutor(exec);
        getCommand("setbuildlocation").setExecutor(exec);
        getCommand("start").setExecutor(exec);
        instance = this;

        PLACED_BLOCKS = new ArrayList<Location>();
        WORDS_LEFT = new ArrayList<GuessWord>();
        PLAYERS_LEFT = new ArrayList<String>();
        PLAYERS_GUESSED_RIGHT = new ArrayList<String>();

        for(World w : Bukkit.getWorlds()){
            prepareWorld(w);
        }

        loadWords();

        DISALLOWED_ITEMS.add(Material.FLINT_AND_STEEL);
        DISALLOWED_ITEMS.add(Material.MINECART);
        DISALLOWED_ITEMS.add(Material.COMMAND_MINECART);
        DISALLOWED_ITEMS.add(Material.EXPLOSIVE_MINECART);
        DISALLOWED_ITEMS.add(Material.HOPPER_MINECART);
        DISALLOWED_ITEMS.add(Material.POWERED_MINECART);
        DISALLOWED_ITEMS.add(Material.STORAGE_MINECART);
        DISALLOWED_ITEMS.add(Material.PAINTING);
        DISALLOWED_ITEMS.add(Material.ITEM_FRAME);
        DISALLOWED_ITEMS.add(Material.BOAT);
        DISALLOWED_ITEMS.add(Material.SIGN);
        DISALLOWED_ITEMS.add(Material.SIGN_POST);
        DISALLOWED_ITEMS.add(Material.WALL_SIGN);
        DISALLOWED_ITEMS.add(Material.BED);
        DISALLOWED_ITEMS.add(Material.BED_BLOCK);
        DISALLOWED_ITEMS.add(Material.ARMOR_STAND);
        DISALLOWED_ITEMS.add(Material.SUGAR_CANE);
        DISALLOWED_ITEMS.add(Material.SUGAR_CANE_BLOCK);
    }

    public void startCountdown(){
        if(lobbyCountdown == null){
            lobbyCountdown = new BukkitRunnable() {
                @Override
                public void run() {
                    if(countdown > 0){
                        for(Player all : Bukkit.getOnlinePlayers()){
                            all.setExp((float) ((double) countdown / 40D));
                            all.setLevel(countdown);
                        }
                    } else {
                        for(Player all : Bukkit.getOnlinePlayers()){
                            all.setExp(0);
                            all.setLevel(0);
                        }
                    }

                    if(countdown == 60 || countdown == 30 || countdown == 20 || countdown == 10 || countdown == 5 || countdown == 4 || countdown == 3 || countdown == 2 || countdown == 1){
                        for(Player all : Bukkit.getOnlinePlayers()){
                            if(!ChestUser.isLoaded(all)) continue;
                            ChestUser a = ChestUser.getUser(all);
                            //all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GOLD + a.getTranslatedMessage("The game starts in %ts!").replace("%t",ChatColor.GREEN + String.valueOf(countdown) + ChatColor.GOLD));
                            all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GOLD + a.getTranslatedMessage("The lobby phase ends in %s seconds!").replace("%s",ChatColor.AQUA.toString() + countdown + ChatColor.GOLD.toString()));
                        }
                    } else if(countdown == 0){
                        cancel();
                        lobbyCountdown = null;

                        ServerSettingsManager.updateGameState(GameState.INGAME);
                        ServerSettingsManager.VIP_JOIN = false;
                        GameManager.initializeNewGame(ServerSettingsManager.RUNNING_GAME,null);

                        for(Player all : Bukkit.getOnlinePlayers()){
                            BuildPlayer m = BuildPlayer.getPlayer(all);
                            ChestUser a = ChestUser.getUser(all);
                            all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GOLD + a.getTranslatedMessage("The game starts NOW!"));
                            a.clearScoreboard();
                            m.updateScoreboard();
                            m.addPlayedGames(1);
                            PLAYERS_LEFT.add(all.getName());
                            GameManager.getCurrentGames().get(0).getParticipants().add(all.getUniqueId());
                            all.getInventory().clear();
                            all.getInventory().setArmorContents(null);
                        }

                        startNewRound(false);
                    }

                    countdown--;
                }
            }.runTaskTimer(this,20L,20L);
        }
    }

    public void cancelCountdown(){
        if(lobbyCountdown != null){
            lobbyCountdown.cancel();
            lobbyCountdown = null;
            countdown = 40;

            for(Player all : Bukkit.getOnlinePlayers()){
                all.setExp((float) ((double) countdown / 40D));
                all.setLevel(countdown);

                ChestUser a = ChestUser.getUser(all);
                all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + a.getTranslatedMessage("The countdown has been cancelled."));
            }
        }
    }

    public String getWordTranslation(GuessWord w, Player p){
        ChestUser u = ChestUser.getUser(p);

        if(u.getCurrentLanguage().getLanguageKey().equals("DE") && w.german != null){
            return w.german;
        } else {
            return w.english;
        }
    }

    public void startNewRound(boolean delayed){
        if(ServerSettingsManager.CURRENT_GAMESTATE == GameState.INGAME){
            for(Location loc : PLACED_BLOCKS){
                loc.getBlock().setType(Material.AIR);
            }
            PLACED_BLOCKS.clear();
            BuildPlayer oldPlayer = null;
            if(BUILDING_PLAYER != null){
                BUILDING_PLAYER.teleport(BuildAndGuess.getInstance().spawnLocation);
                BUILDING_PLAYER.setGameMode(GameMode.SURVIVAL);
                BUILDING_PLAYER.setAllowFlight(false);
                BUILDING_PLAYER.getInventory().clear();
                BUILDING_PLAYER.getInventory().setArmorContents(null);
                PLAYERS_LEFT.remove(BUILDING_PLAYER.getName());
                oldPlayer = BuildPlayer.getPlayer(BUILDING_PLAYER);

                if(PLAYERS_GUESSED_RIGHT.size() > 0) oldPlayer.addPoints(2);
            }

            if(CURRENT_WORD != null) WORDS_LEFT.remove(CURRENT_WORD);
            BUILDING_PLAYER = null;
            MAY_GUESS = false;

            if(delayed){
                if(PLAYERS_LEFT.size() == 0 || WORDS_LEFT.size() == 0){
                    chooseWinner();
                } else {
                    for(Player all : Bukkit.getOnlinePlayers()){
                        ChestUser a = ChestUser.getUser(all);
                        if(CURRENT_WORD != null){
                            all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GOLD + a.getTranslatedMessage("The correct word was %w!").replace("%w",ChatColor.AQUA + getWordTranslation(CURRENT_WORD,all) + ChatColor.GOLD));

                            if(PLAYERS_GUESSED_RIGHT.size() == 0){
                                all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GOLD + a.getTranslatedMessage("Nobody knew the correct word!"));
                            } else if(PLAYERS_GUESSED_RIGHT.size() == 1){
                                all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GOLD + a.getTranslatedMessage("%p was the only one to know the correct word!").replace("%p",ChatColor.AQUA + Bukkit.getPlayer(PLAYERS_GUESSED_RIGHT.get(0)).getDisplayName() + ChatColor.GOLD));
                                if(PLAYERS_GUESSED_RIGHT.contains(all.getName())){
                                    all.playSound(all.getEyeLocation(),Sound.LEVEL_UP,1f,1f);
                                }
                            } else {
                                String t = "";
                                for(String w : PLAYERS_GUESSED_RIGHT){
                                    Player wp = Bukkit.getPlayer(w);

                                    if(wp != null){
                                        if(t.equals("")){
                                            t = t + wp.getDisplayName();
                                        } else {
                                            t = t + ChatColor.GOLD + ", " + wp.getDisplayName();
                                        }
                                    }
                                }

                                all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GOLD + a.getTranslatedMessage("The players %t knew the correct word!").replace("%t",t + ChatColor.GOLD));
                                if(PLAYERS_GUESSED_RIGHT.contains(all.getName())){
                                    all.playSound(all.getEyeLocation(),Sound.LEVEL_UP,1f,1f);
                                }
                            }
                        }

                        all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GOLD + a.getTranslatedMessage("The next round starts in 5 seconds!"));
                        all.setLevel(0);
                        all.setExp(0);
                        BuildPlayer.getPlayer(all).updateScoreboard();
                    }

                    PLAYERS_GUESSED_RIGHT.clear();

                    Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable(){
                        public void run(){
                            startNewRound(false);
                        }
                    }, 5*20);
                }
            } else {
                MAY_GUESS = true;
                PLAYERS_GUESSED_RIGHT.clear();

                Collections.shuffle(PLAYERS_LEFT);
                String n = PLAYERS_LEFT.get(0);
                Player newPlayer = Bukkit.getPlayer(n);

                Collections.shuffle(WORDS_LEFT);
                GuessWord newWord = WORDS_LEFT.get(0);

                CURRENT_WORD = newWord;
                BUILDING_PLAYER = newPlayer;
                ChestUser u = ChestUser.getUser(BUILDING_PLAYER);

                MAY_GUESS = true;
                BUILDING_PLAYER.getInventory().clear();
                BUILDING_PLAYER.getInventory().setArmorContents(null);

                BUILDING_PLAYER.teleport(buildSpaceLocation);
                BUILDING_PLAYER.setGameMode(GameMode.CREATIVE);
                BUILDING_PLAYER.setAllowFlight(true);
                BUILDING_PLAYER.sendMessage("  ");
                BUILDING_PLAYER.sendMessage("  ");
                BUILDING_PLAYER.sendMessage("  ");
                BUILDING_PLAYER.sendMessage("  ");
                BUILDING_PLAYER.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + u.getTranslatedMessage("You are now building!"));
                BUILDING_PLAYER.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + u.getTranslatedMessage("You have to build the word %w!").replace("%w",ChatColor.YELLOW.toString() + ChatColor.BOLD + getWordTranslation(CURRENT_WORD,BUILDING_PLAYER) + ChatColor.GREEN));
                BUILDING_PLAYER.sendMessage("  ");

                BountifulAPI.sendTitle(BUILDING_PLAYER,0,3*20,1*10,ChatColor.YELLOW + getWordTranslation(CURRENT_WORD,BUILDING_PLAYER),"");

                GUESSING_TIME = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if(GUESS_TIME_LEFT == 0){
                            MAY_GUESS = false;
                            cancel();
                            GUESS_TIME_LEFT = 90;
                            GUESSING_TIME = null;

                            startNewRound(true);
                        } else {
                            for(Player all : Bukkit.getOnlinePlayers()){
                                ChestUser a = ChestUser.getUser(all);
                                all.setExp((float) ((double) GUESS_TIME_LEFT / 90D));
                                all.setLevel(GUESS_TIME_LEFT);

                                if(GUESS_TIME_LEFT == 60 || GUESS_TIME_LEFT == 30 || GUESS_TIME_LEFT == 20 || GUESS_TIME_LEFT == 10 || GUESS_TIME_LEFT == 5 || GUESS_TIME_LEFT == 4 || GUESS_TIME_LEFT == 3 || GUESS_TIME_LEFT == 2 || GUESS_TIME_LEFT == 1){
                                    all.playSound(all.getEyeLocation(),Sound.NOTE_BASS,1f,1f);
                                    if(BUILDING_PLAYER == all){
                                        all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GOLD + a.getTranslatedMessage("You have %s seconds left to build the word!").replace("%s",ChatColor.AQUA.toString() + GUESS_TIME_LEFT + ChatColor.GOLD.toString()));
                                    } else {
                                        if(!PLAYERS_GUESSED_RIGHT.contains(all.getName())){
                                            all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GOLD + a.getTranslatedMessage("You have %s seconds left to guess the word!").replace("%s",ChatColor.AQUA.toString() + GUESS_TIME_LEFT + ChatColor.GOLD.toString()));
                                        }
                                    }
                                }
                            }

                            GUESS_TIME_LEFT--;
                        }
                    }
                }.runTaskTimer(this,20L,20L);
            }
        }
    }

    public void chooseWinner(){
        chooseWinner(null);
    }

    public void chooseWinner(Player toExclude){
        ArrayList<Player> potentials = new ArrayList<Player>();

        int highestPoints = 0;

        for(Player all : Bukkit.getOnlinePlayers()){
            BuildPlayer.getPlayer(all).updateScoreboard();
            if(toExclude != null && toExclude == all) continue;
            if(BuildPlayer.getPlayer(all).getCurrentPoints() > highestPoints){
                potentials.clear();
                potentials.add(all);
                highestPoints = BuildPlayer.getPlayer(all).getCurrentPoints();
            } else if(BuildPlayer.getPlayer(all).getCurrentPoints() == highestPoints){
                potentials.add(all);
            }
            all.setLevel(0);
            all.setExp(0f);
        }

        Collections.shuffle(potentials);
        Player p = potentials.get(0);
        BuildPlayer m = BuildPlayer.getPlayer(p);
        ChestUser u = m.getUser();

        GameManager.getCurrentGames().get(0).getWinners().add(p.getUniqueId());
        GameManager.getCurrentGames().get(0).setCompleted(true);
        GameManager.getCurrentGames().get(0).saveData();
        m.addVictories(1);
        m.getUser().giveExp(17);
        p.playSound(p.getEyeLocation(),Sound.LEVEL_UP,1f,1f);
        /*if(m.getVictories() >= 10) u.achieve(5);
        if(m.getVictories() >= 25) u.achieve(6);
        if(m.getVictories() >= 50) u.achieve(7);

        if(m.getCurrentPoints() >= 16) u.achieve(4);*/

        for(Player all : Bukkit.getOnlinePlayers()){
            ChestUser a = ChestUser.getUser(all);
            if(CURRENT_WORD != null) all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GOLD + a.getTranslatedMessage("The correct word was %w!").replace("%w",ChatColor.AQUA + getWordTranslation(CURRENT_WORD,all) + ChatColor.GOLD));
            ChestUser.getUser(all).addCoins(BuildPlayer.getPlayer(all).getCurrentPoints());
            all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GOLD + ChestUser.getUser(all).getTranslatedMessage("The player %p has won the game with %c points! Congratulations!").replace("%p",p.getDisplayName() + ChatColor.GOLD).replace("%c",ChatColor.AQUA.toString() + m.getCurrentPoints() + ChatColor.GOLD));
            BountifulAPI.sendTitle(all,1*20,5*20,1*20,p.getDisplayName(),ChatColor.GRAY + a.getTranslatedMessage("is the WINNER!"));
        }

        u.playVictoryEffect();
        m.handleAchievements();

        ServerSettingsManager.updateGameState(GameState.ENDING);

        Bukkit.getScheduler().scheduleSyncDelayedTask(this,new Runnable(){
            public void run(){
                for(Player all : Bukkit.getOnlinePlayers()){
                    ChestAPI.giveAfterGameCrate(new Player[]{p});
                }
            }
        }, 2*20);

        Bukkit.getScheduler().scheduleSyncDelayedTask(this,new Runnable(){
            public void run(){
                for(Player all : Bukkit.getOnlinePlayers()){
                    ChestUser.getUser(all).sendGameLogMessage(GameManager.getCurrentGames().get(0).getID());
                }
            }
        }, 3*20);

        Bukkit.getScheduler().scheduleSyncDelayedTask(this,new Runnable(){
            public void run(){
                for(Player all : Bukkit.getOnlinePlayers()){
                    ChestUser.getUser(all).connectToLobby();
                }
            }
        }, 10*20);

        Bukkit.getScheduler().scheduleSyncDelayedTask(this,new Runnable(){
            public void run(){
                //Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"restart");
                ChestAPI.stopServer();
            }
        }, 15*20);
    }

    private void loadWords(){
        try {
            PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `bg_words` ORDER BY RAND() LIMIT 12");
            ResultSet rs = ps.executeQuery();

            rs.beforeFirst();
            while(rs.next()){
                String word = rs.getString("word");

                WORDS_LEFT.add(new GuessWord(word,rs.getString("wordDE")));
            }

            MySQLManager.getInstance().closeResources(rs,ps);

            if(WORDS_LEFT.size() < 12){
                System.err.println("COULD NOT LOAD ENOUGH WORDS!! Restarting..");
                System.err.println("COULD NOT LOAD ENOUGH WORDS!! Restarting..");
                System.err.println("COULD NOT LOAD ENOUGH WORDS!! Restarting..");
                System.err.println("COULD NOT LOAD ENOUGH WORDS!! Restarting..");
                System.err.println("COULD NOT LOAD ENOUGH WORDS!! Restarting..");

                Bukkit.shutdown();
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public void onDisable(){
        for(World w : Bukkit.getWorlds()){
            prepareWorld(w);
        }

        for(BuildPlayer b : BuildPlayer.STORAGE.values()){
            BuildPlayer.unregister(b.getPlayer());
        }
    }

    public void prepareWorld(World w){
        for(Entity e : w.getEntities()){
            if(e.getType() != EntityType.ARMOR_STAND && e.getType() != EntityType.PAINTING && e.getType() != EntityType.ITEM_FRAME){
                e.remove();
            }
        }

        w.setGameRuleValue("doDaylightCycle","false");
        w.setGameRuleValue("doTileDrops","false");
        w.setGameRuleValue("doMobSpawning","false");
        w.setGameRuleValue("mobGriefing","false");
        w.setGameRuleValue("randomTickspeed","0");
        w.setTime(5000);
        w.setStorm(false);
        w.setThundering(false);
    }

    public static BuildAndGuess getInstance(){
        return instance;
    }

    public void setSpawn(Location loc){
        spawnLocation = loc;

        getConfig().set("locations.spawn.world",loc.getWorld().getName());
        getConfig().set("locations.spawn.x",loc.getX());
        getConfig().set("locations.spawn.y",loc.getY());
        getConfig().set("locations.spawn.z",loc.getZ());
        getConfig().set("locations.spawn.yaw",loc.getYaw());
        getConfig().set("locations.spawn.pitch",loc.getPitch());
        saveConfig();
    }

    public void setBuildSpaceLocation(Location loc){
        buildSpaceLocation = loc;

        getConfig().set("locations.buildspace.world",loc.getWorld().getName());
        getConfig().set("locations.buildspace.x",loc.getX());
        getConfig().set("locations.buildspace.y",loc.getY());
        getConfig().set("locations.buildspace.z",loc.getZ());
        getConfig().set("locations.buildspace.yaw",loc.getYaw());
        getConfig().set("locations.buildspace.pitch",loc.getPitch());
        saveConfig();
    }
}
