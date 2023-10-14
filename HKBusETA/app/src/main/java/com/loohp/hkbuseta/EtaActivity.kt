package com.loohp.hkbuseta

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberSwipeableState
import androidx.wear.compose.material.swipeable
import com.aghajari.compose.text.AnnotatedText
import com.aghajari.compose.text.asAnnotatedString
import com.loohp.hkbuseta.compose.AutoResizeText
import com.loohp.hkbuseta.compose.FontSizeRange
import com.loohp.hkbuseta.shared.Registry
import com.loohp.hkbuseta.shared.Registry.ETAQueryResult
import com.loohp.hkbuseta.shared.Shared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.ActivityUtils
import com.loohp.hkbuseta.utils.RemoteActivityUtils
import com.loohp.hkbuseta.utils.ScreenSizeUtils
import com.loohp.hkbuseta.utils.StringUtils
import com.loohp.hkbuseta.utils.clamp
import com.loohp.hkbuseta.utils.equivalentDp
import com.loohp.hkbuseta.utils.sameValueAs
import com.loohp.hkbuseta.utils.sp
import com.loohp.hkbuseta.utils.toSpanned
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject


class EtaActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shared.setDefaultExceptionHandler(this)

        val stopId = intent.extras!!.getString("stopId")
        val co = intent.extras!!.getString("co")
        val index = intent.extras!!.getInt("index")
        val stop = intent.extras!!.getString("stop")?.let { JSONObject(it) }
        val route = intent.extras!!.getString("route")?.let { JSONObject(it) }
        if (stopId == null || co == null || stop == null || route == null) {
            throw RuntimeException()
        }
        setContent {
            EtaElement(stopId, co, index, stop, route, this)
        }
    }

    override fun onStart() {
        super.onStart()
        Shared.setSelfAsCurrentActivity(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            Shared.removeSelfFromCurrentActivity(this)
        }
    }

}

@OptIn(ExperimentalWearMaterialApi::class)
@SuppressLint("MutableCollectionMutableState")
@Composable
fun EtaElement(stopId: String, co: String, index: Int, stop: JSONObject, route: JSONObject, instance: EtaActivity) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val swipe = rememberSwipeableState(initialValue = false)
    var swiping by remember { mutableStateOf(swipe.offset.value != 0F) }

    val routeNumber = route.optString("route")

    if (swipe.currentValue) {
        instance.runOnUiThread {
            val text = if (Shared.language == "en") {
                "Nearby Interchange Routes of ".plus(stop.optJSONObject("name")!!.optString("en"))
            } else {
                "".plus(stop.optJSONObject("name")!!.optString("zh")).plus(" 附近轉乘路線")
            }
            Toast.makeText(instance, text, Toast.LENGTH_LONG).show()
        }
        val intent = Intent(instance, NearbyActivity::class.java)
        intent.putExtra("interchangeSearch", true)
        intent.putExtra("lat", stop.optJSONObject("location")!!.optDouble("lat"))
        intent.putExtra("lng", stop.optJSONObject("location")!!.optDouble("lng"))
        intent.putExtra("exclude", arrayListOf(route.optString("route")))
        ActivityUtils.startActivity(instance, intent) { _ ->
            scope.launch {
                swipe.snapTo(false)
            }
        }
    }
    if (!swiping && !swipe.offset.value.sameValueAs(0F)) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        swiping = true
    } else if (swipe.offset.value.sameValueAs(0F)) {
        swiping = false
    }

    HKBusETATheme {
        Box (
            modifier = Modifier
                .fillMaxSize()
                .composed {
                    this.offset(0.dp, swipe.offset.value.coerceAtMost(0F).equivalentDp)
                }
                .swipeable(
                    state = swipe,
                    anchors = mapOf(0F to false, -ScreenSizeUtils.getScreenHeight(instance).toFloat() to true),
                    orientation = Orientation.Vertical
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background),
                verticalArrangement = Arrangement.Top
            ) {
                Shared.MainTime()
            }
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var eta: ETAQueryResult by remember { mutableStateOf(ETAQueryResult.EMPTY) }

                val lat = stop.optJSONObject("location")!!.optDouble("lat")
                val lng = stop.optJSONObject("location")!!.optDouble("lng")

                LaunchedEffect (Unit) {
                    while (true) {
                        Thread { eta = Registry.getEta(stopId, co, route, instance) }.start()
                        delay(Shared.ETA_UPDATE_INTERVAL)
                    }
                }

                Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
                Title(index, stop.optJSONObject("name")!!, lat, lng, routeNumber, co, instance)
                SubTitle(Registry.getInstance(instance).getStopSpecialDestinations(stopId, co, route), lat, lng, routeNumber, co, instance)
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(9, instance).dp))
                EtaText(eta, 1, instance)
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(3, instance).dp))
                EtaText(eta, 2, instance)
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(3, instance).dp))
                EtaText(eta, 3, instance)
                Spacer(modifier = Modifier.size(StringUtils.scaledSize(3, instance).dp))
                Button(
                    onClick = {
                        val intent = Intent(instance, EtaMenuActivity::class.java)
                        intent.putExtra("stopId", stopId)
                        intent.putExtra("co", co)
                        intent.putExtra("index", index)
                        intent.putExtra("stop", stop.toString())
                        intent.putExtra("route", route.toString())
                        instance.startActivity(intent)
                    },
                    modifier = Modifier
                        .width(StringUtils.scaledSize(55, instance).dp)
                        .height(StringUtils.scaledSize(24, instance).dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.secondary,
                        contentColor = MaterialTheme.colors.primary
                    ),
                    content = {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colors.primary,
                            fontSize = TextUnit(StringUtils.scaledSize(12F, instance), TextUnitType.Sp).clamp(max = 12.dp),
                            text = if (Shared.language == "en") "More" else "更多"
                        )
                    }
                )
            }
        }
    }
}

