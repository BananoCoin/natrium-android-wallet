package com.banano.kaliumwallet.db;

import android.support.annotation.NonNull;

import io.realm.DynamicRealm;
import io.realm.RealmMigration;
import io.realm.RealmObjectSchema;
import io.realm.RealmSchema;

/**
 * Migration for Adding a UUID field to Realm
 */

public class Migration implements RealmMigration {

    @Override
    public void migrate(@NonNull DynamicRealm realm, long oldVersion, long newVersion) {
        RealmSchema schema = realm.getSchema();

        // Add Contact class
        if (oldVersion == 1) {
            schema.create("Contact")
                    .addField("name", String.class)
                    .addField("address", String.class);
            oldVersion++;
        }
    }

    @Override
    public int hashCode() {
        return 37;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof RealmMigration);
    }

}

