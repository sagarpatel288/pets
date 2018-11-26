/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.pets.ui.activities;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.android.pets.R;
import com.example.android.pets.data.PetContract;
import com.example.android.pets.utils.EditTextUtils;
import com.example.android.pets.utils.StringUtils;
import com.example.android.pets.utils.ValidationUtil;

/**
 * Allows user to create a new pet or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int PET_LOADER = 1;

    /**
     * EditText field to enter the pet's name
     */
    private EditText mNameEditText;

    /**
     * EditText field to enter the pet's breed
     */
    private EditText mBreedEditText;

    /**
     * EditText field to enter the pet's weight
     */
    private EditText mWeightEditText;

    /**
     * EditText field to enter the pet's gender
     */
    private Spinner mGenderSpinner;

    /**
     * Gender of the pet. The possible values are:
     * 0 for unknown gender, 1 for male, 2 for female.
     */
    private int mGender = 0;
    private Uri contentUri;
    private int currentGender;
    private String currentWeight;
    private String currentBreed;
    private String currentName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_pet_name);
        mBreedEditText = (EditText) findViewById(R.id.edit_pet_breed);
        mWeightEditText = (EditText) findViewById(R.id.edit_pet_weight);
        mGenderSpinner = (Spinner) findViewById(R.id.spinner_gender);

        setupSpinner();

        if (getIntent() != null) {
            if (getIntent().getData() != null) {
                contentUri = getIntent().getData();
                setTitle(getString(R.string.label_edit_data));
                initLoader();
            } else {
                setTitle(getString(R.string.label_add_data));
                // Note: 11/26/2018 by sagar  No need to show delete option for blank pet entry
                invalidateOptionsMenu();
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new pet, hide the "Delete" menu item.
        if (contentUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the pet.
                deletePet();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the pet.
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
     * Perform the deletion of the pet in the database.
     */
    private void deletePet() {
        int affectedRow = getContentResolver().delete(contentUri, null, null);
        if (!(affectedRow < 0)){
            Toast.makeText(this, getString(R.string.msg_pet_deleted), Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    /**
     * Setup the dropdown spinner that allows the user to select the gender of the pet.
     */
    private void setupSpinner() {
        // Create adapter for spinner. The list options are from the String array it will use
        // the spinner will use the default layout
        ArrayAdapter genderSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_gender_options, android.R.layout.simple_spinner_item);

        // Specify dropdown layout style - simple list view with 1 item per line
        genderSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        // Apply the adapter to the spinner
        mGenderSpinner.setAdapter(genderSpinnerAdapter);

        // Set the integer mSelected to the constant values
        mGenderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {
                    if (selection.equals(getString(R.string.gender_male))) {
                        mGender = PetContract.PetEntry.GENDER_MALE; // Male
                    } else if (selection.equals(getString(R.string.gender_female))) {
                        mGender = PetContract.PetEntry.GENDER_FEMALE; // Female
                    } else {
                        mGender = PetContract.PetEntry.GENDER_UNKNOWN; // Unknown
                    }
                }
            }

            // Because AdapterView is an abstract class, onNothingSelected must be defined
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mGender = 0; // Unknown
            }
        });
    }

    private void initLoader() {
        getLoaderManager().initLoader(PET_LOADER, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Do nothing for now
                savePet();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the pet hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!hasChanges()) {
                    NavUtils.navigateUpFromSameTask(this);
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

    private void savePet() {
        if (hasValidData()) {
            ContentValues contentValues = getContentValues();

//        SQLiteDatabase db = new PetDbHelper(this).getWritableDatabase();
//        long id = db.insert(PetContract.PetEntry.TABLE_NAME, null, contentValues);

            // Note: 11/25/2018 by sagar  Instead of dealing with database directly, we are doing so through Content Resolver
            long id = 0;
            if (contentUri == null) {
                Uri resultUri = getContentResolver().insert(PetContract.PetEntry.CONTENT_URI, contentValues);
                // Note: 11/26/2018 by sagar  Gives id of newly inserted pet
                id = ContentUris.parseId(resultUri);
            } else {
                // Note: 11/26/2018 by sagar  Gives total numbers of affected rows due to update operation
                id = getContentResolver().update(contentUri, contentValues, null, null);
            }

            if (!(id < 0)) {
                if (contentUri == null || hasChanges()) {
                    Toast.makeText(this, getString(R.string.msg_pet_saved), Toast.LENGTH_SHORT).show();
                } else if (contentUri != null && !hasChanges()){
                    Toast.makeText(this, getString(R.string.msg_no_changes), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, getString(R.string.label_error_with_saving_pet), Toast.LENGTH_SHORT).show();
            }
            finish();
        }
    }

    private void deleteEntry() {

    }

    private boolean hasValidData() {
        if (!ValidationUtil.hasValue(mNameEditText)) {
            Toast.makeText(this, getString(R.string.msg_please_enter_name_of_pet), Toast.LENGTH_SHORT).show();
            return false;
        } else if (!ValidationUtil.hasValue(mBreedEditText)) {
            Toast.makeText(this, getString(R.string.msg_please_enter_breed_of_the_pet), Toast.LENGTH_SHORT).show();
            return false;
        } else if (!ValidationUtil.hasValue(mWeightEditText)) {
            Toast.makeText(this, getString(R.string.msg_please_enter_weight_of_the_pet), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private ContentValues getContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(PetContract.PetEntry.COLUMN_PET_NAME, mNameEditText.getText().toString().trim());
        contentValues.put(PetContract.PetEntry.COLUMN_PET_BREED, mBreedEditText.getText().toString().trim());
        contentValues.put(PetContract.PetEntry.COLUMN_PET_GENDER, mGender);
        contentValues.put(PetContract.PetEntry.COLUMN_PET_WEIGHT, mWeightEditText.getText().toString());
        return contentValues;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String projection[] = new String[]
                {PetContract.PetEntry._ID,
                        PetContract.PetEntry.COLUMN_PET_NAME,
                        PetContract.PetEntry.COLUMN_PET_BREED,
                        PetContract.PetEntry.COLUMN_PET_GENDER,
                        PetContract.PetEntry.COLUMN_PET_WEIGHT};
        return new CursorLoader(this, contentUri, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        cursor.moveToFirst();
        // Note: 11/26/2018 by sagar  Get column indices
        int columnName = cursor.getColumnIndex(PetContract.PetEntry.COLUMN_PET_NAME);
        int columnBreed = cursor.getColumnIndex(PetContract.PetEntry.COLUMN_PET_BREED);
        int columnGender = cursor.getColumnIndex(PetContract.PetEntry.COLUMN_PET_GENDER);
        int columnWeight = cursor.getColumnIndex(PetContract.PetEntry.COLUMN_PET_WEIGHT);

        // Note: 11/26/2018 by sagar  Take a reference to existing values
        currentGender = cursor.getInt(columnGender);
        currentWeight = cursor.getString(columnWeight);
        currentBreed = cursor.getString(columnBreed);
        currentName = cursor.getString(columnName);

        // Note: 11/26/2018 by sagar  Set values
        mNameEditText.setText(currentName);
        mBreedEditText.setText(currentBreed);
        mWeightEditText.setText(currentWeight);
        mGenderSpinner.setSelection(currentGender);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        EditTextUtils.clearEditText(mNameEditText, mBreedEditText, mWeightEditText);
        mGenderSpinner.setSelection(0);
    }

    @Override
    public void onBackPressed() {
        if (!hasChanges()){
            super.onBackPressed();
        } else {
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

    private boolean hasChanges() {
        if (contentUri != null && StringUtils.isNotNullNotEmpty(currentName)
                && StringUtils.isNotNullNotEmpty(currentBreed)
                && StringUtils.isNotNullNotEmpty(currentWeight)
                && currentGender == mGender
                && currentName.equalsIgnoreCase(EditTextUtils.getString(mNameEditText))
                && currentBreed.equalsIgnoreCase(EditTextUtils.getString(mBreedEditText))
                && currentWeight.equalsIgnoreCase(EditTextUtils.getString(mWeightEditText))){
            return false;
        } else return contentUri != null || currentGender != mGender;
    }

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
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}