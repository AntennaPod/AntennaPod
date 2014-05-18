package instrumentationTest.de.test.antennapod.util.service.download;

import android.util.Base64;
import android.util.Log;
import de.danoeh.antennapod.BuildConfig;

import java.io.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

/**
 * Http server for testing purposes
 * <p/>
 * Supported features:
 * <p/>
 * /status/code: Returns HTTP response with the given status code
 * /redirect/n:  Redirects n times
 * /delay/n:     Delay response for n seconds
 * /basic-auth/username/password: Basic auth with username and password
 * /gzip/n:      Send gzipped data of size n bytes
 * /files/id:     Accesses the file with the specified ID (this has to be added first via serveFile).
 */
public class HTTPBin extends NanoHTTPD {
    private static final String TAG = "HTTPBin";
    public static final int PORT = 8124;

    private static final String MIME_HTML = "text/html";
    private static final String MIME_PLAIN = "text/plain";

    public HTTPBin(int port) {
        super(port);
    }

    public HTTPBin() {
        super(PORT);
    }

    @Override
    public Response serve(IHTTPSession session) {

        if (BuildConfig.DEBUG) Log.d(TAG, "Requested url: " + session.getUri());

        String[] segments = session.getUri().split("/");
        if (segments.length < 3) {
            Log.w(TAG, String.format("Invalid number of URI segments: %d %s", segments.length, Arrays.toString(segments)));
            get404Error();
        }

        final String func = segments[1];
        final String param = segments[2];
        final Map<String, String> headers = session.getHeaders();

        if (func.equalsIgnoreCase("status")) {
            try {
                int code = Integer.parseInt(param);
                return getStatus(code);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return getInternalError();
            }

        } else if (func.equalsIgnoreCase("redirect")) {
            try {
                int times = Integer.parseInt(param);
                if (times < 0) {
                    throw new NumberFormatException("times <= 0: " + times);
                }

                return getRedirectResponse(times - 1);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return getInternalError();
            }
        } else if (func.equalsIgnoreCase("delay")) {
            try {
                int sec = Integer.parseInt(param);
                if (sec <= 0) {
                    throw new NumberFormatException("sec <= 0: " + sec);
                }

                Thread.sleep(sec * 1000L);
                return getOKResponse();
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return getInternalError();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return getInternalError();
            }
        } else if (func.equalsIgnoreCase("basic-auth")) {
            if (!headers.containsKey("authorization")) {
                Log.w(TAG, "No credentials provided");
                return getUnauthorizedResponse();
            }
            try {
                String credentials = new String(Base64.decode(headers.get("authorization").split(" ")[1], 0), "UTF-8");
                String[] credentialParts = credentials.split(":");
                if (credentialParts.length != 2) {
                    Log.w(TAG, "Unable to split credentials: " + Arrays.toString(credentialParts));
                    return getInternalError();
                }
                if (credentialParts[0].equals(segments[2])
                        && credentialParts[1].equals(segments[3])) {
                    Log.i(TAG, "Credentials accepted");
                    return getOKResponse();
                } else {
                    Log.w(TAG, String.format("Invalid credentials. Expected %s, %s, but was %s, %s",
                            segments[2], segments[3], credentialParts[0], credentialParts[1]));
                    return getUnauthorizedResponse();
                }

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return getInternalError();
            }
        } else if (func.equalsIgnoreCase("gzip")) {
            try {
                int size = Integer.parseInt(param);
                if (size <= 0) {
                    Log.w(TAG, "Invalid size for gzipped data: " + size);
                    throw new NumberFormatException();
                }

                return getGzippedResponse(size);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return getInternalError();
            } catch (IOException e) {
                e.printStackTrace();
                return getInternalError();
            }
        }

        return get404Error();
    }

    private Response getGzippedResponse(int size) throws IOException {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        final byte[] buffer = new byte[size];
        Random random = new Random(System.currentTimeMillis());
        random.nextBytes(buffer);

        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(compressed);
        gzipOutputStream.write(buffer);

        InputStream inputStream = new ByteArrayInputStream(compressed.toByteArray());
        Response response = new Response(Response.Status.OK, MIME_PLAIN, inputStream);
        response.addHeader("Content-encoding", "gzip");
        response.addHeader("Content-length", String.valueOf(compressed.size()));
        return response;
    }

    private Response getStatus(final int code) {
        Response.IStatus status = (code == 200) ? Response.Status.OK :
                (code == 201) ? Response.Status.CREATED :
                        (code == 206) ? Response.Status.PARTIAL_CONTENT :
                                (code == 301) ? Response.Status.REDIRECT :
                                        (code == 304) ? Response.Status.NOT_MODIFIED :
                                                (code == 400) ? Response.Status.BAD_REQUEST :
                                                        (code == 401) ? Response.Status.UNAUTHORIZED :
                                                                (code == 403) ? Response.Status.FORBIDDEN :
                                                                        (code == 404) ? Response.Status.NOT_FOUND :
                                                                                (code == 405) ? Response.Status.METHOD_NOT_ALLOWED :
                                                                                        (code == 416) ? Response.Status.RANGE_NOT_SATISFIABLE :
                                                                                                (code == 500) ? Response.Status.INTERNAL_ERROR : new Response.IStatus() {
                                                                                                    @Override
                                                                                                    public int getRequestStatus() {
                                                                                                        return code;
                                                                                                    }

                                                                                                    @Override
                                                                                                    public String getDescription() {
                                                                                                        return "Unknown";
                                                                                                    }
                                                                                                };
        return new Response(status, MIME_HTML, "");

    }

    private Response getRedirectResponse(int times) {
        if (times > 0) {
            Response response = new Response(Response.Status.REDIRECT, MIME_HTML, "This resource has been moved permanently");
            response.addHeader("Location", "/redirect/" + times);
            return response;
        } else if (times == 0) {
            return getOKResponse();
        } else {
            return getInternalError();
        }
    }

    private Response getUnauthorizedResponse() {
        Response response = new Response(Response.Status.UNAUTHORIZED, MIME_HTML, "");
        response.addHeader("WWW-Authenticate", "Basic realm=\"Test Realm\"");
        return response;
    }

    private Response getOKResponse() {
        return new Response(Response.Status.OK, MIME_HTML, "");
    }

    private Response getInternalError() {
        return new Response(Response.Status.INTERNAL_ERROR, MIME_HTML, "The server encountered an internal error");

    }

    private Response get404Error() {
        return new Response(Response.Status.NOT_FOUND, MIME_HTML, "The requested URL was not found on this server");
    }
}
