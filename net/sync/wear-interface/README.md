# :net:sync:wear-interface

This module contains the shared data paths and serialization logic for communication between the phone app and the Wear OS companion app.
`WearDataPaths` defines message path constants; `WearSerializer` handles JSON encoding. Both `:app` and `:app-wearos` depend on this module.