fun handleOpenMaps(lat: Double, lng: Double, label: String, instance: EtaActivity, longClick: Boolean, haptics: HapticFeedback): () -> Unit {
    return {
        val intent = Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(Uri.parse("geo:0,0?q=".plus(lat).plus(",").plus(lng).plus("(").plus(label).plus(")")))
        if (longClick) {
            instance.startActivity(intent)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            RemoteActivityUtils.intentToPhone(
                instance = instance,
                intent = intent,
                noPhone = {
                    instance.startActivity(intent)
                },
                failed = {
                    instance.startActivity(intent)
                },
                success = {
                    instance.runOnUiThread {
                        Toast.makeText(instance, if (Shared.language == "en") "Please check your phone" else "請在手機上繼續", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Title(index: Int, stopName: JSONObject, lat: Double, lng: Double, routeNumber: String, co: String, instance: EtaActivity) {
    val haptic = LocalHapticFeedback.current
    val name = if (Shared.language == "en") stopName.optString("en") else stopName.optString("zh")
    AutoResizeText (
        modifier = Modifier
            .fillMaxWidth()
            .padding(37.dp, 0.dp)
            .combinedClickable(
                onClick = handleOpenMaps(lat, lng, name, instance, false, haptic),
                onLongClick = handleOpenMaps(lat, lng, name, instance, true, haptic)
            ),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = if (co == "mtr") name else index.toString().plus(". ").plus(name),
        maxLines = 2,
        fontWeight = FontWeight(900),
        fontSizeRange = FontSizeRange(
            min = StringUtils.scaledSize(1F, instance).dp.sp,
            max = StringUtils.scaledSize(17F, instance).sp.clamp(max = StringUtils.scaledSize(17F, instance).dp)
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SubTitle(destName: JSONObject, lat: Double, lng: Double, routeNumber: String, co: String, instance: EtaActivity) {
    val haptic = LocalHapticFeedback.current
    val name = if (Shared.language == "en") {
        val routeName = if (co == "mtr") Shared.getMtrLineName(routeNumber, "???") else routeNumber
        routeName.plus(" To ").plus(destName.optString("en"))
    } else {
        val routeName = if (co == "mtr") Shared.getMtrLineName(routeNumber, "???") else routeNumber
        routeName.plus(" 往").plus(destName.optString("zh"))
    }
    AutoResizeText(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 0.dp)
            .combinedClickable(
                onClick = handleOpenMaps(lat, lng, name, instance, false, haptic),
                onLongClick = handleOpenMaps(lat, lng, name, instance, true, haptic)
            ),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = name,
        maxLines = 1,
        fontSizeRange = FontSizeRange(
            min = StringUtils.scaledSize(1F, instance).dp.sp,
            max = StringUtils.scaledSize(11F, instance).sp.clamp(max = StringUtils.scaledSize(11F, instance).dp)
        )
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EtaText(lines: ETAQueryResult, seq: Int, instance: EtaActivity) {
    val textSize = StringUtils.scaledSize(16F, instance).sp.clamp(max = StringUtils.scaledSize(16F, instance).dp)
    AnnotatedText(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 0.dp)
            .basicMarquee(iterations = Int.MAX_VALUE),
        textAlign = TextAlign.Center,
        fontSize = textSize,
        color = MaterialTheme.colors.primary,
        maxLines = 1,
        text = lines.getLine(seq).toSpanned(instance, textSize.value).asAnnotatedString()
    )
}