package com.example.android.pets.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.android.pets.R;
import com.example.android.pets.data.PetContract;

/**
 * {@link PetCursorAdapter} is an adapter for a list or grid view
 * that uses a {@link Cursor} of pet data as its data source. This adapter knows
 * how to create list items for each row of pet data in the {@link Cursor}.
 */
public class PetCursorAdapter extends CursorAdapter {

    /**
     * Constructs a new {@link PetCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public PetCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
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
        return LayoutInflater.from(context).inflate(R.layout.item_list, parent, false);
    }

    /**
     * This method binds the pet data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current pet can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        // Note: 11/25/2018 by sagar  Bind views
        TextView tvName = view.findViewById(R.id.name);
        TextView tvSummary = view.findViewById(R.id.summary);

        // Note: 11/25/2018 by sagar  Get the column indices for values
        int columnName = cursor.getColumnIndex(PetContract.PetEntry.COLUMN_PET_NAME);
        int columnBreed = cursor.getColumnIndex(PetContract.PetEntry.COLUMN_PET_BREED);

        // Note: 11/25/2018 by sagar  Use column indices to retrieve values
        String petName = cursor.getString(columnName);
        String petBreed = cursor.getString(columnBreed);

        if (petBreed.isEmpty()){
            petBreed = context.getResources().getString(R.string.label_unknown_breed);
        }

        // Note: 11/25/2018 by sagar  set values to view
        tvName.setText(petName);
        tvSummary.setText(petBreed);
    }
}
