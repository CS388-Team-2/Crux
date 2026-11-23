package com.climb.companion.ui.feed

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class OpenBetaClient : OutdoorSource {

    override fun loadStates(): List<OutdoorArea> {
        val statesBody = post(STATES_QUERY)
        val children = data(statesBody)
            .getJSONObject("area")
            .getJSONArray("children")
        val states = buildList {
            for (index in 0 until children.length()) {
                val child = children.getJSONObject(index)
                val uuid = text(child, "uuid")
                if (!uuid.isUuid()) continue
                add(OutdoorArea(uuid = uuid, name = text(child, "area_name"), photoCount = 0))
            }
        }
        if (states.isEmpty()) return emptyList()
        val counts = photoCounts(states.map { it.uuid })
        return states.map { state ->
            state.copy(photoCount = counts[state.uuid] ?: 0)
        }.filter { it.photoCount > 0 }
    }

    override fun photos(areaUuid: String, after: String?): PhotoPage {
        val body = post(photosQuery(areaUuid, after))
        val connection = data(body)
            .getJSONObject("areaMediaPagination")
            .getJSONObject("mediaConnection")
        val hasNextPage = connection.getJSONObject("pageInfo").optBoolean("hasNextPage", false)
        val edges = connection.optJSONArray("edges")
        val photos = buildList {
            if (edges == null) return@buildList
            for (index in 0 until edges.length()) {
                val edge = edges.getJSONObject(index)
                val node = edge.optJSONObject("node") ?: continue
                val tags = readTags(node.optJSONArray("entityTags"))
                add(
                    TaggedPhoto(
                        cursor = text(edge, "cursor"),
                        mediaUrl = text(node, "mediaUrl"),
                        tags = tags,
                    )
                )
            }
        }
        return PhotoPage(hasNextPage = hasNextPage, photos = photos)
    }

    private fun readTags(tagsJson: org.json.JSONArray?): List<EntityTag> {
        if (tagsJson == null) return emptyList()
        return buildList {
            for (tagIndex in 0 until tagsJson.length()) {
                val tag = tagsJson.optJSONObject(tagIndex) ?: continue
                add(
                    EntityTag(
                        type = tag.optInt("type", -1),
                        targetId = text(tag, "targetId"),
                        climbName = text(tag, "climbName"),
                        areaName = text(tag, "areaName"),
                    )
                )
            }
        }
    }

    override fun grades(climbIds: List<String>): Map<String, GradePair> {
        val ids = climbIds.filter { it.isUuid() }.distinct()
        if (ids.isEmpty()) return emptyMap()
        val body = post(gradesQuery(ids))
        val payload = data(body)
        return ids.mapIndexed { index, id ->
            val climb = payload.optJSONObject("c$index")
            val grades = climb?.optJSONObject("grades")
            id to GradePair(
                yds = grades?.let { text(it, "yds") },
                vscale = grades?.let { text(it, "vscale") },
            )
        }.toMap()
    }

    private fun photoCounts(uuids: List<String>): Map<String, Int> {
        val valid = uuids.filter { it.isUuid() }
        if (valid.isEmpty()) return emptyMap()
        val body = post(countsQuery(valid))
        val payload = data(body)
        return valid.mapIndexed { index, uuid ->
            val total = payload.optJSONObject("a$index")
                ?.optJSONObject("mediaConnection")
                ?.optJSONObject("pageInfo")
                ?.optInt("totalItems", 0)
                ?: 0
            uuid to total
        }.toMap()
    }

    private fun post(query: String): String {
        val connection = (URL(API_URL).openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 20_000
            connection.readTimeout = 20_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "Crux")
            val payload = JSONObject().put("query", query).toString()
            connection.outputStream.use { stream ->
                stream.write(payload.toByteArray(Charsets.UTF_8))
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IOException("OpenBeta request failed ($code)")
            return response
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val API_URL = "https://api.openbeta.io"
        private const val USA_AREA_ID = "1db1e8ba-a40e-587c-88a4-64f5ea814b8e"
        private val UUID_PATTERN =
            Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

        private val STATES_QUERY = """
            query {
              area(uuid: "$USA_AREA_ID") {
                children { uuid area_name }
              }
            }
        """.trimIndent()

        private fun countsQuery(uuids: List<String>): String {
            val fields = uuids.mapIndexed { index, uuid ->
                """
                a$index: areaMediaPagination(input: { areaUuid: "$uuid", first: 1 }) {
                  mediaConnection { pageInfo { totalItems } }
                }
                """.trimIndent()
            }.joinToString("\n")
            return "query {\n$fields\n}"
        }

        private fun photosQuery(areaUuid: String, after: String?): String {
            require(areaUuid.isUuid()) { "area id" }
            val afterPart = if (after.isNullOrBlank()) "" else ", after: ${graphqlQuoted(after)}"
            return """
                query {
                  areaMediaPagination(input: { areaUuid: "$areaUuid", first: 30$afterPart }) {
                    mediaConnection {
                      pageInfo { hasNextPage }
                      edges {
                        cursor
                        node {
                          mediaUrl
                          entityTags { type targetId climbName areaName }
                        }
                      }
                    }
                  }
                }
            """.trimIndent()
        }

        private fun gradesQuery(ids: List<String>): String {
            val fields = ids.mapIndexed { index, id ->
                """c$index: climb(uuid: "$id") { grades { yds vscale } }"""
            }.joinToString("\n")
            return "query {\n$fields\n}"
        }

        private fun graphqlQuoted(value: String): String {
            val escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "")
            return "\"$escaped\""
        }

        private fun String.isUuid(): Boolean = UUID_PATTERN.matches(this)
    }
}

private fun data(body: String): JSONObject {
    val root = JSONObject(body)
    val payload = root.optJSONObject("data")
    if (payload == null) throw IOException("OpenBeta returned no data")
    return payload
}

private fun text(obj: JSONObject, key: String): String {
    if (!obj.has(key) || obj.isNull(key)) return ""
    return obj.optString(key)
}
