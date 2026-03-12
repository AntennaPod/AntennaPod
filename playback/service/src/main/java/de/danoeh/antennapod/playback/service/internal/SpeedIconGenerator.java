package de.danoeh.antennapod.playback.service.internal;

import de.danoeh.antennapod.playback.service.R;

class SpeedIconGenerator {
    private static final int[][] RESOURCE_IDS = {
            {},
            {},
            {R.drawable.ic_speed_step_1_of_2, R.drawable.ic_speed_step_2_of_2},
            {R.drawable.ic_speed_step_1_of_3, R.drawable.ic_speed_step_2_of_3,
                    R.drawable.ic_speed_step_3_of_3},
            {R.drawable.ic_speed_step_1_of_4, R.drawable.ic_speed_step_2_of_4,
                    R.drawable.ic_speed_step_3_of_4, R.drawable.ic_speed_step_4_of_4},
            {R.drawable.ic_speed_step_1_of_5, R.drawable.ic_speed_step_2_of_5,
                    R.drawable.ic_speed_step_3_of_5, R.drawable.ic_speed_step_4_of_5,
                    R.drawable.ic_speed_step_5_of_5},
            {R.drawable.ic_speed_step_1_of_6, R.drawable.ic_speed_step_2_of_6,
                    R.drawable.ic_speed_step_3_of_6, R.drawable.ic_speed_step_4_of_6,
                    R.drawable.ic_speed_step_5_of_6, R.drawable.ic_speed_step_6_of_6},
            {R.drawable.ic_speed_step_1_of_7, R.drawable.ic_speed_step_2_of_7,
                    R.drawable.ic_speed_step_3_of_7, R.drawable.ic_speed_step_4_of_7,
                    R.drawable.ic_speed_step_5_of_7, R.drawable.ic_speed_step_6_of_7,
                    R.drawable.ic_speed_step_7_of_7},
            {R.drawable.ic_speed_step_1_of_8, R.drawable.ic_speed_step_2_of_8,
                    R.drawable.ic_speed_step_3_of_8, R.drawable.ic_speed_step_4_of_8,
                    R.drawable.ic_speed_step_5_of_8, R.drawable.ic_speed_step_6_of_8,
                    R.drawable.ic_speed_step_7_of_8, R.drawable.ic_speed_step_8_of_8}
    };

    static int getSpeedStepIconResId(int totalSteps, int activeStep) {
        if (totalSteps < 2 || totalSteps > 8 || activeStep < 1 || activeStep > totalSteps) {
            return R.drawable.ic_notification_playback_speed;
        }
        return RESOURCE_IDS[totalSteps][activeStep - 1];
    }
}
