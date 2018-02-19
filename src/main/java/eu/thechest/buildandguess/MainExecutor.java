package eu.thechest.buildandguess;

import eu.thechest.buildandguess.user.BuildPlayer;
import eu.thechest.chestapi.mysql.MySQLManager;
import eu.thechest.chestapi.server.ServerSettingsManager;
import eu.thechest.chestapi.user.ChestUser;
import eu.thechest.chestapi.user.Rank;
import eu.thechest.chestapi.util.PlayerUtilities;
import eu.thechest.chestapi.util.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by zeryt on 12.03.2017.
 */
public class MainExecutor implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(cmd.getName().equalsIgnoreCase("setspawn")){
            if(sender instanceof Player){
                Player p = (Player)sender;
                ChestUser u = ChestUser.getUser(p);

                if(u.hasPermission(Rank.ADMIN)){
                    BuildAndGuess.getInstance().setSpawn(p.getLocation());
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + u.getTranslatedMessage("The location has been saved."));
                } else {
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You aren't permitted to execute this command."));
                }
            }
        }

        if(cmd.getName().equalsIgnoreCase("setbuildlocation")){
            if(sender instanceof Player){
                Player p = (Player)sender;
                ChestUser u = ChestUser.getUser(p);

                if(u.hasPermission(Rank.ADMIN)){
                    BuildAndGuess.getInstance().setBuildSpaceLocation(p.getLocation());
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + u.getTranslatedMessage("The location has been saved."));
                } else {
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You aren't permitted to execute this command."));
                }
            }
        }

        return false;
    }
}
