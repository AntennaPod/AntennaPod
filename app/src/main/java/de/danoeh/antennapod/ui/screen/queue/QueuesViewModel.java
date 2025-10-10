package de.danoeh.antennapod.ui.screen.queue;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import de.danoeh.antennapod.model.queue.Queue;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class QueuesViewModel extends ViewModel {
    public static final String TAG = "QueuesViewModel";

    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<List<Queue>> queues = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LiveData<List<Queue>> getQueues() {
        return queues;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadQueues() {
        disposables.add(
                Observable.fromCallable(DBReader::df_getQueues)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    queues::postValue,
                    error -> {
                        Log.e(TAG, "Failed to load queues", error);
                        errorMessage.postValue("Failed to load queues");
                    }
                )
        );
    }

    public void addQueue(Context context, String queueName) {
        if (queueName == null || queueName.trim().isEmpty()) {
            errorMessage.postValue("Queue name cannot be empty");
            return;
        }
        disposables.add(
                Observable.fromCallable(() -> DBWriter.df_addQueue(context, queueName))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                success -> loadQueues(), // On success, reload the list
                                error -> {
                                    Log.e(TAG, "Failed to add queue", error);
                                    errorMessage.postValue("Failed to add queue: " + error.getMessage());
                                }
                        )
        );
    }

    public void removeQueue(Context context, long queueId) {
        if (queueId == 0) {
            errorMessage.postValue("Queue id cannot be 0");
            return;
        }
        disposables.add(
                Observable.fromCallable(() -> DBWriter.df_removeQueue(context, queueId))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                success -> loadQueues(), // On success, reload the list
                                error -> {
                                    Log.e(TAG, "Failed to delete queue", error);
                                    errorMessage.postValue("Failed to delete queue: " + error.getMessage());
                                }
                        )
        );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}
