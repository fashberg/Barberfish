package com.jpweytjens.barberfish.datatype

import android.content.Context
import com.jpweytjens.barberfish.R
import com.jpweytjens.barberfish.datatype.shared.ConvertType
import com.jpweytjens.barberfish.datatype.shared.FieldColor
import com.jpweytjens.barberfish.datatype.shared.FieldState
import com.jpweytjens.barberfish.datatype.shared.cyclePreview
import com.jpweytjens.barberfish.extension.streamDataFlow
import com.jpweytjens.barberfish.extension.streamUserProfile
import com.jpweytjens.barberfish.extension.toErrorFieldState
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UserProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

private const val LABEL_LEFT = "Dist\nLeft"
private const val LABEL_DONE = "Dist\nDone"
private val ICON = R.drawable.ic_time_to_dest

@OptIn(ExperimentalCoroutinesApi::class)
class DistanceToDestinationField(private val karooSystem: KarooSystemService) :
    BarberfishDataType("barberfish", "distance-to-destination") {

    override fun liveFlow(context: Context): Flow<FieldState> =
        karooSystem.streamUserProfile().flatMapLatest { profile ->
            combine(
                karooSystem.streamDataFlow(DataType.Type.DISTANCE_TO_DESTINATION),
                karooSystem.streamDataFlow(DataType.Type.DISTANCE),
            ) { destState, distState -> toFieldState(destState, distState, profile) }
        }

    override fun previewFlow(context: Context): Flow<FieldState> =
        karooSystem.streamUserProfile().flatMapLatest { profile ->
            cyclePreview(previewStates(profile))
        }

    companion object {
        fun toFieldState(destState: StreamState, distState: StreamState, profile: UserProfile): FieldState {
            destState.toErrorFieldState(LABEL_LEFT, ICON)?.let { return it }
            val streaming = destState as StreamState.Streaming
            val onRoute = streaming.dataPoint.values[DataType.Field.ON_ROUTE]
            val rawDest = if (onRoute != null && onRoute == 0.0) null
                          else streaming.dataPoint.values[DataType.Field.DISTANCE_TO_DESTINATION]

            if (rawDest != null) {
                return FieldState(
                    primary = "%.1f".format(ConvertType.DISTANCE.apply(rawDest, profile)),
                    label = LABEL_LEFT,
                    color = FieldColor.Default,
                    iconRes = ICON,
                )
            }

            // No route — fall back to distance ridden
            val rawRidden = (distState as? StreamState.Streaming)
                ?.dataPoint?.values?.get(DataType.Field.DISTANCE)
            return FieldState(
                primary = "%.1f".format(ConvertType.DISTANCE.apply(rawRidden ?: 0.0, profile)),
                label = LABEL_DONE,
                color = FieldColor.Default,
                iconRes = ICON,
            )
        }

        fun previewStates(profile: UserProfile): List<FieldState> =
            // raw values in meters: 42200, 15000, 3500
            listOf(42200.0, 15000.0, 3500.0).map { rawM ->
                FieldState(
                    primary = "%.1f".format(ConvertType.DISTANCE.apply(rawM, profile)),
                    label = LABEL_LEFT,
                    color = FieldColor.Default,
                    iconRes = ICON,
                )
            }
    }
}
