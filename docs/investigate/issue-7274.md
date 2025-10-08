### Investigation of Issue #7274: HTTP Basic Auth for Images

The user is reporting that images for podcasts protected with HTTP Basic Authentication are not loading. This is because the image loading requests do not include the necessary authentication credentials.

My investigation of the codebase reveals the following:

1.  **Image Loading:** The app uses the [Glide](https://github.com/bumptech/glide) library for image loading. The main configuration for Glide is in `ApGlideModule.java`.

2.  **Networking:** Glide is configured to use a custom `ApOkHttpUrlLoader`, which in turn uses `AntennapodHttpClient`. This is the correct place to integrate the authentication logic.

3.  **Authentication:** The `AntennapodHttpClient` is capable of handling HTTP Basic Authentication. However, the image loading process does not provide the credentials to the HTTP client. The `HttpDownloader` for feeds and media files, on the other hand, correctly uses the credentials from the `DownloadRequest`.

4.  **The Problem:** The image loading is initiated with just a URL string. At that point, the authentication information associated with the podcast feed is not being passed to the networking layer.

### Proposed Solution

To fix this, I propose the following changes:

1.  **Create a custom Glide model:**
    Introduce a new class, for example `AuthenticatedImageUrl`, that will encapsulate both the image URL and the authentication credentials (username and password).

2.  **Create a custom `ModelLoader`:**
    Implement a `ModelLoader` and a corresponding `ModelLoaderFactory` for the `AuthenticatedImageUrl` class. This loader will be responsible for handling our custom model.

3.  **Create a custom `DataFetcher`:**
    The `ModelLoader` will create a `DataFetcher` that:
    *   Receives the `AuthenticatedImageUrl` object.
    *   Creates a dedicated `OkHttpClient` instance.
    *   Configures the client with an `Authenticator` that provides the username and password from the `AuthenticatedImageUrl` model.
    *   Executes the image request using this authenticated client.

4.  **Update UI Components:**
    Modify the UI components that load images (e.g., `SubscriptionViewHolder`, `EpisodeItemViewHolder`) to use the new `AuthenticatedImageUrl` model instead of a plain URL string. These components have access to the `Feed` object, which contains the authentication credentials.

This approach will ensure that image requests for protected feeds are properly authenticated, resolving the issue reported by the user.

### Alternative Approach

This approach avoids creating a new Glide model and modifying UI components. Instead, it uses a global cache for authentication credentials.

1.  **Create a credential cache:**
    Introduce a new singleton class, for example `ImageAuthenticator`, to cache authentication credentials. This class will store a mapping from a hostname to the corresponding `Feed`'s authentication credentials.

2.  **Populate the cache:**
    In `storage/database/src/main/java/de/danoeh/antennapod/storage/database/DBWriter.java`, modify the `setCompleteFeed` method (around line 688). After a feed is saved, extract its hostname and authentication credentials and store them in the `ImageAuthenticator` cache.

    ```java
    // In DBWriter.java, inside setCompleteFeed method
    for (Feed feed : feeds) {
        if (feed.getHttpAuthenticator() != null) {
            try {
                URI uri = new URI(feed.getDownloadUrl());
                String host = uri.getHost();
                ImageAuthenticator.getInstance().storeCredentials(host,
                        feed.getHttpHttpAuthenticator().getUsername(),
                        feed.getHttpAuthenticator().getPassword());
            } catch (URISyntaxException e) {
                // ignore
            }
        }
    }
    ```

3.  **Add an authentication interceptor:**
    In `ui/glide/src/main/java/de/danoeh/antennapod/ui/glide/ApOkHttpUrlLoader.java`, add a new `Interceptor` to the `OkHttpClient` created in the `ApOkHttpUrlLoader.Factory`. This interceptor will:
    *   For each outgoing image request, get the hostname from the request URL.
    *   Look up the hostname in the `ImageAuthenticator` cache.
    *   If credentials are found, add the `Authorization` header to the request.

    ```java
    // In ApOkHttpUrlLoader.Factory.getInternalClient method
    OkHttpClient.Builder builder = AntennapodHttpClient.newBuilder();
    builder.interceptors().add(new NetworkAllowanceInterceptor());
    builder.interceptors().add(new UserAgentInterceptor());
    builder.interceptors().add(new AuthenticationInterceptor()); // Add this
    builder.cache(null); // Handled by Glide
    internalClient = builder.build();
    ```

This alternative approach is less intrusive as it does not require changes to the UI layer. It centralizes the authentication logic for images within the networking layer.

### Fourth Approach: Fetching Credentials from Database

This approach avoids a credential cache and instead fetches the credentials from the database on-demand for each image request.

**Note on Performance:** This approach will execute a database query for every new image request. While this is done on a background thread, it is less performant than the caching approach and may impact image loading performance, especially on devices with slow storage.

Here is a breakdown of the proposed changes:

#### 1.  `storage/database/src/main/java/de/danoeh/antennapod/storage/database/DBReader.java`

Add a new method to fetch feed credentials by hostname.

```java
@Nullable
public static String[] getFeedCredentialsByHost(final String host) {
    if (host == null) {
        return null;
    }
    PodDBAdapter adapter = PodDBAdapter.getInstance();
    adapter.open();
    try (Cursor cursor = adapter.getFeedsWithCredentialsCursor()) {
        while (cursor.moveToNext()) {
            String url = cursor.getString(cursor.getColumnIndex(PodDBAdapter.KEY_DOWNLOAD_URL));
            String username = cursor.getString(cursor.getColumnIndex(PodDBAdapter.KEY_HTTP_USERNAME));
            String password = cursor.getString(cursor.getColumnIndex(PodDBAdapter.KEY_HTTP_PASSWORD));
            if (url != null && username != null && password != null) {
                try {
                    URI uri = new URI(url);
                    if (host.equals(uri.getHost())) {
                        return new String[]{username, password};
                    }
                } catch (URISyntaxException e) {
                    // ignore
                }
            }
        }
    } finally {
        adapter.close();
    }
    return null;
}
```

#### 2.  `storage/database/src/main/java/de/danoeh/antennapod/storage/database/PodDBAdapter.java`

Add a new method to get a cursor for feeds with credentials.

```java
public Cursor getFeedsWithCredentialsCursor() {
    return db.query(TABLE_NAME,
            new String[]{KEY_ID, KEY_DOWNLOAD_URL, KEY_HTTP_USERNAME, KEY_HTTP_PASSWORD},
            KEY_HTTP_USERNAME + " IS NOT NULL AND " + KEY_HTTP_PASSWORD + " IS NOT NULL",
            null, null, null, null);
}
```

#### 3.  `ui/glide/src/main/java/de/danoeh/antennapod/ui/glide/ApOkHttpUrlLoader.java`

Modify the interceptor in the `getInternalClient` method to fetch credentials from the database.

**Around line 50**, in the `getInternalClient` method of the `Factory` class, add the interceptor:

```java
private static OkHttpClient getInternalClient() {
    if (internalClient == null) {
        synchronized (Factory.class) {
            if (internalClient == null) {
                OkHttpClient.Builder builder = AntennapodHttpClient.newBuilder();
                builder.interceptors().add(new NetworkAllowanceInterceptor());
                // Add the new interceptor here
                builder.interceptors().add(chain -> {
                    Request request = chain.request();
                    String host = request.url().host();
                    String[] credentials = DBReader.getFeedCredentialsByHost(host);
                    if (credentials != null) {
                        String credential = Credentials.basic(credentials[0], credentials[1]);
                        Request newRequest = request.newBuilder()
                                .addHeader("Authorization", credential)
                                .build();
                        return chain.proceed(newRequest);
                    }
                    return chain.proceed(request);
                });
                builder.cache(null); // Handled by Glide
                internalClient = builder.build();
            }
        }
    }
    return internalClient;
}
```
### Simpler Alternative Approach

A simpler approach is to create a centralized cache for authentication credentials that can be accessed by the image loading logic. This avoids passing credentials through multiple layers of the application and requires minimal code changes.

Here is a breakdown of the proposed changes:

#### 1.  `net/common/src/main/java/de/danoeh/antennapod/net/common/AntennapodHttpClient.java`

We can add a static map to this class to store the authentication credentials for each host.

**Around line 35**, add the following:

```java
private static final Map<String, String> imageAuthCache = new ConcurrentHashMap<>();

public static void addImageAuth(String host, String username, String password) {
    imageAuthCache.put(host, Credentials.basic(username, password));
}

public static String getImageAuth(String host) {
    return imageAuthCache.get(host);
}
```

#### 2.  `storage/database/src/main/java/de/danoeh/antennapod/storage/database/DBWriter.java`

We need to populate the cache when a feed with authentication is saved. The `setCompleteFeed` method is the perfect place for this.

**Around line 688**, inside the `setCompleteFeed` method, add the following logic:

```java
for (Feed feed : feeds) {
    if (feed.getHttpAuthenticator() != null) {
        try {
            URI uri = new URI(feed.getDownloadUrl());
            String host = uri.getHost();
            if (host != null) {
                AntennapodHttpClient.addImageAuth(host,
                        feed.getHttpAuthenticator().getUsername(),
                        feed.getHttpAuthenticator().getPassword());
            }
        } catch (URISyntaxException e) {
            Log.e(TAG, "Could not parse feed download url to store image auth info", e);
        }
    }
}
```

#### 3.  `ui/glide/src/main/java/de/danoeh/antennapod/ui/glide/ApOkHttpUrlLoader.java`

Finally, we need to use the cached credentials when loading images with Glide. We can do this by adding an interceptor to the `OkHttpClient` used by Glide.

**Around line 50**, in the `getInternalClient` method of the `Factory` class, add the interceptor:

```java
private static OkHttpClient getInternalClient() {
    if (internalClient == null) {
        synchronized (Factory.class) {
            if (internalClient == null) {
                OkHttpClient.Builder builder = AntennapodHttpClient.newBuilder();
                builder.interceptors().add(new NetworkAllowanceInterceptor());
                // Add the new interceptor here
                builder.interceptors().add(chain -> {
                    Request request = chain.request();
                    String host = request.url().host();
                    String credentials = AntennapodHttpClient.getImageAuth(host);
                    if (credentials != null) {
                        Request newRequest = request.newBuilder()
                                .addHeader("Authorization", credentials)
                                .build();
                        return chain.proceed(newRequest);
                    }
                    return chain.proceed(request);
                });
                builder.cache(null); // Handled by Glide
                internalClient = builder.build();
            }
        }
    }
    return internalClient;
}
```
