package com.climb.companion.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FeedScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Kilter feed", style = MaterialTheme.typography.headlineMedium)
        Text("Swipe-up feed and route detail coming in Sprint 2.", style = MaterialTheme.typography.bodyMedium)
        ClimbCard(name = "Neon Traverse", grade = "V5", angle = "40°")
        ClimbCard(name = "Blue Moon", grade = "V3", angle = "20°")
    }
}

@Composable
private fun ClimbCard(name: String, grade: String, angle: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(name, style = MaterialTheme.typography.titleLarge)
            Text("$grade · $angle board", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
