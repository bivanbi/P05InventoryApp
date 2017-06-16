package com.example.android.p05inventoryapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.p05inventoryapp.data.ProductContract.ProductEntry;
import com.example.android.p05inventoryapp.data.ProductFileHelper;

import java.io.File;
import java.text.NumberFormat;

/**
 * Created by bivanbi on 2017.06.09
 * Udacity - Android Basics Nanodegree
 * 5. Android Basics: Data Storage
 * Project: Inventory App
 * <p>
 * Balazs Lengyak balazs.lengyak@gmail.com
 * <p>
 * {@link ProductCursorAdapter} is an adapter for a list or grid view
 * that uses a {@link Cursor} of product data as its data source. This adapter knows
 * how to create list items for each row of product data in the {@link Cursor}.
 */

class ProductCursorAdapter extends CursorAdapter {

    /**
     * constructor to {@link ProductCursorAdapter}
     *
     * @param context is the calling activity's context
     * @param cursor  is the cursor to products
     */
    ProductCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0 /* flags */);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.product_list_item, parent, false);
    }

    /**
     * This method binds the product data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current product can be set on the name
     * TextView in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        ImageView imageView = (ImageView) view.findViewById(R.id.product_list_item_image);
        TextView nameView = (TextView) view.findViewById(R.id.product_list_item_name);
        TextView priceView = (TextView) view.findViewById(R.id.product_list_item_price);
        TextView quantityAvailableView = (TextView) view.findViewById(R.id.product_list_item_quantity_available);
        Button sellButton = (Button) view.findViewById(R.id.product_list_item_sell_button);
        TextView inStockView = (TextView) view.findViewById(R.id.product_in_stock_text);

        int idColumnIndex = cursor.getColumnIndex(ProductEntry._ID);
        int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
        int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
        int quantityAvailableColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY_AVAILABLE);

        final long productId = cursor.getLong(idColumnIndex);
        final int quantityAvailable = cursor.getInt(quantityAvailableColumnIndex);

        double price = cursor.getDouble(priceColumnIndex);

        File productImageFile = ProductFileHelper.getProductImageFile(context, productId);
        if (productImageFile != null) {
            imageView.setImageDrawable(null);
            imageView.setImageURI(Uri.fromFile(productImageFile));
        }
        nameView.setText(cursor.getString(nameColumnIndex));
        priceView.setText(NumberFormat.getCurrencyInstance().format(price));

        if (quantityAvailable < 1) {
            //  if product is out of stock, set views and sell button accordingly
            sellButton.setEnabled(false);
            inStockView.setText(context.getResources().getString(R.string.out_of_stock));
            quantityAvailableView.setVisibility(View.GONE);
        } else {
            //  if product is in stock, set views and sell button accordingly
            sellButton.setEnabled(true);
            quantityAvailableView.setText(cursor.getString(quantityAvailableColumnIndex));
            inStockView.setText(context.getResources().getString(R.string.in_stock));
            quantityAvailableView.setVisibility(View.VISIBLE);
            sellButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sellItem(context, productId, quantityAvailable);
                }
            });
        }
    }

    /**
     * method to decrease quantity if it is a positive integer and save to database
     *
     * @param context           is the calling context
     * @param productId         is the ID of the product to be sold
     * @param quantityAvailable is the currentyl available quantity
     * @return true on success
     */
    private boolean sellItem(Context context, long productId, int quantityAvailable) {

        Resources resources = context.getResources();

        //  if product is out of stock, let the user know and return false
        if (quantityAvailable < 1) {
            Toast.makeText(context, resources.getString(R.string.out_of_stock), Toast.LENGTH_SHORT).show();
            return false;
        }

        Uri productUri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, productId);
        ContentValues updateData = new ContentValues();
        updateData.put(ProductEntry.COLUMN_PRODUCT_QUANTITY_AVAILABLE, quantityAvailable - 1);

        //  try to save quantity decreased by one
        boolean result = false;
        String toastMessage;
        try {
            int rowsAffected = context.getContentResolver().update(productUri, updateData, null, null);
            if (rowsAffected > 0) {
                toastMessage = resources.getString(R.string.product_sold_successfully);
                result = true;
            } else {
                toastMessage = resources.getString(R.string.db_update_product_update_failed);
            }
        } catch (IllegalArgumentException e) {
            toastMessage = e.getMessage();
        }

        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show();

        return result;
    }
}
