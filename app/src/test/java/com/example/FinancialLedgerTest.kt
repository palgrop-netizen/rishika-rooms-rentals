package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.repository.RentalRepository
import com.example.util.CsvExportHelper
import com.example.util.DateUtils
import com.example.util.MoneyUtils
import com.example.util.PdfAndPrintHelper
import com.example.util.ReceiptPrintData
import com.example.util.ReportPrintData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FinancialLedgerTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repo: RentalRepository

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = RentalRepository(db)
        repo.ensureInitialized("2026-03-20")
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `rent generation preserves anniversary day across month ends and suppresses deleted cycles`() = runBlocking {
        val addRes = repo.addTenant(
            name = "Aarav Sharma",
            mobileNumber = "9876543210",
            roomNumber = 1,
            monthlyRentPaise = 500_000L, // Rs. 5,000
            entryDate = "2026-01-31",
            kycType = "Aadhaar Card",
            kycNumber = "1234-5678-9012",
            kycDocumentPath = null,
            kycDocumentName = null,
            notes = "",
            asOfIso = "2026-04-05"
        )
        assertTrue(addRes.isSuccess)
        val tenantId = addRes.getOrThrow()

        val rents = repo.rentEntriesFlow.first().filter { it.tenantId == tenantId }.sortedBy { it.cycleIndex }
        assertEquals(3, rents.size)
        assertEquals("2026-01-31", rents[0].dueDate)
        assertEquals("2026-02-28", rents[1].dueDate) // Feb adjustment
        assertEquals("2026-03-31", rents[2].dueDate) // Original 31st preserved!

        // Leap year check
        val febLeap = DateUtils.addMonthsPreservingDay("2024-01-31", 1)
        assertEquals("2024-02-29", febLeap?.toIso())

        // No duplicate generation
        val dupCount = repo.generateDueRentForAllActiveTenants("2026-04-05").getOrThrow()
        assertEquals(0, dupCount)

        // Delete unpaid March entry and confirm it does NOT regenerate
        val marchRent = rents[2]
        assertTrue(repo.deleteRentEntry(marchRent.id).isSuccess)
        val afterDeleteGen = repo.generateDueRentForAllActiveTenants("2026-04-05").getOrThrow()
        assertEquals(0, afterDeleteGen)
        val remainingRents = repo.rentEntriesFlow.first().filter { it.tenantId == tenantId }
        assertEquals(2, remainingRents.size)
    }

    @Test
    fun `payment allocation follows exact Rent first Electricity second Advance third rule`() = runBlocking {
        val tenantId = repo.addTenant(
            name = "Priya Verma",
            mobileNumber = "9998887776",
            roomNumber = 2,
            monthlyRentPaise = 100_000L, // Rs. 1,000
            entryDate = "2026-03-15",
            kycType = "PAN Card",
            kycNumber = "ABCDE1234F",
            kycDocumentPath = null,
            kycDocumentName = null,
            notes = "",
            asOfIso = "2026-03-15"
        ).getOrThrow()

        // Electricity bill: 25 units @ Rs. 10 = Rs. 250 (25_000 paise)
        repo.addElectricityBill(
            roomNumber = 2,
            tenantId = tenantId,
            periodFrom = "2026-03-01",
            periodTo = "2026-03-15",
            previousReading = 100.0,
            currentReading = 125.0,
            ratePerUnitPaise = 1_000L
        ).getOrThrow()

        // Exact Rs. 1,250 -> Rent 1,000 + Electricity 250 + Advance 0
        val previewExact = repo.previewPaymentAllocation(tenantId, 125_000L)
        assertEquals(100_000L, previewExact.rentToSettlePaise)
        assertEquals(25_000L, previewExact.electricityToSettlePaise)
        assertEquals(0L, previewExact.advanceToAddPaise)

        // Overpayment Rs. 1,500 -> Rent 1,000 + Electricity 250 + Advance 250
        val receipt = repo.receivePayment(
            tenantId = tenantId,
            amountPaise = 150_000L,
            paymentDate = "2026-03-16",
            paymentMode = "UPI",
            remarks = "Full + Advance"
        ).getOrThrow()

        assertEquals(100_000L, receipt.rentAdjustedPaise)
        assertEquals(25_000L, receipt.electricityAdjustedPaise)
        assertEquals(25_000L, receipt.advanceAddedPaise)

        val tenant = repo.tenantsFlow.first().first { it.id == tenantId }
        assertEquals(25_000L, tenant.advanceBalancePaise)
        assertEquals(0L, repo.rentEntriesFlow.first().filter { it.tenantId == tenantId }.sumOf { it.pendingPaise })
        assertEquals(0L, repo.electricityBillsFlow.first().filter { it.tenantId == tenantId }.sumOf { it.pendingPaise })
    }

    @Test
    fun `full regression sequence electricity edit reconciliation receipt deletion writeoff transfer and backup restore`() = runBlocking {
        // 1. Add tenant to Room 1
        val tenantId = repo.addTenant(
            name = "Rahul Nair",
            mobileNumber = "9123456780",
            roomNumber = 1,
            monthlyRentPaise = 500_000L, // Rs. 5,000
            entryDate = "2026-01-15",
            kycType = "Voter ID",
            kycNumber = "VOT9876543",
            kycDocumentPath = null,
            kycDocumentName = null,
            notes = "Corner room",
            asOfIso = "2026-02-20"
        ).getOrThrow()

        // 2. Verify Jan & Feb rent generated (Rs. 10,000 total)
        val rentsInitial = repo.rentEntriesFlow.first().filter { it.tenantId == tenantId }.sortedBy { it.cycleIndex }
        assertEquals(2, rentsInitial.size)

        // 3. Add electricity bill: 50 units @ Rs. 8 = Rs. 400 (40_000 paise)
        val billId = repo.addElectricityBill(
            roomNumber = 1,
            tenantId = tenantId,
            periodFrom = "2026-01-15",
            periodTo = "2026-02-15",
            previousReading = 100.0,
            currentReading = 150.0,
            ratePerUnitPaise = 800L
        ).getOrThrow()

        // Overlapping bill rejected
        val overlapRes = repo.addElectricityBill(
            roomNumber = 1,
            tenantId = tenantId,
            periodFrom = "2026-02-01",
            periodTo = "2026-02-20",
            previousReading = 150.0,
            currentReading = 160.0,
            ratePerUnitPaise = 800L
        )
        assertTrue(overlapRes.isFailure)

        // 4. Receive partial payment Rs. 3,000
        val rec1 = repo.receivePayment(tenantId, 300_000L, "2026-02-16", "Cash", "Part 1").getOrThrow()
        assertEquals(300_000L, rec1.rentAdjustedPaise)

        // 5. Receive payment Rs. 8,000 (settles remaining Rs. 7,000 rent + Rs. 400 elec + Rs. 600 advance)
        val rec2 = repo.receivePayment(tenantId, 800_000L, "2026-02-17", "UPI", "Part 2").getOrThrow()
        assertEquals(700_000L, rec2.rentAdjustedPaise)
        assertEquals(40_000L, rec2.electricityAdjustedPaise)
        assertEquals(60_000L, rec2.advanceAddedPaise)

        // Paid bill cannot be deleted directly
        assertTrue(repo.deleteElectricityBill(billId).isFailure)

        // 6. Edit electricity bill downward from 150 to 125 units (Rs. 200 instead of Rs. 400)
        // Excess Rs. 200 should reconcile to Advance (Rs. 600 -> Rs. 800)!
        assertTrue(
            repo.editElectricityBill(billId, "2026-01-15", "2026-02-15", 100.0, 125.0, 800L).isSuccess
        )
        val tenantAfterEdit = repo.tenantsFlow.first().first { it.id == tenantId }
        assertEquals(80_000L, tenantAfterEdit.advanceBalancePaise)

        // 7. Generate March rent (Rs. 5,000) and apply Rs. 500 advance to March rent
        repo.generateDueRentForAllActiveTenants("2026-03-16").getOrThrow()
        val marRent = repo.rentEntriesFlow.first().first { it.tenantId == tenantId && it.cycleIndex == 2 }
        assertTrue(repo.applyAdvanceToRent(tenantId, marRent.id, 50_000L, "2026-03-16", "Apply adv").isSuccess)

        // 8. Delete receipt #2 (Rs. 8,000) -> must reverse dependent advance usage + rent/elec allocations!
        assertTrue(repo.deleteReceipt(rec2.id).isSuccess)
        val tenantAfterRecDel = repo.tenantsFlow.first().first { it.id == tenantId }
        assertEquals(0L, tenantAfterRecDel.advanceBalancePaise)
        val marRentAfterDel = repo.rentEntriesFlow.first().first { it.id == marRent.id }
        assertEquals(500_000L, marRentAfterDel.pendingPaise) // Reclaimed cleanly!
        val billAfterDel = repo.electricityBillsFlow.first().first { it.id == billId }
        assertEquals(20_000L, billAfterDel.pendingPaise) // Restored to Rs. 200 pending!

        // 9. Write off Rs. 500 on Jan rent, then undo it
        val janRent = repo.rentEntriesFlow.first().first { it.tenantId == tenantId && it.cycleIndex == 0 }
        assertEquals(200_000L, janRent.pendingPaise)
        val woId = repo.writeOffRent(janRent.id, 50_000L, "2026-03-17", "Discount").getOrThrow()
        assertEquals(150_000L, repo.rentEntriesFlow.first().first { it.id == janRent.id }.pendingPaise)
        assertTrue(repo.undoWriteOff(woId).isSuccess)
        assertEquals(200_000L, repo.rentEntriesFlow.first().first { it.id == janRent.id }.pendingPaise)

        // 10. Transfer tenant from Room 1 to Room 5
        assertTrue(repo.transferTenant(tenantId, 5, "2026-03-18", "Bigger room").isSuccess)
        val rooms = repo.roomsFlow.first().associateBy { it.roomNumber }
        assertEquals(null, rooms[1]?.currentTenantId)
        assertEquals(tenantId, rooms[5]?.currentTenantId)

        // Add another tenant to Room 1 -> balances must NOT mix!
        val tenant2Id = repo.addTenant(
            name = "Sneha Kapoor",
            mobileNumber = "9000011111",
            roomNumber = 1,
            monthlyRentPaise = 400_000L,
            entryDate = "2026-03-18",
            kycType = "Aadhaar Card",
            kycNumber = "9999-8888-7777",
            kycDocumentPath = null,
            kycDocumentName = null,
            notes = "",
            asOfIso = "2026-03-18"
        ).getOrThrow()
        assertEquals(400_000L, repo.rentEntriesFlow.first().filter { it.tenantId == tenant2Id }.sumOf { it.pendingPaise })

        // 11. Export full JSON backup -> Delete all -> Validate & Restore -> Verify consistency!
        val json = repo.exportBackupJson().getOrThrow()
        assertTrue(repo.deleteAllApplicationData().isSuccess)
        assertEquals(0, repo.tenantsFlow.first().size)

        val payload = repo.validateBackupJson(json).getOrThrow()
        assertTrue(repo.restoreBackupPayload(payload).isSuccess)

        val restoredTenants = repo.tenantsFlow.first()
        assertEquals(2, restoredTenants.size)
        val restoredRahul = restoredTenants.first { it.id == tenantId }
        assertEquals(5, restoredRahul.currentRoomNumber)
        assertEquals(200_000L, repo.rentEntriesFlow.first().first { it.id == janRent.id }.pendingPaise)
        assertEquals(1, repo.transfersFlow.first().size)

        // 12. Verify Receipt & Report rendering and CSV generation
        val receiptBmp = PdfAndPrintHelper.renderReceiptBitmap(
            context,
            ReceiptPrintData(
                receiptNumber = rec1.receiptNumber,
                tenantName = "Rahul Nair",
                roomNumber = 5,
                paymentDate = rec1.paymentDate,
                totalAmountPaise = rec1.totalAmountPaise,
                paymentMode = rec1.paymentMode,
                rentAdjustedPaise = rec1.rentAdjustedPaise,
                electricityAdjustedPaise = rec1.electricityAdjustedPaise,
                advanceAddedPaise = rec1.advanceAddedPaise,
                remarks = rec1.remarks
            )
        )
        assertEquals(595, receiptBmp.width)
        assertEquals(842, receiptBmp.height)

        val reportBmp = PdfAndPrintHelper.renderReportBitmap(
            context,
            ReportPrintData(
                title = "Tenant Report",
                subtitle = "Test",
                headers = listOf("Name", "Room", "KYC Number"),
                rows = listOf(listOf("Rahul Nair", "Room 5", "VOT9876543"))
            )
        )
        assertEquals(595, reportBmp.width)
        assertEquals(842, reportBmp.height)

        val csv = CsvExportHelper.buildCsv(
            listOf("Name", "KYC Number"),
            listOf(listOf("Rahul, Nair", "VOT9876543"))
        )
        assertTrue(csv.contains("\"Rahul, Nair\""))
        assertFalse(csv.contains("null"))
        assertEquals("Rs. 1,250", MoneyUtils.formatRupees(125_000L))
    }

    @Test
    fun `invalid backups rejected and vacate or delete preserves isolation`() = runBlocking {
        // Invalid JSON / wrong version rejected
        assertTrue(repo.validateBackupJson("{invalid").isFailure)
        assertTrue(repo.validateBackupJson("""{"app":"Other","backupVersion":1}""").isFailure)
        assertTrue(repo.validateBackupJson("""{"app":"Rishika Rooms & Rentals","backupVersion":99}""").isFailure)

        // Add Tenant A (Room 3) and Tenant B (Room 4)
        val tA = repo.addTenant(
            name = "Tenant A", mobileNumber = "9111111111", roomNumber = 3,
            monthlyRentPaise = 300_000L, entryDate = "2026-03-01",
            kycType = "Aadhaar Card", kycNumber = "1111",
            kycDocumentPath = null, kycDocumentName = null, notes = "", asOfIso = "2026-03-01"
        ).getOrThrow()
        val tB = repo.addTenant(
            name = "Tenant B", mobileNumber = "9222222222", roomNumber = 4,
            monthlyRentPaise = 400_000L, entryDate = "2026-03-01",
            kycType = "PAN Card", kycNumber = "2222",
            kycDocumentPath = null, kycDocumentName = null, notes = "", asOfIso = "2026-03-01"
        ).getOrThrow()

        // Vacate Tenant A -> Room 3 becomes vacant, Tenant A's rent history remains intact
        assertTrue(repo.vacateTenant(tA, "2026-03-10").isSuccess)
        val room3 = repo.roomsFlow.first().first { it.roomNumber == 3 }
        assertEquals(null, room3.currentTenantId)
        val archivedA = repo.tenantsFlow.first().first { it.id == tA }
        assertFalse(archivedA.isActive)
        assertEquals(300_000L, repo.rentEntriesFlow.first().filter { it.tenantId == tA }.sumOf { it.pendingPaise })

        // Delete Tenant A -> Tenant B remains completely unaffected
        assertTrue(repo.deleteTenant(tA).isSuccess)
        val remainingTenants = repo.tenantsFlow.first()
        assertEquals(1, remainingTenants.size)
        assertEquals(tB, remainingTenants.first().id)
        assertEquals(400_000L, repo.rentEntriesFlow.first().filter { it.tenantId == tB }.sumOf { it.pendingPaise })
    }
}
