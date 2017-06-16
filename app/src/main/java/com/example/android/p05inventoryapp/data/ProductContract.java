package com.example.android.p05inventoryapp.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.provider.BaseColumns;

import com.example.android.p05inventoryapp.R;

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
 * This is the contract to provide database / table information, provide means to check user
 * input etc.
 */

public final class ProductContract {
    static final String CONTENT_AUTHORITY = "com.example.android.p05inventoryapp";
    static final String PATH_PRODUCTS = "products";
    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    /**
     * empty private constructor to prevent instantiation of this final class
     */
    private ProductContract() {
    }

    /**
     * inner class to hold constants and helper methods for products table and entries
     */
    public static final class ProductEntry implements BaseColumns {
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_PRODUCTS);
        public static final String _ID = BaseColumns._ID;
        public static final String COLUMN_PRODUCT_NAME = "name";
        public static final String COLUMN_PRODUCT_PRICE = "price";
        public static final String COLUMN_PRODUCT_QUANTITY_AVAILABLE = "quantity_available";
        public static final String COLUMN_PRODUCT_SUPPLIER = "supplier_name";
        public static final String COLUMN_PRODUCT_RESUPPLY_PHONE = "resupply_phone";
        public static final String COLUMN_PRODUCT_RESUPPLY_URL = "resupply_url";
        //  this is only needed to check if image has been added to product, it will never be
        //  saved to database
        public static final String DUMMY_COLUMN_PRODUCT_HAS_IMAGE = "dummy_product_has_image";

        static final String TABLE_NAME = "products";
        //  set validation mode so only fields to be updated are checked for validity
        static final int PRODUCT_VALIDATION_MODE_INSERT = 1;
        static final int PRODUCT_VALIDATION_MODE_UPDATE = 2;
        //  uri for accessing all products
        static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY
                        + "/" + PATH_PRODUCTS;
        //  uri for accessing a specific item
        static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY
                        + "/" + PATH_PRODUCTS;

        /**
         * method to check if a data to be entered into database is sane. Insert mode is more
         * restrictive, it requires every mandatory field to be set while update mode permits
         * omitting fields that are not to be changed.
         *
         * @param context        is the calling context
         * @param contentValues  is the container for key-value pairs to be entered into database
         * @param validationMode is either PRODUCT_VALIDATION_MODE_INSERT or UPDATE
         * @param doIdCheck      force _ID field check
         * @return true if contentValues are sane
         */
        static boolean isValidProduct(Context context, ContentValues contentValues, int validationMode, boolean doIdCheck) {
            Integer id = contentValues.getAsInteger(_ID);
            Resources resources = context.getResources();
            //  if id is provided, check regardless of doIdCheck
            //  if doIdCheck is true, do not allow missing ID
            if (id != null || doIdCheck) {
                if (!isValidNonNegativeInteger(id, false)) {
                    throw new IllegalArgumentException("ID must be a non-negative integer");
                }
            }

            if (!isValidNonEmptyString(contentValues, COLUMN_PRODUCT_NAME, validationMode)) {
                throw new IllegalArgumentException(resources.getString(R.string.product_validator_message_invalid_name));
            }

            if (!isValidPositiveDouble(contentValues, COLUMN_PRODUCT_PRICE, validationMode)) {
                throw new IllegalArgumentException(resources.getString(R.string.product_validator_message_invalid_price));
            }

            if (!isValidNonNegativeInteger(contentValues, COLUMN_PRODUCT_QUANTITY_AVAILABLE, validationMode, false)) {
                throw new IllegalArgumentException(resources.getString(R.string.product_validator_message_invalid_quantity));
            }

            if (!isValidNonEmptyString(contentValues, COLUMN_PRODUCT_SUPPLIER, validationMode)) {
                throw new IllegalArgumentException(resources.getString(R.string.product_validator_message_invalid_supplier_name));
            }

            if (!isValidUrl(contentValues, COLUMN_PRODUCT_RESUPPLY_URL, validationMode, true)) {
                throw new IllegalArgumentException(resources.getString(R.string.product_validator_message_invalid_url));
            }

            if (!isValidBoolean(contentValues, DUMMY_COLUMN_PRODUCT_HAS_IMAGE, validationMode, true)) {
                throw new IllegalArgumentException(resources.getString(R.string.product_validator_message_missing_product_image));
            }

            return true;
        }

        /**
         * method to check if a particular key value in contentValues is a non negative integer
         *
         * @param contentValues     is the container for key-value pairs to be entered into database
         * @param key               is the key of value to be checked
         * @param validationMode    is either PRODUCT_VALIDATION_MODE_INSERT or UPDATE
         * @param permitEmptyOrNull true if emtpy or null values permitted
         * @return true if data checks out
         */
        static boolean isValidNonNegativeInteger(ContentValues contentValues, String key,
                                                 int validationMode, boolean permitEmptyOrNull) {
            if (contentValues.containsKey(key)) {
                if (!isValidNonNegativeInteger(contentValues.getAsInteger(key), permitEmptyOrNull)) {
                    return false;
                }
            } else if (validationMode != PRODUCT_VALIDATION_MODE_UPDATE) {
                return false;
            }
            return true;
        }

