package com.echo28.bukkit.clans;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;


/**
 * Clans for Bukkit
 * 
 * @author Nodren
 */
public class Clans extends JavaPlugin
{
	private List<Clan> clans = new ArrayList<Clan>();

	public boolean appendTag = false;
	public String appendTagTo = "name";

	private final ClanPlayerListener playerListener = new ClanPlayerListener(this);
	private final ClanEntityListener entityListener = new ClanEntityListener(this);
	public final ClanModel model = new ClanModel(this);
	private final Logger log = Logger.getLogger("Minecraft");

	private Clan consoleClan = null;

	private HashMap<Player, Clan> adminClans = new HashMap<Player, Clan>();

	public Clans(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader)
	{
		super(pluginLoader, instance, desc, folder, plugin, cLoader);

		folder.mkdirs();

		File yml = new File(getDataFolder(), "config.yml");
		if (!yml.exists())
		{
			try
			{
				yml.createNewFile();
			}
			catch (IOException e)
			{
				log.log(Level.SEVERE, "Could not create file: " + e.getMessage(), e);
			}
		}
		appendTag = getConfiguration().getBoolean("append-clan-tag", false);
		appendTagTo = getConfiguration().getString("append-clan-tag-to", "name");
	}

	public void onDisable()
	{
		unLoadClans();

		log.info(getDescription().getName() + " " + getDescription().getVersion() + " unloaded.");
	}

	public void onEnable()
	{
		model.load();
		loadClans();

		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.ENTITY_DAMAGEDBY_ENTITY, entityListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.ENTITY_DEATH, entityListener, Priority.Monitor, this);

