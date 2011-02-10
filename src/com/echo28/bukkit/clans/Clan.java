package com.echo28.bukkit.clans;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;


public class Clan
{
	private String name;
	private Clans plugin;

	private List<Player> online = new ArrayList<Player>();
	private List<String> members = new ArrayList<String>();
	private List<String> council = new ArrayList<String>();
	private List<String> wars = new ArrayList<String>();
	private String leader = null;
	private Location location = null;

	private boolean chat = false;
	private String tag;

	private final Logger log = Logger.getLogger("Minecraft");

	public Clan(Clans plugin, String name)
	{
		this.plugin = plugin;
		this.name = name;
		load();
	}

	public List<Player> getOnline()
	{
		return online;
	}

	private void load()
	{
		File yml = new File(plugin.getDataFolder(), name + ".yml");
		if (yml.exists())
		{
			log.info("loading clan: " + name);
			Configuration config = new Configuration(yml);
			config.load();
			members = config.getStringList("members", null);
			council = config.getStringList("council", null);
			leader = config.getString("settings.leader", "");
			tag = config.getString("settings.tag", "");
			chat = config.getBoolean("settings.chat", false);
			double db = 0;
			if (config.getDouble("settings.location.x", db) != 0)
			{
				location = new Location(getWorld(config.getInt("settings.location.world", 0)), config.getDouble("settings.location.x", db), config.getDouble("settings.location.y",
						db), config.getDouble("settings.location.z", db), (float) config.getDouble("settings.location.yaw", db), (float) config.getDouble(
						"settings.location.pitch", db));
			}
		}
		// this probably wont do anything unless someone hits /reload
		online = new ArrayList<Player>();
		for (Player player : plugin.getServer().getOnlinePlayers())
		{
			joined(player);
		}

	}

	public void loadWars()
	{
		// load existing wars into memory
		String[] wars = plugin.model.getWars(getName());
		for (String clan : wars)
		{
			Clan warringClan = plugin.getClan(clan);
			if (warringClan != null)
			{
				if (!this.equals(warringClan))
				{
					this.wars.add(warringClan.getName());
				}
			}
		}
	}

	private World getWorld(long id)
	{
		List<World> worlds = plugin.getServer().getWorlds();
		for (World world : worlds)
		{
			if (world.getId() == id) { return world; }
		}
		return null;
	}

	private void save()
	{
		File yml = new File(plugin.getDataFolder(), name + ".yml");
		if (!yml.exists())
		{
			try
			{
				yml.createNewFile();
			}
			catch (IOException ex)
			{
			}
		}
		Configuration config = new Configuration(yml);
		config.setProperty("members", members);
		config.setProperty("council", council);
		config.setProperty("settings.leader", leader);
		config.setProperty("settings.tag", tag);
		config.setProperty("settings.chat", chat);
		if (location != null)
		{
			config.setProperty("settings.location.x", location.getX());
			config.setProperty("settings.location.y", location.getY());
			config.setProperty("settings.location.z", location.getZ());
			config.setProperty("settings.location.yaw", location.getYaw());
			config.setProperty("settings.location.pitch", location.getPitch());
			log.info("world:" + location.getWorld());
			log.info("world id:" + location.getWorld().getId());
			config.setProperty("settings.location.world", location.getWorld().getId());

		}
		config.save();
	}

	public void disband()
	{
		File yml = new File(plugin.getDataFolder(), name + ".yml");
		yml.delete();
	}

	public String getLeaderPlayer()
	{
		Player player = plugin.getServer().getPlayer(leader);
		if (player != null) { return player.getDisplayName(); }
		return leader;
	}

	public List<String> getMembers()
	{
		return members;
	}

	public List<String> getCouncil()
	{
		return council;
	}

	public String getName()
	{
		return name;
	}

	public boolean isMember(Player player)
	{
		return members.contains(player.getName());
	}

	public boolean isCouncil(Player player)
	{
		if (members.contains(player.getName()))
		{
			return council.contains(player.getName());
		}
		else
		{
			return false;
		}
	}

	public boolean isLeader(Player player)
	{
		if (leader == null) { return false; }
		if (player.getName().equalsIgnoreCase(leader)) { return true; }
		return false;
	}

	public void addMember(Player player)
	{
		if (isMember(player)) { return; }
		if (members.size() == 0)
		{
			leader = player.getName();
		}
		members.add(player.getName());
		joined(player);
		if (council.size() == 0)
		{
			council.add(player.getName());
		}
		save();
	}

	public void addCouncil(Player player)
	{
		if (isCouncil(player)) { return; }
		council.add(player.getName());
		save();
	}

	public void removeMember(Player player)
	{
		if (!isMember(player)) { return; }
		members.remove(player.getName());
		quit(player);
		save();
		player.sendMessage(ChatColor.RED + "You were removed from your clan.");
		sendMessage(player.getDisplayName() + ChatColor.RED + " was removed from the clan");
	}

