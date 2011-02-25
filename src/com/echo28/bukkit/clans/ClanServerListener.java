package com.echo28.bukkit.clans;

import org.bukkit.event.server.PluginEvent;
import org.bukkit.event.server.ServerListener;

import com.nijikokun.bukkit.Permissions.Permissions;


public class ClanServerListener extends ServerListener
{
	private Clans plugin;

	public ClanServerListener(Clans plugin)
	{
		this.plugin = plugin;
	}

	public void onPluginEnabled(PluginEvent event)
	{
		if (event.getPlugin().getDescription().getName().equalsIgnoreCase("permissions"))
		{
			plugin.perm = ((Permissions) event.getPlugin()).getHandler();
		}
	}
}