        /**
         * method to check if a particular Integer is actually a non negative integer
         *
         * @param integer           is the value to be checked
         * @param permitEmptyOrNull true if emtpy or null values permitted
         * @return true if data checks out
         */
        static boolean isValidNonNegativeInteger(Integer integer, boolean permitEmptyOrNull) {
            if (permitEmptyOrNull) {
                return (integer == null || integer >= 0);
            } else {
                return (integer != null && integer >= 0);
            }
        }

        /**
         * method to check if a particular key value in contentValues is a positive Double
         *
         * @param contentValues  is the container for key-value pairs to be entered into database
         * @param key            is the key of value to be checked
         * @param validationMode is either PRODUCT_VALIDATION_MODE_INSERT or UPDATE
         * @return true if data checks out
         */
        static boolean isValidPositiveDouble(ContentValues contentValues, String key, int validationMode) {
            if (contentValues.containsKey(key)) {
                if (!isValidPositiveDouble(contentValues.getAsDouble(key))) {
                    return false;
                }
            } else if (validationMode != PRODUCT_VALIDATION_MODE_UPDATE) {
                return false;
            }
            return true;
        }

        /**
         * method to check if a particular Double is actually a positive Double
         *
         * @param aDouble is the value to be checked
         * @return true if data checks out
         */
        static boolean isValidPositiveDouble(Double aDouble) {
            return (aDouble != null && aDouble > 0);
        }

        /**
         * method to check if a particular key value in contentValues is a non empty string
         *
         * @param contentValues  is the container for key-value pairs to be entered into database
         * @param key            is the key of value to be checked
         * @param validationMode is either PRODUCT_VALIDATION_MODE_INSERT or UPDATE
         * @return true if data checks out
         */
        static boolean isValidNonEmptyString(ContentValues contentValues, String key, int validationMode) {
            if (contentValues.containsKey(key)) {
                if (!isValidNonEmptyString(contentValues.getAsString(key))) {
                    return false;
                }
            } else if (validationMode != PRODUCT_VALIDATION_MODE_UPDATE) {
                return false;
            }
            return true;
        }

        /**
         * method to check if a particular String is actually a non empty String
         *
         * @param aString is the value to be checked
         * @return true if data checks out
         */
        static boolean isValidNonEmptyString(String aString) {
            return (aString != null && !aString.isEmpty());
        }

        /**
         * method to check if a particular key boolean value in contentValues equals to required
         *
         * @param contentValues  is the container for key-value pairs to be entered into database
         * @param key            is the key of value to be checked
         * @param validationMode is either PRODUCT_VALIDATION_MODE_INSERT or UPDATE
         * @return true if data checks out
         */
        static boolean isValidBoolean(ContentValues contentValues, String key, int validationMode, boolean requiredValue) {
            if (contentValues.containsKey(key)) {
                if (!isValidBoolean(contentValues.getAsBoolean(key), requiredValue)) {
                    return false;
                }
            } else if (validationMode != PRODUCT_VALIDATION_MODE_UPDATE) {
                return false;
            }
            return true;
        }

        /**
         * method to check if a particular Boolean value is present and is set to either true or false
         *
         * @param aBoolean      is the value to be checked
         * @param requiredValue is the value the boolean is required to be set to
         * @return true if data checks out
         */
        static boolean isValidBoolean(Boolean aBoolean, Boolean requiredValue) {
            return (aBoolean == requiredValue);
        }

        /**
         * method to check if a particular key value in contentValues is a valid URL
         *
         * @param contentValues     is the container for key-value pairs to be entered into database
         * @param key               is the key of value to be checked
         * @param validationMode    is either PRODUCT_VALIDATION_MODE_INSERT or UPDATE
         * @param permitEmptyOrNull true if emtpy or null values permitted
         * @return true if data checks out
         */
        static boolean isValidUrl(ContentValues contentValues, String key,
                                  int validationMode, boolean permitEmptyOrNull) {
            if (contentValues.containsKey(key)) {
                if (!isValidUrl(contentValues.getAsString(key), permitEmptyOrNull)) {
                    return false;
                }
            } else if (validationMode != PRODUCT_VALIDATION_MODE_UPDATE && !permitEmptyOrNull) {
                return false;
            }
            return true;
        }

        /**
         * method to check if a particular string is actually a valid URL
         *
         * @param aString           is the value to be checked
         * @param permitEmptyOrNull true if emtpy or null values permitted
         * @return true if data checks out
         */
        static boolean isValidUrl(String aString, boolean permitEmptyOrNull) {
            if (aString == null || aString.isEmpty()) {
                return (permitEmptyOrNull);
            } else {
                try {
                    new URL(aString);
                } catch (MalformedURLException e) {
                    return false;
                }
                return true;
            }
        }
    }
}
