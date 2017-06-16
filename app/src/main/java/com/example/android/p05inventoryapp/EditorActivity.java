package com.example.android.p05inventoryapp;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.android.p05inventoryapp.data.ProductContract.ProductEntry;
import com.example.android.p05inventoryapp.data.ProductFileHelper;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by bivanbi on 2017.06.09
 * Udacity - Android Basics Nanodegree
 * 5. Android Basics: Data Storage
 * Project: Inventory App
 * <p>
 * Balazs Lengyak balazs.lengyak@gmail.com
 * <p>
 * Activity to add / edit product data, provide user with buttons to call resupply phone number,
 * open resupply URL, increase / decrease quantity available
 */

public class EditorActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    // ID for getPickImageIntent. This ID will be returned as requestCode when onActivityResult is
    // called. The actual number does not matter, just make sure it is unique so
    // your onActivityResult will know *what* activity sent the actual result.
    private static final int PICK_IMAGE_REQUEST_CODE = 1;

    //  this key will be used by onSaveInstance / onRestoreInstance to store hasTemporaryProductImage
    private static final String PRODUCT_TEMPORARY_IMAGE_STATE_KEY = "has_temporary_image";
    //  loader identifier for loading product
    private final static int SINGLE_PRODUCT_LOADER_ID = 2;
    //  if we are editing an existing product, currentProductUri will hold an Uri for that product
    private Uri currentProductUri = null;
    //  whenever user interacts with editor inputs, productHasChanged will be set to true so
    //  user can be notified of unsaved changes upon leaving this activity
    private boolean productHasChanged = false;
    private ImageView productImageView;
    private EditText nameEditText;
    private EditText priceEditText;
    private EditText quantityAvailableEditText;
    private EditText supplierNameEditText;
    private EditText resupplyPhoneEditText;
    private EditText resupplyUrlEdittext;

    //  temporary product image
    private File productTemporaryImageFile = null;
    private boolean hasTemporaryProductImage = false;

    //  to tell if an existing product already has an image so do not bother the user
    private boolean productHasImage = false;
    /**
     * method to handle product image touch - will prompt the user to select image from gallery
     * or use camera to take a new photo
     */
    private View.OnTouchListener onImageTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            Intent imagePickerIntent = ImagePicker.getPickImageIntent(getApplicationContext());
            startActivityForResult(imagePickerIntent, PICK_IMAGE_REQUEST_CODE);
            return false;
        }
    };
    /**
     * method to set productHasChanged if user touches any inputs. this is used to display
     * confirmation dialog if user is about to leave the activity without saving changes
     */
    private View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            productHasChanged = true;
            return false;
        }
    };
    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.edit_product_decrease_quantity:
                    decreaseProductQuantity();
                    break;
                case R.id.edit_product_increase_quantity:
                    increaseProductQuantity();
                    break;
                case R.id.edit_product_call_resupply_phone_button:
                    callResupplyPhone();
                    break;
                case R.id.edit_product_open_resupply_url_button:
                    openResupplyUrl();
                    break;
                default:
                    //  actually nothing to be done by default
            }
        }
    };

    /**
     * method to initialize activity on create
     *
     * @param savedInstanceState is the saved instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        LoaderManager loaderManager = getLoaderManager();

        // Find all relevant views that we will need to read user input from
        productImageView = (ImageView) findViewById(R.id.edit_product_image);
        nameEditText = (EditText) findViewById(R.id.edit_product_name);
        priceEditText = (EditText) findViewById(R.id.edit_product_price);
        quantityAvailableEditText = (EditText) findViewById(R.id.edit_product_quantity_in_stock);
        supplierNameEditText = (EditText) findViewById(R.id.edit_supplier_name);
        resupplyPhoneEditText = (EditText) findViewById(R.id.edit_resupply_phone);
        resupplyUrlEdittext = (EditText) findViewById(R.id.edit_resupply_url);

        //  buttons to open URL, place a call, increase or decrease quantity avialable
        ImageButton increaseQuantityButton = (ImageButton) findViewById(R.id.edit_product_increase_quantity);
        ImageButton decreaseQuantityButton = (ImageButton) findViewById(R.id.edit_product_decrease_quantity);
        ImageButton callResupplyPhoneButton = (ImageButton) findViewById(R.id.edit_product_call_resupply_phone_button);
        ImageButton openResupplyUrlButton = (ImageButton) findViewById(R.id.edit_product_open_resupply_url_button);

        //  when image is clicked, call and intent to choose new image for the product
        productImageView.setOnTouchListener(onImageTouchListener);

        //  when any of the inputs is touched, listener will set productHasChanged
        nameEditText.setOnTouchListener(onTouchListener);
        priceEditText.setOnTouchListener(onTouchListener);
        quantityAvailableEditText.setOnTouchListener(onTouchListener);
        supplierNameEditText.setOnTouchListener(onTouchListener);
        resupplyPhoneEditText.setOnTouchListener(onTouchListener);
        resupplyUrlEdittext.setOnTouchListener(onTouchListener);

        //  assign listener to take care of button actions
        increaseQuantityButton.setOnClickListener(onClickListener);
        decreaseQuantityButton.setOnClickListener(onClickListener);
        callResupplyPhoneButton.setOnClickListener(onClickListener);
        openResupplyUrlButton.setOnClickListener(onClickListener);

        Intent intent = getIntent();
        Uri productUri = intent.getData();

        if (productUri == null) {
            //  no product URI received with intent data, so this is a new product to be inserted
            //  optionsmenu with delete item etc. is invalidated as it does not apply to a new
            //  product
            setTitle(getResources().getString(R.string.editor_activity_title_new_product));
            invalidateOptionsMenu();
        } else {
            //  if we receive productUri with intent data, this means our activity will need to edit
            //  an existing product.
            setTitle(getResources().getString(R.string.editor_activity_title_edit_product));
            currentProductUri = productUri;

            //  if defined, assign product image from internal storage
            File productImageFile = ProductFileHelper.getProductImageFile(this, currentProductUri);
            if (productImageFile != null) {
                productHasImage = true;
                productImageView.setImageURI(Uri.fromFile(productImageFile));
            }

            //  initiate loader to get data from provider
            loaderManager.initLoader(SINGLE_PRODUCT_LOADER_ID, null, this);
        }
    }

    /**
     * method to save instance state when activity is about to be brought down.
     * most of the hard work about EditText fields etc. is done by Android itself, so we only
     * need to take care about temporary image state.
     *
     * @param outState is the bundle to store instance state data into
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(PRODUCT_TEMPORARY_IMAGE_STATE_KEY, hasTemporaryProductImage);
    }

    /**
     * method to restore instance state if we are returning to this activity from a saved state.
     * currently only temporary image needs to be loaded
     *
     * @param savedInstanceState is the saved instance state to be restored
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.containsKey(PRODUCT_TEMPORARY_IMAGE_STATE_KEY)) {
            hasTemporaryProductImage = savedInstanceState.getBoolean(PRODUCT_TEMPORARY_IMAGE_STATE_KEY);

            if (hasTemporaryProductImage) {
                productTemporaryImageFile = ProductFileHelper.getProductTemporaryImageFile(this);
                if (productTemporaryImageFile == null) {
                    hasTemporaryProductImage = false;
                    Toast.makeText(this,
                            getResources().getString(R.string.error_reading_temporary_image_file_on_instance_restoration),
                            Toast.LENGTH_LONG)
                            .show();
                } else {
                    productImageView.setImageURI(Uri.fromFile(productTemporaryImageFile));
                }
            }
        }
    }

    /**
     * Inflate the menu options from the res/menu/menu_editor.xml file.
     * This adds menu items to the app bar.
     *
     * @param menu is the menu object
     * @return true always
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new product, hide the "Delete" menu item.
        if (currentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    /**
     * if delete menu option has been selected, display
     * a confirmation dialog to prevent accidental data loss
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the product.
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * delete currently displayed product
     *
     * @return true on successful deletion
     */
    public boolean deleteProduct() {
        //  only delete in edit mode
        if (currentProductUri == null) {
            return false;
        }

        String toastMessage;
        boolean result = false;

        int rowsAffected = getContentResolver().delete(currentProductUri, null, null);

        if (rowsAffected > 0) {
            toastMessage = getResources().getString(R.string.editor_delete_product_successful);
            //  delete image too
            ProductFileHelper.deleteProductImage(this, currentProductUri);
            result = true;
        } else {
            toastMessage = getResources().getString(R.string.editor_delete_product_failed);
        }

        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
        if (result) {
            finish();
        }
        return result;
    }

    /**
     * method to save new or update existing product depending on currentProductUri
     *
     * @return true if successful operation
     */
    public boolean saveProduct() {
        ContentValues insertData = new ContentValues();

        //  this column will never be saved to database, this is only required to check if
        //  product has all the required attributes before saving to database
        insertData.put(ProductEntry.DUMMY_COLUMN_PRODUCT_HAS_IMAGE,(productHasImage || hasTemporaryProductImage));
        insertData.put(ProductEntry.COLUMN_PRODUCT_NAME, nameEditText.getText().toString().trim());
        insertData.put(ProductEntry.COLUMN_PRODUCT_PRICE, priceEditText.getText().toString().trim());
        insertData.put(ProductEntry.COLUMN_PRODUCT_QUANTITY_AVAILABLE, quantityAvailableEditText.getText().toString().trim());
        insertData.put(ProductEntry.COLUMN_PRODUCT_SUPPLIER, supplierNameEditText.getText().toString().trim());
        insertData.put(ProductEntry.COLUMN_PRODUCT_RESUPPLY_PHONE, resupplyPhoneEditText.getText().toString().trim());
        insertData.put(ProductEntry.COLUMN_PRODUCT_RESUPPLY_URL, resupplyUrlEdittext.getText().toString().trim());

        boolean result = false;
        String toastMessage;
        //  save textual data to database
        try {
            if (currentProductUri == null) {
                Uri resultUri = getContentResolver().insert(ProductEntry.CONTENT_URI, insertData);
                if (resultUri == null) {
                    toastMessage = getResources().getString(R.string.db_insert_product_failed);
                } else {
                    toastMessage = getResources().getString(R.string.db_insert_product_succeeded);
                    currentProductUri = resultUri;
                    result = true;
                }
            } else {
                int rowsAffected = getContentResolver().update(currentProductUri, insertData, null, null);
                if (rowsAffected > 0) {
                    toastMessage = getResources().getString(R.string.db_update_product_updated);
                    result = true;
                } else {
                    toastMessage = getResources().getString(R.string.db_update_product_update_failed);
                }
            }
        } catch (IllegalArgumentException e) {
            toastMessage = e.getMessage();
        }

        //  save image to filesystem
        if (hasTemporaryProductImage && result) {
            if (!ProductFileHelper.saveTemporaryFileToProductImageFile(this, currentProductUri)) {
                toastMessage = getResources().getString(R.string.failed_to_save_product_image);
                result = false;
            }
            if (result) {
                //  saved our product so we do not need temporary image any more
                hasTemporaryProductImage = false;
            }
        }

        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
        return result;
    }

    /**
     * method to handle selected menu option
     *
     * @param item is the menuitem that has been selected
     * @return true if we processed this menu option
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // save product
                //  if save was successful, finish
                if (saveProduct()) {
                    finish();
                }
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Do nothing for now
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // Navigate back to parent activity (CatalogActivity)
                if (!productHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

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
        return new CursorLoader(this, currentProductUri, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null) {
            return;
        } else if (!cursor.moveToNext()) {
            return;
        }

        int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
        int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
        int quantityAvailableColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY_AVAILABLE);
        int supplierColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_SUPPLIER);
        int resupplyPhoneColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_RESUPPLY_PHONE);
        int resupplyUrlColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_RESUPPLY_URL);

        nameEditText.setText(cursor.getString(nameColumnIndex));
        priceEditText.setText(cursor.getString(priceColumnIndex));
        quantityAvailableEditText.setText(cursor.getString(quantityAvailableColumnIndex));
        supplierNameEditText.setText(cursor.getString(supplierColumnIndex));
        resupplyPhoneEditText.setText(cursor.getString(resupplyPhoneColumnIndex));
        resupplyUrlEdittext.setText(cursor.getString(resupplyUrlColumnIndex));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        nameEditText.setText("");
        priceEditText.setText("");
        quantityAvailableEditText.setText("");
        supplierNameEditText.setText("");
        resupplyPhoneEditText.setText("");
        resupplyUrlEdittext.setText("");
    }

    /**
     * when our image picker activity returns a result, this method will be called, and resulting
     * bitmap may be accessed.
     *
     * @param requestCode is the original request code we sent our intent with
     * @param resultCode  is the result code from image picker activity
     * @param data        is the intent data returned by activity
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PICK_IMAGE_REQUEST_CODE:
                saveAndSetTemporaryProductImage(ImagePicker.getImageFromResult(this, resultCode, data));
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    /**
     * method to save temporary image data and set the view to display it
     *
     * @param bitmap is the bitmap object containing image data
     */
    private void saveAndSetTemporaryProductImage(Bitmap bitmap) {
        if (bitmap == null) {
            Toast.makeText(this, getResources().getString(R.string.pick_image_intent_returned_null), Toast.LENGTH_LONG).show();
        } else {
            productTemporaryImageFile = ProductFileHelper.putProductTemporaryImageFile(this, bitmap);

            if (productTemporaryImageFile == null) {
                hasTemporaryProductImage = false;
                Toast.makeText(this,
                        getResources().getString(R.string.failed_to_save_temporary_product_image),
                        Toast.LENGTH_LONG)
                        .show();
            } else {
                productHasChanged = true;
                hasTemporaryProductImage = true;
                productImageView.setImageURI(Uri.fromFile(productTemporaryImageFile));
            }
        }
    }

    /**
     * method to decrease product quantity by one.
     */
    private void decreaseProductQuantity() {
        int quantity = Integer.parseInt(quantityAvailableEditText.getText().toString().trim());
        if (quantity > 0) {
            productHasChanged = true;
            quantityAvailableEditText.setText(String.valueOf(quantity - 1));
        }
    }

    /**
     * method to increase product quantity by one.
     */
    private void increaseProductQuantity() {
        productHasChanged = true;
        int quantity = Integer.parseInt(quantityAvailableEditText.getText().toString().trim());
        quantityAvailableEditText.setText(String.valueOf(quantity + 1));
    }

    /**
     * method to call ACTION_DIAL intent if phone number is set. Does not require special permission
     * as the actual work will be done by the dialer application.
     */
    private void callResupplyPhone() {
        String phoneNumber = resupplyPhoneEditText.getText().toString();
        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, getResources().getString(R.string.message_please_set_phonenumber_first), Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.fromParts("tel", phoneNumber, null));
        startActivity(intent);

        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, getResources().getString(R.string.error_message_no_activity_to_place_call), Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            //  theoretically we should not get here as we only send an implicit intent to call
            //  a number, but anyways, play nice and catch the exception.
            Toast.makeText(this, getResources().getString(R.string.error_message_no_permission_to_place_call), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * open resupply URL if it is set and valid
     */
    private void openResupplyUrl() {
        String urlString = resupplyUrlEdittext.getText().toString();
        if (urlString.isEmpty()) {
            Toast.makeText(this, getResources().getString(R.string.message_please_set_url_first), Toast.LENGTH_SHORT).show();
            return;
        }

        URL url;
        //  make sure URL is valid
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            Toast.makeText(this, getResources().getString(R.string.error_message_malformed_url), Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url.toString()));
        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, getResources().getString(R.string.error_message_no_activity_to_open_url), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * if product data has been changed (any input has been touched, image has been replaced),
     * display a confirmation dialog to prevent accidental data loss
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * handle back button press - display confirmation dialog if product has changed and not been
     * saved yet
     */
    @Override
    public void onBackPressed() {
        // If the product hasn't changed, continue with handling back button press
        if (!productHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }
}
