package eu.thechest.buildandguess.user;

import eu.thechest.buildandguess.BuildAndGuess;
import eu.thechest.chestapi.ChestAPI;
import eu.thechest.chestapi.mysql.MySQLManager;
import eu.thechest.chestapi.server.GameState;
import eu.thechest.chestapi.server.ServerSettingsManager;
import eu.thechest.chestapi.server.ServerUtil;
import eu.thechest.chestapi.user.ChestUser;
import eu.thechest.chestapi.user.Rank;
import eu.thechest.chestapi.util.StringUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/**
 * Created by zeryt on 12.03.2017.
 */
public class BuildPlayer {
    public static HashMap<Player,BuildPlayer> STORAGE = new HashMap<Player,BuildPlayer>();

    public static BuildPlayer getPlayer(Player p){
        if(STORAGE.containsKey(p)){
            return STORAGE.get(p);
        } else {
            new BuildPlayer(p);

            if(STORAGE.containsKey(p)){
                return STORAGE.get(p);
            } else {
                return null;
            }
        }
    }

    public static void unregister(Player p){
        if(STORAGE.containsKey(p)){
            STORAGE.get(p).saveData();
            STORAGE.remove(p);
        }
    }

    private Player p;
    private int startPoints;
    private int points = 0;
    private int startPlayedGames;
    private int playedGames = 0;
    private int startVictories;
    private int victories = 0;
    private int startGuessedWords;
    private int guessedWords = 0;
    private Timestamp firstGame;

