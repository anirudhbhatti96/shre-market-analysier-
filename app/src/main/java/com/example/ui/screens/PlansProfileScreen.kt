package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.RiskProfile
import com.example.data.model.SubscriptionTier
import com.example.data.model.UserAdvisoryProfile
import com.example.ui.theme.SovereignGold
import com.example.ui.viewmodel.UiState
import kotlin.math.roundToInt

@Composable
fun PlansProfileScreen(
    profileState: UiState<UserAdvisoryProfile>,
    onSelectTier: (SubscriptionTier) -> Unit,
    onSavePreferences: (RiskProfile, Double, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = (profileState as? UiState.Success)?.data
    val activeTier = SubscriptionTier.fromId(profile?.subscriptionTier ?: SubscriptionTier.STUDENT.id)

    var selectedRisk by remember(profile?.riskProfile) {
        mutableStateOf(RiskProfile.fromId(profile?.riskProfile ?: RiskProfile.MODERATE.id))
    }
    var thresholdSlider by remember(profile?.alertThresholdPct) {
        mutableFloatStateOf((profile?.alertThresholdPct ?: 2.5).toFloat())
    }
    var notificationsEnabled by remember(profile?.notificationsEnabled) {
        mutableStateOf(profile?.notificationsEnabled ?: true)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "D2C Subscription Plans",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "FinAdvisor uses specialized AI models tailored to Student vs. Adult investors.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Student & Adult D2C Subscription Cards
        SubscriptionTier.entries.forEach { tier ->
            item(key = tier.id) {
                val isSelected = tier == activeTier
                val accent = if (tier == SubscriptionTier.STUDENT) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    SovereignGold
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectTier(tier) }
                        .testTag("tier_card_${tier.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) accent.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) accent else MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (tier == SubscriptionTier.STUDENT) Icons.Filled.School else Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = tier.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = tier.targetSegment,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                color = accent.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = tier.priceLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = accent,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "AI Engine: ${tier.aiEngineLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        Text(
                            text = tier.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        tier.features.forEach { feat ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = feat,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { onSelectTier(tier) },
                            enabled = !isSelected,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("activate_tier_${tier.id}_button")
                        ) {
                            Text(if (isSelected) "Active Plan" else "Switch to ${tier.displayName}")
                        }
                    }
                }
            }
        }

        // Advisory Risk & Alert Preferences
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Advisory & Alert Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text("Risk Appetite", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RiskProfile.entries.forEach { risk ->
                            FilterChip(
                                selected = selectedRisk == risk,
                                onClick = { selectedRisk = risk },
                                label = { Text(risk.label) },
                                modifier = Modifier.testTag("risk_chip_${risk.id}")
                            )
                        }
                    }

                    Text(
                        text = "Default Volatility Alert Threshold: ${String.format("%.1f", (thresholdSlider * 2).roundToInt() / 2.0)}%",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = thresholdSlider,
                        onValueChange = { thresholdSlider = it },
                        valueRange = 0.5f..10.0f
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Personalized Price Notifications",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it },
                            modifier = Modifier.testTag("notifications_switch")
                        )
                    }

                    Button(
                        onClick = {
                            val rounded = (thresholdSlider * 2).roundToInt() / 2.0
                            onSavePreferences(selectedRisk, rounded, notificationsEnabled)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_preferences_button")
                    ) {
                        Text("Save Advisory Preferences")
                    }
                }
            }
        }

        // MBA Entrepreneurship D2C Model Summary Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Filled.BusinessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "FinAdvisor D2C Business Model",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "• Dual-Model AI Cost Architecture: Student tier routes to ultra-fast Flash-Lite for high-volume literacy ($4.99/mo); Adult Pro routes to Gemini 3.8 Flash for multi-factor quant alpha ($19.99/mo).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "• Global Wedge: Combines US equities with NIKKEI, DAX & BSE divergence analytics so retail investors capture cross-border trends early.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
