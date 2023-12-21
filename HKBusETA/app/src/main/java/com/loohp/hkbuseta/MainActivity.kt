/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.loohp.hkbuseta

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Stable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.loohp.hkbuseta.app.MainLoading
import com.loohp.hkbuseta.appcontext.appContext
import com.loohp.hkbuseta.common.objects.gmbRegion
import com.loohp.hkbuseta.common.objects.operator
import com.loohp.hkbuseta.common.shared.Tiles
import com.loohp.hkbuseta.shared.AndroidShared
import com.loohp.hkbuseta.tiles.EtaTileServiceCommon
import com.loohp.hkbuseta.utils.asImmutableState


@Stable
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        AndroidShared.setDefaultExceptionHandler(this)
        Tiles.providePlatformUpdate { EtaTileServiceCommon.requestTileUpdate(it) }

        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val stopId = intent.extras?.getString("stopId")
        val co = intent.extras?.getString("co")?.operator
        val index = intent.extras?.getInt("index")
        val stop = intent.extras?.get("stop")
        val route = intent.extras?.get("route")

        val listStopRoute = intent.extras?.getByteArray("stopRoute")
        val listStopScrollToStop = intent.extras?.getString("scrollToStop")
        val listStopShowEta = intent.extras?.getBoolean("showEta")
        val listStopIsAlightReminder = intent.extras?.getBoolean("isAlightReminder")

        val queryKey = intent.extras?.getString("k")
        val queryRouteNumber = intent.extras?.getString("r")
        val queryBound = intent.extras?.getString("b")
        val queryCo = intent.extras?.getString("c")?.operator
        val queryDest = intent.extras?.getString("d")
        val queryGMBRegion = intent.extras?.getString("g")?.gmbRegion
        val queryStop = intent.extras?.getString("s")
        val queryStopIndex = intent.extras?.getInt("si")?: 0
        val queryStopDirectLaunch = intent.extras?.getBoolean("sd", false)?: false

        setContent {
            MainLoading(appContext, stopId, co, index, stop.asImmutableState(), route.asImmutableState(), listStopRoute.asImmutableState(), listStopScrollToStop, listStopShowEta, listStopIsAlightReminder, queryKey, queryRouteNumber, queryBound, queryCo, queryDest, queryGMBRegion, queryStop, queryStopIndex, queryStopDirectLaunch)
        }
    }
}