    public BuildPlayer(Player p){
        if(STORAGE.containsKey(p)) return;
        this.p = p;

        try {
            PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `bg_stats` WHERE `uuid` = ?");
            ps.setString(1,p.getUniqueId().toString());
            ResultSet rs = ps.executeQuery();

            if(rs.first()){
                this.startPoints = rs.getInt("points");
                this.startPlayedGames = rs.getInt("playedGames");
                this.startVictories = rs.getInt("victories");
                this.startGuessedWords = rs.getInt("guessedWords");
                this.firstGame = rs.getTimestamp("firstGame");
                STORAGE.put(p,this);
            } else {
                PreparedStatement insert = MySQLManager.getInstance().getConnection().prepareStatement("INSERT INTO `bg_stats` (`uuid`) VALUES(?)");
                insert.setString(1,p.getUniqueId().toString());
                insert.execute();
                insert.close();

                new BuildPlayer(p);
            }

            MySQLManager.getInstance().closeResources(rs,ps);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public Player getPlayer(){
        return this.p;
    }

    public ChestUser getUser(){
        return ChestUser.getUser(p);
    }

    public int getPoints(){
        return this.startPoints+this.points;
    }

    public void addPoints(int points){
        for(int i = 0; i < points; i++){
            if((startPoints+this.points+i)<0) break;

            this.points++;
        }
    }

    public void reducePoints(int points){
        for(int i = 0; i < points; i++){
            if((startPoints+this.points+(i/-1))<0) break;

            this.points--;
        }
    }

    public int getCurrentPoints(){
        return this.points;
    }

    public int getPlayedGames(){
        return this.startPlayedGames+this.playedGames;
    }

    public void addPlayedGames(int i){
        this.playedGames += i;
    }

    public int getVictories(){
        return this.startVictories+this.victories;
    }

    public void addVictories(int i){
        this.victories += i;
    }

    public int getGuessedWords(){
        return this.startGuessedWords+this.guessedWords;
    }

    public void addGuessedWords(int i){
        this.guessedWords += i;
    }

    public void handleAchievements(){
        if(getVictories() >= 10) getUser().achieve(60);
        if(getVictories() >= 25) getUser().achieve(61);
        if(getVictories() >= 50) getUser().achieve(62);
    }

    public void updateScoreboard(){
        updateScoreboard(0);
    }

    public void updateScoreboard(int reducePlayerAmount){
        Scoreboard b = getUser().getScoreboard();

        Objective ob = null;

        if(b.getObjective(DisplaySlot.SIDEBAR) != null){
            b.getObjective(DisplaySlot.SIDEBAR).unregister();
        }

        ob = b.registerNewObjective("side","dummy");

        if(ServerSettingsManager.CURRENT_GAMESTATE == GameState.INGAME){
            ob.setDisplayName(ChatColor.DARK_PURPLE + "Build & Guess");
            ob.setDisplaySlot(DisplaySlot.SIDEBAR);

            ArrayList<Player> a = new ArrayList<Player>();
            a.addAll(Bukkit.getOnlinePlayers());

            Collections.sort(a, new Comparator<Player>() {
                public int compare(Player p1, Player p2) {
                    Integer points1 = BuildPlayer.getPlayer(p1).getCurrentPoints();
                    Integer points2 = BuildPlayer.getPlayer(p2).getCurrentPoints();

                    return points2.compareTo(points1);
                }
            });

            ob.getScore(" ").setScore(11);
            ob.getScore(ChatColor.AQUA.toString() + ChatColor.BOLD.toString() + getUser().getTranslatedMessage("Your score") + ":").setScore(10);
            ob.getScore(ChatColor.YELLOW + String.valueOf(points)).setScore(9);
            ob.getScore("  ").setScore(8);
            ob.getScore(ChatColor.AQUA.toString() + ChatColor.BOLD.toString() + getUser().getTranslatedMessage("Leading") + ":").setScore(7);
            int score = 6;
            int i = 1;

            for(Player s : a){
                if(i > 3) break;
                ChestUser ss = ChestUser.getUser(s);
                String st = null;
                if(getUser().hasPermission(Rank.VIP)){
                    st = StringUtils.limitString(ss.getRank().getColor() + s.getName(), 16);
                } else {
                    st = StringUtils.limitString(ss.getRank().getColor() + s.getName(), 16);
                }
                if(st == null) continue;
                int points = BuildPlayer.getPlayer(s).getCurrentPoints();
                ob.getScore(st).setScore(score);
                getUser().setPlayerPrefix(st,i + ". ");
                getUser().setPlayerSuffix(st,ChatColor.WHITE + ": " + points);

                i++;
                score--;
            }

            String c = "";

            while(i <= 3){
                String st = ChatColor.DARK_GRAY + "???" + c;
                ob.getScore(st).setScore(score);
                getUser().setPlayerPrefix(st,i + ". ");
                getUser().setPlayerSuffix(st,ChatColor.WHITE + ": " + 0);
                c = c + " ";

                score--;
                i++;
            }

            ob.getScore("   ").setScore(3);
            ob.getScore(StringUtils.SCOREBOARD_LINE_SEPERATOR).setScore(2);
            ob.getScore(StringUtils.SCOREBOARD_FOOTER_IP).setScore(1);
        } else if(ServerSettingsManager.CURRENT_GAMESTATE == GameState.LOBBY){
            ob.setDisplayName(ChatColor.DARK_PURPLE + "Build & Guess");
            ob.setDisplaySlot(DisplaySlot.SIDEBAR);
            ob.getScore("   ").setScore(10);
            ob.getScore(org.bukkit.ChatColor.AQUA.toString() + org.bukkit.ChatColor.BOLD.toString() + getUser().getTranslatedMessage("Players") + ": " + org.bukkit.ChatColor.YELLOW.toString() + (Bukkit.getOnlinePlayers().size()-reducePlayerAmount)).setScore(9);
            ob.getScore(org.bukkit.ChatColor.AQUA.toString() + org.bukkit.ChatColor.BOLD.toString() + getUser().getTranslatedMessage("Min. Players") + ": " + org.bukkit.ChatColor.YELLOW.toString() + ServerSettingsManager.MIN_PLAYERS).setScore(8);
            ob.getScore(org.bukkit.ChatColor.AQUA.toString() + org.bukkit.ChatColor.BOLD.toString() + getUser().getTranslatedMessage("Max. Players") + ": " + org.bukkit.ChatColor.YELLOW.toString() + ServerSettingsManager.MAX_PLAYERS).setScore(7);
            ob.getScore("    ").setScore(6);
            ob.getScore(org.bukkit.ChatColor.AQUA.toString() + org.bukkit.ChatColor.BOLD.toString() + getUser().getTranslatedMessage("Server") + ": ").setScore(5);
            String s = StringUtils.limitString(ServerUtil.getServerName(),16);
            ob.getScore(s).setScore(4);
            getUser().setPlayerPrefix(s,ChatColor.YELLOW.toString());
            ob.getScore("  ").setScore(3);
            ob.getScore(StringUtils.SCOREBOARD_LINE_SEPERATOR).setScore(2);
            ob.getScore(StringUtils.SCOREBOARD_FOOTER_IP).setScore(1);
        }
    }

    public void saveData(){
        ChestAPI.async(() -> {
            try {
                PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("UPDATE `bg_stats` SET `points`=`points`+?, `monthlyPoints`=`monthlyPoints`+?, `playedGames`=`playedGames`+?, `victories`=`victories`+?, `guessedWords`=`guessedWords`+? WHERE `uuid` = ?");
                ps.setInt(1,this.points);
                ps.setInt(2,this.points);
                ps.setInt(3,this.playedGames);
                ps.setInt(4,this.victories);
                ps.setInt(5,this.guessedWords);
                ps.setString(6,p.getUniqueId().toString());
                ps.executeUpdate();
                ps.close();
            } catch (Exception e){
                e.printStackTrace();
            }
        });
    }
}
