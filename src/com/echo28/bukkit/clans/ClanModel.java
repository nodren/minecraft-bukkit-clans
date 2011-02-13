package com.echo28.bukkit.clans;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ClanModel
{
	private Clans plugin;
	private final Logger log = Logger.getLogger("Minecraft");
	private Connection conn = null;

	public ClanModel(Clans plugin)
	{
		this.plugin = plugin;
	}

	public void load()
	{
		try
		{
			Class.forName("org.sqlite.JDBC");
			File db = new File(plugin.getDataFolder(), "stats.db");
			if (db.exists())
			{
				updateSchema();
			}
			else
			{
				// no db exists, let's make the file and automatically fill the first db version in
				db.createNewFile();
				// try a connection before we move on assuming we can
				openConn();
				closeConn();
				updateSchema(1);
				updateSchema();
			}
		}
		catch (ClassNotFoundException e)
		{
			log.log(Level.SEVERE, "Could not find SQLite jar in plugins/Clans/ folder: " + e.getMessage(), e);
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Could not create database in plugins/Clans/ folder: " + e.getMessage(), e);
		}
	}

	private synchronized Connection conn()
	{
		return conn;
	}

	private synchronized void openConn()
	{
		try
		{
			conn = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + File.separator + "stats.db");
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "Could not connect to SQLite: " + e.getMessage(), e);
		}
	}

	private synchronized void closeConn()
	{
		try
		{
			conn.close();
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "Could not disconnect from SQLite: " + e.getMessage(), e);
		}
	}

	public void updateSchema()
	{
		try
		{
			openConn();
			PreparedStatement prep = conn().prepareStatement("select * from schema where rowid = ?");
			prep.setInt(1, 1);
			ResultSet rs = prep.executeQuery();
			while (rs.next())
			{
				int i = rs.getInt("version");
				i++;
				while (updateSchema(i))
				{
					i++;
				}
			}
			rs.close();
			closeConn();
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "Could not communicate with SQLite: " + e.getMessage(), e);
		}
	}

	public boolean updateSchema(int version)
	{
		try
		{
			openConn();
			Statement stat = conn().createStatement();
			switch (version)
			{
			case 1:
				stat.executeUpdate("CREATE TABLE deaths (id INTEGER PRIMARY KEY, war_id NUMERIC, winner_name TEXT, winner_clan TEXT, loser_name TEXT, loser_clan TEXT, cause TEXT, date NUMERIC)");
				stat.executeUpdate("CREATE TABLE wars (id INTEGER PRIMARY KEY, declaring_clan TEXT, opposing_clan TEXT, start_date NUMERIC, end_date NUMERIC, ended_by TEXT, victor TEXT)");
				stat.executeUpdate("CREATE TABLE schema (id INTEGER PRIMARY KEY, version NUMERIC)");
				stat.executeUpdate("INSERT INTO schema VALUES(1, 1);");
				closeConn();
				return true;
			case 5:
				stat.executeUpdate("UPDATE schema SET version='5' WHERE rowid=1");
				closeConn();
				return true;
			}
			closeConn();
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "Could not communicate with SQLite: " + e.getMessage(), e);
		}
		return false;
	}

	public void storeDeath(String winnerName, String winnerClan, String loserName, String loserClan, String cause)
	{
		storeDeath(0, winnerName, winnerClan, loserName, loserClan, cause);
	}

	public void storeDeath(int warId, String winnerName, String winnerClan, String loserName, String loserClan, String cause)
	{
		try
		{
			openConn();
			PreparedStatement prep = conn().prepareStatement("INSERT INTO deaths VALUES(NULL, ?, ?, ?, ?, ?, ?, ?)");
			prep.setInt(1, warId);
			prep.setString(2, winnerName);
			prep.setString(3, winnerClan);
			prep.setString(4, loserName);
			prep.setString(5, loserClan);
			prep.setString(6, cause);
			prep.setLong(7, new Date().getTime());
			prep.executeUpdate();
			closeConn();
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "Could not communicate with SQLite: " + e.getMessage(), e);
		}
	}

	public int startWar(String declaringClan, String opposingClan)
	{
		try
		{
			openConn();
			PreparedStatement prep = conn().prepareStatement("INSERT INTO wars VALUES(NULL, ?, ?, ?, ?, ?, ?)");
			prep.setString(1, declaringClan);
			prep.setString(2, opposingClan);
			prep.setLong(3, new Date().getTime());
			prep.setInt(4, 0);
			prep.setString(5, "");
			prep.setString(5, "");
			int id = prep.executeUpdate();
			closeConn();
			return id;
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "Could not communicate with SQLite: " + e.getMessage(), e);
		}
		return 0;
	}

	public void endWar(int id, String endingClan, String victor)
	{
		try
		{
			openConn();
			PreparedStatement prep = conn().prepareStatement("UPDATE wars SET end_date = ?, ended_by = ?, victor = ? WHERE rowid = ?");
			prep.setLong(1, new Date().getTime());
			prep.setString(2, endingClan);
			prep.setString(3, victor);
			prep.setInt(4, id);
			prep.executeUpdate();
			closeConn();
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "Could not communicate with SQLite: " + e.getMessage(), e);
		}
	}

	public int warsWon(String declaringClan, String opposingClan, String victorClan)
	{
		int count = 0;
		count += countQuery("SELECT count(id) AS count FROM wars WHERE declaring_clan = ? AND opposing_clan = ? AND victor = ? AND end_date != 0", declaringClan, opposingClan,
				victorClan);
		count += countQuery("SELECT count(id) AS count FROM wars WHERE declaring_clan = ? AND opposing_clan = ? AND victor = ? AND end_date != 0", opposingClan, declaringClan,
				victorClan);
		return count;
	}

	public int warDeathCount(int id, String clan)
	{
		return countQuery("SELECT COUNT(id) AS count FROM deaths WHERE war_id = ? AND loser_clan = ?", id, clan);
	}

	public String warTime(int id)
	{
		try
		{
			openConn();
			PreparedStatement prep = conn().prepareStatement("SELECT start_date FROM wars WHERE rowid = ?");
			prep.setInt(1, id);
			ResultSet rs = prep.executeQuery();
			rs.next();
			long ret = rs.getLong("start_date");
			long range = Math.round((new Date().getTime() - ret) / 1000);
			log.info("time range: " + range);
			rs.close();
			closeConn();
			return timeRange(range);
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "Could not communicate with SQLite: " + e.getMessage(), e);
		}
		return null;
	}

	public String timeRange(long range)
	{
		long minute = 60;
		long hour = minute * 60;
		long day = hour * 24;
		long week = day * 7;
		long num = 0;
		String word = "";

		if (range < minute)
		{
			num = range;
			word = num > 1 ? "seconds" : "second";
		}
		else if (range < hour)
		{
			num = Math.round(range / minute);
			word = num > 1 ? "minutes" : "minute";
		}
		else if (range < day)
		{
			num = Math.round(range / hour);
			word = num > 1 ? "hours" : "hour";
		}
		else if (range < week)
		{
			num = Math.round(range / day);
			word = num > 1 ? "days" : "day";
		}
		else
		{
			num = Math.round(range / week);
			word = num > 1 ? "weeks" : "week";
		}
		return num + " " + word;
	}

	public int warsCount(String declaringClan, String opposingClan)
	{
		return countQuery("SELECT count(id) AS count FROM wars WHERE declaring_clan = ? AND opposing_clan = ?", declaringClan, opposingClan);
	}

	public int warsEnded(String declaringClan, String opposingClan, String endingClan)
	{
		int count = 0;
		count += countQuery("SELECT count(id) AS count FROM wars WHERE declaring_clan = ? AND opposing_clan = ? AND ended_by = ? AND end_date != 0", declaringClan, opposingClan,
				endingClan);
		count += countQuery("SELECT count(id) AS count FROM wars WHERE declaring_clan = ? AND opposing_clan = ? AND ended_by = ? AND end_date != 0", opposingClan, declaringClan,
				endingClan);
		return count;
	}

	public int getActiveWar(String declaringClan, String opposingClan)
	{
		int id = countQuery("SELECT id AS count FROM wars WHERE declaring_clan = ? AND opposing_clan = ? AND end_date = 0", declaringClan, opposingClan);
		if (id == 0) { return countQuery("SELECT id AS count FROM wars WHERE declaring_clan = ? AND opposing_clan = ? AND end_date = 0", opposingClan, declaringClan); }
		return id;
	}

	public String[] getWars(String clan)
	{
		try
		{
			openConn();
			PreparedStatement prep = conn().prepareStatement("SELECT * FROM wars WHERE (declaring_clan = ? OR opposing_clan = ?) AND end_date = 0");
			prep.setString(1, clan);
			prep.setString(2, clan);
			ResultSet rs = prep.executeQuery();
			List<String> wars = new ArrayList<String>();
			while (rs.next())
			{
				if (rs.getString("declaring_clan").equalsIgnoreCase(clan))
				{
					wars.add(rs.getString("opposing_clan"));
				}
				else
				{
					wars.add(rs.getString("declaring_clan"));
				}
			}
			String[] ret = new String[wars.size()];
			wars.toArray(ret);
			rs.close();
			closeConn();
			return ret;
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "Could not communicate with SQLite: " + e.getMessage(), e);
		}
		return null;
	}

	public int countQuery(String sql, String param)
	{
		try
		{
			openConn();
			PreparedStatement prep = conn().prepareStatement(sql);
			prep.setString(1, param);
			ResultSet rs = prep.executeQuery();
			int ret = 0;
			if (rs.next())
			{
				ret = rs.getInt("count");
			}
			rs.close();
			closeConn();
			return ret;
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "Could not communicate with SQLite: " + e.getMessage(), e);
		}
		return 0;
	}

	public int countQuery(String sql, int param1, String param2)
	{
		try
		{
			openConn();
			PreparedStatement prep = conn().prepareStatement(sql);
			prep.setInt(1, param1);
			prep.setString(2, param2);
			ResultSet rs = prep.executeQuery();
			int ret = 0;
			if (rs.next())
			{
				ret = rs.getInt("count");
			}
			rs.close();
			closeConn();
			return ret;
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "Could not communicate with SQLite: " + e.getMessage(), e);
		}
		return 0;
	}

	public int countQuery(String sql, String param1, String param2)
	{
		try
		{
			openConn();
			PreparedStatement prep = conn().prepareStatement(sql);
			prep.setString(1, param1);
			prep.setString(2, param2);
			ResultSet rs = prep.executeQuery();
			int ret = 0;
			if (rs.next())
			{
				ret = rs.getInt("count");
			}
			rs.close();
			closeConn();
			return ret;
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "Could not communicate with SQLite: " + e.getMessage(), e);
		}
		return 0;
	}

	public int countQuery(String sql, String param1, String param2, String param3)
	{
		try
		{
			openConn();
			PreparedStatement prep = conn().prepareStatement(sql);
			prep.setString(1, param1);
			prep.setString(2, param2);
			prep.setString(3, param3);
			ResultSet rs = prep.executeQuery();
			int ret = 0;
			if (rs.next())
			{
				ret = rs.getInt("count");
			}
			rs.close();
			closeConn();
			return ret;
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "Could not communicate with SQLite: " + e.getMessage(), e);
		}
		return 0;
	}

	public int victoriesOverRivalClans(String clan)
	{
		return countQuery("SELECT COUNT(id) AS count FROM deaths WHERE winner_clan = ? AND loser_clan IS NOT null AND winner_clan != loser_clan", clan);
	}

	public int victoriesOverRivalClan(String clan, String rivalClan)
	{
		return countQuery("SELECT COUNT(id) AS count FROM deaths WHERE winner_clan = ? AND loser_clan = ?", clan, rivalClan);
	}

	public int victoriesOverNonClans(String clan)
	{
		return countQuery("SELECT COUNT(id) AS count FROM deaths WHERE winner_clan = ? AND loser_clan IS null", clan);
	}

	public int lossesOverRivalClans(String clan)
	{
		return countQuery("SELECT COUNT(id) AS count FROM deaths WHERE winner_clan IS NOT null AND loser_clan = ? AND winner_clan != loser_clan", clan);
	}

	public int lossesOverRivalClan(String clan, String rivalClan)
	{
		return countQuery("SELECT COUNT(id) AS count FROM deaths WHERE winner_clan = ? AND loser_clan = ?", rivalClan, clan);
	}

	public int lossesOverNonClans(String clan)
	{
		return countQuery("SELECT COUNT(id) AS count FROM deaths WHERE winner_clan IS null AND loser_clan = ?", clan);
	}

	public int sameClanSparring(String clan)
	{
		return countQuery("SELECT COUNT(id) AS count FROM deaths WHERE winner_clan = ? AND loser_clan = ?", clan, clan);
	}
}