	public void removeCouncil(Player player)
	{
		if (!isCouncil(player)) { return; }
		council.remove(player.getName());
		save();
		player.sendMessage(ChatColor.RED + "You were removed from the council.");
		sendMessage(player.getDisplayName() + ChatColor.RED + " was removed from the council");
	}

	public void setLeader(Player player)
	{
		if (!isCouncil(player)) { return; }
		leader = player.getName();
		save();
		sendMessage(player.getDisplayName() + ChatColor.RED + " is now the clan leader");
	}

	public void setTag(String tag)
	{
		this.tag = tag;
		save();
	}

	public boolean toggleChat()
	{
		if (chat)
		{
			chat = false;
		}
		else
		{
			chat = true;
		}
		save();
		return chat;
	}

	public boolean canChat()
	{
		return chat;
	}

	public void chat(Player player, String message)
	{
		if (!chat) { return; }
		for (Player p : online)
		{
			p.sendMessage(ChatColor.YELLOW + "[CLAN-MSG]" + ChatColor.WHITE + "<" + player.getDisplayName() + ChatColor.WHITE + "> " + message);
		}
	}

	public void councilChat(Player player, String message)
	{
		if (!chat) { return; }
		for (Player p : online)
		{
			if (isCouncil(p))
			{
				p.sendMessage(ChatColor.YELLOW + "[CLAN-COUNCIL-MSG]" + ChatColor.WHITE + "<" + player.getDisplayName() + ChatColor.WHITE + "> " + message);
			}
		}
	}

	public void setHallLocation(Location location)
	{
		this.location = location;
		save();
	}

