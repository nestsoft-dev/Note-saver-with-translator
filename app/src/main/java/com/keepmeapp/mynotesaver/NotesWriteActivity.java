package com.keepmeapp.mynotesaver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions;
import com.keepmeapp.mynotesaver.Model.Notes;
import com.keepmeapp.mynotesaver.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class NotesWriteActivity extends AppCompatActivity {
    EditText editText_title, editText_notes;
    ImageView img_save,mic,copyBtn;
    Notes notes;
    boolean isOldNote = false;
    Switch switch_btn;
    Button translate_btn;
    LinearLayout spinnerLayout;
    TextView translated_tv;
    Spinner fromspinner, tospinner;

    private AdView mAdView;

    String[] fromLanguage = {"From","English","Afrikaans","Arabic","Belarusian","Bulgarian","Bengali","Catalan","Czech",
            "Welsh","Hindi","Urdu"};

    String[] toLanguage = {"To","English","Afrikaans","Arabic","Belarusian","Bulgarian","Bengali","Catalan","Czech",
            "Welsh","Hindi","Urdu"};

    int languageCode,fromlanguageCode,tolanguageCode = 0;


    int REQUEST_PERMISSION_CODE = 202;

    private RewardedAd mRewardedAd;
    private final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes_write);


        editText_notes = findViewById(R.id.edit_notes);
        editText_title = findViewById(R.id.edit_title);
        img_save = findViewById(R.id.img_save);
        switch_btn = findViewById(R.id.switch_btn);
        translate_btn = findViewById(R.id.translate_btn);
        spinnerLayout = findViewById(R.id.spinnerlayout);
        translated_tv = findViewById(R.id.translatedTv);
        mic = findViewById(R.id.mic);
        fromspinner = findViewById(R.id.fromspinner);
        tospinner = findViewById(R.id.tospinner);
        copyBtn = findViewById(R.id.copyBtn);
        notes = new Notes();
        try {
            notes = (Notes) getIntent().getSerializableExtra("old_note");
            editText_title.setText(notes.getTitle());
            editText_notes.setText(notes.getNotes());
            isOldNote = true;
        }catch (Exception e){
            e.printStackTrace();
        }


        MobileAds.initialize(NotesWriteActivity.this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        AdRequest adRequestm = new AdRequest.Builder().build();
        RewardedAd.load(this, "ca-app-pub-3667519865612936/8421358103",
                adRequestm, new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error.
                        Log.d(TAG, loadAdError.getMessage());
                        mRewardedAd = null;
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                        mRewardedAd = rewardedAd;
                        Log.d(TAG, "Ad was loaded.");
                    }
                });

        fromspinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                fromlanguageCode = getLanguageCode(fromLanguage[i]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        copyBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String text = translated_tv.toString();
                        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clipData = ClipData.newPlainText("Text",text);
                        clipboardManager.setPrimaryClip(clipData);
                        copyBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_copied));
                        Toast.makeText(NotesWriteActivity.this, "Copied", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        ArrayAdapter fromadapter = new ArrayAdapter(this,R.layout.text_layout,fromLanguage);
        fromadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fromspinner.setAdapter(fromadapter);
        tospinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                tolanguageCode = getLanguageCode(toLanguage[i]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        ArrayAdapter toadapter = new ArrayAdapter(this,R.layout.text_layout,toLanguage);
       toadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tospinner.setAdapter(toadapter);

        translate_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                translated_tv.setText("");
              if (mRewardedAd !=null){
                  mRewardedAd.show(NotesWriteActivity.this,null);
                  if (editText_notes.getText().toString().isEmpty()){
                      Toast.makeText(NotesWriteActivity.this, "Please Enter text to translate", Toast.LENGTH_SHORT).show();
                  }else {
                      translateText(fromlanguageCode,tolanguageCode,editText_notes.getText().toString());
                  }
              }else{

                  if (editText_notes.getText().toString().isEmpty()){
                      Toast.makeText(NotesWriteActivity.this, "Please Enter text to translate", Toast.LENGTH_SHORT).show();
                  }else {
                      translateText(fromlanguageCode,tolanguageCode,editText_notes.getText().toString());
                  }
              }

            }
        });

        img_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String title = editText_title.getText().toString();
                String description = editText_notes.getText().toString();

                if (description.isEmpty()){
                    Toast.makeText(NotesWriteActivity.this, "Please Enter some notes", Toast.LENGTH_SHORT).show();
                    return;
                }else if (title.isEmpty()){
                    Toast.makeText(NotesWriteActivity.this, "Title can't be Empty", Toast.LENGTH_SHORT).show();
                    return;

                }else{

                    SimpleDateFormat format = new SimpleDateFormat("EEE,d MMM yyy HH:mm a");
                    Date date = new Date();

                    if (!isOldNote){
                        notes = new Notes();
                    }


                    notes.setTitle(title);
                    notes.setNotes(description);
                    notes.setDate(format.format(date));

                    Intent intent = new Intent();
                    intent.putExtra("notes",notes);
                    setResult(Activity.RESULT_OK,intent);
                    if (mRewardedAd !=null){
                        mRewardedAd.show(NotesWriteActivity.this, null);
                    }
                    finish();

                }

            }
        });

        mic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Speak to convert to Text");
                try {
                    startActivityForResult(intent,REQUEST_PERMISSION_CODE);
                }catch (Exception e){
                    e.printStackTrace();
                    Toast.makeText(NotesWriteActivity.this, "Error "+e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        switch_btn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b){
                    translate_btn.setVisibility(View.VISIBLE);
                    spinnerLayout.setVisibility(View.VISIBLE);
                    translated_tv.setVisibility(View.VISIBLE);
                }
                else {
                    translate_btn.setVisibility(View.GONE);
                    spinnerLayout.setVisibility(View.GONE);
                    translated_tv.setVisibility(View.GONE);

                }
            }
        });
    }

    private void translateText(int fromlanguageCode,int tolanguageCode,String source){
        translated_tv.setText("Downloading Models......");
        FirebaseTranslatorOptions options = new FirebaseTranslatorOptions.Builder()
                .setSourceLanguage(fromlanguageCode)
                .setTargetLanguage(tolanguageCode)
                .build();

        FirebaseTranslator translator = FirebaseNaturalLanguage.getInstance().getTranslator(options);
        FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder().build();
        translator.downloadModelIfNeeded(conditions).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                translated_tv.setText("Translating...");
                translator.translate(source).addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String s) {
                        translated_tv.setText(s);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(NotesWriteActivity.this, "failed to translate "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(NotesWriteActivity.this, "Failed to download model "+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int getLanguageCode(String s) {
        int languageCode = 0;
        switch (s){
            case "English":
                languageCode = FirebaseTranslateLanguage.EN;
                break;
            case "Afrikaans":
                languageCode = FirebaseTranslateLanguage.AF;
                break;
            case "Arabic":
                languageCode = FirebaseTranslateLanguage.AR;
                break;
            case "Belarusian":
                languageCode = FirebaseTranslateLanguage.BE;
                break;
            case "Bulgarian":
                languageCode = FirebaseTranslateLanguage.EN;
                break;
            case "Bengali":
                languageCode = FirebaseTranslateLanguage.BN;
                break;
            case "Catalan":
                languageCode = FirebaseTranslateLanguage.CA;
                break;
            case "Czech":
                languageCode = FirebaseTranslateLanguage.CS;
                break;
            case "Welsh":
                languageCode = FirebaseTranslateLanguage.EN;
                break;
            case "Hindi":
                languageCode = FirebaseTranslateLanguage.HI;
                break;
            case "Urdu":
                languageCode = FirebaseTranslateLanguage.UR;
                break;
            default:
                languageCode=0;
        }
        return languageCode;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==REQUEST_PERMISSION_CODE){
            if (resultCode==RESULT_OK && data !=null){
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                editText_notes.setText(result.get(0));
            }
        }
    }
}