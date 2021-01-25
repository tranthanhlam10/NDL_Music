
package com.naman14.timber.dataloaders;

import android.database.AbstractCursor;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;


public class SortedCursor extends AbstractCursor {

    private final Cursor mCursor;

    private ArrayList<Integer> mOrderedPositions;

    private ArrayList<Long> mMissingIds;

    private HashMap<Long, Integer> mMapCursorPositions;

    private ArrayList<Object> mExtraData;

    public SortedCursor(final Cursor cursor, final long[] order, final String columnName,
                        final List<? extends Object> extraData) {
        if (cursor == null) {
            throw new IllegalArgumentException("Non-null cursor is needed");
        }

        mCursor = cursor;
        mMissingIds = buildCursorPositionMapping(order, columnName, extraData);
    }


    private ArrayList<Long> buildCursorPositionMapping(final long[] order,
                                                       final String columnName, final List<? extends Object> extraData) {
        ArrayList<Long> missingIds = new ArrayList<Long>();

        mOrderedPositions = new ArrayList<Integer>(mCursor.getCount());
        mExtraData = new ArrayList<Object>();

        mMapCursorPositions = new HashMap<Long, Integer>(mCursor.getCount());
        final int idPosition = mCursor.getColumnIndex(columnName);

        if (mCursor.moveToFirst()) {

            do {
                mMapCursorPositions.put(mCursor.getLong(idPosition), mCursor.getPosition());
            } while (mCursor.moveToNext());


            for (int i = 0; order != null && i < order.length; i++) {
                final long id = order[i];
                if (mMapCursorPositions.containsKey(id)) {
                    mOrderedPositions.add(mMapCursorPositions.get(id));
                    mMapCursorPositions.remove(id);
                    if (extraData != null) {
                        mExtraData.add(extraData.get(i));
                    }
                } else {
                    missingIds.add(id);
                }
            }

            mCursor.moveToFirst();
        }

        return missingIds;
    }

    public ArrayList<Long> getMissingIds() {
        return mMissingIds;
    }


    public Collection<Long> getExtraIds() {
        return mMapCursorPositions.keySet();
    }

    public Object getExtraData() {
        int position = getPosition();
        return position < mExtraData.size() ? mExtraData.get(position) : null;
    }

    @Override
    public void close() {
        mCursor.close();

        super.close();
    }

    @Override
    public int getCount() {
        return mOrderedPositions.size();
    }

    @Override
    public String[] getColumnNames() {
        return mCursor.getColumnNames();
    }

    @Override
    public String getString(int column) {
        return mCursor.getString(column);
    }

    @Override
    public short getShort(int column) {
        return mCursor.getShort(column);
    }

    @Override
    public int getInt(int column) {
        return mCursor.getInt(column);
    }

    @Override
    public long getLong(int column) {
        return mCursor.getLong(column);
    }

    @Override
    public float getFloat(int column) {
        return mCursor.getFloat(column);
    }

    @Override
    public double getDouble(int column) {
        return mCursor.getDouble(column);
    }

    @Override
    public boolean isNull(int column) {
        return mCursor.isNull(column);
    }

    @Override
    public boolean onMove(int oldPosition, int newPosition) {
        if (newPosition >= 0 && newPosition < getCount()) {
            mCursor.moveToPosition(mOrderedPositions.get(newPosition));
            return true;
        }

        return false;
    }
}
