package com.keepmeapp.mynotesaver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.keepmeapp.mynotesaver.Adapters.NoteListAdapter;
import com.keepmeapp.mynotesaver.Database.RoomDB;
import com.keepmeapp.mynotesaver.Model.Notes;
import com.keepmeapp.mynotesaver.R;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener{

    RecyclerView recyclerView;
    NoteListAdapter noteListAdapter;
    List<Notes> notes = new ArrayList<>();
    RoomDB database;
    FloatingActionButton fabAdd;
    SearchView searc_notes;
    Notes selectedNote;
    private AdView mAdView;
    private InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recycler_home);
        fabAdd = findViewById(R.id.fabAdd);
        searc_notes = findViewById(R.id.searc_note);

        database = RoomDB.getInstance(this);
        notes = database.mainDAO().getAll();


        MobileAds.initialize(MainActivity.this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        AdRequest adRequestm = new AdRequest.Builder().build();

        InterstitialAd.load(MainActivity.this,"ca-app-pub-3667519865612936/4820152302", adRequestm,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        mInterstitialAd = interstitialAd;
                        mInterstitialAd.show(MainActivity.this);
                        Log.i("TAG", "onAdLoaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.i("TAG", loadAdError.getMessage());
                        mInterstitialAd = null;
                    }
                });

        updateRecycler(notes);
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, NotesWriteActivity.class);
                startActivityForResult(intent,101);
            }
        });

        searc_notes.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });
    }

    private void filter(String newText) {
        List<Notes> filteredList = new ArrayList<>();
        for (Notes singleNotes : notes){
            if (singleNotes.getTitle().toLowerCase().contains(newText.toLowerCase())
            || singleNotes.getNotes().toLowerCase().contains(newText.toLowerCase())) {
                filteredList.add(singleNotes);
            }
        }
        noteListAdapter.filterList(filteredList);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode==101){
            if (resultCode == Activity.RESULT_OK){
                Notes new_notes = (Notes) data.getSerializableExtra("notes");
                database.mainDAO().insert(new_notes);
                notes.clear();
                notes.addAll(database.mainDAO().getAll());
                noteListAdapter.notifyDataSetChanged();
            }
        }
        else if (requestCode==102){
            if (resultCode==Activity.RESULT_OK){
                Notes new_notes = (Notes) data.getSerializableExtra("notes");
                database.mainDAO().update(new_notes.getID(), new_notes.getTitle(), new_notes.getNotes());
                notes.clear();
                notes.addAll(database.mainDAO().getAll());
                noteListAdapter.notifyDataSetChanged();
            }
        }

    }

    private void updateRecycler(List<Notes> notes) {
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL));

        noteListAdapter = new NoteListAdapter(MainActivity.this, notes,notesClickListener);

        recyclerView.setAdapter(noteListAdapter);

    }
    private final com.keepmeapp.mynotesaver.NotesClickListener notesClickListener = new com.keepmeapp.mynotesaver.NotesClickListener() {
        @Override
        public void onClick(Notes notes) {

            Intent intent = new Intent(MainActivity.this, NotesWriteActivity.class);
            intent.putExtra("old_note",notes);
            startActivityForResult(intent,102);
        }

        @Override
        public void onLongClicked(Notes notes, CardView cardView) {
        selectedNote = new Notes();
        selectedNote = notes;
        showPopup(cardView);
        }
    };

    private void showPopup(CardView cardView) {
        PopupMenu popupMenu = new PopupMenu(this, cardView);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.popup_menu);
        popupMenu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()){
            case R.id.pin:
//                mInterstitialAd.show(this);
                if (selectedNote.isPinned()){
                    database.mainDAO().pin(selectedNote.getID(),false);
                    Toast.makeText(this, "Unpinned", Toast.LENGTH_SHORT).show();
                }else {
                    database.mainDAO().pin(selectedNote.getID(),true);
                    Toast.makeText(this, "Note Pinned", Toast.LENGTH_SHORT).show();
                }
                notes.clear();
                notes.addAll(database.mainDAO().getAll());
                noteListAdapter.notifyDataSetChanged();
                return true;

            case R.id.delete:
                database.mainDAO().delete(selectedNote);
                notes.remove(selectedNote);
                noteListAdapter.notifyDataSetChanged();
                Toast.makeText(this, "Notes successsfully deleted", Toast.LENGTH_SHORT).show();
              if (mInterstitialAd !=null){
                  mInterstitialAd.show(this);
              }
                return true;
            case R.id.share_note:



                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT,
                       selectedNote.getTitle() + "\n\n" +
                       selectedNote.getNotes() + "\n\n" +
//                                Html.fromHtml(nemosoftsEditText.toHtml()) + "\n" +
                                "https://play.google.com/store/apps/details?id=" + getPackageName()
                );
                sendIntent.setType("text/plain");
                startActivity(sendIntent);
                return true;
            default:
                return false;
        }

    }
}