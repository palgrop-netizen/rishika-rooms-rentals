package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MoneyText
import com.example.ui.components.StatusPill
import com.example.ui.theme.CardSurface
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySecondary
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.StatusGreenBg
import com.example.ui.theme.StatusGreenBorder
import com.example.ui.theme.StatusGreenText
import com.example.ui.theme.StatusRedBg
import com.example.ui.theme.StatusRedBorder
import com.example.ui.theme.StatusRedText
import com.example.ui.theme.StatusVacantBg
import com.example.ui.theme.StatusVacantBorder
import com.example.ui.theme.StatusVacantText
import com.example.ui.theme.WarningBg
import com.example.ui.theme.WarningBorder
import com.example.ui.theme.WarningText
import com.example.ui.viewmodel.RoomStatus
import com.example.ui.viewmodel.RoomTileUi
import com.example.ui.viewmodel.RrrUiState
import com.example.util.MoneyUtils

enum class DashboardRoomFilter(val label: String) {
    ALL("All"),
    DUE("Due"),
    CLEAR("Clear"),
    VACANT("Vacant")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    uiState: RrrUiState,
    onRoomClick: (RoomTileUi) -> Unit,
    onOpenAddTenant: (roomNumber: Int?) -> Unit,
    onOpenReceivePayment: () -> Unit,
    onGenerateDueRent: () -> Unit,
    onOpenOverdueFollowUps: () -> Unit,
    onOpenBackupSection: () -> Unit,
    modifier: Modifier = Modifier
) {
    var roomFilter by remember { mutableStateOf(DashboardRoomFilter.ALL) }

    val clearCount = remember(uiState.roomTiles) { uiState.roomTiles.count { it.status == RoomStatus.CLEAR } }
    val dueCount = remember(uiState.roomTiles) { uiState.roomTiles.count { it.status == RoomStatus.DUE } }
    val vacantCount = remember(uiState.roomTiles) { uiState.roomTiles.count { it.status == RoomStatus.VACANT } }

    val filteredTiles = remember(uiState.roomTiles, roomFilter) {
        when (roomFilter) {
            DashboardRoomFilter.ALL -> uiState.roomTiles
            DashboardRoomFilter.DUE -> uiState.roomTiles.filter { it.status == RoomStatus.DUE }
            DashboardRoomFilter.CLEAR -> uiState.roomTiles.filter { it.status == RoomStatus.CLEAR }
            DashboardRoomFilter.VACANT -> uiState.roomTiles.filter { it.status == RoomStatus.VACANT }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val availableWidth = maxWidth
        val roomColumns = when {
            availableWidth >= 900.dp -> 4
            availableWidth >= 600.dp -> 3
            availableWidth >= 340.dp -> 2
            else -> 1
        }
        val kpiColumns = if (availableWidth >= 700.dp) 4 else 2
        val roomRows = filteredTiles.chunked(roomColumns)

        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("dashboard_lazy_column")
        ) {
            // Overdue Follow-ups Banner on App Open
            if (uiState.overdueFollowUps.isNotEmpty()) {
                item {
                    AlertBannerCard(
                        icon = Icons.Default.NotificationsActive,
                        title = "${uiState.overdueFollowUps.size} Overdue Follow-Up${if (uiState.overdueFollowUps.size > 1) "s" else ""}",
                        subtitle = "Tap to review pending payment promises.",
                        bgColor = StatusRedBg,
                        borderColor = StatusRedBorder,
                        textColor = StatusRedText,
                        onClick = onOpenOverdueFollowUps,
                        testTag = "overdue_followups_banner"
                    )
                }
            }

            // Automatic Backup Reminder Banner
            if (uiState.showBackupReminder) {
                item {
                    AlertBannerCard(
                        icon = Icons.Default.Backup,
                        title = "Backup Recommended",
                        subtitle = "Export a JSON backup to protect your RRR rental ledger.",
                        bgColor = WarningBg,
                        borderColor = WarningBorder,
                        textColor = WarningText,
                        onClick = onOpenBackupSection,
                        testTag = "backup_reminder_banner"
                    )
                }
            }

            // Quick Actions Row (minimum 48.dp touch target)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenReceivePayment,
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1.25f)
                            .heightIn(min = 48.dp)
                            .testTag("dashboard_receive_payment_btn")
                    ) {
                        Icon(Icons.Default.CurrencyRupee, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Receive Payment", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    val firstVacant = uiState.roomTiles.firstOrNull { it.tenant == null }?.room?.roomNumber
                    OutlinedButton(
                        onClick = { onOpenAddTenant(firstVacant) },
                        enabled = firstVacant != null,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (firstVacant != null) NavyPrimary.copy(alpha = 0.5f) else OutlineSubtle),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("dashboard_add_tenant_btn")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Tenant", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    OutlinedButton(
                        onClick = onGenerateDueRent,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NavyPrimary.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("dashboard_generate_rent_btn")
                    ) {
                        Icon(Icons.Default.Autorenew, contentDescription = "Generate due rent", modifier = Modifier.size(20.dp))
                    }
                }
            }

            // 8 Summary KPIs
            item {
                val s = uiState.summary
                val kpis = listOf(
                    KpiItem("Total Rooms", "${s.totalRooms}", NavyPrimary),
                    KpiItem("Occupied Rooms", "${s.occupiedRooms}", NavySecondary),
                    KpiItem("Vacant Rooms", "${s.vacantRooms}", StatusVacantText),
                    KpiItem("Total Rent Received", MoneyUtils.formatRupees(s.totalRentReceivedPaise), StatusGreenText),
                    KpiItem("Total Rent Pending", MoneyUtils.formatRupees(s.totalRentPendingPaise), if (s.totalRentPendingPaise > 0) StatusRedText else StatusGreenText),
                    KpiItem("Electricity Pending", MoneyUtils.formatRupees(s.totalElectricityPendingPaise), if (s.totalElectricityPendingPaise > 0) StatusRedText else StatusGreenText),
                    KpiItem("Total Collected", MoneyUtils.formatRupees(s.totalAmountCollectedPaise), NavyPrimary),
                    KpiItem("Advance / Credit", MoneyUtils.formatRupees(s.totalAdvanceBalancePaise), GoldDark)
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    kpis.chunked(kpiColumns).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowItems.forEach { kpi ->
                                KpiCard(kpi, modifier = Modifier.weight(1f))
                            }
                            repeat(kpiColumns - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Section Header & Quick Color-Status Filter Chips
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "13 Rooms Overview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LegendChip("Clear", StatusGreenBorder)
                            LegendChip("Due", StatusRedBorder)
                            LegendChip("Vacant", StatusVacantBorder)
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StatusFilterChip(
                            label = "All (${uiState.roomTiles.size})",
                            selected = roomFilter == DashboardRoomFilter.ALL,
                            accentColor = NavyPrimary,
                            onClick = { roomFilter = DashboardRoomFilter.ALL },
                            testTag = "filter_rooms_all"
                        )
                        StatusFilterChip(
                            label = "Due ($dueCount)",
                            selected = roomFilter == DashboardRoomFilter.DUE,
                            accentColor = StatusRedBorder,
                            onClick = { roomFilter = DashboardRoomFilter.DUE },
                            testTag = "filter_rooms_due"
                        )
                        StatusFilterChip(
                            label = "Clear ($clearCount)",
                            selected = roomFilter == DashboardRoomFilter.CLEAR,
                            accentColor = StatusGreenBorder,
                            onClick = { roomFilter = DashboardRoomFilter.CLEAR },
                            testTag = "filter_rooms_clear"
                        )
                        StatusFilterChip(
                            label = "Vacant ($vacantCount)",
                            selected = roomFilter == DashboardRoomFilter.VACANT,
                            accentColor = StatusVacantBorder,
                            onClick = { roomFilter = DashboardRoomFilter.VACANT },
                            testTag = "filter_rooms_vacant"
                        )
                    }
                }
            }

            // Filtered / 13 Room Tiles
            if (filteredTiles.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        border = BorderStroke(1.dp, OutlineSubtle),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MeetingRoom,
                                contentDescription = null,
                                tint = NavyPrimary.copy(alpha = 0.45f),
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "No rooms in ${roomFilter.label} status",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = NavyPrimary
                            )
                        }
                    }
                }
            } else {
                items(roomRows.size) { rowIndex ->
                    val rowTiles = roomRows[rowIndex]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowTiles.forEach { tile ->
                            RoomStatusTile(
                                tile = tile,
                                onClick = { onRoomClick(tile) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(roomColumns - rowTiles.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

private data class KpiItem(val label: String, val value: String, val valueColor: Color)

@Composable
private fun KpiCard(item: KpiItem, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, OutlineSubtle),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            MoneyText(
                text = item.value,
                color = item.valueColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StatusFilterChip(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = accentColor.copy(alpha = 0.14f),
            selectedLabelColor = accentColor
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            selectedBorderColor = accentColor,
            borderColor = OutlineSubtle
        ),
        modifier = Modifier.testTag(testTag)
    )
}

@Composable
private fun LegendChip(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = color,
            shape = RoundedCornerShape(50),
            modifier = Modifier.size(8.dp)
        ) {}
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AlertBannerCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    bgColor: Color,
    borderColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = textColor)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = textColor)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = textColor)
            }
        }
    }
}

