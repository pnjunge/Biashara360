package com.app.biashara.data

import com.app.biashara.data.remote.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.*

class NativeShiftClientTest {
    private val access = NativeAccess(listOf("HOSPITALITY"), listOf("hospitality.view", "hospitality.orders"))
    private fun client(block: (String) -> String) = HttpClient(MockEngine { request ->
        respond(block(request.url.encodedPath), headers = headersOf(HttpHeaders.ContentType, "application/json"))
    }) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        defaultRequest { contentType(ContentType.Application.Json) }
    }
    @Test fun nonHospitalityUsersDoNotRequestShiftData() = runBlocking<Unit> {
        val client = client { error("Unexpected request") }
        try { assertNull(NativeShiftClient(client).check(NativeAccess())) } finally { client.close() }
    }
    @Test fun disabledHospitalityDoesNotRequestPersonalDuty() = runBlocking<Unit> {
        val client = client { path ->
            assertEquals("/v1/hospitality/status", path)
            """{"success":true,"data":{"enabled":false,"shiftOpen":false}}"""
        }
        try { assertNull(NativeShiftClient(client).check(access)) } finally { client.close() }
    }
    @Test fun openDayWithoutPersonalShiftStillPromptsCashier() = runBlocking<Unit> {
        val client = client { path -> when (path) {
            "/v1/hospitality/status" -> """{"success":true,"data":{"enabled":true,"shiftOpen":true}}"""
            "/v1/hospitality/duty" -> """{"success":true,"data":{"shift":null}}"""
            else -> error("Unexpected request: $path")
        } }
        try {
            val state = assertNotNull(NativeShiftClient(client).check(access))
            assertTrue(state.needsPrompt)
            assertTrue(state.canStartPersonal)
            assertFalse(state.canOpenDay)
        } finally { client.close() }
    }
    @Test fun alreadyOpenPersonalShiftDoesNotPrompt() = runBlocking<Unit> {
        val client = client { path -> when (path) {
            "/v1/hospitality/status" -> """{"success":true,"data":{"enabled":true,"shiftOpen":true}}"""
            "/v1/hospitality/duty" -> """{"success":true,"data":{"shift":{"id":"shift-1","status":"OPEN"}}}"""
            else -> error("Unexpected request: $path")
        } }
        try { assertFalse(assertNotNull(NativeShiftClient(client).check(access)).needsPrompt) } finally { client.close() }
    }
    @Test fun viewOnlyUsersDoNotFetchPersonalShift() = runBlocking<Unit> {
        val client = client { path ->
            assertEquals("/v1/hospitality/status", path)
            """{"success":true,"data":{"enabled":true,"shiftOpen":true}}"""
        }
        try {
            val viewer = access.copy(permissions = listOf("hospitality.view"))
            assertFalse(assertNotNull(NativeShiftClient(client).check(viewer)).needsPrompt)
        } finally { client.close() }
    }
    @Test fun failedStatusCannotBeMistakenForAnOpenShift() = runBlocking<Unit> {
        val client = client { """{"success":false,"message":"Forbidden"}""" }
        try { assertFailsWith<IllegalStateException> { NativeShiftClient(client).check(access) } } finally { client.close() }
    }
    @Test fun failedPersonalDutyIsShownAsAnError() = runBlocking<Unit> {
        val client = client { path -> if (path.endsWith("/status"))
            """{"success":true,"data":{"enabled":true,"shiftOpen":true}}"""
            else """{"success":false,"message":"Unable to load duty"}"""
        }
        try { assertFailsWith<IllegalStateException> { NativeShiftClient(client).check(access) } } finally { client.close() }
    }
    @Test fun rejectedShiftWritesAreNotReportedAsSuccess() = runBlocking<Unit> {
        val client = client { """{"success":false,"message":"Permission required"}""" }
        try {
            assertFailsWith<IllegalStateException> { NativeShiftClient(client).openDay(0.0) }
            assertFailsWith<IllegalStateException> { NativeShiftClient(client).startPersonal() }
        } finally { client.close() }
    }
    @Test fun writesUseTheBackendShiftEndpointsAndRejectInvalidFloat() = runBlocking<Unit> {
        val paths = mutableListOf<String>()
        val client = client { path -> paths.add(path); """{"success":true,"data":{"id":"shift-1"}}""" }
        try {
            val api = NativeShiftClient(client)
            assertFailsWith<IllegalArgumentException> { api.openDay(Double.NaN) }
            assertFailsWith<IllegalArgumentException> { api.openDay(-1.0) }
            api.openDay(0.0)
            api.startPersonal()
            assertEquals(listOf("/v1/hospitality/operations/shifts/open", "/v1/hospitality/duty/start"), paths)
        } finally { client.close() }
    }
}
