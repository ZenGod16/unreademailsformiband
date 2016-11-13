package com.godzen.unreademailsformiband.helper;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

/**
 * Created by godzen on 13/11/16.
 */
public class GmailHelper {

    private static final String TAG = GmailHelper.class.getSimpleName();


    private static final String ACCOUNT_TYPE_GOOGLE = "com.google";

    private static final String SECTIONED_INBOX_CANONICAL_NAME_PREFIX = "^sq_ig_i_";
    private static final String SECTIONED_INBOX_CANONICAL_NAME_PERSONAL = "^sq_ig_i_personal";


    private static GmailHelper ourInstance = new GmailHelper();

    public static GmailHelper getInstance() {
        return ourInstance;
    }

    private GmailHelper() {
    }


    public int getUnreadEmails(Context context)
    {
        int unread = 0;

        String labelCanonical = GmailContract.Labels.LabelCanonicalNames.CANONICAL_NAME_INBOX;

        Cursor cursor = tryOpenLabelsCursor(context, "mat90c@gmail.com");
        if (cursor == null || cursor.isAfterLast()) {
            Log.e(TAG, "No Gmail inbox information found for account.");
            if (cursor != null) {
                cursor.close();
            }
            return 0;
        }

        int accountUnread = 0;
        String lastUnreadLabelUri = null;

        while (cursor.moveToNext()) {
            int thisUnread = cursor.getInt(LabelsQuery.NUM_UNREAD_CONVERSATIONS);
            String thisCanonicalName = cursor.getString(LabelsQuery.CANONICAL_NAME);
            if (labelCanonical.equals(thisCanonicalName)) {
                accountUnread = thisUnread;
                if (thisUnread > 0) {
                    lastUnreadLabelUri = cursor.getString(LabelsQuery.URI);
                }
                break;
            } else if (!TextUtils.isEmpty(thisCanonicalName)
                    && thisCanonicalName.startsWith(SECTIONED_INBOX_CANONICAL_NAME_PREFIX)) {
                accountUnread += thisUnread;
                if (thisUnread > 0
                        && SECTIONED_INBOX_CANONICAL_NAME_PERSONAL.equals(thisCanonicalName)) {
                    lastUnreadLabelUri = cursor.getString(LabelsQuery.URI);
                }
            }
        }

        if (accountUnread > 0) {
            //unreadPerAccount.add(new Pair<String, Integer>(account, accountUnread));
            unread += accountUnread;
        }

        cursor.close();


        Log.d(TAG, "unread = "+unread);

        return unread;
    }





    private Cursor tryOpenLabelsCursor(Context context, String account) {
        try {
            return context.getContentResolver().query(
                    GmailContract.Labels.getLabelsUri(account),
                    LabelsQuery.PROJECTION,
                    null, // NOTE: the Labels API doesn't allow selections here
                    null,
                    null);

        } catch (Exception e) {
            // From developer console: "Permission Denial: opening provider com.google.android.gsf..
            // From developer console: "SQLiteException: no such table: labels"
            // From developer console: "NullPointerException"
            Log.e(TAG, "Error opening Gmail labels", e);
            return null;
        }
    }

    private interface LabelsQuery {
        String[] PROJECTION = {
                GmailContract.Labels.NUM_UNREAD_CONVERSATIONS,
                GmailContract.Labels.URI,
                GmailContract.Labels.CANONICAL_NAME,
        };

        int NUM_UNREAD_CONVERSATIONS = 0;
        int URI = 1;
        int CANONICAL_NAME = 2;
    }
}
