# :net:sync:wear-interface

This module contains the shared data paths and serialization logic for communication between the phone app and the Wear OS companion app.
`WearDataPaths` defines message path constants; `WearSerializer` handles JSON encoding. Both `:app` and `:app-wearos` depend on this module.

## Communication

Most requests are watch-initiated: the watch sends a message to a path (e.g. `WearDataPaths.QUEUE`), the phone responds with data on the same path.
Rarely, the phone proactively sends a message (playback state changes).
Both directions use `Wearable.getMessageClient().sendMessage(nodeId, path, payload)`.
The actual communication is located in `:app` and `:app-wearos`, the `:net:sync:wear-interface` defines the common protocol.

## Path constants (`WearDataPaths`)

- `NOW_PLAYING`: current episode + playing state; pushed by phone on player status changes and returned on watch request
- `QUEUE`, `DOWNLOADS`, `EPISODES`, `SUBSCRIPTIONS`: episode/feed lists
- `PLAY_PREFIX + itemId`: watch sends to start playback of an episode on the phone
- `PAUSE`: watch sends to pause playback on the phone
- `FEED_EPISODES_PREFIX + feedId`: watch requests episodes for a specific feed
- `OPEN_ON_PHONE_PREFIX + itemId`: watch requests the phone to open an episode in the main app
