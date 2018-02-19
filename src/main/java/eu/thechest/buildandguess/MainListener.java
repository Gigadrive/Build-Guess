package eu.thechest.buildandguess;

import eu.thechest.buildandguess.user.BuildPlayer;
import eu.thechest.chestapi.ChestAPI;
import eu.thechest.chestapi.event.NickChangeEvent;
import eu.thechest.chestapi.event.PlayerDataLoadedEvent;
import eu.thechest.chestapi.game.GameManager;
import eu.thechest.chestapi.items.ItemUtil;
import eu.thechest.chestapi.server.GameState;
import eu.thechest.chestapi.server.ServerSettingsManager;
import eu.thechest.chestapi.user.ChestUser;
import eu.thechest.chestapi.util.StringUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;

import java.util.ArrayList;

/**
 * Created by zeryt on 12.03.2017.
 */
public class MainListener implements Listener {
    @EventHandler
    public void onLogin(PlayerLoginEvent e){
        Player p = e.getPlayer();

        if(ServerSettingsManager.CURRENT_GAMESTATE != GameState.LOBBY){
            e.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            e.setKickMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "The game has already started.");
        }
    }

    @EventHandler
    public void onChange(NickChangeEvent e){
        if(ServerSettingsManager.CURRENT_GAMESTATE != GameState.LOBBY){
            for(Player all : Bukkit.getOnlinePlayers()){
                BuildPlayer.getPlayer(all).updateScoreboard();
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e){
        Player p = e.getPlayer();
        ChestUser u = ChestUser.getUser(p);
        BuildPlayer b = BuildPlayer.getPlayer(p);

        if(ServerSettingsManager.CURRENT_GAMESTATE == GameState.INGAME){
            if(BuildAndGuess.getInstance().BUILDING_PLAYER == p){
                e.setCancelled(true);
                p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You may not chat right now."));
            } else {
                if(BuildAndGuess.getInstance().PLAYERS_GUESSED_RIGHT.contains(p.getName())){
                    e.setCancelled(true);
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You have already guessed the word."));
                } else {
                    boolean logChat = true;

                    if(BuildAndGuess.getInstance().MAY_GUESS && BuildAndGuess.getInstance().CURRENT_WORD != null){
                        if(e.getMessage().trim().equalsIgnoreCase(BuildAndGuess.getInstance().CURRENT_WORD.english) || e.getMessage().trim().equalsIgnoreCase(u.getTranslatedMessage(BuildAndGuess.getInstance().CURRENT_WORD.german))){
                            logChat = false;

                            if(BuildAndGuess.getInstance().PLAYERS_GUESSED_RIGHT.size() == 0){
                                b.addPoints(3);

                                if(BuildAndGuess.GUESS_TIME_LEFT > 30){
                                    BuildAndGuess.GUESS_TIME_LEFT = 30;
                                }
                            } else if(BuildAndGuess.getInstance().PLAYERS_GUESSED_RIGHT.size() == 0){
                                b.addPoints(2);
                            } else {
                                b.addPoints(1);
                            }

                            BuildAndGuess.getInstance().PLAYERS_GUESSED_RIGHT.add(p.getName());
                            b.addGuessedWords(1);
                            b.getUser().giveExp(3);
                            u.achieve(25);
                            e.setCancelled(true);
                            GameManager.getCurrentGames().get(0).addWordGuessedEvent(p,BuildAndGuess.getInstance().CURRENT_WORD.english);

                            if(BuildAndGuess.GUESS_TIME_LEFT <= 3) u.achieve(38);

                            ArrayList<String> s = new ArrayList<String>();
                            for(Player all : Bukkit.getOnlinePlayers()) s.add(all.getName());
                            s.removeAll(BuildAndGuess.getInstance().PLAYERS_GUESSED_RIGHT);
                            s.remove(BuildAndGuess.getInstance().BUILDING_PLAYER.getName());
                            boolean everyBodyGuessed = s.size()==0;

                            for(Player all : Bukkit.getOnlinePlayers()){
                                ChestUser a = ChestUser.getUser(all);
                                all.playSound(all.getEyeLocation(), Sound.ORB_PICKUP,1f,1f);

                                all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GOLD + a.getTranslatedMessage("The player %p has guessed the word!").replace("%p",p.getDisplayName() + ChatColor.GOLD));
                                BuildPlayer.getPlayer(all).updateScoreboard();

                                if(everyBodyGuessed){
                                    all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GOLD + a.getTranslatedMessage("Everybody has guessed the word!"));
                                }
                            }

                            if(everyBodyGuessed) BuildAndGuess.GUESS_TIME_LEFT = 1;
                        }
                    }

                    if(logChat){
                        GameManager.getCurrentGames().get(0).addPlayerChatEvent(p,e.getMessage());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e){
        e.setCancelled(true);
    }

    @EventHandler
    public void onLeafDecay(LeavesDecayEvent e){
        e.setCancelled(true);
    }

    @EventHandler
    public void onSpread(BlockSpreadEvent e){
        e.setCancelled(true);
    }

    @EventHandler
    public void onBurn(BlockBurnEvent e){
        e.setCancelled(true);
    }

    @EventHandler
    public void onFade(BlockFadeEvent e){
        e.setCancelled(true);
    }

    @EventHandler
    public void onFromTo(BlockFromToEvent e){
        e.setCancelled(true);
    }

    @EventHandler
    public void onEmpty(PlayerBucketEmptyEvent e){
        e.setCancelled(true);
    }

    @EventHandler
    public void onPlaceHanging(HangingPlaceEvent e){
        e.setCancelled(true);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e){
        Player p = e.getPlayer();

        e.setJoinMessage(null);

        p.getInventory().setArmorContents(null);
        p.getInventory().clear();
        p.setGameMode(GameMode.SURVIVAL);
        p.setFoodLevel(20);
        p.setFireTicks(0);
        p.setExp((float) ((double) BuildAndGuess.countdown / 40D));
        p.setLevel(BuildAndGuess.countdown);

        p.teleport(BuildAndGuess.getInstance().spawnLocation);

        if(BuildAndGuess.lobbyCountdown == null && Bukkit.getOnlinePlayers().size() >= BuildAndGuess.MIN_PLAYERS){
            BuildAndGuess.getInstance().startCountdown();
        }
    }

    @EventHandler
    public void onLoaded(PlayerDataLoadedEvent e){
        Player p = e.getPlayer();

        ChestAPI.async(() -> {
            ChestUser u = ChestUser.getUser(p);
            BuildPlayer b = BuildPlayer.getPlayer(p);
            StringUtils.sendJoinMessage(p);

            b.handleAchievements();

            for(Player all : Bukkit.getOnlinePlayers()){
                if(ChestUser.isLoaded(all)) ChestAPI.sync(() -> BuildPlayer.getPlayer(all).updateScoreboard());
            }

            p.getInventory().setItem(8, ItemUtil.namedItem(Material.CHEST, org.bukkit.ChatColor.RED + u.getTranslatedMessage("Back to Lobby"), null));
        });
    }

    @EventHandler
    public void onPhysics(BlockPhysicsEvent e){
        e.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e){
        Player p = e.getPlayer();
        BuildPlayer b = BuildPlayer.getPlayer(p);
        ChestUser u = b.getUser();

        e.setQuitMessage(null);

        if(ServerSettingsManager.CURRENT_GAMESTATE == GameState.LOBBY){
            StringUtils.sendQuitMessage(p);

            if(Bukkit.getOnlinePlayers().size()-1 < BuildAndGuess.MIN_PLAYERS){
                BuildAndGuess.getInstance().cancelCountdown();
            }

            for(Player all : Bukkit.getOnlinePlayers()){
                if(ChestUser.isLoaded(all)) BuildPlayer.getPlayer(all).updateScoreboard(1);
            }
        } else if(ServerSettingsManager.CURRENT_GAMESTATE == GameState.INGAME){
            if(Bukkit.getOnlinePlayers().size()-1 == 1){
                /*Player winner = null;

                for(Player a : Bukkit.getOnlinePlayers()){
                    if(a != p){
                        winner = a;
                        break;
                    }
                }

                if(winner != null){
                    MusicalPlayer.get(winner).winGame();
                } else {
                    for(Player a : Bukkit.getOnlinePlayers()){
                        ChestUser.getUser(a).connectToLobby();
                    }
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"restart");
                }*/
                if(BuildAndGuess.GUESSING_TIME != null) BuildAndGuess.GUESSING_TIME.cancel();
                BuildAndGuess.GUESSING_TIME = null;
                BuildAndGuess.getInstance().chooseWinner(p);
            } else {
                if(BuildAndGuess.getInstance().BUILDING_PLAYER == p){
                    if(BuildAndGuess.GUESSING_TIME != null) BuildAndGuess.GUESSING_TIME.cancel();
                    BuildAndGuess.GUESSING_TIME = null;

                    /*for(Player all : Bukkit.getOnlinePlayers()){
                        ChestUser a = ChestUser.getUser(all);
                        if(all != p) all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GOLD + a.getTranslatedMessage("%p has left the server.").replace("%p",p.getDisplayName() + ChatColor.GOLD));
                    }*/
                    StringUtils.sendQuitMessage(p);

                    BuildAndGuess.getInstance().startNewRound(true);
                }

                if(BuildAndGuess.getInstance().PLAYERS_LEFT.contains(p.getName())) BuildAndGuess.getInstance().PLAYERS_LEFT.remove(p.getName());
            }
        }

        BuildPlayer.unregister(p);
    }

    @EventHandler
    public void onSpawn(CreatureSpawnEvent e){
        e.setCancelled(true);
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent e){
        e.setCancelled(true);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e){
        e.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e){
        Player p = e.getPlayer();
        BuildPlayer b = BuildPlayer.getPlayer(p);
        ChestUser u = b.getUser();

        if(ServerSettingsManager.CURRENT_GAMESTATE == GameState.LOBBY){
            if(p.getItemInHand() != null && p.getItemInHand().getItemMeta() != null && p.getItemInHand().getItemMeta().getDisplayName() != null){
                if(p.getItemInHand().getItemMeta().getDisplayName().equals(org.bukkit.ChatColor.RED + u.getTranslatedMessage("Back to Lobby"))){
                    u.connectToLobby();
                }
            }
        }

        if(p.getItemInHand() != null && p.getItemInHand().getType() != null){
            if(BuildAndGuess.DISALLOWED_ITEMS.contains(p.getItemInHand().getType())){
                e.setCancelled(true);
                e.setUseInteractedBlock(Event.Result.DENY);
                e.setUseItemInHand(Event.Result.DENY);
            }
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e){
        Player p = e.getPlayer();
        BuildPlayer b = BuildPlayer.getPlayer(p);
        ChestUser u = b.getUser();

        if(ServerSettingsManager.CURRENT_GAMESTATE == GameState.INGAME){
            if(BuildAndGuess.getInstance().BUILDING_PLAYER == p){
                Location loc = null;

                for(Location l : BuildAndGuess.getInstance().PLACED_BLOCKS){
                    if(e.getBlock().getLocation().getX() == l.getBlock().getX() && e.getBlock().getLocation().getY() == l.getBlock().getY() && e.getBlock().getLocation().getZ() == l.getBlock().getZ()){
                        loc = l;
                    }
                }

                if(loc != null){
                    BuildAndGuess.getInstance().PLACED_BLOCKS.remove(loc);
                } else {
                    e.setCancelled(true);
                }
            } else{
                e.setCancelled(true);
            }
        } else {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e){
        Player p = e.getPlayer();
        BuildPlayer b = BuildPlayer.getPlayer(p);
        ChestUser u = b.getUser();

        if(ServerSettingsManager.CURRENT_GAMESTATE == GameState.INGAME){
            if(BuildAndGuess.getInstance().BUILDING_PLAYER == p){
                if(BuildAndGuess.DISALLOWED_ITEMS.contains(e.getBlockPlaced().getType())){
                    e.setCancelled(true);
                    return;
                }

                if(!e.isCancelled()) BuildAndGuess.getInstance().PLACED_BLOCKS.add(e.getBlockPlaced().getLocation());
            } else {
                e.setCancelled(true);
            }
        } else {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent e){
        e.setCancelled(true);
    }
}
