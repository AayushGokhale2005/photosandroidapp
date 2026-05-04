package com.example.photos;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.photos.model.PhotoLibrary;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PhotoLibrary.getInstance().load(this);
    }
}
