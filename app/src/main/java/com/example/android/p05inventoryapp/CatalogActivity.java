package com.example.android.p05inventoryapp;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.android.p05inventoryapp.data.ProductContract.ProductEntry;
import com.example.android.p05inventoryapp.data.ProductFileHelper;

/**
 * Created by bivanbi on 2017.06.09
 * Udacity - Android Basics Nanodegree
 * 5. Android Basics: Data Storage
 * Project: Inventory App
 * <p>
 * Balazs Lengyak balazs.lengyak@gmail.com
 * <p>
 * App which allows a store to keep track of its inventory of products. The app stores information
 * about price, quantity available, supplier, and a picture of the product.
 * It allows the user to track sales and shipments and makes it easy for the user to order more
 * from the listed supplier.
 * <p>
 * Code is based on section 5. Data Storage Lesson 1-5 material by Udacity.
 * <p>
 * This is the "main" activity that is launched by default if the app is started, and will
 * show a list of products that are available (or probably out of stock).
 */

public class CatalogActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private final static int PRODUCT_LIST_LOADER_ID = 1;

    private ProductCursorAdapter productCursorAdapter = null;

    /**
     * onCreate method to set up activity
     *
     * @param savedInstanceState is the saved instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        LoaderManager loaderManager = getLoaderManager();

        //  assign onclicklistener to floating action button to add new product
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

        ListView productListView = (ListView) findViewById(R.id.list_view_pet);
        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_view);
        productListView.setEmptyView(emptyView);

        productCursorAdapter = new ProductCursorAdapter(this, null);
        productListView.setAdapter(productCursorAdapter);
        loaderManager.initLoader(PRODUCT_LIST_LOADER_ID, null, this);

        //  on item click listener to open EditorActivity whenever a list item is clicked
        productListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);
                intent.setData(ContentUris.withAppendedId(ProductEntry.CONTENT_URI, id));
                startActivity(intent);
            }
        });
    }

    /**
     * method to create overhead options mennu
     *
     * @param menu is the menu object
     * @return true to instruct Android to actually display the menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }

    /**
     * menu to handle menu item selection
     *
     * @param item is the MenuItem selected
     * @return true if we processed this menu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_delete_all_entries) {
            deleteAllProducts();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * method to delete all products from database
     */
    public void deleteAllProducts() {
        getContentResolver().delete(ProductEntry.CONTENT_URI, null, null);
        ProductFileHelper.deleteAllProductImages(this);
        Toast.makeText(this, getResources().getString(R.string.db_delete_all_products), Toast.LENGTH_SHORT).show();
    }

    /**
     * method to create a cursor loader to fetch data from database to be displayed
     *
     * @param id   is a unique loader selector not used here because there is only one loader
     * @param args arguments passed to oncreateloader
     * @return Loader object on success
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_QUANTITY_AVAILABLE,
                ProductEntry.COLUMN_PRODUCT_SUPPLIER,
                ProductEntry.COLUMN_PRODUCT_RESUPPLY_PHONE,
                ProductEntry.COLUMN_PRODUCT_RESUPPLY_URL
        };
        return new CursorLoader(this, ProductEntry.CONTENT_URI, projection, null, null, null);
    }

    /**
     * method to notify cursoradapter when loader is finished loading data from DB
     *
     * @param loader is the Loader object
     * @param data   is the cursor to DB query result
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        productCursorAdapter.changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        productCursorAdapter.changeCursor(null);
    }
}
