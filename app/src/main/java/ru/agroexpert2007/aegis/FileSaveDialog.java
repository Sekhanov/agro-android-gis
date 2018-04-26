package ru.agroexpert2007.aegis;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class FileSaveDialog extends DialogFragment{

    public interface DataExchange {
        public void exchange(String s);
    }

    private DataExchange mDataExchange;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
           mDataExchange = (DataExchange) context;
        } catch (ClassCastException e) {
            Log.d("skhanov", "Activity not implemented DataExchange");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View fileDialogView = layoutInflater.inflate(R.layout.file_dialog_layout, null);
        final EditText editTextFileInput = (EditText) fileDialogView.findViewById(R.id.file_name_text_edit);
        builder.setTitle("Имя KML файда").setView(fileDialogView).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mDataExchange.exchange(editTextFileInput.getText().toString());
            }
        });
        return builder.create();
    }
}