	public boolean canModify(CommandSender sender)
	{
		if (sender.isOp()) { return true; }
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			if (canLeaderModify(player)) { return true; }
			if (canCouncilModify(player)) { return true; }
		}
		return false;
	}

	public boolean canLeaderModify(CommandSender sender)
	{
		if (sender.isOp()) { return true; }
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			if (player.getName() == leader) { return true; }
		}
		return false;
	}

	public boolean canCouncilModify(Player player)
	{
		if (council.contains(player.getName())) { return true; }
		return false;
	}

	public void joined(Player player)
	{
		if (online.contains(player)) { return; }
		if (!members.contains(player.getName())) { return; }
		if (plugin.appendTag)
		{
			if (plugin.appendTagTo.equalsIgnoreCase("name"))
			{
				player.setDisplayName(ChatColor.WHITE + "[" + ChatColor.YELLOW + getName() + ChatColor.WHITE + "]" + player.getName());
			}
			else if (plugin.appendTagTo.equalsIgnoreCase("display-name"))
			{
				player.setDisplayName(ChatColor.WHITE + "[" + ChatColor.YELLOW + getName() + ChatColor.WHITE + "]" + player.getDisplayName());
			}
		}
		online.add(player);
	}

	public void quit(Player player)
	{
		if (online.contains(player))
		{
			online.remove(player);
			if (plugin.appendTag)
			{
				player.setDisplayName(player.getName());
			}
		}
	}

	public boolean isOnline(Player player)
	{
		return online.contains(player);
	}

	public void sendPlayerToHall(Player player)
	{
		if (location != null)
		{
			player.teleportTo(location);
		}
	}

	// winner(no clan) killed loser(this clan)
	public void deathNonClanLoser(Player winner, Player loser, String cause)
	{
		plugin.model.storeDeath(winner.getName(), null, loser.getName(), getName(), cause);
	}

	// winner(this clan) killed loser (no clan)
	public void deathNonClanWinner(Player winner, Player loser, String cause)
	{
		plugin.model.storeDeath(winner.getName(), getName(), loser.getName(), null, cause);
	}

	// winner(this clan) killed loser(loserClan)
	public void deathClanWinner(Player winner, Player loser, String cause, Clan loserClan)
	{
		if (wars.contains(loserClan))
		{
			int id = plugin.model.getActiveWar(getName(), loserClan.getName());
			if (id != 0)
			{
				plugin.model.storeDeath(id, winner.getName(), getName(), loser.getName(), loserClan.getName(), cause);
			}
			else
			{
				plugin.model.storeDeath(winner.getName(), getName(), loser.getName(), loserClan.getName(), cause);
			}
		}
		else
		{
			plugin.model.storeDeath(winner.getName(), getName(), loser.getName(), loserClan.getName(), cause);
		}
	}

	// winner(This clan) killed loser (this clan)
	public void deathSameClan(Player winner, Player loser, String cause)
	{
		plugin.model.storeDeath(winner.getName(), getName(), loser.getName(), getName(), cause);
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof Clan)
		{
			Clan clan = (Clan) o;
			if (getName().equalsIgnoreCase(clan.getName())) { return true; }
		}
		return false;
	}

	public void sendStats(CommandSender sender)
	{
		int victoriesOverRivalClans = plugin.model.victoriesOverRivalClans(getName());
		int lossesOverRivalClans = plugin.model.lossesOverRivalClans(getName());
		int victoriesOverNonClans = plugin.model.victoriesOverNonClans(getName());
		int lossesOverNonClans = plugin.model.lossesOverNonClans(getName());
		int sameClanSparring = plugin.model.sameClanSparring(getName());

		sender.sendMessage(ChatColor.RED + getName() + ": " + ChatColor.YELLOW + "PvP stats");
		sender.sendMessage(ChatColor.YELLOW + "---------");
		sender.sendMessage(ChatColor.RED + "Rival clans: " + ChatColor.YELLOW + "wins:" + ChatColor.GRAY + victoriesOverRivalClans + ChatColor.YELLOW + " losses:" + ChatColor.GRAY
				+ lossesOverRivalClans);
		sender.sendMessage(ChatColor.RED + "Non clans: " + ChatColor.YELLOW + "wins:" + ChatColor.GRAY + victoriesOverNonClans + ChatColor.YELLOW + " losses:" + ChatColor.GRAY
				+ lossesOverNonClans);
		sender.sendMessage(ChatColor.RED + "Clan sparring: " + sameClanSparring);
	}

	// stats about this clan's wars
	public void sendWarStats(CommandSender sender, Clan opposingClan)
	{
		sender.sendMessage(ChatColor.RED + getName() + ": " + ChatColor.YELLOW + "War stats against " + ChatColor.GRAY + opposingClan.getName());
		int warsFought = plugin.model.warsCount(getName(), opposingClan.getName());
		int warsEndedByUs = plugin.model.warsEnded(getName(), opposingClan.getName(), getName());
		int warsWon = plugin.model.warsWon(getName(), opposingClan.getName(), getName());
		int warsLost = plugin.model.warsWon(getName(), opposingClan.getName(), opposingClan.getName());
		sender.sendMessage(ChatColor.RED + "Wars: " + ChatColor.YELLOW + "fought:" + ChatColor.GRAY + warsFought + ChatColor.YELLOW + " won:" + ChatColor.GRAY + warsWon
				+ ChatColor.YELLOW + " lost:" + ChatColor.GRAY + warsLost);
		sender.sendMessage(ChatColor.RED + "Wars we ended:" + ChatColor.YELLOW + warsEndedByUs);
		int id = plugin.model.getActiveWar(getName(), opposingClan.getName());
		if (id != 0)
		{
			int warWins = plugin.model.warDeathCount(id, opposingClan.getName());
			int warLosses = plugin.model.warDeathCount(id, getName());
			String timeRange = plugin.model.warTime(id);
			sender.sendMessage(ChatColor.RED + "Current War stats:");
			sender.sendMessage(ChatColor.RED + "War going on for" + ChatColor.YELLOW + timeRange);
			sender.sendMessage(ChatColor.RED + "Stats: " + ChatColor.YELLOW + "wins:" + ChatColor.GRAY + warWins + ChatColor.YELLOW + " losses:" + ChatColor.GRAY + warLosses);
			sender.sendMessage(ChatColor.RED + "Death toll: " + ChatColor.YELLOW + (warWins + warLosses));
		}
	}

	public void warDeclare(Player player, Clan clan)
	{
		if (wars.contains(clan.getName()))
		{
			player.sendMessage(ChatColor.RED + "You are already at war with " + clan.getName());
			return;
		}
		plugin.model.startWar(getName(), clan.getName());
		wars.add(clan.getName());
		sendMessage(ChatColor.RED + "You are now at war with " + ChatColor.YELLOW + clan.getName());
	}

	public void warEnd(Player player, Clan clan)
	{
		if (!wars.contains(clan.getName()))
		{
			player.sendMessage(ChatColor.RED + "You are not at war with " + clan.getName());
			return;
		}
		int id = plugin.model.getActiveWar(getName(), clan.getName());
		if (id == 0)
		{
			player.sendMessage(ChatColor.RED + "You are not at war with " + clan.getName());
			log.info("SQLite record not found, couldn't remove clan");
			wars.remove(clan);
			return;
		}
		int warWins = plugin.model.warDeathCount(id, clan.getName());
		int warLosses = plugin.model.warDeathCount(id, getName());
		String victor = "";
		if (warWins == warLosses)
		{
			player.sendMessage(ChatColor.RED + "You cannot end this war in a tie.");
			return;
		}
		else if (warWins > warLosses)
		{
			sendMessage(ChatColor.RED + "Your war with " + ChatColor.YELLOW + clan.getName() + ChatColor.RED + "is over, you won!");
			victor = getName();
		}
		else
		{
			sendMessage(ChatColor.RED + "Your war with " + ChatColor.YELLOW + clan.getName() + ChatColor.RED + "is over, you lost!");
			victor = clan.getName();
		}
		plugin.model.endWar(id, getName(), victor);
		wars.remove(clan);
	}

	public void sendMessage(String message)
	{
		for (Player player : online)
		{
			player.sendMessage(message);
		}
	}

}
