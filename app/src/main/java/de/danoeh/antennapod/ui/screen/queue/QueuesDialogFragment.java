package de.danoeh.antennapod.ui.screen.queue;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.queue.Queue;

public class QueuesDialogFragment extends DialogFragment {
    public static final String TAG = "QueuesDialogFragment";

    private QueuesDialogRecyclerAdapter.OnQueueActionsListener listener;
    private QueuesViewModel viewModel;
    private QueuesDialogRecyclerAdapter recyclerAdapter;

    public static QueuesDialogFragment newInstance(QueuesDialogRecyclerAdapter.OnQueueActionsListener listener) {
        QueuesDialogFragment fragment = new QueuesDialogFragment();
        fragment.listener = listener;
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(QueuesViewModel.class);

        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.queues_dialog, null);

        RecyclerView recyclerView = view.findViewById(R.id.queues_dialog_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerAdapter = new QueuesDialogRecyclerAdapter();
        recyclerView.setAdapter(recyclerAdapter);

        recyclerAdapter.setOnQueueActionsListener(new QueuesDialogRecyclerAdapter.OnQueueActionsListener() {
            @Override
            public void onQueueClicked(Queue queue) {

                if (listener != null) {
                    listener.onQueueClicked(queue);
                }
                dismiss();
            }

            @Override
            public void onQueueDeleteClicked(Queue queue) {
                viewModel.removeQueue(requireContext().getApplicationContext(), queue.getId());
            }
        });

        TextInputLayout addQueueTextInput = view.findViewById(R.id.queues_dialog_new_queue_text_input);

        addQueueTextInput.setEndIconOnClickListener(v -> {
            EditText queueNameEditText = view.findViewById(R.id.queues_dialog_new_queue_name);
            if (queueNameEditText != null) {
                String queueName = queueNameEditText.getText().toString().trim();
                viewModel.addQueue(requireContext().getApplicationContext(), queueName);
                queueNameEditText.setText("");
            }
        });

        viewModel.getQueues().observe(this, queues -> recyclerAdapter.submitList(queues));

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.loadQueues(requireContext());

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle("Queues").setView(view);

        return builder.create();
    }
}
