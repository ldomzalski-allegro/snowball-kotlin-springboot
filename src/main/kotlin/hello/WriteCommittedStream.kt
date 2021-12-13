package hello

import com.google.api.core.ApiFuture
import com.google.cloud.ServiceOptions
import com.google.cloud.bigquery.storage.v1.AppendRowsResponse
import com.google.cloud.bigquery.storage.v1.BigQueryWriteClient
import com.google.cloud.bigquery.storage.v1.CreateWriteStreamRequest
import com.google.cloud.bigquery.storage.v1.JsonStreamWriter
import com.google.cloud.bigquery.storage.v1.TableName
import com.google.cloud.bigquery.storage.v1.WriteStream
import org.json.JSONArray
import org.json.JSONObject
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class WriteCommittedStream() {

    private val projectId: String = ServiceOptions.getDefaultProjectId()
    private val datasetName = "snowball"
    private val tableName = "events"

    var jsonStreamWriter: JsonStreamWriter? = null
    fun send(arena: Arena): ApiFuture<AppendRowsResponse> {
        val now = Instant.now()
        val jsonArray = JSONArray()
        arena.state.forEach { (url, playerState) ->
            val jsonObject = JSONObject()
            jsonObject.put("x", playerState.x)
            jsonObject.put("y", playerState.y)
            jsonObject.put("direction", playerState.direction)
            jsonObject.put("wasHit", playerState.wasHit)
            jsonObject.put("score", playerState.score)
            jsonObject.put("player", url)
            jsonObject.put("timestamp", now.epochSecond * 1000 * 1000)
            jsonArray.put(jsonObject)
        }
        return jsonStreamWriter!!.append(jsonArray)
    }

    init {
        BigQueryWriteClient.create().use { client ->
            val stream = WriteStream.newBuilder().setType(WriteStream.Type.COMMITTED).build()
            val parentTable = TableName.of(projectId, datasetName, tableName)
            val createWriteStreamRequest = CreateWriteStreamRequest.newBuilder()
                .setParent(parentTable.toString())
                .setWriteStream(stream)
                .build()
            val writeStream = client.createWriteStream(createWriteStreamRequest)
            jsonStreamWriter = JsonStreamWriter.newBuilder(writeStream.name, writeStream.tableSchema).build()
        }
    }
}