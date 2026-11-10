package com.climb.companion.ui.permissions

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
private val PermissionStatus.isGranted: Boolean
    get() = this is PermissionStatus.Granted

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val permissionList = buildList {
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }
    val permissionsState = rememberMultiplePermissionsState(permissionList)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Permissions", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Location helps switch the feed to your gym. Motion and notifications support live sessions. Camera is for logbook photos later.",
            style = MaterialTheme.typography.bodyMedium,
        )
        permissionList.forEach { permission ->
            val granted = permissionsState.permissions
                .find { it.permission == permission }
                ?.status
                ?.isGranted == true
            Text(
                text = "${permission.substringAfterLast('.')}: ${if (granted) "Allowed" else "Not yet"}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Button(
            onClick = { permissionsState.launchMultiplePermissionRequest() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Allow permissions")
        }
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text("Continue to feed")
        }
        OutlinedButton(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text("Not now")
        }
        TextButton(onClick = onBack) {
            Text("Back")
        }
    }
}
