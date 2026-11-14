package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.RrrTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `verify brand strings and dashboard renders 13 rooms and end to end ui flow`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("RRR", context.getString(R.string.app_name))
        assertEquals("Rishika Rooms & Rentals", context.getString(R.string.app_full_name))

        composeRule.setContent {
            RrrTheme {
                RrrAppRoot()
            }
        }
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("13").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Rishika Rooms & Rentals").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_lazy_column")
            .performScrollToNode(hasTestTag("room_tile_1"))
        composeRule.onNodeWithTag("room_tile_1").assertIsDisplayed()

        // Tap Room 1 -> Allot Room -> Fill New Tenant Form -> Save
        composeRule.onNodeWithTag("room_tile_1").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("allot_room_btn").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tenant_name_input").performTextInput("Vikram Patel")
        composeRule.onNodeWithTag("tenant_mobile_input").performTextInput("9876501234")
        composeRule.onNodeWithTag("tenant_rent_input").performTextInput("4000")
        composeRule.onNodeWithTag("tenant_kyc_number_input").performTextInput("AAD11223344")
        composeRule.onNodeWithTag("tenant_save_button").performClick()
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasTestTag("tenant_save_button")).fetchSemanticsNodes().isEmpty() &&
                composeRule.onAllNodesWithText("Vikram Patel").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Vikram Patel").assertIsDisplayed()

        // Navigate back to Dashboard -> Room 1 should show Vikram Patel & Balance Outstanding
        composeRule.onNodeWithTag("top_bar_back_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dashboard_lazy_column")
            .performScrollToNode(hasTestTag("room_tile_1"))
        composeRule.onNodeWithText("Vikram Patel").assertIsDisplayed()

        // Record full payment Rs. 4000 via Payment Received button
        composeRule.onNodeWithTag("dashboard_lazy_column")
            .performScrollToNode(hasTestTag("dashboard_receive_payment_btn"))
        composeRule.onNodeWithTag("dashboard_receive_payment_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("payment_amount_input").performTextInput("4000")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("payment_save_button").performClick()
        composeRule.waitForIdle()

        // Receipt viewer should appear immediately after saving
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("OFFICIAL PAYMENT RECEIPT").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Close").performClick()
        composeRule.waitForIdle()

        // Verify Room 1 immediately shows Rent & Electricity Clear
        composeRule.onNodeWithTag("dashboard_lazy_column")
            .performScrollToNode(hasTestTag("room_tile_1"))
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Rent & Electricity Clear").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Rent & Electricity Clear").performScrollTo()
        composeRule.onNodeWithText("Rent & Electricity Clear").assertIsDisplayed()

        // Verify all 5 bottom navigation tabs work
        composeRule.onNodeWithTag("nav_tab_receipts").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("main_receive_payment_button").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_tab_electricity").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("add_electricity_bill_btn").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_tab_follow_ups").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("add_followup_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("followup_remarks_input").performTextInput("Follow up call")
        composeRule.onNodeWithTag("followup_save_button").performClick()
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Remarks: Follow up call").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("nav_tab_reports").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("report_kind_dropdown").assertIsDisplayed()
        composeRule.onNodeWithTag("reports_lazy_column")
            .performScrollToNode(hasTestTag("report_csv_btn"))
        composeRule.onNodeWithTag("report_csv_btn").assertIsDisplayed()
    }

    @Test
    fun `dashboard displays 13 room tiles with color coded rent electricity statuses and filters`() {
        val rooms = (1..13).map { num ->
            val tenant = when (num) {
                1 -> com.example.data.db.TenantEntity(
                    id = 1L, name = "Clear Tenant", mobileNumber = "9000000001",
                    currentRoomNumber = 1, monthlyRentPaise = 500_000L, entryDate = "2026-01-01",
                    kycType = "Aadhaar Card", kycNumber = "1111"
                )
                2 -> com.example.data.db.TenantEntity(
                    id = 2L, name = "Rent Due Tenant", mobileNumber = "9000000002",
                    currentRoomNumber = 2, monthlyRentPaise = 500_000L, entryDate = "2026-01-01",
                    kycType = "Aadhaar Card", kycNumber = "2222"
                )
                3 -> com.example.data.db.TenantEntity(
                    id = 3L, name = "Elec Due Tenant", mobileNumber = "9000000003",
                    currentRoomNumber = 3, monthlyRentPaise = 500_000L, entryDate = "2026-01-01",
                    kycType = "Aadhaar Card", kycNumber = "3333"
                )
                else -> null
            }
            val rentPend = if (num == 2) 500_000L else 0L
            val elecPend = if (num == 3) 25_000L else 0L
            val status = when {
                tenant == null -> com.example.ui.viewmodel.RoomStatus.VACANT
                rentPend == 0L && elecPend == 0L -> com.example.ui.viewmodel.RoomStatus.CLEAR
                else -> com.example.ui.viewmodel.RoomStatus.DUE
            }
            com.example.ui.viewmodel.RoomTileUi(
                room = com.example.data.db.RoomEntity(roomNumber = num, currentTenantId = tenant?.id),
                tenant = tenant,
                rentPendingPaise = rentPend,
                electricityPendingPaise = elecPend,
                totalDuePaise = rentPend + elecPend,
                status = status
            )
        }
        val state = com.example.ui.viewmodel.RrrUiState(
            isReady = true,
            roomTiles = rooms,
            summary = com.example.ui.viewmodel.DashboardSummary(
                totalRooms = 13,
                occupiedRooms = 3,
                vacantRooms = 10,
                totalRentPendingPaise = 500_000L,
                totalElectricityPendingPaise = 25_000L
            )
        )

        composeRule.setContent {
            RrrTheme {
                com.example.ui.screens.DashboardScreen(
                    uiState = state,
                    onRoomClick = {},
                    onOpenAddTenant = {},
                    onOpenReceivePayment = {},
                    onGenerateDueRent = {},
                    onOpenOverdueFollowUps = {},
                    onOpenBackupSection = {}
                )
            }
        }
        composeRule.waitForIdle()

        // Filter to Clear -> Room 1 shown, Room 2 hidden
        composeRule.onNodeWithTag("dashboard_lazy_column").performScrollToNode(hasTestTag("filter_rooms_clear"))
        composeRule.onNodeWithTag("filter_rooms_clear").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dashboard_lazy_column").performScrollToNode(hasTestTag("room_tile_1"))
        composeRule.onNodeWithText("Clear Tenant").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodes(hasTestTag("room_tile_2")).fetchSemanticsNodes().size)

        // Filter to Due -> Room 2 (rent pending) & Room 3 (electricity pending) shown
        composeRule.onNodeWithTag("dashboard_lazy_column").performScrollToNode(hasTestTag("filter_rooms_due"))
        composeRule.onNodeWithTag("filter_rooms_due").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dashboard_lazy_column").performScrollToNode(hasTestTag("room_tile_2"))
        composeRule.onNodeWithText("Rent Due Tenant").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_lazy_column").performScrollToNode(hasTestTag("room_tile_3"))
        composeRule.onNodeWithText("Elec Due Tenant").assertIsDisplayed()

        // Filter to All -> Room 13 reachable
        composeRule.onNodeWithTag("dashboard_lazy_column").performScrollToNode(hasTestTag("filter_rooms_all"))
        composeRule.onNodeWithTag("filter_rooms_all").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dashboard_lazy_column").performScrollToNode(hasTestTag("room_tile_13"))
        composeRule.onNodeWithTag("room_tile_13").assertIsDisplayed()
    }
}
