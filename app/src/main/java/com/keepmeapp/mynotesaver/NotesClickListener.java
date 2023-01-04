package com.keepmeapp.mynotesaver;

import androidx.cardview.widget.CardView;

import com.keepmeapp.mynotesaver.Model.Notes;

public interface NotesClickListener {
    void onClick(Notes notes);
    void onLongClicked(Notes notes, CardView cardView);

}
