package com.example.android.p05inventoryapp.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.android.p05inventoryapp.data.ProductContract.ProductEntry;

/**
 * Created by bivanbi on 2017.06.09
 * Udacity - Android Basics Nanodegree
 * 5. Android Basics: Data Storage
 * Project: Inventory App
 * <p>
 * Balazs Lengyak balazs.lengyak@gmail.com
 * <p>
 * Class to implement SQLite DB interface functions.
 */

class ProductDbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "inventory.db";

    /**
     * constructor for ProductDbHelper
     *
     * @param context is the calling context
     */
    ProductDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * method to create SQLite Database
     *
     * @param db is the DB handler
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        String SQL_CREATE_PRODUCTS_TABLE = "CREATE TABLE " + ProductEntry.TABLE_NAME + " (" +
                ProductEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                ProductEntry.COLUMN_PRODUCT_NAME + " TEXT NOT NULL, " +
                ProductEntry.COLUMN_PRODUCT_PRICE + " INTEGER NOT NULL, " +
                ProductEntry.COLUMN_PRODUCT_QUANTITY_AVAILABLE + " INTEGER NOT NULL DEFAULT 0, " +
                ProductEntry.COLUMN_PRODUCT_SUPPLIER + " TEXT NOT NULL, " +
                ProductEntry.COLUMN_PRODUCT_RESUPPLY_PHONE + " TEXT, " +
                ProductEntry.COLUMN_PRODUCT_RESUPPLY_URL + " TEXT " +
                ");";

        db.execSQL(SQL_CREATE_PRODUCTS_TABLE);
    }

    /**
     * method to upgrade SQLite database
     *
     * @param db         is the DB handler
     * @param oldVersion is the old database version
     * @param newVersion is the new database version
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // we are at the initial version of the database, so onUpgrade should be empty for now
    }
}
