package com.example.hashpotatoesv20.Utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

public class SendMailTask extends AsyncTask {

    private ProgressDialog statusDialog;
    private Activity sendMailActivity;

    public SendMailTask(Activity activity) {
        sendMailActivity = activity;
    }

    protected void onPreExecute() {
        statusDialog = new ProgressDialog(sendMailActivity);
        statusDialog.setMessage("Sending Report...");
        statusDialog.setIndeterminate(false);
        statusDialog.setCancelable(false);
        statusDialog.show();
    }

    @Override
    protected Object doInBackground(Object... args) {
        try {
            Log.i("SendMailTask", "About to instantiate Gmail...");
            //publishProgress("Processing input....");
            Gmail androidEmail = new Gmail(args[0].toString(),
                    args[1].toString(), args[2].toString(), args[3].toString(),
                    args[4].toString());
            //publishProgress("Preparing Report....");
            androidEmail.createEmailMessage();
            //publishProgress("Sending Report....");
            androidEmail.sendEmail();
            //publishProgress("Report Sent.");
            Log.i("SendMailTask", "Report Sent.");
        } catch (Exception e) {
            publishProgress(e.getMessage());
            Log.e("SendMailTask", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void onProgressUpdate(Object... values) {
        statusDialog.setMessage(values[0].toString());

    }

    @Override
    public void onPostExecute(Object result) {
        statusDialog.dismiss();
    }

}