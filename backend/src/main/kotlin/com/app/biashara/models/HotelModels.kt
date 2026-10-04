package com.app.biashara.models

import kotlinx.serialization.Serializable

@Serializable data class HotelTypeRequest(val name: String, val description: String = "", val capacity: Int = 2, val nightlyRateCents: Long, val isActive: Boolean = true)
@Serializable data class HotelTypeResponse(val id: String, val name: String, val description: String, val capacity: Int, val nightlyRateCents: Long, val isActive: Boolean)
@Serializable data class HotelRoomRequest(val typeId: String, val number: String, val floor: String = "", val isActive: Boolean = true)
@Serializable data class HotelRoomResponse(val id: String, val typeId: String, val number: String, val floor: String, val housekeepingStatus: String, val notes: String, val isActive: Boolean, val available: Boolean)
@Serializable data class HotelHousekeepingRequest(val status: String, val notes: String = "")
@Serializable data class HotelReservationRequest(val roomId: String, val guestName: String, val guestPhone: String = "", val guestEmail: String = "", val guests: Int = 1, val arrival: String, val departure: String, val source: String = "DIRECT", val reference: String = "", val notes: String = "")
@Serializable data class HotelReservationResponse(val id: String, val roomId: String, val guestName: String, val guestPhone: String, val guestEmail: String, val guests: Int, val arrival: String, val departure: String, val nightlyRateCents: Long, val status: String, val source: String, val reference: String, val notes: String, val checkedInAt: String?, val checkedOutAt: String?, val balanceCents: Long)
@Serializable data class HotelFolioRequest(val kind: String, val description: String = "", val amountCents: Long = 0, val paymentId: String? = null, val orderId: String? = null, val requestId: String)
@Serializable data class HotelFolioEntryResponse(val id: String, val kind: String, val description: String, val amountCents: Long, val paymentId: String?, val orderId: String?, val createdAt: String)
@Serializable data class HotelFolioResponse(val reservation: HotelReservationResponse, val entries: List<HotelFolioEntryResponse>, val roomChargeCents: Long, val totalChargeCents: Long, val totalPaidCents: Long, val balanceCents: Long)
@Serializable data class HotelBlockRequest(val roomId: String, val arrival: String, val departure: String, val reason: String)
@Serializable data class HotelBlockResponse(val id: String, val roomId: String, val arrival: String, val departure: String, val reason: String, val source: String)
@Serializable data class HotelCalendarImportRequest(val source: String, val calendar: String)
@Serializable data class HotelPaymentRequest(val amountCents: Long, val method: String, val requestId: String)
@Serializable data class HotelPaymentResponse(val orderId: String, val paymentMethod: String, val paymentStatus: String, val amountCents: Long)
@Serializable data class HotelDashboard(val roomTypes: List<HotelTypeResponse>, val rooms: List<HotelRoomResponse>, val reservations: List<HotelReservationResponse>, val blocks: List<HotelBlockResponse>)
@Serializable data class HotelReportResponse(val startDate: String, val endDate: String, val roomNightsAvailable: Int, val roomNightsBooked: Int, val occupancyPercent: Double, val accommodationRevenueCents: Long, val averageDailyRateCents: Long, val revParCents: Long, val outstandingBalanceCents: Long, val arrivals: Int, val departures: Int, val collectedCents: Long, val refundableCreditCents: Long)
