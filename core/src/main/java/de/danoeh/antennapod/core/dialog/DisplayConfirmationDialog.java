package de.danoeh.antennapod.core.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import androidx.annotation.StringRes;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


public class DisplayConfirmationDialog {

    public static  <T> void display(Context context, @StringRes int title,
                                    @StringRes int message, Callable<? extends T> task,
                                    StatusListener  callBack) {
        ConfirmationDialog dialog = new ConfirmationDialog(context, title, message) {
            @Override
            @SuppressLint("CheckResult")
            public void onConfirmButtonPressed(DialogInterface clickedDialog) {
                clickedDialog.dismiss();
                Observable.fromCallable(task)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> callBack.onActionSuccess(),
                                callBack::onActionFailure);
            }
        };
        dialog.createNewDialog().show();
    }
}
