package com.echo28.bukkit.clans;

import java.util.logging.Logger;

import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerListener;


public class ClanPlayerListener extends PlayerListener
{
	private final Clans plugin;
	@SuppressWarnings("unused")
	private final Logger log = Logger.getLogger("Minecraft");

	public ClanPlayerListener(Clans instance)
	{
		plugin = instance;
	}

	@Override
	public void onPlayerJoin(PlayerEvent event)
	{
		plugin.playerJoined(event.getPlayer());
	}

	@Override
	public void onPlayerQuit(PlayerEvent event)
	{
		plugin.playerQuit(event.getPlayer());
	}

}
