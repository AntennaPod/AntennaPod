package de.danoeh.antennapod.wearos.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.Text
import de.danoeh.antennapod.ui.common.R as CommonR

@Composable
fun ListScaffold(
    titleRes: Int,
    scrollState: ScalingLazyListState,
    isPhoneSupported: Boolean,
    isTimedOut: Boolean,
    isLoading: Boolean,
    isEmpty: Boolean,
    content: ScalingLazyListScope.() -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = scrollState
        ) {
            item {
                ListHeader(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(titleRes))
                }
            }

            when {
                !isPhoneSupported -> item {
                    Text(
                        text = stringResource(CommonR.string.wearos_phone_not_supported),
                        modifier = Modifier.padding(8.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                !isLoading && !isEmpty -> content()

                isTimedOut -> item {
                    Text(
                        text = stringResource(CommonR.string.wearos_phone_not_reachable),
                        modifier = Modifier.padding(8.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                isLoading -> item { CircularProgressIndicator() }

                else -> item { Text(stringResource(CommonR.string.wearos_no_items)) }
            }
        }
        ScrollIndicator(state = scrollState, modifier = Modifier.align(Alignment.CenterEnd))
    }
}
