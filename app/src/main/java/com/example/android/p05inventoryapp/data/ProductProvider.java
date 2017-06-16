package com.example.android.p05inventoryapp.data;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.example.android.p05inventoryapp.R;
import com.example.android.p05inventoryapp.data.ProductContract.ProductEntry;

/**
 * Created by bivanbi on 2017.06.09
 * Udacity - Android Basics Nanodegree
 * 5. Android Basics: Data Storage
 * Project: Inventory App
 * <p>
 * Balazs Lengyak balazs.lengyak@gmail.com
 * <p>
 * Class to implement ContentProvider to be used for interacting with database and file storage
 */

public class ProductProvider extends ContentProvider {

    /** URI matcher code for the content URI for the products table */
    private static final int PRODUCTS = 100;

    /** URI matcher code for the content URI for a single pet in the products table */
    private static final int PRODUCT_ID = 101;

    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private ContentResolver contentResolver = null;

    // Static initializer. This is run the first time anything is called from this class.
    static {
        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a corresponding code to return
        // when a match is found.
        uriMatcher.addURI(ProductContract.CONTENT_AUTHORITY, ProductContract.PATH_PRODUCTS, PRODUCTS);
        uriMatcher.addURI(ProductContract.CONTENT_AUTHORITY, ProductContract.PATH_PRODUCTS + "/#", PRODUCT_ID);
    }

    private ProductDbHelper dbHelper = null;

    @Override
    public boolean onCreate() {
        dbHelper = new ProductDbHelper(getContext());
        Context context = getContext();
        if (context != null)
            contentResolver = getContext().getContentResolver();
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        // Get readable database
        SQLiteDatabase database = dbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = uriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                // For the PETS code, query the pets table directly with the given
                // projection, selection, selection arguments, and sort order. The cursor
                // could contain multiple rows of the pets table.
                cursor = database.query(ProductEntry.TABLE_NAME,null,null,null,null,null,null);
                cursor.setNotificationUri(contentResolver,uri);
                break;
            case PRODUCT_ID:
                // For the PET_ID code, extract out the ID from the URI.
                // For an example URI such as "content://com.example.android.pets/pets/3",
                // the selection will be "_id=?" and the selection argument will be a
                // String array containing the actual ID of 3 in this case.
                //
                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments' String array.
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };

                // This will perform a query on the pets table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                cursor = database.query(ProductEntry.TABLE_NAME, projection, selection,
                        selectionArgs, null, null, sortOrder);
                cursor.setNotificationUri(contentResolver,uri);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        int match = uriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return ProductEntry.CONTENT_LIST_TYPE;
            case PRODUCT_ID:
                return ProductEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("getType is not supported for " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        int match = uriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                Uri resultUri = insertProduct(uri, contentValues);
                contentResolver.notifyChange(resultUri, null);
                return resultUri;
            default:
                throw new IllegalArgumentException("Cannot insert unknown URI " + uri);
        }
    }

    public Uri insertProduct(Uri uri, ContentValues contentValues) {
        //  sanity check insert values
        if (!ProductEntry.isValidProduct(getContext(), contentValues, ProductEntry.PRODUCT_VALIDATION_MODE_INSERT, false)) {
            throw new IllegalArgumentException(getContext().getResources().getString(R.string.product_has_missing_or_invalid_arguments));
        }

        //  remove dummy field ProductEntry.DUMMY_COLUMN_PRODUCT_HAS_IMAGE which is only required
        //  to check input, will never get saved to database
        contentValues.remove(ProductEntry.DUMMY_COLUMN_PRODUCT_HAS_IMAGE);

        if (contentValues.getAsDouble(ProductEntry.COLUMN_PRODUCT_QUANTITY_AVAILABLE) == null) {
            contentValues.put(ProductEntry.COLUMN_PRODUCT_QUANTITY_AVAILABLE,0);
        }

        // Create and/or open a database to read from it
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = db.insert(ProductEntry.TABLE_NAME, null, contentValues);
        if (id < 0) {
            return null;
        } else {
            return ContentUris.withAppendedId(uri, id);
        }
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        int match = uriMatcher.match(uri);
        int numberOfRowsAffected;
        switch (match) {
            case PRODUCTS:
                numberOfRowsAffected = updateProduct(uri, contentValues, selection, selectionArgs);
                if (numberOfRowsAffected > 0) {
                    contentResolver.notifyChange(uri, null);
                }
                break;
            case PRODUCT_ID:
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                numberOfRowsAffected = updateProduct(uri, contentValues, selection, selectionArgs);
                if (numberOfRowsAffected > 0) {
                    contentResolver.notifyChange(uri, null);
                }
                break;
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
        return numberOfRowsAffected;
    }

    public int updateProduct(Uri uri, ContentValues contentValues, String selection,
                         String[] selectionArgs) {

        //  sanity check insert values
        if (!ProductEntry.isValidProduct(getContext(), contentValues, ProductEntry.PRODUCT_VALIDATION_MODE_UPDATE, false)) {
            throw new IllegalArgumentException("Pet have missing or invalid arguments");
        }

        //  remove dummy field ProductEntry.DUMMY_COLUMN_PRODUCT_HAS_IMAGE which is only required
        //  to check input, will never get saved to database
        contentValues.remove(ProductEntry.DUMMY_COLUMN_PRODUCT_HAS_IMAGE);

        if (contentValues.getAsDouble(ProductEntry.COLUMN_PRODUCT_QUANTITY_AVAILABLE) == null) {
            contentValues.put(ProductEntry.COLUMN_PRODUCT_QUANTITY_AVAILABLE,0);
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.update(ProductEntry.TABLE_NAME, contentValues, selection, selectionArgs);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        int match = uriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                int numberOfRowsAffected = deleteProduct(uri, selection, selectionArgs);
                if (numberOfRowsAffected > 0)
                    contentResolver.notifyChange(uri, null);
                return numberOfRowsAffected;

            case PRODUCT_ID:
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                numberOfRowsAffected = deleteProduct(uri, selection, selectionArgs);
                if (numberOfRowsAffected > 0)
                    contentResolver.notifyChange(uri, null);
                return numberOfRowsAffected;

            default:
                throw new IllegalArgumentException("Delete is not supported for " + uri);
        }
    }

    public int deleteProduct(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(ProductEntry.TABLE_NAME, selection, selectionArgs);
    }
}
