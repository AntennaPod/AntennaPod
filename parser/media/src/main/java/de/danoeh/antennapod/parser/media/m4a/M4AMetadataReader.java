package de.danoeh.antennapod.parser.media.m4a;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Reads of an M4A file.
 */
public class M4AMetadataReader extends M4AReader {
    private static final String TAG = "M4AMetadataReader";
    private static final String ATOM_METADATA = "moov.udta.meta";
    private static final String[] DESCRIPTION_ATOMS = {
        "moov.udta.meta.ilst.desc.data",
        "moov.udta.meta.ilst.ldes.data",
        "moov.udta.meta.ilst.©cmt.data"
    };
    private static final int FULL_BOX_HEADER_LENGTH = 4;
    private static final int DATA_HEADER_LENGTH = 8;
    private static final int DATA_TYPE_UTF8 = 1;
    private static final int MAX_DESCRIPTION_LENGTH = 1024 * 1024;

    private String description = null;

    public M4AMetadataReader(InputStream input) {
        super(input, DESCRIPTION_ATOMS);
    }

    public void readInputStream() throws IOException {
        readAtoms();
    }

    @Override
    protected int childAtomsOffset(String path) {
        // Skip version and flags
        return ATOM_METADATA.equals(path) ? FULL_BOX_HEADER_LENGTH : 0;
    }

    @Override
    protected void onAtom(String path, long payloadLength) throws IOException {
        if (payloadLength <= DATA_HEADER_LENGTH || payloadLength > MAX_DESCRIPTION_LENGTH) {
            Log.d(TAG, "Skipping " + path + " of unrealistic length " + payloadLength);
            return;
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate((int) payloadLength).order(ByteOrder.BIG_ENDIAN);
        readFully(byteBuffer.array());
        if (byteBuffer.getInt() != DATA_TYPE_UTF8) {
            Log.d(TAG, "Skipping " + path + " because it does not hold text");
            return;
        }
        String value = new String(byteBuffer.array(), DATA_HEADER_LENGTH,
                (int) payloadLength - DATA_HEADER_LENGTH, StandardCharsets.UTF_8);
        if (description == null || value.length() > description.length()) {
            description = value;
        }
    }

    public String getDescription() {
        return description;
    }
}
