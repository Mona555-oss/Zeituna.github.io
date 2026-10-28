package com.alfleyla.zeituna.data

import com.alfleyla.zeituna.data.models.*
import com.alfleyla.zeituna.utils.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

object BookititApiService {
    private const val HOST = "app.bookitit.com"
    private const val ROOT = "api/11"
    private const val PUBLIC_KEY = "10c907a8df9bc380e1cf9ba485948f5f7"
    private const val PRIVATE_KEY = "17b24296caf70b766778711a15df5ce3d"

    private var proxyUrl: String? = null

    fun setProxy(url: String?) {
        proxyUrl = url
        platformLog("BookititAPI", "Proxy set to: $url")
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60000 
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 60000
        }
    }

    private fun ensureBkt(id: String): String {
        val trimmed = id.trim()
        if (trimmed.isEmpty()) return ""
        return if (!trimmed.startsWith("bkt", ignoreCase = true)) "bkt$trimmed" else trimmed
    }

    private fun buildPath(urlPart: String): String {
        return "$ROOT/${urlPart.trimStart('/')}"
    }

    private fun getBaseUrl(): String {
        val p = proxyUrl
        return if (p != null) "${p.trimEnd('/')}/" else "https://$HOST/"
    }

    private fun makeBasicAuth(urlPart: String): String {
        val cleanPath = urlPart.trimStart('/')
        val hashBytes = hmacMd5(PRIVATE_KEY.encodeToByteArray(), cleanPath.encodeToByteArray())
        val signature = hashBytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
        val credentials = "$PUBLIC_KEY:$signature"
        return "Basic ${base64Encode(credentials.encodeToByteArray())}"
    }

    private fun safeParseJson(input: String): JsonElement? {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || !trimmed.startsWith("{")) return null
        return try { Json.parseToJsonElement(trimmed.replace("'", "\"")) } catch (e: Exception) { null }
    }

    private fun extractHours(json: JsonElement): List<String> {
        return try {
            val root = json.jsonObject
            val slotsData = root["slots"]?.jsonObject 
                ?: (if (root["freeslots"] is JsonObject) root["freeslots"]?.jsonObject else null)
                ?: root
            
            val hoursNode = if (slotsData is JsonObject) {
                slotsData["hours"] ?: slotsData["hour"] ?: slotsData
            } else if (root["freeslots"] is JsonArray) {
                root["freeslots"]
            } else slotsData

            when (hoursNode) {
                is JsonArray -> hoursNode.mapNotNull {
                    if (it is JsonPrimitive) it.content
                    else it.jsonObject["hour"]?.jsonPrimitive?.content
                }
                is JsonObject -> {
                    val hourNode = hoursNode["hour"]
                    if (hourNode is JsonArray) hourNode.map { it.jsonPrimitive.content }
                    else if (hourNode is JsonPrimitive) listOf(hourNode.content)
                    else {
                        hoursNode.values.mapNotNull {
                            if (it is JsonPrimitive) it.content
                            else it.jsonObject["hour"]?.jsonPrimitive?.content
                        }
                    }
                }
                is JsonPrimitive -> listOf(hoursNode.content)
                else -> emptyList()
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getAvailability(serviceId: String, date: String, agendaId: String): List<BookititSlot> {
        val sId = ensureBkt(serviceId)
        val aId = ensureBkt(agendaId)

        if (sId.isEmpty() || aId.isEmpty() || date.isBlank()) return emptyList()

        val urlPart = "getfreeslots/$PUBLIC_KEY/$sId/$aId/${date.trim()}"
        val fullPath = buildPath(urlPart)

        return try {
            val response = client.get("${getBaseUrl()}$fullPath") {
                header("Authorization", makeBasicAuth(urlPart))
                header("Accept", "application/json")
                if (proxyUrl != null) header("X-Requested-With", "XMLHttpRequest")
            }
            val body = response.bodyAsText()
            val json = safeParseJson(body) ?: return emptyList()
            
            // Check for explicit Bookitit errors returned as JSON
            if (json.jsonObject["slots"]?.jsonObject?.get("status")?.jsonPrimitive?.content == "false") {
                platformLog("BookititAPI", "API Error: ${json.jsonObject["slots"]?.jsonObject?.get("message")}")
                return emptyList()
            }

            extractHours(json).map { BookititSlot(start = it.replace("*", ""), end = "") }
        } catch (e: Exception) {
            platformLog("BookititAPI", "Availability error: ${e.message}", true)
            emptyList()
        }
    }

    suspend fun getAgendaConfiguration(agendaId: String): Result<AgendaConfig> {
        val aId = ensureBkt(agendaId)
        val urlPart = "getagendaconfiguration/$PUBLIC_KEY/$aId"
        val fullPath = buildPath(urlPart)
        return try {
            val response = client.get("${getBaseUrl()}$fullPath") {
                header("Authorization", makeBasicAuth(urlPart))
            }
            val body = response.bodyAsText()
            val json = safeParseJson(body)
            val configNode = json?.jsonObject?.get("configuration") ?: json?.jsonObject?.get("data")?.jsonObject?.get("configuration")
            if (configNode != null) Result.success(Json { ignoreUnknownKeys = true }.decodeFromJsonElement<AgendaConfig>(configNode))
            else Result.failure(Exception("Config not found"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun findClientByEmail(email: String): String? {
        val urlPart = "getclientbyvalidationfield/$PUBLIC_KEY"
        val fullPath = buildPath(urlPart)
        return try {
            val response = client.post("${getBaseUrl()}$fullPath") {
                header("Authorization", makeBasicAuth(urlPart))
                setBody(FormDataContent(Parameters.build {
                    append("p_sFieldType", "email")
                    append("p_sFieldvalue", email.trim().lowercase())
                    append("format", "json")
                }))
            }
            val body = response.bodyAsText()
            val json = safeParseJson(body)
            val clientNode = json?.jsonObject?.get("client") ?: json?.jsonObject?.get("data")?.jsonObject?.get("client")
            clientNode?.jsonObject?.get("id")?.jsonPrimitive?.content
        } catch (e: Exception) { null }
    }

    suspend fun addClient(name: String, email: String): String? {
        val urlPart = "addclient/$PUBLIC_KEY"
        val fullPath = buildPath(urlPart)
        return try {
            val response = client.post("${getBaseUrl()}$fullPath") {
                header("Authorization", makeBasicAuth(urlPart))
                setBody(FormDataContent(Parameters.build {
                    append("p_sName", name)
                    append("p_sEmail", email.trim().lowercase())
                    append("p_sWebaccess", "true")
                    append("format", "json")
                }))
            }
            val body = response.bodyAsText()
            val json = safeParseJson(body)
            val clientNode = json?.jsonObject?.get("client") ?: json?.jsonObject?.get("data")?.jsonObject?.get("client")
            clientNode?.jsonObject?.get("id")?.jsonPrimitive?.content
        } catch (e: Exception) { null }
    }

    suspend fun createBooking(request: BookititBookingRequest, agendaId: String): Result<String> {
        val urlPart = "addevent/$PUBLIC_KEY"
        val fullPath = buildPath(urlPart)
        val startMins = try {
            val parts = request.time.split(":")
            parts[0].trim().toInt() * 60 + parts[1].trim().take(2).toInt()
        } catch (e: Exception) { 0 }

        return try {
            val response = client.post("${getBaseUrl()}$fullPath") {
                header("Authorization", makeBasicAuth(urlPart))
                header("Accept", "application/json")
                setBody(FormDataContent(Parameters.build {
                    append("p_sAgendaID", ensureBkt(agendaId))
                    append("p_sServiceID", ensureBkt(request.service_id))
                    append("p_sDateFrom", request.date)
                    append("p_sDateTo", request.date)
                    append("p_iStartMinutes", startMins.toString())
                    append("p_iEndMinutes", (startMins + request.duration).toString())
                    append("p_sComments", "Web Booking")
                    if (!request.clientId.isNullOrBlank()) append("p_sIdClient", ensureBkt(request.clientId))
                    else {
                        append("p_sClientName", request.name)
                        append("p_sClientEmail", request.email)
                    }
                    append("format", "json")
                }))
            }
            val body = response.bodyAsText()
            val json = safeParseJson(body)
            val eventIdNode = json?.jsonObject?.get("event") ?: json?.jsonObject?.get("data")?.jsonObject?.get("event")
            val id = eventIdNode?.jsonObject?.get("id")?.jsonPrimitive?.content
            if (id != null) Result.success(id)
            else Result.failure(Exception("Booking failed: $body"))
        } catch (e: Exception) { Result.failure(e) }
    }
    
    suspend fun getAgendaEvents(agendaId: String, start: String, end: String, timezone: String? = null): List<BookititEvent> {
        val urlPart = "getagendaevents/$PUBLIC_KEY/${ensureBkt(agendaId)}/$start/$end"
        val fullPath = buildPath(urlPart)
        return try {
            val response = client.get("${getBaseUrl()}$fullPath") {
                header("Authorization", makeBasicAuth(urlPart))
                if (timezone != null) parameter("p_sTimezone", timezone)
            }
            val body = response.bodyAsText()
            val json = safeParseJson(body)
            val eventsNode = json?.jsonObject?.get("events") ?: json?.jsonObject?.get("data")?.jsonObject?.get("events")
            if (eventsNode is JsonArray) Json { ignoreUnknownKeys = true }.decodeFromJsonElement<List<BookititEvent>>(eventsNode)
            else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getClientEvents(clientId: String, timezone: String? = null): List<BookititEvent> {
        val urlPart = "getclientevents/$PUBLIC_KEY/${ensureBkt(clientId)}"
        val fullPath = buildPath(urlPart)
        return try {
            val response = client.get("${getBaseUrl()}$fullPath") {
                header("Authorization", makeBasicAuth(urlPart))
                if (timezone != null) parameter("p_sTimezone", timezone)
            }
            val body = response.bodyAsText()
            val json = safeParseJson(body)
            val eventsNode = json?.jsonObject?.get("events") ?: json?.jsonObject?.get("data")?.jsonObject?.get("events")
            if (eventsNode is JsonArray) Json { ignoreUnknownKeys = true }.decodeFromJsonElement<List<BookititEvent>>(eventsNode)
            else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun deleteEvent(eventId: String, sendNotification: Boolean, deletedBy: String): Result<Unit> {
        val urlPart = "deleteevent/$PUBLIC_KEY/${ensureBkt(eventId)}"
        val fullPath = buildPath(urlPart)
        return try {
            val response = client.post("${getBaseUrl()}$fullPath") {
                header("Authorization", makeBasicAuth(urlPart))
                setBody(FormDataContent(Parameters.build {
                    append("p_bSendNotification", if (sendNotification) "1" else "0")
                    append("p_sDeletedBy", deletedBy)
                    append("format", "json")
                }))
            }
            if (response.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("Delete failed: ${response.status}"))
        } catch (e: Exception) { Result.failure(e) }
    }
}