		log.info(getDescription().getName() + " " + getDescription().getVersion() + " loaded.");
	}

	public void unLoadClans()
	{
		for (Clan clan : clans)
		{
			List<Player> online = clan.getOnline();
			for (Player player : online)
			{
				if (appendTag)
				{
					player.setDisplayName(player.getName());
				}
			}
		}
	}

	public void loadClans()
	{
		for (File file : getDataFolder().listFiles())
		{
			if (file.getName().equalsIgnoreCase("config.yml"))
			{
				continue;
			}
			String[] name = file.getName().split("\\.");
			if ((name.length == 2) && (name[1].equalsIgnoreCase("yml")))
			{
				Clan clan = new Clan(this, name[0]);
				clans.add(clan);
			}
		}
		for (Clan clan : clans)
		{
			clan.loadWars();
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args)
	{
		if (command.getName().equalsIgnoreCase("cc"))
		{
			if (args.length > 0) { return chat(sender, args); }
			showChatHelp(sender);
			return true;
		}
		if (command.getName().equalsIgnoreCase("ccc"))
		{
			if (args.length > 0) { return councilChat(sender, args); }
			showCouncilChatHelp(sender);
			return true;
		}
		if (command.getName().equalsIgnoreCase("ch"))
		{
			clanHall(sender);
			return true;
		}
		if (command.getName().equalsIgnoreCase("clan"))
		{
			if (args.length > 2)
			{
				if (args[0].equalsIgnoreCase("chat"))
				{
					String[] newArgs = new String[args.length - 1];
					int i = 0;
					for (String arg : args)
					{
						if (i == 0)
						{
							i++;
							continue;
						}
						newArgs[i - 1] = arg;
						i++;
					}
					return chat(sender, newArgs);

				}
			}
			if (args.length > 3)
			{
				if ((args[0].equalsIgnoreCase("council")) && (args[0].equalsIgnoreCase("chat")))
				{
					String[] newArgs = new String[args.length - 1];
					int i = 0;
					for (String arg : args)
					{
						if ((i == 0) || (i == 1))
						{
							i++;
							continue;
						}
						newArgs[i - 2] = arg;
						i++;
					}
					return councilChat(sender, newArgs);
				}
			}
			if (args.length == 0)
			{
				showHelp(sender);
				return true;
			}
			if (args.length == 1)
			{
				if (args[0].equalsIgnoreCase("chat"))
				{
					showChatHelp(sender);
					return true;
				}
				if (args[0].equalsIgnoreCase("help"))
				{
					showHelp(sender);
					return true;
				}
				if (args[0].equalsIgnoreCase("list"))
				{
					clanList(sender);
					return true;
				}
				if (args[0].equalsIgnoreCase("listen"))
				{
					listen(sender);
					return true;
				}
				if (args[0].equalsIgnoreCase("near"))
				{
					nearList(sender);
					return true;
				}
				if (args[0].equalsIgnoreCase("hall"))
				{
					clanHall(sender);
					return true;
				}
				if (args[0].equalsIgnoreCase("members"))
				{
					getMembers(sender);
					return true;
				}
				if (args[0].equalsIgnoreCase("council"))
				{
					getCouncil(sender);
					return true;
				}
				if (args[0].equalsIgnoreCase("leader"))
				{
					getLeader(sender);
					return true;
				}
				if (args[0].equalsIgnoreCase("reset"))
				{
					reset(sender);
					return true;
				}
				if (args[0].equalsIgnoreCase("stats"))
				{
					stats(sender);
					return true;
				}
				if (args[0].equalsIgnoreCase("disband"))
				{
					disband(sender);
					return true;
				}
			}
			if (args.length == 2)
			{
				if ((args[0].equalsIgnoreCase("council")) && (args[0].equalsIgnoreCase("chat")))
				{
					showCouncilChatHelp(sender);
					return true;
				}
				if (args[0].equalsIgnoreCase("help"))
				{
					showHelp(sender, Integer.parseInt(args[1]));
					return true;
				}
				if (args[0].equalsIgnoreCase("create"))
				{
					create(sender, args[1]);
					return true;
				}
				if (args[0].equalsIgnoreCase("set"))
				{
					if (args[1].equalsIgnoreCase("chat"))
					{
						setChat(sender);
						return true;
					}
					if (args[1].equalsIgnoreCase("hall"))
					{
						setHall(sender);
						return true;
					}
				}
				if (args[0].equalsIgnoreCase("admin"))
				{
					admin(sender, args[1]);
					return true;
				}
				if (args[0].equalsIgnoreCase("members"))
				{
					getMembers(sender, args[1]);
					return true;
				}
				if (args[0].equalsIgnoreCase("council"))
				{
					getCouncil(sender, args[1]);
					return true;
				}
				if (args[0].equalsIgnoreCase("stats"))
				{
					stats(sender, args[1]);
					return true;
				}
				if ((args[0].equalsIgnoreCase("disband")) && (args[1].equalsIgnoreCase("yes")))
				{
					disband(sender, true);
					return true;
				}
			}
			else if (args.length == 3)
			{
				if (args[0].equalsIgnoreCase("war"))
				{
					if (args[1].equalsIgnoreCase("declare"))
					{
						warDeclare(sender, args[2]);
					}
					else if (args[1].equalsIgnoreCase("end"))
					{
						warEnd(sender, args[2]);
					}
					else if (args[1].equalsIgnoreCase("stats"))
					{
						warStats(sender, args[2]);
					}
					return true;
				}
				if (args[0].equalsIgnoreCase("set"))
				{
					if (args[1].equalsIgnoreCase("tag"))
					{
						setTag(sender, args[2]);
						return true;
					}
					if (args[1].equalsIgnoreCase("leader"))
					{
						setLeader(sender, args[2]);
						return true;
					}
				}
				if (args[0].equalsIgnoreCase("add"))
				{
					if (args[1].equalsIgnoreCase("member"))
					{
						addMember(sender, args[2]);
						return true;
					}
					if (args[1].equalsIgnoreCase("council"))
					{
						addCouncil(sender, args[2]);
						return true;
					}
				}
				if (args[0].equalsIgnoreCase("remove"))
				{
					if (args[1].equalsIgnoreCase("member"))
					{
						removeMember(sender, args[2]);
						return true;
					}
					if (args[1].equalsIgnoreCase("council"))
					{
						removeCouncil(sender, args[2]);
						return true;
					}
				}
			}
		}
		showHelp(sender);
		return true;
	}

	private void showHelp(CommandSender sender)
	{
		showHelp(sender, 1);
	}

	private void showHelp(CommandSender sender, int page)
	{
		int pageSize = 7;
		String[] commands = compileCommands(sender);
		int totalPages = (int) Math.ceil((double) commands.length / (double) pageSize);
		if (page > totalPages) { return; }
		sender.sendMessage(ChatColor.WHITE + "Help page " + ChatColor.RED + page + ChatColor.WHITE + " of " + ChatColor.RED + totalPages);
		int i = 1;
		for (String command : commands)
		{
			if (i < ((page * pageSize) - pageSize))
			{
				i++;
				continue;
			}
			sender.sendMessage(ChatColor.RED + command);
			if (i == (page * pageSize))
			{
				break;
			}
			i++;
		}
	}

	private String[] compileCommands(CommandSender sender)
	{
		if (sender instanceof ConsoleCommandSender) { return opCommands; }
		String[] commands = new String[0];
		if (sender instanceof Player)
		{
			commands = combineArrays(commands, playerCommands);
			Clan clan = getClan(sender);
			if (clan != null)
			{
				commands = combineArrays(commands, memberCommands);
				Player player = (Player) sender;
				if (clan.isCouncil(player))
				{
					commands = combineArrays(commands, councilCommands);
					if (clan.isLeader(player))
					{
						commands = combineArrays(commands, leaderCommands);
					}
				}
			}
			if (sender.isOp())
			{
				log.info("op:true");
				commands = combineArrays(commands, opCommands);
			}
		}
		return commands;
	}

	private String[] combineArrays(String[] A, String[] B)
	{
		String[] C = new String[A.length + B.length];
		System.arraycopy(A, 0, C, 0, A.length);
		System.arraycopy(B, 0, C, A.length, B.length);
		return C;
	}

	private String[] playerCommands =
	{
			"/clan help [page] -" + ChatColor.GRAY + " This help screen",
			"/clan list -" + ChatColor.GRAY + " List of all clans",
			"/clan near -" + ChatColor.GRAY + " List of all nearby players, and their clans",
			"/clan members [clan] -" + ChatColor.GRAY + " List of clan members",
			"/clan council [clan] -" + ChatColor.GRAY + " List of clan council",
			"/clan stats [clan] -" + ChatColor.GRAY + " Provide stats on clan's PvP activities" };
	private String[] memberCommands =
	{
			"/cc <msg> or /clan chat <msg> -" + ChatColor.GRAY + " Talk to your clan members",
			"/ch or /clan hall -" + ChatColor.GRAY + " Warp to your clan hall",
			"/clan listen -" + ChatColor.GRAY + " Toggle your ability to hear clan chat",
			"/clan war stats <clan> -" + ChatColor.GRAY + " Provide war time stats", };
	private String[] councilCommands =
	{
			"/clan add member <player> -" + ChatColor.GRAY + " Add a user to your clan",
			"/clan remove member <player> -" + ChatColor.GRAY + " remove a user from your clan",
			"/ccc <msg> or /clan council chat <msg>" + ChatColor.GRAY + " Talk to your council" };
	private String[] leaderCommands =
	{
			"/clan add council <player> -" + ChatColor.GRAY + " Add a user to council",
			"/clan remove council <player> -" + ChatColor.GRAY + " remove a user from council",
			"/clan set leader <player> -" + ChatColor.GRAY + " Set your clan's leader",
			"/clan war declare <clan> -" + ChatColor.GRAY + " Declare war against another clan",
			"/clan war end <clan> -" + ChatColor.GRAY + " End a war, either side can do this",
			"/clan set chat -" + ChatColor.GRAY + " Toggle clan chat",
			"/clan set tag <tag> -" + ChatColor.GRAY + " Set your clan's tag",
			"/clan set hall -" + ChatColor.GRAY + " Set your clan hall to your location",
			"/clan disband - " + ChatColor.GRAY + " Disband your clan" };
	private String[] opCommands =
	{
			"/clan create <name> -" + ChatColor.GRAY + " Create a new clan",
			"/clan admin <clan> -" + ChatColor.GRAY + " Administer clan you specify",
			"/clan reset -" + ChatColor.GRAY + " undoes /clan admin, resets you to your own clan." };

	private void showChatHelp(CommandSender sender)
	{
		sender.sendMessage(ChatColor.RED + "Invalid syntax: use /cc <msg> or /clan chat <msg>");
	}

	private void showCouncilChatHelp(CommandSender sender)
	{
		sender.sendMessage(ChatColor.RED + "Invalid syntax: use /ccc <msg> or /clan council chat <msg>");
	}

	private void listen(CommandSender sender)
	{
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			Clan clan = getClan(player);
			if (clan == null)
			{
				sender.sendMessage(ChatColor.RED + "You are not in a clan.");
				return;
			}
			listen(sender, clan);
		}
		else
		{
			sender.sendMessage("That doesn't work here.");
		}
	}

	private void listen(CommandSender sender, Clan clan)
	{
		Player player = (Player) sender;
		if (clan.isOnline(player))
		{
			clan.quit(player);
			player.sendMessage(ChatColor.RED + clan.getName() + ": " + ChatColor.YELLOW + "ignoring clan chat");
		}
		else
		{
			clan.joined(player);
			player.sendMessage(ChatColor.RED + clan.getName() + ": " + ChatColor.YELLOW + "listening to clan chat");
		}
	}

	private void clanList(CommandSender sender)
	{
		String message = "List of Clans: ";
		for (Clan clan : clans)
		{
			message += clan.getName() + " ";
		}
		sender.sendMessage(ChatColor.YELLOW + message);
	}

	private void nearList(CommandSender sender)
	{
		if (!(sender instanceof Player))
		{
			sender.sendMessage("That doesn't work from here.");
			return;
		}
		Player player = (Player) sender;
		Player[] players = getServer().getOnlinePlayers();
		String message = "List of Nearby Players: ";
		Boolean found = false;
		for (Player p : players)
		{
			if (getDistance(player, p) <= 20 && !p.equals(player))
			{
				found = true;
				message += p.getDisplayName() + " ";
			}
		}
		if (found)
		{
			player.sendMessage(ChatColor.YELLOW + message + ChatColor.WHITE);
		}
		else
		{
			player.sendMessage(ChatColor.RED + "No players nearby.");
		}
	}

	private void create(CommandSender sender, String name)
	{
		if (!(sender instanceof Player))
		{
			sender.sendMessage("That doesn't work from here.");
			return;
		}
		if (!sender.isOp())
		{
			sender.sendMessage(ChatColor.RED + "Access denied.");
			return;
		}
		Player player = (Player) sender;
		if (name.equalsIgnoreCase("config"))
		{
			player.sendMessage(ChatColor.RED + "Invalid clan name.");
			return;
		}
		Clan clan = new Clan(this, name);
		if (getClan(sender) == null)
		{
			clan.addMember(player);
		}
		clans.add(clan);
		player.sendMessage(ChatColor.RED + clan.getName() + ": " + ChatColor.YELLOW + "Clan created");
	}

	private void stats(CommandSender sender)
	{
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			Clan clan = getClan(player);
			if (clan == null)
			{
				sender.sendMessage(ChatColor.RED + "You are not in a clan.");
				return;
			}
			stats(sender, clan);
		}
		else
		{
			sender.sendMessage("That doesn't work here.");
		}
	}

	private void disband(CommandSender sender)
	{
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			Clan clan = getClan(sender);
			if (!clan.isLeader(player))
			{
				sender.sendMessage(ChatColor.RED + "Access denied.");
				return;
			}
			sender.sendMessage(ChatColor.RED + "This cannot be undone, are you sure?");
			sender.sendMessage(ChatColor.RED + "type /clan disband yes to continue.");
		}
		else
		{
			sender.sendMessage("That doesn't work here.");
		}
	}

	private void disband(CommandSender sender, boolean confirm)
	{
		if (!confirm) { return; }
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			Clan clan = getClan(sender);
			clan.sendMessage(ChatColor.RED + "This clan has been disbanded by " + player.getDisplayName() + ChatColor.WHITE);
			log.info(player.getName() + " disbanded the clan " + clan.getName());
			clan.disband();
			clans.remove(clan);
		}
		else
		{
			sender.sendMessage("That doesn't work here.");
		}
	}

	private void stats(CommandSender sender, String name)
	{
		Clan clan = getClan(name);
		if (clan == null)
		{
			sender.sendMessage(ChatColor.RED + "Clan cannot be found.");
			return;
		}
		stats(sender, clan);
	}

	private void stats(CommandSender sender, Clan clan)
	{
		clan.sendStats(sender);
	}

	private void warStats(CommandSender sender, String name)
	{
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			Clan yourClan = getClan(player);
			if (yourClan == null)
			{
				sender.sendMessage(ChatColor.RED + "You are not in a clan.");
				return;
			}
			Clan opposingClan = getClan(name);
			if (opposingClan == null)
			{
				sender.sendMessage(ChatColor.RED + "Clan cannot be found.");
				return;
			}
			warStats(sender, yourClan, opposingClan);
		}
		else
		{
			sender.sendMessage("That doesn't work here.");
		}
	}

	private void warStats(CommandSender sender, Clan yourClan, Clan opposingClan)
	{
		yourClan.sendWarStats(sender, opposingClan);
	}

	private void getMembers(CommandSender sender)
	{
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			Clan clan = getClan(player);
			if (clan == null)
			{
				sender.sendMessage(ChatColor.RED + "You are not in a clan.");
				return;
			}
			getMembers(sender, clan);
		}
		else
		{
			sender.sendMessage("That doesn't work here.");
		}
	}

	private void getMembers(CommandSender sender, String name)
	{
		Clan clan = getClan(name);
		if (clan == null)
		{
			sender.sendMessage(ChatColor.RED + "Clan cannot be found.");
			return;
		}
		getMembers(sender, clan);
	}

	private void getMembers(CommandSender sender, Clan clan)
	{
		String message = "List of members: ";
		List<String> members = clan.getMembers();
		int i = 0;
		for (String member : members)
		{
			message += member + " ";
			i++;
		}
		sender.sendMessage(ChatColor.RED + clan.getName() + ": " + ChatColor.YELLOW + message);
	}

	private void getCouncil(CommandSender sender)
	{
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			Clan clan = getClan(player);
			if (clan == null)
			{
				sender.sendMessage(ChatColor.RED + "You are not in a clan.");
				return;
			}
			getCouncil(sender, clan);
		}
		else
		{
			sender.sendMessage("That doesn't work here.");
		}
	}

	private void getCouncil(CommandSender sender, String name)
	{
		Clan clan = getClan(name);
		if (clan == null)
		{
			sender.sendMessage(ChatColor.RED + "Clan cannot be found.");
			return;
		}
		getCouncil(sender, clan);
	}

	private void getCouncil(CommandSender sender, Clan clan)
	{
		String message = "List of clan council: ";
		for (String member : clan.getCouncil())
		{
			message += member + ", ";
		}
		sender.sendMessage(ChatColor.RED + clan.getName() + ": " + ChatColor.YELLOW + message);
	}

	private void getLeader(CommandSender sender)
	{
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			Clan clan = getClan(player);
			if (clan == null)
			{
				sender.sendMessage(ChatColor.RED + "You are not in a clan.");
				return;
			}
			getLeader(sender, clan);
		}
		else
		{
			sender.sendMessage("That doesn't work here.");
		}
	}

	private void getLeader(CommandSender sender, Clan clan)
	{
		sender.sendMessage(ChatColor.RED + clan.getName() + ": " + ChatColor.YELLOW + "Clan leader is " + clan.getLeaderPlayer());
	}

	private void setChat(CommandSender sender)
	{
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			Clan clan = getClan(player);
			if (clan == null)
			{
				sender.sendMessage(ChatColor.RED + "You are not in a clan.");
				return;
			}
			setChat(sender, clan);
		}
		else
		{
			sender.sendMessage("That doesn't work here.");
		}
	}

	private void setChat(CommandSender sender, Clan clan)
	{
		if (!clan.canModify(sender))
		{
			sender.sendMessage(ChatColor.RED + "Access denied.");
			return;
		}
		if (clan.toggleChat())
		{
			sender.sendMessage(ChatColor.RED + clan.getName() + ": " + ChatColor.YELLOW + "Chat is now enabled.");
		}
		else
		{
			sender.sendMessage(ChatColor.RED + clan.getName() + ": " + ChatColor.YELLOW + "Chat is now disabled.");
		}
	}

	private void setHall(CommandSender sender)
	{
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			Clan clan = getClan(player);
			if (clan == null)
			{
				sender.sendMessage(ChatColor.RED + "You are not in a clan.");
				return;
			}
			setHall(sender, clan);
		}
		else
		{
			sender.sendMessage("That doesn't work here.");
		}
	}

	private void setHall(CommandSender sender, Clan clan)
	{
		if (!(sender instanceof Player))
		{
			sender.sendMessage("That doesn't work from here.");
			return;
		}
		Player player = (Player) sender;
		if (!clan.canModify(player))
		{
			player.sendMessage(ChatColor.RED + "Access denied.");
			return;
		}
		clan.setHallLocation(player.getLocation());
		player.sendMessage(ChatColor.RED + clan.getName() + ": " + ChatColor.YELLOW + "Clan hall is now set to your current location.");
	}

	private void addMember(CommandSender sender, String playerName)
	{
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			Clan clan = getClan(player);
			if (clan == null)
			{
				sender.sendMessage(ChatColor.RED + "You are not in a clan.");
				return;
			}
			addMember(sender, clan, playerName);
		}
		else
		{
			sender.sendMessage("That doesn't work here.");
		}
	}

	private void addMember(CommandSender sender, Clan clan, String playerName)
	{
		if (!clan.canModify(sender))
		{
			sender.sendMessage(ChatColor.RED + "Access denied.");
			return;
		}
		List<Player> players = getServer().matchPlayer(playerName);
		if (players.size() == 1)
		{
			Player p = players.get(0);
			if (getClan(p, true) != null)
			{
				sender.sendMessage(ChatColor.RED + p.getDisplayName() + ChatColor.RED + " is already in a clan.");
				return;
			}
			if (clan.isMember(p))
			{
				sender.sendMessage(ChatColor.RED + p.getDisplayName() + ChatColor.RED + " is already a member.");
				return;
			}
			clan.addMember(p);
			clan.sendMessage(ChatColor.RED + clan.getName() + ": " + ChatColor.YELLOW + p.getDisplayName() + " was added.");
		}
		else
		{
			sender.sendMessage(ChatColor.RED + "Player cannot be found.");
		}
	}

	private void addCouncil(CommandSender sender, String playerName)
	{
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			Clan clan = getClan(player);
			if (clan == null)
			{
				sender.sendMessage(ChatColor.RED + "You are not in a clan.");
				return;
			}
			addCouncil(sender, clan, playerName);
		}
		else
		{
			sender.sendMessage("That doesn't work here.");
		}
	}

	private void addCouncil(CommandSender sender, Clan clan, String playerName)
	{
		if (!clan.canLeaderModify(sender))
		{
			sender.sendMessage(ChatColor.RED + "Access denied.");
			return;
		}
		List<Player> players = getServer().matchPlayer(playerName);
		if (players.size() == 1)
		{
			Player player = players.get(0);
			if (clan.isCouncil(player))
			{
				sender.sendMessage(ChatColor.RED + player.getDisplayName() + ChatColor.RED + " is already on the clan council.");
				return;
			}
			clan.addCouncil(player);
			clan.sendMessage(ChatColor.RED + clan.getName() + ": " + ChatColor.YELLOW + player.getDisplayName() + " was added to the clan council.");
		}
		else
		{
			sender.sendMessage(ChatColor.RED + "Player cannot be found.");
		}
	}

	private void removeMember(CommandSender sender, String playerName)
	{
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			Clan clan = getClan(player);
			if (clan == null)
			{
				sender.sendMessage(ChatColor.RED + "You are not in a clan.");
				return;
			}
			removeMember(sender, clan, playerName);
		}
		else
		{
			sender.sendMessage("That doesn't work here.");
		}
	}

	private void removeMember(CommandSender sender, Clan clan, String playerName)
	{
		if (!clan.canModify(sender))
		{
			sender.sendMessage(ChatColor.RED + "Access denied.");
			return;
		}
		List<Player> players = getServer().matchPlayer(playerName);
		if (players.size() == 1)
		{
			Player p = players.get(0);
			if (getClan(p, true) == null)
			{
				sender.sendMessage(ChatColor.RED + p.getDisplayName() + ChatColor.RED + " is not in a clan.");
				return;
			}
			if (!clan.isMember(p))
			{
				sender.sendMessage(ChatColor.RED + p.getDisplayName() + ChatColor.RED + " is not a member.");
				return;
			}
			if (clan.isCouncil(p))
			{
				sender.sendMessage(ChatColor.RED + p.getDisplayName() + ChatColor.RED + " is in council and cannot be removed.");
				return;
			}
			clan.removeMember(p);
		}
		else
		{
			sender.sendMessage(ChatColor.RED + "Player cannot be found.");
		}
	}

	private void removeCouncil(CommandSender sender, String playerName)
	{
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			Clan clan = getClan(player);
			if (clan == null)
			{
				sender.sendMessage(ChatColor.RED + "You are not in a clan.");
				return;
			}
			removeCouncil(sender, clan, playerName);
		}
		else
		{
			sender.sendMessage("That doesn't work here.");
		}
	}

	private void removeCouncil(CommandSender sender, Clan clan, String playerName)
	{
		if (!clan.canLeaderModify(sender))
		{
			sender.sendMessage(ChatColor.RED + "Access denied.");
			return;
		}
		List<Player> players = getServer().matchPlayer(playerName);
		if (players.size() == 1)
		{
			Player player = players.get(0);
			if (!clan.isCouncil(player))
			{
				sender.sendMessage(ChatColor.RED + player.getDisplayName() + ChatColor.RED + " is not on the clan council.");
				return;
			}
			if (clan.isLeader(player))
			{
				sender.sendMessage(ChatColor.RED + player.getDisplayName() + ChatColor.RED + " is the clan leader and cannot be removed.");
				return;
			}
			clan.removeCouncil(player);
		}
		else
		{
			sender.sendMessage(ChatColor.RED + "Player cannot be found.");
		}
	}

	private void setTag(CommandSender sender, String tag)
	{
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			Clan clan = getClan(player);
			if (clan == null)
			{
				sender.sendMessage(ChatColor.RED + "You are not in a clan.");
				return;
			}
			setTag(sender, clan, tag);
		}
		else
		{
			sender.sendMessage("That doesn't work here.");
		}
	}

	private void setTag(CommandSender sender, Clan clan, String tag)
	{
		if (!clan.canModify(sender))
		{
			sender.sendMessage(ChatColor.RED + "Access denied.");
			return;
		}
		clan.setTag(tag);
		sender.sendMessage(ChatColor.RED + clan.getName() + ": " + ChatColor.YELLOW + "Tag set to " + tag + ".");
	}

	private void setLeader(CommandSender sender, String playerName)
	{
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			Clan clan = getClan(player);
			if (clan == null)
			{
				sender.sendMessage(ChatColor.RED + "You are not in a clan.");
				return;
			}
			setLeader(sender, clan, playerName);
		}
		else
		{
			sender.sendMessage("That doesn't work here.");
		}
	}

	private void setLeader(CommandSender sender, Clan clan, String playerName)
	{
		if (!clan.canLeaderModify(sender))
		{
			sender.sendMessage(ChatColor.RED + "Access denied.");
			return;
		}
		Player player = getServer().getPlayer(playerName);
		if (player == null)
		{
			sender.sendMessage(ChatColor.RED + "Player cannot be found.");
			return;
		}
		if (!clan.isCouncil(player))
		{
			sender.sendMessage(ChatColor.RED + player.getDisplayName() + ChatColor.RED + " is not on the clan council and cannot be promoted.");
			return;
		}
		clan.setLeader(player);
		sender.sendMessage(ChatColor.RED + clan.getName() + ": " + ChatColor.YELLOW + "Leader set to " + player.getDisplayName() + ".");
	}

	private void warDeclare(CommandSender sender, String name)
	{
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			Clan declaringClan = getClan(player);
			if (declaringClan == null)
			{
				sender.sendMessage(ChatColor.RED + "You are not in a clan.");
				return;
			}
			Clan opposingClan = getClan(name);
			if (opposingClan == null)
			{
				sender.sendMessage(ChatColor.RED + "Clan does not exist.");
			}
			warDeclare(player, declaringClan, opposingClan);
		}
		else
		{
			sender.sendMessage("That doesn't work here.");
		}
	}

	private void warDeclare(Player player, Clan declaringClan, Clan opposingClan)
	{
		if (!declaringClan.canLeaderModify(player))
		{
			player.sendMessage(ChatColor.RED + "Access denied.");
			return;
		}
		declaringClan.warDeclare(player, opposingClan);
	}

	private void warEnd(CommandSender sender, String name)
	{
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			Clan declaringClan = getClan(player);
			if (declaringClan == null)
			{
				sender.sendMessage(ChatColor.RED + "You are not in a clan.");
				return;
			}
			Clan opposingClan = getClan(name);
			if (opposingClan == null)
			{
				sender.sendMessage(ChatColor.RED + "Clan does not exist.");
			}
			warEnd(player, declaringClan, opposingClan);
		}
		else
		{
			sender.sendMessage("That doesn't work here.");
		}
	}

	private void warEnd(Player player, Clan declaringClan, Clan opposingClan)
	{

		if (!declaringClan.isLeader(player))
		{
			player.sendMessage(ChatColor.RED + "Access denied.");
			return;
		}
		declaringClan.warEnd(player, opposingClan);
	}

	private void clanHall(CommandSender sender)
	{
		Player player = (Player) sender;
		Clan clan = getClan(player);
		if (clan == null)
		{
			sender.sendMessage(ChatColor.RED + "Clan cannot be found.");
			return;
		}
		clanHall(sender, clan);
	}

	private void clanHall(CommandSender sender, Clan clan)
	{
		if (!(sender instanceof Player))
		{
			sender.sendMessage("That doesn't work from here.");
			return;
		}
		Player player = (Player) sender;
		clan.sendPlayerToHall(player);
	}

	private void admin(CommandSender sender, String clanName)
	{
		Clan clan = getClan(clanName);
		if (clan == null)
		{
			sender.sendMessage(ChatColor.RED + "Clan cannot be found.");
			return;
		}
		if (sender instanceof ConsoleCommandSender)
		{
			consoleClan = clan;
			return;
		}
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			if (adminClans.get(player) != null)
			{
				adminClans.remove(player);
			}
			adminClans.put(player, clan);
			return;
		}
	}

	private void reset(CommandSender sender)
	{
		if (sender instanceof ConsoleCommandSender)
		{
			consoleClan = null;
			return;
		}
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			if (adminClans.get(player) != null)
			{
				adminClans.remove(player);
			}
		}
	}

	/**
	 * Get's a clan by it's name
	 * 
	 * @param name
	 * @return Clan
	 */
	public Clan getClan(String name)
	{
		for (Clan c : clans)
		{
			if (c.getName().equalsIgnoreCase(name)) { return c; }
		}
		return null;
	}

	/**
	 * Get's a clan by a player
	 * 
	 * @param player
	 * @return Clan
	 */
	public Clan getClan(CommandSender sender)
	{
		return getClan(sender, false);
	}

	public Clan getClan(CommandSender sender, Boolean noAdmins)
	{
		if ((!noAdmins) && (sender instanceof ConsoleCommandSender)) { return consoleClan; }
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			if ((!noAdmins) && (adminClans.get(player) != null)) { return adminClans.get(player); }
			for (Clan c : clans)
			{
				if (c.isMember(player)) { return c; }
			}
		}
		return null;
	}

	public double getDistance(Player player1, Player player2)
	{
		Location loc1 = player1.getLocation();
		Location loc2 = player1.getLocation();
		return Math.sqrt(Math.pow(loc1.getX() - loc2.getX(), 2) + Math.pow(loc1.getY() - loc2.getY(), 2) + Math.pow(loc1.getZ() - loc2.getZ(), 2));
	}

	private Boolean chat(CommandSender sender, String[] messageParts)
	{
		if (!(sender instanceof Player))
		{
			sender.sendMessage("That doesn't work from here.");
			return true;
		}
		Player player = (Player) sender;
		Clan clan = getClan(player);
		if (clan == null)
		{
			player.sendMessage(ChatColor.RED + "You are not in a clan.");
			return true;
		}
		if (!clan.canChat())
		{
			player.sendMessage(ChatColor.RED + "Chat is disabled for your clan.");
			return true;
		}
		String message = "";
		for (String part : messageParts)
		{
			message += part + " ";
		}
		clan.chat(player, message);
		return true;
	}

	private Boolean councilChat(CommandSender sender, String[] messageParts)
	{
		if (!(sender instanceof Player))
		{
			sender.sendMessage("That doesn't work from here.");
			return true;
		}
		Player player = (Player) sender;
		Clan clan = getClan(player);
		if (clan == null)
		{
			player.sendMessage(ChatColor.RED + "You are not in a clan.");
			return true;
		}
		if (!clan.canChat())
		{
			player.sendMessage(ChatColor.RED + "Chat is disabled for your clan.");
			return true;
		}
		if (!clan.isCouncil(player))
		{
			player.sendMessage(ChatColor.RED + "You are not on council.");
			return true;
		}
		String message = "";
		for (String part : messageParts)
		{
			message += part + " ";
		}
		clan.councilChat(player, message);
		return true;
	}

	public void playerJoined(Player player)
	{
		for (Clan clan : clans)
		{
			clan.joined(player);
		}
	}

	public void playerQuit(Player player)
	{
		for (Clan clan : clans)
		{
			clan.quit(player);
		}
	}

	public void reportPVPDeath(Player winner, Player loser, String cause)
	{
		Clan winnerClan = getClan(winner, true);
		Clan loserClan = getClan(loser, true);
		if ((winnerClan == null) && (loserClan != null))
		{
			loserClan.deathNonClanLoser(winner, loser, cause);
		}
		else if ((winnerClan != null) && (loserClan == null))
		{
			winnerClan.deathNonClanWinner(winner, loser, cause);
		}
		else if ((winnerClan != null) && (loserClan != null))
		{
			if (winnerClan.equals(loserClan))
			{
				winnerClan.deathSameClan(winner, loser, cause);
			}
			else
			{
				winnerClan.deathClanWinner(winner, loser, cause, loserClan);
			}
		}
	}
}