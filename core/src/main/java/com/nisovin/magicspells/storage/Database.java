package com.nisovin.magicspells.storage;

import java.sql.Connection;
import java.sql.SQLException;

import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;

public abstract class Database extends StorageHandler {

	protected Connection connection;

	protected String dbLocation;

	public Database(MagicSpells plugin, String dbLocation) {
		super(plugin);
		this.dbLocation = dbLocation;
	}

	public abstract Connection openConnection();

	public abstract void createTables();

	public void closeConnection() {
		try {
			if (connection != null && !connection.isClosed()) connection.close();
		} catch (SQLException ignored) { }

		connection = null;
	}

	public Connection getConnection() {
		if (connection == null) connection = openConnection();
		return connection;
	}

	@Override
	public void initialize() {
		openConnection();
		createTables();
	}

	@Override
	public void load(Spellbook spellbook) {

	}

	@Override
	public void save(Spellbook spellbook) {

	}

	@Override
	public void disable() {
		closeConnection();
	}

}