@Composable
fun RoomStatusTile(
    tile: RoomTileUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetBg = when (tile.status) {
        RoomStatus.CLEAR -> StatusGreenBg
        RoomStatus.DUE -> StatusRedBg
        RoomStatus.VACANT -> StatusVacantBg
    }
    val targetBorder = when (tile.status) {
        RoomStatus.CLEAR -> StatusGreenBorder
        RoomStatus.DUE -> StatusRedBorder
        RoomStatus.VACANT -> StatusVacantBorder
    }
    val targetAccentText = when (tile.status) {
        RoomStatus.CLEAR -> StatusGreenText
        RoomStatus.DUE -> StatusRedText
        RoomStatus.VACANT -> StatusVacantText
    }

    val animatedBg by animateColorAsState(targetValue = targetBg, animationSpec = tween(250), label = "tileBg")
    val animatedBorder by animateColorAsState(targetValue = targetBorder, animationSpec = tween(250), label = "tileBorder")

    Card(
        colors = CardDefaults.cardColors(containerColor = animatedBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.5.dp, animatedBorder.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("room_tile_${tile.room.roomNumber}")
    ) {
        Column {
            // Color-coded status accent bar along top edge of each room tile
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(animatedBorder, animatedBorder.copy(alpha = 0.55f))
                        )
                    )
            )

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = NavyPrimary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Room ${tile.room.roomNumber}",
                            color = GoldAccent,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Icon(
                        imageVector = when (tile.status) {
                            RoomStatus.CLEAR -> Icons.Default.CheckCircle
                            RoomStatus.DUE -> Icons.Default.Warning
                            RoomStatus.VACANT -> Icons.Default.MeetingRoom
                        },
                        contentDescription = when (tile.status) {
                            RoomStatus.CLEAR -> "Room ${tile.room.roomNumber} clear"
                            RoomStatus.DUE -> "Room ${tile.room.roomNumber} payment due"
                            RoomStatus.VACANT -> "Room ${tile.room.roomNumber} vacant"
                        },
                        tint = targetAccentText,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (tile.tenant == null) {
                    Text(
                        text = "Vacant",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = targetAccentText
                    )
                    Text(
                        text = if (tile.room.lastMeterReading > 0.0) {
                            "Last meter: ${MoneyUtils.formatUnits(tile.room.lastMeterReading)} units · Tap to allot"
                        } else {
                            "Tap to allot room or view past occupants"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = targetAccentText.copy(alpha = 0.85f)
                    )
                    StatusPill(
                        text = "Available",
                        bgColor = Color.White,
                        borderColor = StatusVacantBorder,
                        textColor = StatusVacantText
                    )
                } else {
                    Text(
                        text = tile.tenant.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    TileAmountLine("Monthly Rent", MoneyUtils.formatRupees(tile.tenant.monthlyRentPaise))
                    TileAmountLine(
                        "Rent Pending",
                        MoneyUtils.formatRupees(tile.rentPendingPaise),
                        if (tile.rentPendingPaise > 0) StatusRedText else StatusGreenText
                    )
                    TileAmountLine(
                        "Elec Pending",
                        MoneyUtils.formatRupees(tile.electricityPendingPaise),
                        if (tile.electricityPendingPaise > 0) StatusRedText else StatusGreenText
                    )

                    HorizontalDivider(color = animatedBorder.copy(alpha = 0.25f))

                    TileAmountLine(
                        "Total Due",
                        MoneyUtils.formatRupees(tile.totalDuePaise),
                        targetAccentText,
                        bold = true
                    )

                    if (tile.tenant.advanceBalancePaise > 0L) {
                        TileAmountLine(
                            "Advance Credit",
                            MoneyUtils.formatRupees(tile.tenant.advanceBalancePaise),
                            GoldDark
                        )
                    }

                    if (tile.status == RoomStatus.CLEAR) {
                        StatusPill(
                            text = "Rent & Electricity Clear",
                            bgColor = Color.White,
                            borderColor = StatusGreenBorder,
                            textColor = StatusGreenText
                        )
                    } else {
                        StatusPill(
                            text = "Balance Outstanding",
                            bgColor = Color.White,
                            borderColor = StatusRedBorder,
                            textColor = StatusRedText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TileAmountLine(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        MoneyText(
            text = value,
            color = valueColor,
            fontSize = if (bold) 13.sp else 12.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium
        )
    }
}
