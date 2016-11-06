package com.example.android.pets.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import static com.example.android.pets.data.PetContract.CONTENT_AUTHORITY;
import static com.example.android.pets.data.PetContract.PATH_PETS;

import com.example.android.pets.data.PetContract.PetEntry;

/**
 * Created by pms on 2016. 11. 4..
 */

public class PetProvider extends ContentProvider {

    public static final String LOG_TAG = PetProvider.class.getSimpleName();


    /**
     * URI matcher code for the content URI for the pets table
     */
    private static final int PETS = 100;

    /**
     * URI matcher code for the content URI for a single pet in the pets table
     */
    private static final int PET_ID = 101;


    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS, PETS);
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS + "/#", PET_ID);

    }

    private PetDbHelper mDbHelper;

    @Override
    public boolean onCreate() {

        mDbHelper = new PetDbHelper(getContext());

        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        Cursor cursor;

        int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                cursor = database.query(
                        PetEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);

                break;
            case PET_ID:
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                cursor = database.query(PetEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException();
        }

        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return insertPet(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return updatePet(uri, contentValues, selection, selectionArgs);
            case PET_ID:
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[] {
                        String.valueOf(ContentUris.parseId(uri))
                };
                return updatePet(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return database.delete(PetEntry.TABLE_NAME , selection, selectionArgs);
            case PET_ID:
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[] {
                        String.valueOf(ContentUris.parseId(uri))
                };
                return database.delete(PetEntry.TABLE_NAME, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return PetEntry.CONTENT_LIST_TYPE;
            case PET_ID:
                return PetEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }


    private Uri insertPet(Uri uri, ContentValues values) {

        String name = values.getAsString(PetEntry.COLUMN_PET_NAME);
        String breed = values.getAsString(PetEntry.COLUMN_PET_BREED);
        Integer gender = values.getAsInteger(PetEntry.COLUMN_PET_GENDER);
        Integer weight = values.getAsInteger(PetEntry.COLUMN_PET_WEIGHT);

        if (name == null) {
            throw new IllegalArgumentException("Pet requires a name");
        }
        if (breed == null) {
            throw new IllegalArgumentException("Pet requires a breed");
        }
        if (gender == null || !PetEntry.isValidGender(gender)) {
            throw new IllegalArgumentException("Pet requires a gender");
        }
        if (weight != null && weight < 0) {
            throw new IllegalArgumentException("Pet requires a weight");
        }

        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        long id = database.insert(PetEntry.TABLE_NAME, null, values);

        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        Log.v("CatalogActivity", "New row ID " + id);

        return ContentUris.withAppendedId(uri, id);
    }
    private int updatePet(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        if (values.containsKey(PetEntry.COLUMN_PET_NAME)) {

            String name = values.getAsString(PetEntry.COLUMN_PET_NAME);

            if (name == null) {
                throw new IllegalArgumentException("Pet requires a name");
            }
        }


        if (values.containsKey(PetEntry.COLUMN_PET_GENDER)) {

            Integer gender = values.getAsInteger(PetEntry.COLUMN_PET_GENDER);

            if (gender == null || !PetEntry.isValidGender(gender)) {
                throw new IllegalArgumentException("Pet requires a gender");
            }

        }

        if (values.containsKey(PetEntry.COLUMN_PET_WEIGHT)) {

            Integer weight = values.getAsInteger(PetEntry.COLUMN_PET_WEIGHT);

            if (weight != null && weight < 0) {
                throw new IllegalArgumentException("Pet requires a weight");
            }

        }
        // No need to check the breed, any value is valid (including null).

        // If there are no values to update, then don't try to update the database
        if (values.size() == 0) {
            return 0;
        }


        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        return database.update(PetEntry.TABLE_NAME, values, selection, selectionArgs);
    }
}
