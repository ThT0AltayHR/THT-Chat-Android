package com.turkhackteam.org;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseException;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

public class app extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (DatabaseException ignored) {
            // Persistence may already be configured by Firebase initialization.
        }

        Picasso.Builder builder = new Picasso.Builder(this);
        builder.downloader(new OkHttp3Downloader(this, Integer.MAX_VALUE));
        Picasso built = builder.build();
        built.setIndicatorsEnabled(false);
        built.setLoggingEnabled(true);
        Picasso.setSingletonInstance(built);
    }
}
