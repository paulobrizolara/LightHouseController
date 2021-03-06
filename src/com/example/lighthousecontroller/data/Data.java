package com.example.lighthousecontroller.data;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import com.example.lighthousecontroller.LightHouseControllerApplication;

public class Data {
	private static Data singleton;
	public static Data instance(){
		if(singleton == null){
			singleton = new Data();
		}
		return singleton;
	}
	
	private LightHouseControllerDatabase database;
	private LampDAO lampDAO;
	private ServerDAO serverDAO;
	
	public Data() {
		Context context=LightHouseControllerApplication.getApplication();
		database = new LightHouseControllerDatabase(context);
		lampDAO = new LampDAO(context, database);
		serverDAO = new ServerDAO(context, database);
		setupDatabase();
	}
	
	private final void setupDatabase() {
		List<Table> tables = createTables();
		for(Table table : tables){
			database.addTable(table);
		}
	}

	private List<Table> createTables() {
		List<Table> tables = new ArrayList<>();
		tables.addAll(lampDAO.getTables());
		tables.addAll(serverDAO.getTables());
		return tables;
	}

	public LampDAO getLampDAO(){
		return lampDAO;
	}
	public ServerDAO getServerDAO(){
		return serverDAO;
	}
}
