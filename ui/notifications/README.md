# :ui:notifications

This module contains generic notification-related resources, for example the notification channel IDs and notification icons. The icons specified in this module are rasterized to png. This is necessary to support old Android versions that do not support VectorDrawables. This is only needed for notification icons - for the icons within the app, appcompat handles drawing the vectors.

This module is not meant for all notification handling (this should be done in the respective modules), just generic setup code and icons.

