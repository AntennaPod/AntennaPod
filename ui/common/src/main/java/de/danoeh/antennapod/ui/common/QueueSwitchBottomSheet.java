package de.danoeh.antennapod.ui.common;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import de.danoeh.antennapod.model.feed.DefaultQueueException;
import de.danoeh.antennapod.model.feed.Queue;
import de.danoeh.antennapod.model.feed.QueueRepository;

/**
 * Bottom sheet dialog for queue selection and management.
 *
 * <p>This bottom sheet displays:
 * - List of all queues with visual indicators (color, icon, active status)
 * - "Create New Queue" button at bottom
 * - Delete functionality for non-default queues (via long-press)
 *
 * <p>Features:
 * - Tap to switch to a queue
 * - Long-press to delete a queue (with confirmation)
 * - Visual highlight for active queue
 * - Automatic dismissal after queue selection
 *
 * <p>Usage:
 * <pre>
 * QueueSwitchBottomSheet bottomSheet = QueueSwitchBottomSheet.newInstance(
 *     queueRepository,
 *     this::onQueueSelected,
 *     this::onCreateNewQueue
 * );
 * bottomSheet.show(getSupportFragmentManager(), "queue_switch");
 * </pre>
 */
public class QueueSwitchBottomSheet extends BottomSheetDialogFragment {

    private static final String TAG = "QueueSwitchBottomSheet";

    private QueueRepository queueRepository;
    private OnQueueSelectedListener queueSelectedListener;
    private OnCreateNewQueueListener createNewQueueListener;

    private RecyclerView queueListView;
    private Button createButton;
    @SuppressWarnings("FieldCanBeLocal")
    private TextView headerTitle;
    private QueueButtonAdapter adapter;

    private List<Queue> allQueues;
    private Queue activeQueue;

    /**
     * Interface for queue selection events.
     */
    public interface OnQueueSelectedListener {
        /**
         * Called when a queue is selected for switching.
         *
         * @param queue Selected queue
         */
        void onQueueSelected(@NonNull Queue queue);
    }

    /**
     * Interface for create new queue events.
     */
    public interface OnCreateNewQueueListener {
        /**
         * Called when the "Create New Queue" button is clicked.
         */
        void onCreateNewQueue();
    }

    /**
     * Creates a new QueueSwitchBottomSheet instance.
     *
     * <p>Use this factory method instead of the constructor to ensure proper
     * argument passing that survives configuration changes.
     *
     * @param queueRepository        Repository for queue operations
     * @param queueSelectedListener  Listener for queue selection
     * @param createNewQueueListener Listener for create new queue action
     * @return New QueueSwitchBottomSheet instance
     */
    public static QueueSwitchBottomSheet newInstance(
            @NonNull QueueRepository queueRepository,
            @NonNull OnQueueSelectedListener queueSelectedListener,
            @Nullable OnCreateNewQueueListener createNewQueueListener) {
        QueueSwitchBottomSheet sheet = new QueueSwitchBottomSheet();
        sheet.queueRepository = queueRepository;
        sheet.queueSelectedListener = queueSelectedListener;
        sheet.createNewQueueListener = createNewQueueListener;
        return sheet;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.queue_switch_bottom_sheet, container, false);

        // Initialize views
        headerTitle = view.findViewById(R.id.header_title);
        queueListView = view.findViewById(R.id.queue_list);
        createButton = view.findViewById(R.id.create_queue_button);

        // Set up RecyclerView
        queueListView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Load queue data
        loadQueues();

        // Set up adapter
        adapter = new QueueButtonAdapter(
                activeQueue != null ? activeQueue.getId() : -1,
                this::onQueueClicked,
                this::onQueueLongClicked
        );
        adapter.setQueues(allQueues);
        queueListView.setAdapter(adapter);

        // Set up create button
        createButton.setOnClickListener(v -> onCreateButtonClicked());

        return view;
    }

    /**
     * Loads queue data from repository.
     */
    private void loadQueues() {
        if (queueRepository != null) {
            allQueues = queueRepository.getAllQueues();
            activeQueue = queueRepository.getActiveQueue();
        }
    }

    /**
     * Handles queue button click.
     *
     * @param queue Clicked queue
     */
    private void onQueueClicked(@NonNull Queue queue) {
        if (queueSelectedListener != null) {
            queueSelectedListener.onQueueSelected(queue);
        }
        dismiss();
    }

    /**
     * Handles queue button long-click for deletion.
     *
     * @param queue Long-clicked queue
     * @return True if handled
     */
    private boolean onQueueLongClicked(@NonNull Queue queue) {
        // Cannot delete default queue
        if (queue.isDefault()) {
            Toast.makeText(requireContext(),
                    "Cannot delete the default queue",
                    Toast.LENGTH_SHORT).show();
            return true;
        }

        // Show confirmation dialog
        showDeleteConfirmationDialog(queue);
        return true;
    }

    /**
     * Shows confirmation dialog for queue deletion.
     *
     * @param queue Queue to delete
     */
    private void showDeleteConfirmationDialog(@NonNull Queue queue) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Queue")
                .setMessage("Are you sure you want to delete \"" + queue.getName() + "\"?\n\n"
                        + "All episodes in this queue will be removed from the queue "
                        + "(episodes themselves will not be deleted).")
                .setPositiveButton("Delete", (dialog, which) -> deleteQueue(queue))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * Deletes a queue via repository.
     *
     * @param queue Queue to delete
     */
    private void deleteQueue(@NonNull Queue queue) {
        if (queueRepository == null) {
            return;
        }

        try {
            queueRepository.deleteQueue(queue.getId()).get();
            // Reload queues after deletion
            loadQueues();
            adapter.setQueues(allQueues);
            adapter.setActiveQueueId(activeQueue != null ? activeQueue.getId() : -1);
            Toast.makeText(requireContext(),
                    "Queue \"" + queue.getName() + "\" deleted",
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            String errorMessage = "Failed to delete queue";
            if (e.getCause() instanceof DefaultQueueException) {
                errorMessage = "Cannot delete the default queue";
            }
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handles "Create New Queue" button click.
     */
    private void onCreateButtonClicked() {
        if (createNewQueueListener != null) {
            createNewQueueListener.onCreateNewQueue();
        }
        dismiss();
    }

    /**
     * Updates the displayed queue list.
     *
     * <p>Call this method to refresh the queue list after external changes.
     */
    public void refreshQueues() {
        loadQueues();
        if (adapter != null) {
            adapter.setQueues(allQueues);
            adapter.setActiveQueueId(activeQueue != null ? activeQueue.getId() : -1);
        }
    }
}
