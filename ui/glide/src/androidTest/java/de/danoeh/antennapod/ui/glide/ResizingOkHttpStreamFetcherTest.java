import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow; 
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher.DataCallback;
import com.bumptech.glide.load.model.GlideUrl;
import okhttp3.Call;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

@RunWith(AndroidJUnit4.class)
public class ResizingOkHttpStreamFetcherTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private ResizingOkHttpStreamFetcher fetcher;
    private Call.Factory mockClient;
    private GlideUrl mockUrl;
    private DataCallback<InputStream> mockCallback;
    
    @Before
    public void setUp() {
        mockClient = mock(Call.Factory.class);
        mockUrl = mock(GlideUrl.class);
        mockCallback = mock(DataCallback.class);
        fetcher = new ResizingOkHttpStreamFetcher(mockClient, mockUrl);
    }

    @After
    public void tearDown() {
        fetcher.cleanup();
    }

    @Test
    public void testLoadDataWithNullInput() {
        fetcher.loadData(Priority.NORMAL, mockCallback);
        verify(mockCallback).onDataReady(null);
    }

    @Test
    public void testLoadSmallFile() throws Exception {
        File smallFile = temporaryFolder.newFile();
        try (FileOutputStream fos = new FileOutputStream(smallFile)) {
            byte[] smallContent = new byte[1024]; 
            fos.write(smallContent);
        }

        InputStream inputStream = mock(InputStream.class);
        when(inputStream.read(any())).thenReturn(-1); // EOF

        fetcher.loadData(Priority.NORMAL, mockCallback);
        
        verify(mockCallback, never()).onLoadFailed(any());
        verify(mockCallback).onDataReady(any(InputStream.class));
    }

    @Test
    public void testLoadLargeFileRequiringResize() throws Exception {
        Bitmap testBitmap = Bitmap.createBitmap(2000, 2000, Bitmap.Config.ARGB_8888);
        File largeFile = temporaryFolder.newFile();
        try (FileOutputStream fos = new FileOutputStream(largeFile)) {
            testBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        }

        InputStream inputStream = mock(InputStream.class);
        when(inputStream.read(any())).thenReturn(-1);

        fetcher.loadData(Priority.NORMAL, mockCallback);

        verify(mockCallback, never()).onLoadFailed(any());
        verify(mockCallback).onDataReady(any(InputStream.class));
    }

    @Test
    public void testErrorHandling() {
        InputStream errorStream = mock(InputStream.class);
        when(errorStream.read(any())).thenThrow(new IOException("Test error"));

        fetcher.loadData(Priority.NORMAL, mockCallback);

        verify(mockCallback).onLoadFailed(any(IOException.class));
    }

    @Test
    public void testCleanup() {
        fetcher.loadData(Priority.NORMAL, mockCallback);
        fetcher.cleanup();
        
        verify(mockCallback, never()).onLoadFailed(any());
        verify(delegate).cleanup();
    }

    @Test
    public void testCancel() {
        fetcher.loadData(Priority.NORMAL, mockCallback);
        fetcher.cancel();
        
        verify(delegate).cancel();
        verify(mockCallback, never()).onLoadFailed(any());
    }

    @Test
    public void testInvalidImage() throws Exception {
        File invalidFile = temporaryFolder.newFile();
        try (FileOutputStream fos = new FileOutputStream(invalidFile)) {
            fos.write("Not an image".getBytes());
        }

        InputStream inputStream = mock(InputStream.class);
        when(inputStream.read(any())).thenReturn(-1);

        fetcher.loadData(Priority.NORMAL, mockCallback);

        verify(mockCallback).onLoadFailed(any(IOException.class));
    }

    @Test
    public void testCompressionFormat() {
        Bitmap.CompressFormat expectedFormat = Build.VERSION.SDK_INT < 30 
            ? Bitmap.CompressFormat.WEBP 
            : Bitmap.CompressFormat.WEBP_LOSSY;
    
        Bitmap mockBitmap = mock(Bitmap.class);
        when(mockBitmap.compress(eq(expectedFormat), anyInt(), any())).thenReturn(true);
    
        fetcher.loadData(Priority.NORMAL, mockCallback);
    
        verify(mockBitmap).compress(eq(expectedFormat), anyInt(), any());
    }

    
}
