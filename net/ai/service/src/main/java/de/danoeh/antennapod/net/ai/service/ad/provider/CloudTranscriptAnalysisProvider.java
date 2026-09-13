package de.danoeh.antennapod.net.ai.service.ad.provider;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiresApi(api = Build.VERSION_CODES.O)
public class CloudTranscriptAnalysisProvider implements TranscriptAnalysisProvider {
    private static final String DEFAULT_MODEL_NAME = "gpt-5-nano";
    private static final Pattern EPISODE_DURATION_PATTERN =
            Pattern.compile("Episode duration seconds: ([\\d.]+)");

    // Base system message for ad classification
    private static final String SYSTEM_MESSAGE_BASE = """
            You are an expert at detecting advertisement and promotional segments in podcast transcripts. \
            Locate every ad/sponsor segment with precise start and end times, and report nothing else.

            INPUT FORMAT
            - The transcript is WebVTT. Each cue is a time range "HH:MM:SS.mmm --> HH:MM:SS.mmm" \
            followed by the spoken text on the next line(s).
            - All cue timestamps are absolute, measured from the start of the episode.
            - You may be given only a portion of a longer episode; judge solely from the text shown \
            and use its timestamps exactly as they appear.

            COUNTS AS AN AD (include)
            - Host-read sponsor spots and dynamically inserted ads (pre-roll, mid-roll, post-roll).
            - Paid promotions, affiliate offers, discount or promo codes, coupon URLs, giveaways.
            - Cross-promotion of other shows and the host's own paid offerings \
            (Patreon, memberships, merch, courses, live-show tickets).

            NOT AN AD (never include)
            - Normal episode content: interviews, discussion, storytelling, news.
            - Host banter, housekeeping, listener mail, credits, and intros/outros that do not promote an offer.
            - Incidental brand mentions that are part of the conversation rather than a promotion.

            BOUNDARIES (be precise)
            - startSeconds = start time of the FIRST cue where the ad read begins; \
            endSeconds = end time of the LAST cue where it ends.
            - Merge consecutive cues belonging to the same ad break into ONE segment; never split one ad into pieces.
            - Exclude surrounding non-ad sentences. Anchor times to the actual cue timestamps; do not invent times.

            PRECISION OVER RECALL
            - Report a segment only when you are confident it is an ad or promotion. \
            When unsure, leave it out: wrongly skipping real content is worse than missing a borderline ad.

            OUTPUT
            - Convert each timestamp to total seconds from episode start as a number \
            (e.g. 00:12:30.500 becomes 750.5).
            - Respond with ONLY valid minified JSON, no prose and no markdown, matching:
              {"ads":[{"startSeconds":number,"endSeconds":number,"reason":string,"confidence":number}]}
            - reason: a short phrase naming the advertiser or offer (e.g. "Squarespace sponsor read").
            - confidence: your certainty from 0.0 to 1.0 that the segment is truly an ad.
            - Sort segments by startSeconds, do not overlap them, and ensure endSeconds > startSeconds.
            - If there are no ads, respond exactly with {"ads":[]}.
            """;

    private final OpenAIClient client;
    private final String modelName;

    public CloudTranscriptAnalysisProvider(Context context) {
        this.client = CloudAiClientFactory.createAnalysisClient(context);
        String storedModel = CloudAiClientFactory.getAnalysisModelName(context);
        this.modelName = TextUtils.isEmpty(storedModel) ? DEFAULT_MODEL_NAME : storedModel;
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public String analyzeTranscript(String prompt) {
        return analyzeTranscript(prompt, null);
    }

    @Override
    public String analyzeTranscript(String prompt, ProgressListener listener) {
        if (listener != null) {
            listener.onProgress(10);
        }

        // Extract transcript and duration from the prompt
        int durationMs = extractDuration(prompt);

        // Build system message and user message separately
        String systemMessage = buildSystemMessage(durationMs);
        String userMessage = buildUserMessage(prompt);

        ChatCompletionCreateParams chatParams = ChatCompletionCreateParams.builder()
                .addSystemMessage(systemMessage)
                .addUserMessage(userMessage)
                .model(ChatModel.of(modelName))
                .build();
        if (listener != null) {
            listener.onProgress(30);
        }
        ChatCompletion completion = client.chat().completions().create(chatParams);
        if (completion.choices().isEmpty()) {
            throw new IllegalStateException("AI provider returned no choices");
        }

        if (listener != null) {
            listener.onProgress(100);
        }
        return completion.choices().get(0).message().content().orElse("");
    }

    /**
     * Build the system message with context about the episode.
     */
    private String buildSystemMessage(int durationMs) {
        StringBuilder sb = new StringBuilder(SYSTEM_MESSAGE_BASE);
        if (durationMs > 0) {
            sb.append("\n\nCONTEXT\n- Episode duration: ").append(durationMs / 1000f)
                    .append(" seconds. No ad may extend beyond this time.");
        }
        return sb.toString();
    }

    /**
     * Build the user message containing the transcript.
     */
    private String buildUserMessage(String transcript) {
        return "Find all advertisement and promotional segments in this WebVTT transcript. "
                + "Return only the JSON object described in the instructions.\n\n"
                + "Transcript (WebVTT):\n" + transcript;
    }


    private int extractDuration(String prompt) {
        Matcher matcher = EPISODE_DURATION_PATTERN.matcher(prompt);
        if (matcher.find()) {
            String duration = matcher.group(1);
            if (duration == null) {
                return 0;
            }
            try {
                return (int) (Float.parseFloat(duration) * 1000);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    @Override
    public void close() {
    }
}
