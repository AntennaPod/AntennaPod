package de.danoeh.antennapod.core;

import android.database.sqlite.SQLiteDatabase;

/**
 * Callbacks for the classes in the storage package of the core module.
 */
public interface StorageCallbacks {

    /**
     * Returns the current version of the database.
     *
     * @return The non-negative version number of the database.
     */
    public int getDatabaseVersion();

    /**
     * Upgrades the given database from an old version to a newer version.
     *
     * @param db         The database that is supposed to be upgraded.
     * @param oldVersion The old version of the database.
     * @param newVersion The version that the database is supposed to be upgraded to.
     */
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);


}
