package com.test.geocoords

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MapActivity : ComponentActivity() {

    private var locationManager: LocationManager? = null

    private val latitudeState = mutableStateOf("正在获取...")
    private val longitudeState = mutableStateOf("正在获取...")
    private val altitudeState = mutableStateOf("--")
    private val speedState = mutableStateOf("--")
    private val providerState = mutableStateOf("等待GPS信号...")

    private val hasValidLocationState = mutableStateOf(false)

    private val historyState = mutableStateListOf<String>()
    private val totalRecordCountState = mutableStateOf(0)

    private var lastRecordedLat: Double? = null
    private var lastRecordedLng: Double? = null

    // 海拔平滑缓冲区：大小会根据当前速度动态调整
    private val altitudeBuffer = mutableListOf<Double>()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startLocationUpdates()
        } else {
            Toast.makeText(this@MapActivity, "未授予定位权限，无法获取坐标", Toast.LENGTH_LONG).show()
            latitudeState.value = "权限被拒绝"
            longitudeState.value = "权限被拒绝"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Scaffold(
                    modifier = Modifier.statusBarsPadding(),
                    topBar = {
                        Surface(
                            color = Color(0xFF1E1E1E),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "实时坐标定位",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF121212))
                            .padding(innerPadding),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(text = "📍", fontSize = 40.sp)

                            val isReady = hasValidLocationState.value

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = isReady) { copyToClipboard() }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "当前纬度 (Latitude)", fontSize = 13.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = latitudeState.value,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50)
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(text = "当前经度 (Longitude)", fontSize = 13.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = longitudeState.value,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50)
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(text = "海拔 (m)", fontSize = 12.sp, color = Color.Gray)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = altitudeState.value,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF64B5F6)
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(text = "速度 (m/s)", fontSize = 12.sp, color = Color.Gray)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = speedState.value,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF64B5F6)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(text = "定位状态: ${providerState.value}", fontSize = 12.sp, color = Color.LightGray)

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = if (isReady) "点击卡片复制坐标" else "等待定位成功后可复制",
                                        fontSize = 11.sp,
                                        color = Color(0xFF757575)
                                    )
                                }
                            }

                            Button(
                                onClick = { checkAndRequestLocationPermission() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(text = "重新获取定位", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Text(
                                text = "轨迹记录 (${totalRecordCountState.value})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            )

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                if (historyState.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "暂无记录", fontSize = 13.sp, color = Color.Gray)
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(historyState.reversed()) { entry ->
                                            Text(
                                                text = entry,
                                                fontSize = 12.sp,
                                                color = Color.LightGray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        checkAndRequestLocationPermission()
    }

    private fun copyToClipboard() {
        if (!hasValidLocationState.value) return
        val text = "${latitudeState.value}, ${longitudeState.value}"
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("坐标", text))
        Toast.makeText(this, "已复制坐标: $text", Toast.LENGTH_SHORT).show()
    }

    private fun checkAndRequestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                startLocationUpdates()
            }
            else -> {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
        val isNetworkEnabled = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

        if (!isGpsEnabled && !isNetworkEnabled) {
            Toast.makeText(this, "请先在手机设置中打开GPS定位服务", Toast.LENGTH_LONG).show()
            providerState.value = "GPS未开启"
            return
        }

        providerState.value = "正在监听定位..."

        val lastLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        lastLocation?.let { updateUiWithLocation(it) }

        try {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000L,
                1f,
                object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        updateUiWithLocation(location)
                        providerState.value = "定位成功 (${location.provider})"
                    }
                    override fun onProviderEnabled(provider: String) {
                        providerState.value = "GPS已启用"
                    }
                    override fun onProviderDisabled(provider: String) {
                        providerState.value = "GPS已关闭"
                    }
                }
            )
        } catch (e: Exception) {
            providerState.value = "监听失败: ${e.message}"
        }
    }

    private fun updateUiWithLocation(location: Location) {
        latitudeState.value = location.latitude.toString()
        longitudeState.value = location.longitude.toString()

        // 根据当前速度动态决定平滑程度：
        // 移动越快，缓冲区越小，海拔跟得越紧，避免过山车之类的场景显示滞后
        if (location.hasAltitude()) {
            val speed = if (location.hasSpeed()) location.speed else 0f
            val bufferSize = when {
                speed > 5.0f -> 1   // 快速移动（车速以上）：不平滑，直接用最新值
                speed > 1.0f -> 3   // 中速移动（走路/慢跑）：轻度平滑
                else -> 5           // 静止或极慢：多平滑，消除抖动
            }

            altitudeBuffer.add(location.altitude)
            while (altitudeBuffer.size > bufferSize) altitudeBuffer.removeAt(0)

            val avgAltitude = altitudeBuffer.average()
            altitudeState.value = String.format(Locale.getDefault(), "%.1f", avgAltitude)
        } else {
            altitudeState.value = "--"
        }

        speedState.value = if (location.hasSpeed()) {
            String.format(Locale.getDefault(), "%.1f", location.speed)
        } else "--"

        hasValidLocationState.value = true

        val isSameAsLast = lastRecordedLat == location.latitude && lastRecordedLng == location.longitude
        if (!isSameAsLast) {
            lastRecordedLat = location.latitude
            lastRecordedLng = location.longitude

            totalRecordCountState.value += 1

            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val entry = "$time  ${"%.5f".format(location.latitude)}, ${"%.5f".format(location.longitude)}"
            historyState.add(entry)

            if (historyState.size > 30) {
                historyState.removeAt(0)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager = null
    }
}