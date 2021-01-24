

package com.naman14.timber.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;

import java.util.HashSet;
import java.util.Iterator;


public class SongPlayCount {

    private static final int NUM_WEEKS = 52;
    private static SongPlayCount sInstance = null;

    private static Interpolator sInterpolator = new AccelerateInterpolator(1.5f);

    private static int INTERPOLATOR_HEIGHT = 50;

    private static int INTERPOLATOR_BASE = 25;
    private static int ONE_WEEK_IN_MS = 1000 * 60 * 60 * 24 * 7;
    private static String WHERE_ID_EQUALS = SongPlayCountColumns.ID + "=?";
    private MusicDB mMusicDatabase = null;

    private int mNumberOfWeeksSinceEpoch;


    private boolean mDatabaseUpdated;


    public SongPlayCount(final Context context) {
        mMusicDatabase = MusicDB.getInstance(context);

        long msSinceEpoch = System.currentTimeMillis();
        mNumberOfWeeksSinceEpoch = (int) (msSinceEpoch / ONE_WEEK_IN_MS);

        mDatabaseUpdated = false;
    }


    public static final synchronized SongPlayCount getInstance(final Context context) {
        if (sInstance == null) {
            sInstance = new SongPlayCount(context.getApplicationContext());
        }
        return sInstance;
    }


    private static float calculateScore(final int[] playCounts) {
        if (playCounts == null) {
            return 0;
        }

        float score = 0;
        for (int i = 0; i < Math.min(playCounts.length, NUM_WEEKS); i++) {
            score += playCounts[i] * getScoreMultiplierForWeek(i);
        }

        return score;
    }


    private static String getColumnNameForWeek(final int week) {
        return SongPlayCountColumns.WEEK_PLAY_COUNT + String.valueOf(week);
    }


    private static float getScoreMultiplierForWeek(final int week) {
        return sInterpolator.getInterpolation(1 - (week / (float) NUM_WEEKS)) * INTERPOLATOR_HEIGHT
                + INTERPOLATOR_BASE;
    }


    private static int getColumnIndexForWeek(final int week) {

        return 1 + week;
    }

    public void onCreate(final SQLiteDatabase db) {

        StringBuilder builder = new StringBuilder();
        builder.append("CREATE TABLE IF NOT EXISTS ");
        builder.append(SongPlayCountColumns.NAME);
        builder.append("(");
        builder.append(SongPlayCountColumns.ID);
        builder.append(" INT UNIQUE,");

        for (int i = 0; i < NUM_WEEKS; i++) {
            builder.append(getColumnNameForWeek(i));
            builder.append(" INT DEFAULT 0,");
        }

        builder.append(SongPlayCountColumns.LAST_UPDATED_WEEK_INDEX);
        builder.append(" INT NOT NULL,");

        builder.append(SongPlayCountColumns.PLAYCOUNTSCORE);
        builder.append(" REAL DEFAULT 0);");

        db.execSQL(builder.toString());
    }

    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {

    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // If we ever have downgrade, drop the table to be safe
        db.execSQL("DROP TABLE IF EXISTS " + SongPlayCountColumns.NAME);
        onCreate(db);
    }


    public void bumpSongCount(final long songId) {
        if (songId < 0) {
            return;
        }

        final SQLiteDatabase database = mMusicDatabase.getWritableDatabase();
        updateExistingRow(database, songId, true);
    }


    private void createNewPlayedEntry(final SQLiteDatabase database, final long songId) {
        // no row exists, create a new one
        float newScore = getScoreMultiplierForWeek(0);
        int newPlayCount = 1;

        final ContentValues values = new ContentValues(3);
        values.put(SongPlayCountColumns.ID, songId);
        values.put(SongPlayCountColumns.PLAYCOUNTSCORE, newScore);
        values.put(SongPlayCountColumns.LAST_UPDATED_WEEK_INDEX, mNumberOfWeeksSinceEpoch);
        values.put(getColumnNameForWeek(0), newPlayCount);

        database.insert(SongPlayCountColumns.NAME, null, values);
    }


    private void updateExistingRow(final SQLiteDatabase database, final long id, boolean bumpCount) {
        String stringId = String.valueOf(id);


        database.beginTransaction();


        final Cursor cursor = database.query(SongPlayCountColumns.NAME, null, WHERE_ID_EQUALS,
                new String[]{stringId}, null, null, null);


        if (cursor != null && cursor.moveToFirst()) {

            int lastUpdatedIndex = cursor.getColumnIndex(SongPlayCountColumns.LAST_UPDATED_WEEK_INDEX);
            int lastUpdatedWeek = cursor.getInt(lastUpdatedIndex);
            int weekDiff = mNumberOfWeeksSinceEpoch - lastUpdatedWeek;

            if (Math.abs(weekDiff) >= NUM_WEEKS) {

                deleteEntry(database, stringId);
                if (bumpCount) {
                    createNewPlayedEntry(database, id);
                }
            } else if (weekDiff != 0) {

                int[] playCounts = new int[NUM_WEEKS];

                if (weekDiff > 0) {

                    for (int i = 0; i < NUM_WEEKS - weekDiff; i++) {
                        playCounts[i + weekDiff] = cursor.getInt(getColumnIndexForWeek(i));
                    }
                } else if (weekDiff < 0) {

                    for (int i = 0; i < NUM_WEEKS + weekDiff; i++) {
                        playCounts[i] = cursor.getInt(getColumnIndexForWeek(i - weekDiff));
                    }
                }


                if (bumpCount) {
                    playCounts[0]++;
                }

                float score = calculateScore(playCounts);


                if (score < .01f) {
                    deleteEntry(database, stringId);
                } else {

                    ContentValues values = new ContentValues(NUM_WEEKS + 2);
                    values.put(SongPlayCountColumns.LAST_UPDATED_WEEK_INDEX, mNumberOfWeeksSinceEpoch);
                    values.put(SongPlayCountColumns.PLAYCOUNTSCORE, score);

                    for (int i = 0; i < NUM_WEEKS; i++) {
                        values.put(getColumnNameForWeek(i), playCounts[i]);
                    }


                    database.update(SongPlayCountColumns.NAME, values, WHERE_ID_EQUALS,
                            new String[]{stringId});
                }
            } else if (bumpCount) {

                ContentValues values = new ContentValues(2);

                int scoreIndex = cursor.getColumnIndex(SongPlayCountColumns.PLAYCOUNTSCORE);
                float score = cursor.getFloat(scoreIndex) + getScoreMultiplierForWeek(0);
                values.put(SongPlayCountColumns.PLAYCOUNTSCORE, score);

                values.put(getColumnNameForWeek(0), cursor.getInt(getColumnIndexForWeek(0)) + 1);


                database.update(SongPlayCountColumns.NAME, values, WHERE_ID_EQUALS,
                        new String[]{stringId});
            }

            cursor.close();
        } else if (bumpCount) {

            createNewPlayedEntry(database, id);
        }

        database.setTransactionSuccessful();
        database.endTransaction();
    }

