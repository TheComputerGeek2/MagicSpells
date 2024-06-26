package com.nisovin.magicspells.storage.databases;

import java.sql.*;

import java.io.File;
import java.io.IOException;

import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.storage.Database;

public class SQLiteDatabase extends Database {

	public SQLiteDatabase(MagicSpells plugin, String dbLocation) {
		super(plugin, dbLocation);
	}

	@Override
	public Connection openConnection() {
		File folder = plugin.getDataFolder();
		if (!folder.exists()) folder.mkdirs();

		File file = new File(folder, dbLocation);
		if (!(file.exists())) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				MagicSpells.error("There was an error with creating the SQLiteDatabase file: " + e.getMessage());
			}
		}

		Connection connection = null;
		try {
			Class.forName("org.sqlite.JDBC");
			connection = DriverManager.getConnection("jdbc:sqlite:" + folder.toPath() + "/" + dbLocation);
		} catch (Exception e) {
			MagicSpells.error("There was an error with creating a connection for the SQLiteDatabase: " + e.getMessage());
		}
		return connection;
	}

	@Override
	public void createTables() {
		Connection connection = getConnection();
		try {
			Statement statement = connection.createStatement();

			// playerData
			statement.execute("CREATE TABLE IF NOT EXISTS playerData ("
					+ "id INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ "playerID VARCHAR(256) NOT NULL UNIQUE"
					+ ");"
			);

			// spells
			statement.execute("CREATE TABLE IF NOT EXISTS spells (id INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ "internalName VARCHAR(256) NOT NULL,"
					+ "playerID VARCHAR(256) NOT NULL,"
					+ "worldName VARCHAR(256) NOT NULL,"
					+ "FOREIGN KEY (playerID) REFERENCES playerData(playerID));"
			);

			// binds
			statement.execute("CREATE TABLE IF NOT EXISTS binds (id INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ "playerID VARCHAR(256) NOT NULL,"
					+ "internalName VARCHAR(256) NOT NULL,"
					+ "worldName VARCHAR(256) NOT NULL,"
					+ "magicItem VARCHAR(256) NOT NULL,"
					+ "FOREIGN KEY (playerID) REFERENCES playerData(playerID));"
			);

		} catch (SQLException e) {
			MagicSpells.error("There was an error with creating a table for the SQLiteDatabase: " + e.getMessage());
		}
	}


	// LOAD FROM DATABASE
	@Override
	public void load(Spellbook spellbook) {
		/*Player pl = spellbook.getPlayer();
		String worldName = pl.getWorld().getName();
		String id = Util.getUniqueId(pl);

		String worldSeparate = "";

		if (MagicSpells.arePlayerSpellsSeparatedPerWorld()) {
			//worldSeparate = " and "
		}

		*/
	}

	// SAVE TO DATABASE
	@Override
	public void save(Spellbook spellbook) {
		/*Player pl = spellbook.getPlayer();
		String worldName = pl.getWorld().getName();
		String id = Util.getUniqueId(pl);


		String query = "";

		for (Spell spell : spellbook.getSpells()) {
			if (spellbook.isTemporary(spell)) continue;

		}

		if (MagicSpells.arePlayerSpellsSeparatedPerWorld()) {

		}*/
	}

}
