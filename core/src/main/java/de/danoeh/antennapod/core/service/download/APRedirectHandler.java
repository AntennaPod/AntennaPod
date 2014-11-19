package de.danoeh.antennapod.core.service.download;

import android.util.Log;
import de.danoeh.antennapod.core.BuildConfig;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultRedirectHandler;
import org.apache.http.protocol.HttpContext;

import java.net.URI;

public class APRedirectHandler extends DefaultRedirectHandler {
	// Identifier for logger
	private static final String TAG = "APRedirectHandler";
	// Header field, which has to be potentially fixed
	private static final String LOC = "Location";
	// Regular expressions for character strings, which should not appear in URLs
	private static final String CHi[] = { "\\{", "\\}", "\\|", "\\\\", "\\^", "~",   "\\[", "\\]", "\\`"};
	private static final String CHo[] = { "%7B", "%7D", "%7C", "%5C",  "%5E", "%7E", "%5B", "%5D", "%60"};

	/**
	 * Workaround for broken URLs in redirection.
	 * Proper solution involves LaxRedirectStrategy() which is not available in
	 * current API yet.
	 */
	@Override
	public URI getLocationURI(HttpResponse response, HttpContext context)
			throws org.apache.http.ProtocolException {

		Header h[] = response.getHeaders(LOC);
		if (h.length>0) {
			String s = h[0].getValue();

			// Fix broken URL
			for(int i=0; i<CHi.length;i++)
				s = s.replaceAll(CHi[i], CHo[i]);

			// If anything had to be fixed, then replace the header
			if (!s.equals(h[0].getValue()))
			{
				if (BuildConfig.DEBUG)
					Log.d(TAG, "Original URL: " + h[0].getValue());
				
				response.setHeader(LOC, s);

				if (BuildConfig.DEBUG)
					Log.d(TAG, "Fixed URL:    " + s);
			}
		}

		// call DefaultRedirectHandler with fixed URL
		return super.getLocationURI(response, context);
	}
}
