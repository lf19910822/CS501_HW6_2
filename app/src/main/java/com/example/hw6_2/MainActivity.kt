package com.example.hw6_2

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hw6_2.ui.theme.HW6_2Theme

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var magnetometer: Sensor? = null
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    
    private var gravity = FloatArray(3)
    private var geomagnetic = FloatArray(3)
    
    private var heading by mutableStateOf(0f)
    private var roll by mutableStateOf(0f)
    private var pitch by mutableStateOf(0f)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        
        enableEdgeToEdge()
        setContent {
            HW6_2Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CompassAndLevelScreen(
                        heading = heading,
                        roll = roll,
                        pitch = pitch,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }
    
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    gravity = it.values.clone()
                    updateCompassHeading()
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    geomagnetic = it.values.clone()
                    updateCompassHeading()
                }
                Sensor.TYPE_GYROSCOPE -> {
                    updateDigitalLevel()
                }
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
    
    private fun updateCompassHeading() {
        if (gravity.isNotEmpty() && geomagnetic.isNotEmpty()) {
            val R = FloatArray(9)
            val I = FloatArray(9)
            
            if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(R, orientation)
                
                // Azimuth (heading) in radians, convert to degrees
                val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                heading = (azimuth + 360) % 360
            }
        }
    }
    
    private fun updateDigitalLevel() {
        if (gravity.isNotEmpty()) {
            val R = FloatArray(9)
            val I = FloatArray(9)
            
            if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(R, orientation)
                
                // Roll and Pitch in radians, convert to degrees
                roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
                pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
            }
        }
    }
}

@Composable
fun CompassAndLevelScreen(
    heading: Float,
    roll: Float,
    pitch: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Compass Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Compass",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Compass needle
            CompassNeedle(heading = heading)
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Heading: ${heading.toInt()}°",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 20.sp
            )
            Text(
                text = getDirection(heading),
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp
            )
        }
        
        // Digital Level Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Digital Level",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Roll: ${roll.toInt()}°",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Pitch: ${pitch.toInt()}°",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 20.sp
            )
        }
    }
}

@Composable
fun CompassNeedle(heading: Float) {
    Canvas(
        modifier = Modifier.size(200.dp)
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.minDimension / 2
        
        // Draw compass circle
        drawCircle(
            color = Color.LightGray,
            radius = radius,
            center = Offset(centerX, centerY)
        )
        
        // Draw cardinal directions
        drawCircle(
            color = Color.Black,
            radius = 5f,
            center = Offset(centerX, centerY)
        )
        
        // Rotate and draw needle
        rotate(degrees = -heading, pivot = Offset(centerX, centerY)) {
            // North pointer (red)
            drawLine(
                color = Color.Red,
                start = Offset(centerX, centerY),
                end = Offset(centerX, centerY - radius * 0.7f),
                strokeWidth = 8f
            )

            // South pointer (gray)
            drawLine(
                color = Color.Gray,
                start = Offset(centerX, centerY),
                end = Offset(centerX, centerY + radius * 0.5f),
                strokeWidth = 8f
            )
        }

        // Draw "N" at top
        drawCircle(
            color = Color.Red,
            radius = 8f,
            center = Offset(centerX, 20f)
        )
    }
}

fun getDirection(heading: Float): String {
    return when {
        heading >= 337.5 || heading < 22.5 -> "North"
        heading >= 22.5 && heading < 67.5 -> "Northeast"
        heading >= 67.5 && heading < 112.5 -> "East"
        heading >= 112.5 && heading < 157.5 -> "Southeast"
        heading >= 157.5 && heading < 202.5 -> "South"
        heading >= 202.5 && heading < 247.5 -> "Southwest"
        heading >= 247.5 && heading < 292.5 -> "West"
        heading >= 292.5 && heading < 337.5 -> "Northwest"
        else -> "Unknown"
    }
}