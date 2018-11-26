package com.example.android.pets.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.example.android.pets.data.PetContract.QueryType;

import static android.util.Log.d;
import static com.example.android.pets.data.PetContract.CONTENT_AUTHORITY;
import static com.example.android.pets.data.PetContract.PATH_PETS;
import static com.example.android.pets.data.PetContract.PATH_PETS_ID;
import static com.example.android.pets.data.PetContract.QueryType.QUERY_TYPE_UPDATE;

/**
 * {@link ContentProvider} for Pets app.
 */
public class PetProvider extends ContentProvider {

    /**
     * Tag for the log messages
     */
    public static final String LOG_TAG = PetProvider.class.getSimpleName();
    /**
     * URI matcher code for the content URI for the pets table
     */
    private static final int PETS = 100;
    /**
     * URI matcher code for the content URI for a single pet in the pets table
     */
    private static final int PET_ID = 101;
    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a corresponding code to return
        // when a match is found.

        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_PETS, 100);
        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_PETS_ID, 101);
    }

    /**
     * Database helper object
     */
    private PetDbHelper mPetDbHelper;

    /**
     * Initialize the provider and the database helper object.
     */
    @Override
    public boolean onCreate() {
        // Make sure the variable is a global variable, so it can be referenced from other
        // ContentProvider methods.
        if (mPetDbHelper == null) {
            mPetDbHelper = new PetDbHelper(getContext());
        }
        return true;
    }

    /**
     * Perform the query for the given URI. Use the given projection, selection, selection arguments, and sort order.
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Get readable database
        SQLiteDatabase database = mPetDbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                // For the PETS code, query the pets table directly with the given
                // projection, selection, selection arguments, and sort order. The cursor
                // could contain multiple rows of the pets table.
                cursor = database.query(PetContract.PetEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case PET_ID:
                // For the PET_ID code, extract out the ID from the URI.
                // For an example URI such as "content://com.example.android.pets/pets/3",
                // the selection will be "_id=?" and the selection argument will be a
                // String array containing the actual ID of 3 in this case.
                //
                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments' String array.
                selection = PetContract.PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                // This will perform a query on the pets table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                cursor = database.query(PetContract.PetEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Note: 11/26/2018 by sagar  Notifies changes
        if (getContext() != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursor;
    }

    /**
     * Returns the MIME type of data for the content URI.
     */
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return PetContract.PetEntry.CONTENT_LIST_TYPE;
            case PET_ID:
                return PetContract.PetEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    /**
     * Insert new data into the provider with the given ContentValues.
     */
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        int uriCode = sUriMatcher.match(uri);
        Uri uriResult;
        switch (uriCode) {
            case PETS:
                uriResult = insertPet(uri, contentValues);
                break;

            default:
                throw new IllegalArgumentException("Cannot insert unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return uriResult;
    }

    private Uri insertPet(Uri uri, ContentValues contentValues) {
        // Note: 11/25/2018 by sagar  Go ahead only after validation
        if (validation(contentValues, PetContract.QueryType.QUERY_TYPE_INSERT)) {
            // Note: 11/25/2018 by sagar  We need writable database for insert operation
            SQLiteDatabase database = mPetDbHelper.getWritableDatabase();
            long id = database.insert(PetContract.PetEntry.TABLE_NAME, null, contentValues);
            // Note: 11/25/2018 by sagar  Once we know id of newly inserted row, return the new uri
            // that contains generated id
            return ContentUris.withAppendedId(uri, id);
        } else {
            return uri;
        }
    }

    private boolean validation(ContentValues contentValues, int queryType) {
        //Check the type of query to ease conditional validation process
        boolean isInsertQuery = isInsertQuery(queryType);

        // Check that the name is not null
        if (isInsertQuery || contentValues.containsKey(PetContract.PetEntry.COLUMN_PET_NAME)) {
            String name = contentValues.getAsString(PetContract.PetEntry.COLUMN_PET_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Pet requires a name");
            }
        }

        if (isInsertQuery || contentValues.containsKey(PetContract.PetEntry.COLUMN_PET_BREED)) {
            String breed = contentValues.getAsString(PetContract.PetEntry.COLUMN_PET_BREED);
            if (breed == null) {
                d("PetProvider: ", "PetProvider: validation: breed is null");
            }
        }

        if (isInsertQuery || contentValues.containsKey(PetContract.PetEntry.COLUMN_PET_GENDER)) {
            int gender = contentValues.getAsInteger(PetContract.PetEntry.COLUMN_PET_GENDER);
            if (gender != PetContract.PetEntry.GENDER_UNKNOWN
                    && gender != PetContract.PetEntry.GENDER_MALE
                    && gender != PetContract.PetEntry.GENDER_FEMALE) {
                throw new IllegalArgumentException("Invalid pet gender: " + gender);
            }
        }

        if (isInsertQuery || contentValues.containsKey(PetContract.PetEntry.COLUMN_PET_WEIGHT)) {
            int weight = contentValues.getAsInteger(PetContract.PetEntry.COLUMN_PET_WEIGHT);
            if (weight < 0) {
                throw new IllegalArgumentException("Pet weight cannot be negative: " + weight);
            }
        }

        return contentValues.size() > 0;
    }

    /**
     * Delete the data at the given selection and selection arguments.
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                // Delete all rows that match the selection and selection args
                return deletePet(uri, selection, selectionArgs);
            case PET_ID:
                // Delete a single row given by the ID in the URI
                selection = PetContract.PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return deletePet(uri, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);

        }
    }

    private int deletePet(Uri uri, String selection, String[] selectionArgs){
        // Get writeable database
        SQLiteDatabase database = mPetDbHelper.getWritableDatabase();
        int affectedRows = database.delete(PetContract.PetEntry.TABLE_NAME, selection, selectionArgs);
        if (affectedRows != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return affectedRows;
    }

    /**
     * Updates the data at the given selection and selection arguments, with the new ContentValues.
     */
    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        // Note: 11/25/2018 by sagar  Go ahead only after validation
        if (validation(contentValues, QUERY_TYPE_UPDATE)) {
//            SQLiteDatabase database = mPetDbHelper.getWritableDatabase();
//            long id = database.update(PetContract.PetEntry.TABLE_NAME, contentValues, selection, selectionArgs);
            final int match = sUriMatcher.match(uri);
            switch (match) {
                case PETS:
                    return updatePet(uri, contentValues, selection, selectionArgs);
                case PET_ID:
                    // For the PET_ID code, extract out the ID from the URI,
                    // so we know which row to update. Selection will be "_id=?" and selection
                    // arguments will be a String array containing the actual ID.
                    selection = PetContract.PetEntry._ID + "=?";
                    selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                    return updatePet(uri, contentValues, selection, selectionArgs);
                default:
                    throw new IllegalArgumentException("Update is not supported for " + uri);
            }
        } else {
            return -1;
        }
    }

    /**
     * Update pets in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more pets).
     * Return the number of rows that were successfully updated.
     */
    private int updatePet(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase database = mPetDbHelper.getWritableDatabase();
        long affectedRows = database.update(PetContract.PetEntry.TABLE_NAME, values, selection, selectionArgs);
        if (affectedRows != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return (int) affectedRows;
    }

    /**
     * @param queryType Type of query {@link QueryType}
     * @return true if the query type is insert query
     */
    private boolean isInsertQuery(int queryType) {
        return queryType == QueryType.QUERY_TYPE_INSERT;
    }

    /**
     * @param queryType Type of query {@link QueryType}
     * @return true if the query type is update query
     */
    private boolean isUpdateQuery(int queryType) {
        return queryType == QUERY_TYPE_UPDATE;
    }
}