package com.echo28.bukkit.clans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;


public class ClanEntityListener extends EntityListener
{
	private final Clans plugin;
	private HashMap<String, List<String>> possibleDeaths = new HashMap<String, List<String>>();

	public ClanEntityListener(Clans instance)
	{
		plugin = instance;
	}

	@Override
	public void onEntityDamage(EntityDamageEvent event)
	{
		if (event instanceof EntityDamageByEntityEvent)
		{
			EntityDamageByEntityEvent sub = (EntityDamageByEntityEvent) event;
			if (sub.isCancelled()) { return; }
			Entity damager = sub.getDamager();
			Entity damagee = sub.getEntity();
			if ((damager instanceof Player) && (damagee instanceof Player))
			{
				// this is PVP
				Player winner = (Player) damager;
				Player loser = (Player) damagee;
				if ((loser.getHealth() > 0) && ((loser.getHealth() - sub.getDamage()) <= 0))
				{
					// the loser has died because of this damage
					List<String> details = new ArrayList<String>();
					details.add(winner.getName());
					details.add(sub.getCause().toString());
					possibleDeaths.put(loser.getName(), details);
					Timer timer = new Timer();
					timer.schedule(new RemovePlayerTimerTask(loser.getName()), 5000);
				}
			}
		}
	}

	public void onEntityDeath(EntityDeathEvent event)
	{
		Entity entity = event.getEntity();
		if (entity instanceof Player)
		{
			Player deceased = (Player) entity;
			if (possibleDeaths.get(deceased.getName()) != null)
			{
				List<String> details = possibleDeaths.get(deceased.getName());
				plugin.reportPVPDeath(plugin.getServer().getPlayer(details.get(0)), deceased, details.get(1));
			}

		}
	}

	public class RemovePlayerTimerTask extends TimerTask
	{
		private String name;

		public RemovePlayerTimerTask(String name)
		{
			this.name = name;
		}

		public void run()
		{
			possibleDeaths.remove(this.name);
		}
	}
}