    public void deleteAll() {
        final SQLiteDatabase database = mMusicDatabase.getWritableDatabase();
        database.delete(SongPlayCountColumns.NAME, null, null);
    }


    public Cursor getTopPlayedResults(int numResults) {
        updateResults();

        final SQLiteDatabase database = mMusicDatabase.getReadableDatabase();
        return database.query(SongPlayCountColumns.NAME, new String[]{SongPlayCountColumns.ID},
                null, null, null, null, SongPlayCountColumns.PLAYCOUNTSCORE + " DESC",
                (numResults <= 0 ? null : String.valueOf(numResults)));
    }


    public long[] getTopPlayedResultsForList(long[] ids) {
        final int MAX_NUMBER_SONGS_TO_ANALYZE = 250;

        if (ids == null || ids.length == 0) {
            return null;
        }

        HashSet<Long> uniqueIds = new HashSet<Long>(ids.length);


        StringBuilder selection = new StringBuilder();
        selection.append(SongPlayCountColumns.ID);
        selection.append(" IN (");


        uniqueIds.add(ids[0]);
        selection.append(ids[0]);

        for (int i = 1; i < ids.length; i++) {
            // if the new id doesn't exist
            if (uniqueIds.add(ids[i])) {

                selection.append(",");


                selection.append(ids[i]);


                if (uniqueIds.size() >= MAX_NUMBER_SONGS_TO_ANALYZE) {
                    break;
                }
            }
        }


        selection.append(")");

        long[] sortedList = new long[uniqueIds.size()];


        final SQLiteDatabase database = mMusicDatabase.getReadableDatabase();
        Cursor topSongsCursor = null;
        int idx = 0;

        try {
            topSongsCursor = database.query(SongPlayCountColumns.NAME,
                    new String[]{SongPlayCountColumns.ID}, selection.toString(), null, null,
                    null, SongPlayCountColumns.PLAYCOUNTSCORE + " DESC");

            if (topSongsCursor != null && topSongsCursor.moveToFirst()) {
                do {

                    long id = topSongsCursor.getLong(0);
                    sortedList[idx++] = id;
                    uniqueIds.remove(id);
                } while (topSongsCursor.moveToNext());
            }
        } finally {
            if (topSongsCursor != null) {
                topSongsCursor.close();
                topSongsCursor = null;
            }
        }


        Iterator<Long> iter = uniqueIds.iterator();
        while (iter.hasNext()) {
            sortedList[idx++] = iter.next();
        }

        return sortedList;
    }


    private synchronized void updateResults() {
        if (mDatabaseUpdated) {
            return;
        }

        final SQLiteDatabase database = mMusicDatabase.getWritableDatabase();

        database.beginTransaction();

        int oldestWeekWeCareAbout = mNumberOfWeeksSinceEpoch - NUM_WEEKS + 1;

        database.delete(SongPlayCountColumns.NAME, SongPlayCountColumns.LAST_UPDATED_WEEK_INDEX
                + " < " + oldestWeekWeCareAbout, null);


        Cursor cursor = database.query(SongPlayCountColumns.NAME,
                new String[]{SongPlayCountColumns.ID},
                null, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {

            do {
                updateExistingRow(database, cursor.getLong(0), false);
            } while (cursor.moveToNext());

            cursor.close();
            cursor = null;
        }

        mDatabaseUpdated = true;
        database.setTransactionSuccessful();
        database.endTransaction();
    }


    public void removeItem(final long songId) {
        final SQLiteDatabase database = mMusicDatabase.getWritableDatabase();
        deleteEntry(database, String.valueOf(songId));
    }


    private void deleteEntry(final SQLiteDatabase database, final String stringId) {
        database.delete(SongPlayCountColumns.NAME, WHERE_ID_EQUALS, new String[]{stringId});
    }

    public interface SongPlayCountColumns {

        String NAME = "songplaycount";
        String ID = "songid";
        String WEEK_PLAY_COUNT = "week";
        String LAST_UPDATED_WEEK_INDEX = "weekindex";
        String PLAYCOUNTSCORE = "playcountscore";
    }
}
